"""
🛡️ RATE-LIMITED GEMINI CLIENT
==============================
Wraps all Gemini API calls with automatic rate limiting.

YOUR LIMITS (Gemini 3 Flash Preview):
- 5 RPM (Requests Per Minute)
- 20 RPD (Requests Per Day)
- 250k TPM (Tokens Per Minute)

This module:
- Automatically delays between calls to respect RPM
- Tracks daily usage to warn before hitting RPD
- Provides async-safe rate limiting

Usage:
    from src.utils.rate_limited_client import get_rate_limited_client, check_quota
    
    client = get_rate_limited_client()
    if check_quota():
        response = await rate_limited_generate(client, model, contents, config)
"""

import os
import json
import time
import asyncio
from pathlib import Path
from datetime import datetime, timedelta
from typing import Any, Optional
from functools import wraps

from google import genai
from google.genai import types

# =============================================================================
# RATE LIMIT CONFIGURATION
# =============================================================================
RPM_LIMIT = 5       # Requests per minute
RPD_LIMIT = 20      # Requests per day
MIN_DELAY = 12.5    # Seconds between requests (60/5 = 12s, add buffer)

# Cache file for persistence
CACHE_FILE = Path(__file__).parent.parent.parent / ".rate_limit_cache.json"

# =============================================================================
# RATE LIMITER CLASS
# =============================================================================
class GeminiRateLimiter:
    """Thread-safe rate limiter for Gemini API calls."""
    
    _instance = None
    _lock = asyncio.Lock() if asyncio.get_event_loop().is_running() else None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        self._initialized = True
        self.last_request_time = 0
        self.load_cache()
    
    def load_cache(self):
        """Load usage data from disk."""
        if CACHE_FILE.exists():
            try:
                with open(CACHE_FILE, 'r') as f:
                    self.data = json.load(f)
            except:
                self.data = self._empty_cache()
        else:
            self.data = self._empty_cache()
        
        # Reset if new day
        today = datetime.now().strftime("%Y-%m-%d")
        if self.data.get("date") != today:
            self.data = self._empty_cache()
            self.data["date"] = today
            self.save_cache()
    
    def _empty_cache(self):
        return {
            "date": datetime.now().strftime("%Y-%m-%d"),
            "daily_count": 0,
            "minute_requests": []
        }
    
    def save_cache(self):
        """Save usage data to disk."""
        try:
            with open(CACHE_FILE, 'w') as f:
                json.dump(self.data, f, indent=2)
        except Exception as e:
            print(f"⚠️ Could not save rate limit cache: {e}")
    
    def _clean_minute_requests(self):
        """Remove requests older than 1 minute."""
        cutoff = time.time() - 60
        self.data["minute_requests"] = [
            ts for ts in self.data["minute_requests"] 
            if ts > cutoff
        ]
    
    def get_status(self) -> dict:
        """Get current rate limit status."""
        self._clean_minute_requests()
        return {
            "date": self.data["date"],
            "rpm_used": len(self.data["minute_requests"]),
            "rpm_limit": RPM_LIMIT,
            "rpm_remaining": max(0, RPM_LIMIT - len(self.data["minute_requests"])),
            "rpd_used": self.data["daily_count"],
            "rpd_limit": RPD_LIMIT,
            "rpd_remaining": max(0, RPD_LIMIT - self.data["daily_count"]),
        }
    
    def can_request(self) -> bool:
        """Check if we can make another request."""
        self._clean_minute_requests()
        return (
            len(self.data["minute_requests"]) < RPM_LIMIT and
            self.data["daily_count"] < RPD_LIMIT
        )
    
    async def wait_for_slot(self) -> bool:
        """
        Wait until we can make a request.
        Returns True if OK to proceed, False if daily limit reached.
        """
        self.load_cache()  # Refresh from disk
        
        # Check daily limit
        if self.data["daily_count"] >= RPD_LIMIT:
            print(f"⛔ DAILY LIMIT REACHED ({RPD_LIMIT} requests). Try tomorrow!")
            return False
        
        self._clean_minute_requests()
        
        # If we have room in RPM, check minimum delay
        now = time.time()
        time_since_last = now - self.last_request_time
        
        if time_since_last < MIN_DELAY:
            wait_time = MIN_DELAY - time_since_last
            print(f"⏳ Rate limiting: waiting {wait_time:.1f}s...")
            await asyncio.sleep(wait_time)
        
        # If RPM is full, wait for oldest to expire
        if len(self.data["minute_requests"]) >= RPM_LIMIT:
            oldest = min(self.data["minute_requests"])
            wait_time = 60 - (time.time() - oldest) + 1
            if wait_time > 0:
                print(f"⏳ RPM limit reached: waiting {wait_time:.1f}s...")
                await asyncio.sleep(wait_time)
        
        return True
    
    def record_request(self):
        """Record that a request was made."""
        now = time.time()
        self.last_request_time = now
        self.data["minute_requests"].append(now)
        self.data["daily_count"] += 1
        self.save_cache()
        
        status = self.get_status()
        print(f"📊 API Call [{status['rpd_used']}/{status['rpd_limit']} today]")


# =============================================================================
# GLOBAL INSTANCE
# =============================================================================
_rate_limiter: Optional[GeminiRateLimiter] = None

def get_rate_limiter() -> GeminiRateLimiter:
    """Get the global rate limiter instance."""
    global _rate_limiter
    if _rate_limiter is None:
        _rate_limiter = GeminiRateLimiter()
    return _rate_limiter


# =============================================================================
# PUBLIC API
# =============================================================================
def check_quota() -> dict:
    """Check current API quota status."""
    return get_rate_limiter().get_status()


async def rate_limited_generate(
    client: genai.Client,
    model: str,
    contents: Any,
    config: Optional[types.GenerateContentConfig] = None
) -> Any:
    """
    Rate-limited wrapper for generate_content.
    
    Automatically:
    - Waits between requests to respect RPM
    - Tracks daily usage
    - Raises exception if daily limit reached
    """
    limiter = get_rate_limiter()
    
    # Wait for available slot
    can_proceed = await limiter.wait_for_slot()
    if not can_proceed:
        raise Exception("Daily API quota exhausted (20 RPD). Try again tomorrow.")
    
    # Record the request
    limiter.record_request()
    
    # Make the actual API call
    loop = asyncio.get_event_loop()
    response = await loop.run_in_executor(
        None,
        lambda: client.models.generate_content(
            model=model,
            contents=contents,
            config=config
        )
    )
    
    return response


def print_quota_status():
    """Print current quota status to console."""
    status = check_quota()
    print(f"\n{'='*50}")
    print(f"🛡️ GEMINI API QUOTA STATUS")
    print(f"{'='*50}")
    print(f"📅 Date: {status['date']}")
    print(f"⏱️  RPM: {status['rpm_used']}/{status['rpm_limit']} (remaining: {status['rpm_remaining']})")
    print(f"📊 RPD: {status['rpd_used']}/{status['rpd_limit']} (remaining: {status['rpd_remaining']})")
    print(f"{'='*50}\n")
