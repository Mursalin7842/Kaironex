"""
🛡️ KAIRONEX RATE LIMITER
=========================
Helps you stay within Gemini API quotas without modifying original code.

YOUR LIMITS:
- 5 RPM (Requests Per Minute)
- 20 RPD (Requests Per Day)  
- 250k TPM (Tokens Per Minute)

USAGE:
    from rate_limiter import RateLimiter, run_with_limit
    
    # Check before making a call
    limiter = RateLimiter()
    if limiter.can_request():
        # make your API call
        limiter.record_request()
    
    # Or use the decorator
    @run_with_limit
    async def my_api_call():
        ...

Run this file directly to see your current usage:
    python rate_limiter.py
"""

import os
import json
import time
import asyncio
from datetime import datetime, timedelta
from pathlib import Path
from functools import wraps

# Rate limit config
RPM_LIMIT = 5      # Requests per minute
RPD_LIMIT = 20     # Requests per day
CACHE_FILE = Path(__file__).parent / ".rate_limit_cache.json"


class RateLimiter:
    """Tracks API usage to prevent quota exhaustion."""
    
    def __init__(self):
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
            "minute_requests": []  # timestamps of last minute's requests
        }
    
    def save_cache(self):
        """Save usage data to disk."""
        with open(CACHE_FILE, 'w') as f:
            json.dump(self.data, f, indent=2)
    
    def _clean_minute_requests(self):
        """Remove requests older than 1 minute."""
        cutoff = time.time() - 60
        self.data["minute_requests"] = [
            ts for ts in self.data["minute_requests"] 
            if ts > cutoff
        ]
    
    def get_rpm_count(self) -> int:
        """Get current requests in the last minute."""
        self._clean_minute_requests()
        return len(self.data["minute_requests"])
    
    def get_rpd_count(self) -> int:
        """Get current daily request count."""
        return self.data["daily_count"]
    
    def can_request(self) -> bool:
        """Check if we can make another request."""
        self._clean_minute_requests()
        
        rpm_ok = len(self.data["minute_requests"]) < RPM_LIMIT
        rpd_ok = self.data["daily_count"] < RPD_LIMIT
        
        return rpm_ok and rpd_ok
    
    def wait_time(self) -> float:
        """Get seconds to wait before next request is allowed."""
        self._clean_minute_requests()
        
        # Check daily limit first
        if self.data["daily_count"] >= RPD_LIMIT:
            # Calculate time until midnight
            now = datetime.now()
            midnight = (now + timedelta(days=1)).replace(
                hour=0, minute=0, second=0, microsecond=0
            )
            return (midnight - now).total_seconds()
        
        # Check minute limit
        if len(self.data["minute_requests"]) >= RPM_LIMIT:
            oldest = min(self.data["minute_requests"])
            return max(0, 60 - (time.time() - oldest))
        
        return 0
    
    def record_request(self):
        """Record that a request was made."""
        self.data["minute_requests"].append(time.time())
        self.data["daily_count"] += 1
        self.save_cache()
    
    def status(self) -> dict:
        """Get current rate limit status."""
        self._clean_minute_requests()
        return {
            "date": self.data["date"],
            "rpm_used": len(self.data["minute_requests"]),
            "rpm_limit": RPM_LIMIT,
            "rpm_remaining": RPM_LIMIT - len(self.data["minute_requests"]),
            "rpd_used": self.data["daily_count"],
            "rpd_limit": RPD_LIMIT,
            "rpd_remaining": RPD_LIMIT - self.data["daily_count"],
            "can_request": self.can_request(),
            "wait_seconds": self.wait_time()
        }
    
    def reset_daily(self):
        """Manually reset daily counter (use with caution)."""
        self.data = self._empty_cache()
        self.save_cache()
        print("✅ Daily counter reset!")


# Global instance
_limiter = RateLimiter()


def run_with_limit(func):
    """
    Decorator that rate-limits async functions.
    Will wait if limit reached, or skip if daily limit exhausted.
    """
    @wraps(func)
    async def wrapper(*args, **kwargs):
        limiter = RateLimiter()
        
        # Check daily limit
        if limiter.get_rpd_count() >= RPD_LIMIT:
            print(f"⛔ Daily limit reached ({RPD_LIMIT} requests). Try tomorrow!")
            return {"error": "daily_limit_reached", "rpd_used": RPD_LIMIT}
        
        # Wait for minute limit if needed
        wait = limiter.wait_time()
        if wait > 0 and wait < 60:  # Only wait if it's a minute limit
            print(f"⏳ Rate limit: waiting {wait:.1f}s...")
            await asyncio.sleep(wait + 0.5)  # Add buffer
        
        # Record and execute
        limiter.record_request()
        status = limiter.status()
        print(f"🔄 API Call [{status['rpd_used']}/{status['rpd_limit']} today] [{status['rpm_used']}/{status['rpm_limit']} this min]")
        
        return await func(*args, **kwargs)
    
    return wrapper


def check_before_call() -> bool:
    """
    Simple check to call before any API request.
    Returns True if OK to proceed, False if should skip.
    
    Usage:
        from rate_limiter import check_before_call
        
        if check_before_call():
            # make API call
            response = await engine.reason(...)
        else:
            print("Skipping due to rate limit")
    """
    limiter = RateLimiter()
    status = limiter.status()
    
    if not status["can_request"]:
        if status["rpd_remaining"] <= 0:
            print(f"⛔ DAILY LIMIT REACHED: {status['rpd_used']}/{status['rpd_limit']}")
            return False
        else:
            wait = status["wait_seconds"]
            print(f"⏳ Rate limited. Wait {wait:.0f}s or {wait/60:.1f}min")
            return False
    
    return True


def record_call():
    """Record that an API call was made."""
    limiter = RateLimiter()
    limiter.record_request()


# CLI Interface
if __name__ == "__main__":
    print("\n" + "="*50)
    print("🛡️  KAIRONEX RATE LIMITER STATUS")
    print("="*50)
    
    limiter = RateLimiter()
    status = limiter.status()
    
    print(f"\n📅 Date: {status['date']}")
    print(f"\n⏱️  REQUESTS PER MINUTE (RPM):")
    print(f"   Used:      {status['rpm_used']}/{status['rpm_limit']}")
    print(f"   Remaining: {status['rpm_remaining']}")
    
    print(f"\n📊 REQUESTS PER DAY (RPD):")
    print(f"   Used:      {status['rpd_used']}/{status['rpd_limit']}")
    print(f"   Remaining: {status['rpd_remaining']}")
    
    if status["can_request"]:
        print(f"\n✅ STATUS: Ready for requests!")
    else:
        wait = status["wait_seconds"]
        if wait > 3600:
            print(f"\n⛔ STATUS: Daily limit reached. Reset at midnight.")
        else:
            print(f"\n⏳ STATUS: Wait {wait:.0f}s ({wait/60:.1f} minutes)")
    
    print("\n" + "="*50)
    print("Commands:")
    print("  python rate_limiter.py        - Show status")
    print("  python rate_limiter.py reset  - Reset daily counter")
    print("="*50 + "\n")
    
    # Handle reset command
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == "reset":
        limiter.reset_daily()
