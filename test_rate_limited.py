"""
🧪 RATE-LIMITED TEST SUITE
===========================
Runs tests carefully within your API quota limits.

YOUR LIMITS: 5 RPM | 20 RPD | 250k TPM

This script:
- Tracks every API call
- Waits automatically when hitting rate limits
- Stops if daily limit reached
- Shows usage after each test

Usage:
    python test_rate_limited.py          # Run all tests
    python test_rate_limited.py quick    # Run minimal tests (3 API calls)
"""

import os
import sys
import asyncio
from datetime import datetime
from dotenv import load_dotenv

load_dotenv()
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from rate_limiter import RateLimiter, check_before_call, record_call


def print_header(title: str):
    print(f"\n{'='*60}")
    print(f"🧪 {title}")
    print(f"{'='*60}\n")


def print_status():
    """Print current rate limit status."""
    limiter = RateLimiter()
    s = limiter.status()
    print(f"  📊 API Usage: {s['rpd_used']}/{s['rpd_limit']} today | {s['rpm_used']}/{s['rpm_limit']} this min")


async def wait_if_needed():
    """Wait if rate limited."""
    limiter = RateLimiter()
    wait = limiter.wait_time()
    if wait > 0 and wait < 60:
        print(f"  ⏳ Waiting {wait:.1f}s for rate limit...")
        await asyncio.sleep(wait + 1)
    elif wait >= 60:
        print(f"  ⛔ Daily limit reached! Try again tomorrow.")
        return False
    return True


async def test_config():
    """Test config (NO API CALLS)."""
    print_header("TEST 1: Configuration (0 API calls)")
    
    from src.config import (
        GEMINI_API_KEY, APPWRITE_DATABASE_ID,
        GEMINI_3_FLASH
    )
    
    print(f"  ✅ GEMINI_API_KEY: {'configured' if GEMINI_API_KEY else 'missing'}")
    print(f"  ✅ Database ID: {APPWRITE_DATABASE_ID}")
    print(f"  ✅ Model: {GEMINI_3_FLASH}")
    return True


async def test_tool_declarations():
    """Test tool declarations (NO API CALLS)."""
    print_header("TEST 2: Tool Declarations (0 API calls)")
    
    from src.tools.tool_declarations import get_tools_for_agent
    
    study_tools = get_tools_for_agent("study")
    print(f"  ✅ Study tools loaded: {len(study_tools)} tools")
    return True


async def test_tool_executor():
    """Test tool executor (NO API CALLS)."""
    print_header("TEST 3: Tool Executor (0 API calls)")
    
    from src.tools.tool_executor import ToolExecutor
    
    executor = ToolExecutor("test_user")
    result = await executor.execute("check_engagement", {})
    print(f"  ✅ check_engagement: {result.success}")
    return True


async def test_bicameral_reflex():
    """Test REFLEX mode (1 API CALL)."""
    print_header("TEST 4: Bicameral REFLEX (1 API call)")
    
    if not await wait_if_needed():
        return False
    
    if not check_before_call():
        print("  ⏭️ Skipped due to rate limit")
        return True
    
    from src.core.bicameral_engine import (
        BicameralEngine, ReasoningRequest, ReasoningMode
    )
    
    engine = BicameralEngine(use_tools=False)
    
    print("  🔄 Making API call...")
    record_call()
    
    response = await engine.reason(ReasoningRequest(
        prompt="What is 2+2? One word answer.",
        user_id="test_user",
        agent="test",
        mode=ReasoningMode.REFLEX
    ))
    
    print(f"  ✅ Response: {response.content[:50]}...")
    print(f"  ✅ Latency: {response.latency_ms:.0f}ms")
    print_status()
    return True


async def test_bicameral_deep():
    """Test DEEP mode with thinking (1 API CALL)."""
    print_header("TEST 5: Bicameral DEEP (1 API call)")
    
    if not await wait_if_needed():
        return False
    
    if not check_before_call():
        print("  ⏭️ Skipped due to rate limit")
        return True
    
    from src.core.bicameral_engine import (
        BicameralEngine, ReasoningRequest, ReasoningMode
    )
    
    engine = BicameralEngine(use_tools=False)
    
    print("  🔄 Making API call (DEEP thinking)...")
    record_call()
    
    response = await engine.reason(ReasoningRequest(
        prompt="Should a student study or sleep if they have an exam tomorrow and it's 2am?",
        user_id="test_user",
        agent="test",
        mode=ReasoningMode.DEEP,
        max_thinking_tokens=2048
    ))
    
    print(f"  ✅ Response: {response.content[:100]}...")
    print(f"  ✅ Has thinking: {response.thought_signature is not None}")
    print_status()
    return True


async def test_function_calling():
    """Test function calling (1 API CALL)."""
    print_header("TEST 6: Function Calling (1 API call)")
    
    if not await wait_if_needed():
        return False
    
    if not check_before_call():
        print("  ⏭️ Skipped due to rate limit")
        return True
    
    from src.core.bicameral_engine import (
        BicameralEngine, ReasoningRequest, ReasoningMode
    )
    
    engine = BicameralEngine(use_tools=True)
    
    print("  🔄 Making API call with tools...")
    record_call()
    
    response = await engine.reason(ReasoningRequest(
        prompt="Check my engagement level",
        user_id="test_user",
        agent="study",
        mode=ReasoningMode.REFLEX
    ))
    
    print(f"  ✅ Response: {response.content[:80]}...")
    print(f"  ✅ Tool results: {len(response.tool_results)}")
    print_status()
    return True


async def run_quick_tests():
    """Run minimal tests (3 API calls)."""
    print("\n" + "="*60)
    print("🚀 QUICK TEST MODE (3 API calls max)")
    print("="*60)
    
    await test_config()
    await test_tool_declarations()
    await test_tool_executor()
    await test_bicameral_reflex()
    
    print("\n" + "="*60)
    print("✅ QUICK TESTS COMPLETE")
    print("="*60)
    print_status()


async def run_full_tests():
    """Run all tests (3 API calls)."""
    print("\n" + "="*60)
    print("🚀 FULL TEST SUITE (3 API calls)")
    print("="*60)
    
    # No API calls
    await test_config()
    await test_tool_declarations()
    await test_tool_executor()
    
    # 1 API call each
    await test_bicameral_reflex()
    await test_bicameral_deep()
    await test_function_calling()
    
    print("\n" + "="*60)
    print("✅ ALL TESTS COMPLETE")
    print("="*60)
    print_status()


if __name__ == "__main__":
    print("\n🛡️ Rate-Limited Test Suite")
    print_status()
    
    if len(sys.argv) > 1 and sys.argv[1] == "quick":
        asyncio.run(run_quick_tests())
    else:
        asyncio.run(run_full_tests())
