"""
🧬 TEST: VITALITY BRAIN v2.0 - Life Logistics Engine
=====================================================
Comprehensive tests for the financial survival & wellness agent.

Tests all key handlers:
1.  one_shot_setup - Financial calibration
2.  fridge_scan - No image error
3.  daily_budget_check - Morning ritual
4.  defcon_update - DEFCON level change
5.  cross_agent_sync - Multi-agent sync
6.  proactive_meal_plan - Meal generation
7.  select_meal_option - Meal selection errors
8.  get_todays_meals - Meal status
9.  marathon_morning_routine - Full morning
10. emergency_fund_withdraw - Invalid + success
11. emergency_fund_deposit - Savings deposit
12. savings_status - Fund overview
13. auto_end_of_day - EOD summary
14. sleep_log / activity_log / meal_log / energy_check / regen_request / resource_update

User: demo_user_001
"""

import sys
import os
import json
import asyncio
from datetime import datetime
from unittest.mock import patch

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from test_mocks import MockDBHelper, MockContext, MockGeminiClient

USER_ID = "demo_user_001"
PASS_COUNT = 0
FAIL_COUNT = 0


def check(test_name, result, condition=True):
    global PASS_COUNT, FAIL_COUNT
    if condition:
        PASS_COUNT += 1
        print(f"  ✅ {test_name}")
    else:
        FAIL_COUNT += 1
        print(f"  ❌ {test_name} — got: {repr(result)[:200]}")


def run_handler(event_type, extra_payload=None, db=None):
    """Run a vitality brain handler with mocked dependencies."""
    if db is None:
        db = MockDBHelper()
    ctx = MockContext()

    payload = {"userId": USER_ID, "type": event_type}
    if extra_payload:
        payload.update(extra_payload)

    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        result = asyncio.run(run_vitality_agent(db, payload, ctx))

    return result, db, ctx


# =============================================================================
# TESTS
# =============================================================================

def test_one_shot_setup():
    """Test Day Zero financial calibration."""
    print("\n💰 TEST: one_shot_setup")

    result, db, ctx = run_handler("one_shot_setup", {
        "financial_info": "I have $2000 in my bank. Rent is $800/month. I get paid in 15 days. I am a student.",
        "favorite_food": "Pizza",
        "dietary_restrictions": ["halal"]
    })

    data = result["data"]
    check("status is setup_complete", data, data.get("status") == "setup_complete")
    check("defcon_level exists", data, data.get("defcon_level") is not None)
    check("daily_runway exists", data, data.get("daily_runway") is not None)
    check("survival_state exists", data, data.get("survival_state") is not None)
    check("message exists", data, len(data.get("message", "")) > 3)
    check("thought_id exists", data, data.get("thought_id") is not None)
    check("actions has financial_setup", data, "financial_setup" in data.get("actions", []))


def test_fridge_scan_no_image():
    """Test fridge scan without image returns error."""
    print("\n🍽️ TEST: fridge_scan (no image)")

    result, db, ctx = run_handler("fridge_scan")
    data = result["data"]
    check("status is error", data, data.get("status") == "error")


def test_daily_budget_check():
    """Test morning budget ritual."""
    print("\n📊 TEST: daily_budget_check")

    # Setup user with financial state first
    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "I have $1500. Rent $600. Paid in 20 days. Student."
        }, MockContext()))
        # Now check budget
        result = asyncio.run(run_vitality_agent(db, {"userId": USER_ID, "type": "daily_budget_check"}, MockContext()))

    data = result["data"]
    check("status is morning_ritual_complete", data, data.get("status") == "morning_ritual_complete")
    check("today_budget exists", data, data.get("today_budget") is not None)
    check("defcon_level exists", data, data.get("defcon_level") is not None)
    check("thought_id exists", data, data.get("thought_id") is not None)


def test_defcon_update():
    """Test DEFCON level update."""
    print("\n🚨 TEST: defcon_update")

    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup first
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "Balance $500. Bills $300. Payday in 10 days."
        }, MockContext()))
        # Update defcon
        result = asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "defcon_update",
            "new_balance": 200, "new_bills": 100
        }, MockContext()))

    data = result["data"]
    check("status is defcon_updated", data, data.get("status") == "defcon_updated")
    check("defcon_level exists", data, data.get("defcon_level") is not None)


def test_cross_agent_sync():
    """Test cross-agent sync."""
    print("\n🔄 TEST: cross_agent_sync")

    result, db, ctx = run_handler("cross_agent_sync")
    data = result["data"]
    check("status is synced", data, data.get("status") == "synced")
    check("synced_agents is list", data, isinstance(data.get("synced_agents"), list))


def test_proactive_meal_plan():
    """Test proactive meal plan generation."""
    print("\n🍽️ TEST: proactive_meal_plan")

    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup financial state
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "Balance $2000. Bills $800. Payday in 15 days."
        }, MockContext()))
        # Generate meal plan
        result = asyncio.run(run_vitality_agent(db, {"userId": USER_ID, "type": "proactive_meal_plan"}, MockContext()))

    data = result["data"]
    check("status is plan_generated or existing", data, 
          data.get("status") in ["plan_generated", "existing_plan"])
    check("has some data", data, len(data) > 2)


def test_select_meal_option_no_id():
    """Test meal selection without option_id."""
    print("\n🍽️ TEST: select_meal_option (no id)")

    result, db, ctx = run_handler("select_meal_option")
    data = result["data"]
    check("status is error", data, data.get("status") == "error")


def test_get_todays_meals_no_plan():
    """Test get today's meals when no plan exists."""
    print("\n🍽️ TEST: get_todays_meals (no plan)")

    result, db, ctx = run_handler("get_todays_meals")
    data = result["data"]
    check("status is no_plan", data, data.get("status") == "no_plan")


def test_marathon_morning_routine():
    """Test full marathon morning routine."""
    print("\n🌅 TEST: marathon_morning_routine")

    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "Balance $1800. Rent $700. Payday in 12 days."
        }, MockContext()))
        # Run morning
        result = asyncio.run(run_vitality_agent(db, {"userId": USER_ID, "type": "marathon_morning_routine"}, MockContext()))

    data = result["data"]
    check("status is marathon_complete", data, data.get("status") == "marathon_complete")
    check("briefing exists", data, len(data.get("briefing", "")) > 5)
    check("components has entries", data, len(data.get("components", [])) >= 1)
    check("thought_id exists", data, data.get("thought_id") is not None)


def test_emergency_fund_withdraw_invalid():
    """Test emergency fund withdrawal with invalid amount."""
    print("\n💸 TEST: emergency_fund_withdraw (invalid)")

    result, db, ctx = run_handler("emergency_fund_withdraw", {
        "amount": -10
    })
    data = result["data"]
    check("status is error", data, data.get("status") == "error")


def test_emergency_fund_deposit():
    """Test emergency fund deposit."""
    print("\n💰 TEST: emergency_fund_deposit")

    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "Balance $2000. Bills $500. Payday in 20 days."
        }, MockContext()))
        # Deposit
        result = asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "emergency_fund_deposit",
            "amount": 50, "fund_type": "emergency"
        }, MockContext()))

    data = result["data"]
    check("status is deposited", data, data.get("status") == "deposited")
    check("amount is 50", data, data.get("amount") == 50)


def test_savings_status():
    """Test savings status check."""
    print("\n📊 TEST: savings_status")

    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "Balance $1000. Bills $400. Payday in 15 days."
        }, MockContext()))
        # Check savings
        result = asyncio.run(run_vitality_agent(db, {"userId": USER_ID, "type": "savings_status"}, MockContext()))

    data = result["data"]
    check("status is ok", data, data.get("status") == "ok")
    check("emergency_fund exists", data, data.get("emergency_fund") is not None)


def test_auto_end_of_day():
    """Test end-of-day summary."""
    print("\n🌙 TEST: auto_end_of_day")

    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        # Setup
        asyncio.run(run_vitality_agent(db, {
            "userId": USER_ID, "type": "one_shot_setup",
            "financial_info": "Balance $1000. Bills $400. Payday in 15 days."
        }, MockContext()))
        # End of day
        result = asyncio.run(run_vitality_agent(db, {"userId": USER_ID, "type": "auto_end_of_day"}, MockContext()))

    data = result["data"]
    check("status is day_complete", data, data.get("status") == "day_complete")
    check("thought_id exists", data, data.get("thought_id") is not None)


def test_sleep_log():
    """Test sleep logging."""
    print("\n😴 TEST: sleep_log")

    result, db, ctx = run_handler("sleep_log", {
        "sleep_hours": 7, "sleep_quality": "good"
    })
    data = result["data"]
    check("status is sleep_logged", data, data.get("status") == "sleep_logged")


def test_activity_log():
    """Test activity logging."""
    print("\n🏃 TEST: activity_log")

    result, db, ctx = run_handler("activity_log", {
        "activity": "running", "duration": 30
    })
    data = result["data"]
    check("status is activity_logged", data, data.get("status") == "activity_logged")


def test_meal_log():
    """Test meal logging."""
    print("\n🍔 TEST: meal_log")

    result, db, ctx = run_handler("meal_log", {
        "meal_type": "lunch", "cost": 8.50, "description": "Chicken rice bowl"
    })
    data = result["data"]
    check("status is meal_logged", data, data.get("status") == "meal_logged")


def test_energy_check():
    """Test energy check."""
    print("\n⚡ TEST: energy_check")

    result, db, ctx = run_handler("energy_check")
    data = result["data"]
    check("status is energy_checked", data, data.get("status") == "energy_checked")
    check("energy exists", data, data.get("energy") is not None)


def test_regen_request():
    """Test regeneration mode."""
    print("\n🔋 TEST: regen_request")

    result, db, ctx = run_handler("regen_request", {"duration": 20})
    data = result["data"]
    check("status is regen_activated", data, data.get("status") == "regen_activated")


def test_resource_update():
    """Test resource logging."""
    print("\n📦 TEST: resource_update")

    result, db, ctx = run_handler("resource_update", {
        "resource_type": "water", "amount": 500
    })
    data = result["data"]
    check("status is resource_logged", data, data.get("status") == "resource_logged")


def test_no_user_id():
    """Test error when no userId provided."""
    print("\n❌ TEST: no userId")

    ctx = MockContext()
    db = MockDBHelper()
    with patch("src.agents.vitality_brain_v2.GeminiClient", MockGeminiClient):
        from src.agents.vitality_brain_v2 import run_vitality_agent
        result = asyncio.run(run_vitality_agent(db, {}, ctx))

    data = result["data"]
    check("returns error", data, "error" in data)


# =============================================================================
# RUNNER
# =============================================================================

def run_all_tests():
    global PASS_COUNT, FAIL_COUNT
    PASS_COUNT = 0
    FAIL_COUNT = 0

    print("=" * 60)
    print("🧬 VITALITY BRAIN v2.0 - LIFE LOGISTICS TEST SUITE")
    print(f"   User: {USER_ID}")
    print(f"   Timestamp: {datetime.now().isoformat()}")
    print("=" * 60)

    tests = [
        test_one_shot_setup,
        test_fridge_scan_no_image,
        test_daily_budget_check,
        test_defcon_update,
        test_cross_agent_sync,
        test_proactive_meal_plan,
        test_select_meal_option_no_id,
        test_get_todays_meals_no_plan,
        test_marathon_morning_routine,
        test_emergency_fund_withdraw_invalid,
        test_emergency_fund_deposit,
        test_savings_status,
        test_auto_end_of_day,
        test_sleep_log,
        test_activity_log,
        test_meal_log,
        test_energy_check,
        test_regen_request,
        test_resource_update,
        test_no_user_id,
    ]

    for test_fn in tests:
        try:
            test_fn()
        except Exception as e:
            FAIL_COUNT += 1
            print(f"  ❌ {test_fn.__name__} CRASHED: {e}")
            import traceback
            traceback.print_exc()

    print("\n" + "=" * 60)
    print(f"🧬 VITALITY BRAIN RESULTS: {PASS_COUNT} passed, {FAIL_COUNT} failed")
    print("=" * 60)

    if FAIL_COUNT == 0:
        print("🎉 ALL TESTS PASSED!")
    else:
        print(f"⚠️ {FAIL_COUNT} test(s) failed")

    return FAIL_COUNT == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
