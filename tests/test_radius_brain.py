"""
🧪 TEST: RADIUS BRAIN v2.0 - Cultural Survival Engine
======================================================
Comprehensive tests for the international student cultural adaptation agent.

Tests all 12 handlers:
1. cultural_setup - Profile creation
2. daily_lesson - Micro-lesson generation
3. complete_lesson - Lesson completion
4. live_agent_prompt - Live teaching agent prompt
5. cultural_scenario - Cultural practice scenario
6. location_change - Location update
7. local_scan - Area safety analysis
8. safehouse_add - Add safe location
9. safehouse_check - Check safehouse status
10. emergency_cultural - Emergency cultural help
11. marathon_cultural_morning - Autonomous morning routine
12. mode_request - Mode change

User: demo_user_001
"""

import sys
import os
import json
from datetime import datetime
from unittest.mock import patch

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from test_mocks import MockDBHelper, MockContext, MockGeminiClient

USER_ID = "demo_user_001"
PASS_COUNT = 0
FAIL_COUNT = 0


def check(test_name, result, condition=True):
    """Assert helper with nice output."""
    global PASS_COUNT, FAIL_COUNT
    if condition:
        PASS_COUNT += 1
        print(f"  ✅ {test_name}")
    else:
        FAIL_COUNT += 1
        print(f"  ❌ {test_name} — got: {result}")


def run_handler(event_type, extra_payload=None):
    """Run a radius brain handler with mocked dependencies."""
    db = MockDBHelper()
    ctx = MockContext()

    payload = {"userId": USER_ID, "type": event_type}
    if extra_payload:
        payload.update(extra_payload)

    with patch("src.agents.radius_brain.GeminiClient", MockGeminiClient):
        from src.agents.radius_brain import run_radius_agent
        result = run_radius_agent(db, payload, ctx)

    return result, db, ctx


# =============================================================================
# TESTS
# =============================================================================

def test_cultural_setup():
    """Test international student profile creation."""
    print("\n🌍 TEST: cultural_setup")

    result, db, ctx = run_handler("cultural_setup", {
        "home_country": "Bangladesh",
        "home_language": "Bengali",
        "current_country": "United States",
        "current_city": "Austin, Texas",
        "target_language": "English",
        "language_level": "intermediate",
        "days_in_country": 14
    })

    data = result["data"]
    check("status is setup_complete", data, data.get("status") == "setup_complete")
    check("profile has home_country", data, data.get("profile", {}).get("home_country") == "Bangladesh")
    check("profile has target_language", data, data.get("profile", {}).get("target_language") == "English")
    check("message exists", data, len(data.get("message", "")) > 5)
    check("thought_id created", data, data.get("thought_id") is not None)
    check("actions list", data, "cultural_profile_created" in data.get("actions", []))
    check("DB has thought sig", db, len(db._thought_signatures) >= 1)


def test_daily_lesson():
    """Test daily cultural micro-lesson generation."""
    print("\n📚 TEST: daily_lesson")

    result, db, ctx = run_handler("daily_lesson")
    data = result["data"]

    check("status is lesson_generated", data, data.get("status") == "lesson_generated")
    check("lesson has title", data, len(data.get("lesson", {}).get("title", "")) > 0)
    check("lesson has content", data, len(data.get("lesson", {}).get("content", "")) > 0)
    check("streak >= 1", data, data.get("streak", 0) >= 1)
    check("thought_id exists", data, data.get("thought_id") is not None)
    check("actions list", data, "lesson_generated" in data.get("actions", []))


def test_daily_lesson_idempotent():
    """Test that calling daily_lesson twice returns existing lesson."""
    print("\n📚 TEST: daily_lesson (idempotent)")

    db = MockDBHelper()
    ctx = MockContext()
    payload = {"userId": USER_ID, "type": "daily_lesson"}

    with patch("src.agents.radius_brain.GeminiClient", MockGeminiClient):
        from src.agents.radius_brain import run_radius_agent
        result1 = run_radius_agent(db, payload, ctx)

        # Second call should return existing (same DB)
        ctx2 = MockContext()
        result2 = run_radius_agent(db, payload, ctx2)

    data2 = result2["data"]
    check("second call returns existing_lesson", data2, data2.get("status") == "existing_lesson")


def test_complete_lesson():
    """Test lesson completion flow."""
    print("\n✅ TEST: complete_lesson")

    db = MockDBHelper()
    ctx = MockContext()

    with patch("src.agents.radius_brain.GeminiClient", MockGeminiClient):
        from src.agents.radius_brain import run_radius_agent
        # First generate a lesson
        run_radius_agent(db, {"userId": USER_ID, "type": "daily_lesson"}, ctx)

        # Now complete it (same DB)
        ctx2 = MockContext()
        result = run_radius_agent(db, {"userId": USER_ID, "type": "complete_lesson"}, ctx2)

    data = result["data"]
    check("status is completed", data, data.get("status") == "completed")
    check("streak exists", data, data.get("streak", 0) >= 1)
    check("cultural_wins >= 1", data, data.get("cultural_wins", 0) >= 1)
    check("actions has lesson_completed", data, "lesson_completed" in data.get("actions", []))


def test_complete_lesson_no_lesson():
    """Test completing when no lesson exists."""
    print("\n✅ TEST: complete_lesson (no lesson)")

    result, db, ctx = run_handler("complete_lesson")
    data = result["data"]
    check("status is no_lesson", data, data.get("status") == "no_lesson")


def test_live_agent_prompt():
    """Test live teaching agent prompt generation."""
    print("\n🎙️ TEST: live_agent_prompt")

    result, db, ctx = run_handler("live_agent_prompt", {
        "topic": "ordering_food",
        "scenario": "At a pizza restaurant"
    })

    data = result["data"]
    check("status is prompt_generated", data, data.get("status") == "prompt_generated")
    check("topic matches", data, data.get("topic") == "ordering_food")
    check("live_agent_prompt data exists", data, data.get("live_agent_prompt") is not None)
    check("thought_id exists", data, data.get("thought_id") is not None)


def test_cultural_scenario():
    """Test cultural practice scenario generation."""
    print("\n🎭 TEST: cultural_scenario")

    result, db, ctx = run_handler("cultural_scenario", {
        "scenario_type": "restaurant"
    })

    data = result["data"]
    check("status is scenario_generated", data, data.get("status") == "scenario_generated")
    check("scenario data exists", data, data.get("scenario") is not None)
    check("thought_id exists", data, data.get("thought_id") is not None)
    check("actions has scenario_generated", data, "scenario_generated" in data.get("actions", []))


def test_location_change():
    """Test location change with cultural context."""
    print("\n📍 TEST: location_change")

    result, db, ctx = run_handler("location_change", {
        "location": "University Library",
        "trigger_voice": False
    })

    data = result["data"]
    check("status is location_updated", data, data.get("status") == "location_updated")
    check("location set", data, data.get("location") == "University Library")
    check("mode is DEEP_FOCUS", data, data.get("mode") == "DEEP_FOCUS")
    check("actions has location_changed", data, "location_changed" in data.get("actions", []))


def test_location_change_campus():
    """Test campus location detection."""
    print("\n📍 TEST: location_change (campus)")

    result, db, ctx = run_handler("location_change", {
        "location": "CS101 Lecture Hall on Campus"
    })

    data = result["data"]
    check("mode is CAMPUS", data, data.get("mode") == "CAMPUS")


def test_location_change_home():
    """Test home location detection."""
    print("\n📍 TEST: location_change (home)")

    result, db, ctx = run_handler("location_change", {
        "location": "My Apartment"
    })

    data = result["data"]
    check("mode is REST", data, data.get("mode") == "REST")


def test_local_scan():
    """Test local area safety scan."""
    print("\n🔍 TEST: local_scan")

    result, db, ctx = run_handler("local_scan", {
        "location": "Downtown Austin",
        "focus": "safety"
    })

    data = result["data"]
    check("status is scan_complete", data, data.get("status") == "scan_complete")
    check("scan_result exists", data, data.get("scan_result") is not None)
    check("thought_id exists", data, data.get("thought_id") is not None)


def test_safehouse_add():
    """Test adding a safehouse."""
    print("\n🏠 TEST: safehouse_add")

    result, db, ctx = run_handler("safehouse_add", {
        "name": "University Library",
        "address": "123 Campus Drive",
        "location_type": "library",
        "emergency_contacts": ["+1-555-0100"]
    })

    data = result["data"]
    check("status is safehouse_added", data, data.get("status") == "safehouse_added")
    check("safehouse has name", data, data.get("safehouse", {}).get("name") == "University Library")
    check("total_safehouses is 1", data, data.get("total_safehouses") == 1)


def test_safehouse_check():
    """Test safehouse status check."""
    print("\n🏠 TEST: safehouse_check")

    result, db, ctx = run_handler("safehouse_check")
    data = result["data"]
    check("status is ok", data, data.get("status") == "ok")
    check("safehouses is list", data, isinstance(data.get("safehouses"), list))


def test_emergency_cultural():
    """Test emergency cultural assistance."""
    print("\n🆘 TEST: emergency_cultural")

    result, db, ctx = run_handler("emergency_cultural", {
        "situation": "Someone is talking to me fast and I cannot understand",
        "urgency": "high"
    })

    data = result["data"]
    check("status is help_provided", data, data.get("status") == "help_provided")
    check("response exists", data, len(data.get("response", "")) > 5)
    check("urgency is high", data, data.get("urgency") == "high")
    check("thought_id exists", data, data.get("thought_id") is not None)
    check("DB has intervention", db, len(db._interventions) >= 1)


def test_marathon_cultural_morning():
    """Test autonomous morning routine."""
    print("\n🌅 TEST: marathon_cultural_morning")

    result, db, ctx = run_handler("marathon_cultural_morning")
    data = result["data"]

    check("status is marathon_complete", data, data.get("status") == "marathon_complete")
    check("lesson generated", data, data.get("lesson") is not None)
    check("live_agent_prompt exists", data, data.get("live_agent_prompt") is not None)
    check("briefing exists", data, len(data.get("briefing", "")) > 5)
    check("components has entries", data, len(data.get("components", [])) >= 2)
    check("thought_id exists", data, data.get("thought_id") is not None)
    check("DB has intervention", db, len(db._interventions) >= 1)


def test_mode_request():
    """Test manual mode change."""
    print("\n🎚️ TEST: mode_request")

    result, db, ctx = run_handler("mode_request", {
        "mode": "DEEP_FOCUS",
        "duration_minutes": 90
    })

    data = result["data"]
    check("status is mode_activated", data, data.get("status") == "mode_activated")
    check("mode is DEEP_FOCUS", data, data.get("mode") == "DEEP_FOCUS")
    check("duration set", data, data.get("duration") == 90)


def test_default_handler():
    """Test unknown event falls back to location_change."""
    print("\n🔀 TEST: unknown event type (fallback)")

    result, db, ctx = run_handler("some_unknown_event", {"user_location": "Park"})
    data = result["data"]
    check("status is location_updated", data, data.get("status") == "location_updated")


# =============================================================================
# RUNNER
# =============================================================================

def run_all_tests():
    global PASS_COUNT, FAIL_COUNT
    PASS_COUNT = 0
    FAIL_COUNT = 0

    print("=" * 60)
    print("🌍 RADIUS BRAIN v2.0 - CULTURAL SURVIVAL TEST SUITE")
    print(f"   User: {USER_ID}")
    print(f"   Timestamp: {datetime.now().isoformat()}")
    print("=" * 60)

    tests = [
        test_cultural_setup,
        test_daily_lesson,
        test_daily_lesson_idempotent,
        test_complete_lesson,
        test_complete_lesson_no_lesson,
        test_live_agent_prompt,
        test_cultural_scenario,
        test_location_change,
        test_location_change_campus,
        test_location_change_home,
        test_local_scan,
        test_safehouse_add,
        test_safehouse_check,
        test_emergency_cultural,
        test_marathon_cultural_morning,
        test_mode_request,
        test_default_handler,
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
    print(f"🌍 RADIUS BRAIN RESULTS: {PASS_COUNT} passed, {FAIL_COUNT} failed")
    print("=" * 60)

    if FAIL_COUNT == 0:
        print("🎉 ALL TESTS PASSED!")
    else:
        print(f"⚠️ {FAIL_COUNT} test(s) failed")

    return FAIL_COUNT == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
