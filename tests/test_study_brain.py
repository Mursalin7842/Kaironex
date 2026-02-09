"""
🎓 TEST: STUDY BRAIN - Cognitive Supply Chain
==============================================
Tests for the study session management agent.

Tests all branches:
1. IN_PROGRESS + low focus - Focus intervention
2. IN_PROGRESS + normal focus - No intervention
3. REQUEST_UNLOCK - Quiz generation
4. resource_ingestion - Resource analysis

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
    global PASS_COUNT, FAIL_COUNT
    if condition:
        PASS_COUNT += 1
        print(f"  ✅ {test_name}")
    else:
        FAIL_COUNT += 1
        print(f"  ❌ {test_name} — got: {repr(result)[:200]}")


def run_handler(extra_payload=None):
    """Run the study brain with mocked dependencies."""
    db = MockDBHelper()
    ctx = MockContext()

    payload = {"userId": USER_ID}
    if extra_payload:
        payload.update(extra_payload)

    with patch("src.agents.study_brain.GeminiClient", MockGeminiClient):
        from src.agents.study_brain import run_study_agent
        result = run_study_agent(db, payload, ctx)

    return result, db, ctx


# =============================================================================
# TESTS
# =============================================================================

def test_focus_drop_intervention():
    """Test focus drop generates intervention."""
    print("\n⚠️ TEST: IN_PROGRESS + focus < 40")

    result, db, ctx = run_handler({
        "status": "IN_PROGRESS",
        "focus_score": 25,
        "topic": "Data Structures"
    })

    data = result["data"]
    check("status is study_processed", data, data.get("status") == "study_processed")
    check("intervention_sent in actions", data, "intervention_sent" in data.get("actions", []))
    check("DB has intervention", db, len(db._interventions) >= 1)
    check("intervention trigger is FOCUS_DROP", db,
          any(i.get("trigger") == "FOCUS_DROP" for i in db._interventions))
    check("debug user_id is correct", data, data.get("debug", {}).get("user_id") == USER_ID)


def test_normal_focus_no_intervention():
    """Test normal focus does NOT trigger intervention."""
    print("\n✅ TEST: IN_PROGRESS + focus >= 40")

    result, db, ctx = run_handler({
        "status": "IN_PROGRESS",
        "focus_score": 80,
        "topic": "Algorithms"
    })

    data = result["data"]
    check("status is study_processed", data, data.get("status") == "study_processed")
    check("no intervention_sent", data, "intervention_sent" not in data.get("actions", []))
    check("DB has no intervention", db, len(db._interventions) == 0)


def test_quiz_generation():
    """Test knowledge gatekeeper quiz generation."""
    print("\n🔐 TEST: REQUEST_UNLOCK (quiz)")

    result, db, ctx = run_handler({
        "status": "REQUEST_UNLOCK",
        "topic": "Operating Systems"
    })

    data = result["data"]
    check("status is study_processed", data, data.get("status") == "study_processed")
    check("quiz_generated in actions", data, "quiz_generated" in data.get("actions", []))
    check("quiz data exists", data, data.get("quiz") is not None)
    check("quiz has question", data, 
          data.get("quiz", {}).get("question") is not None)


def test_resource_ingestion():
    """Test resource upload analysis."""
    print("\n📄 TEST: resource_ingestion")

    result, db, ctx = run_handler({
        "type": "resource_ingestion",
        "status": "resource_ingestion",
        "title": "Machine Learning Notes",
        "resourceType": "document",
        "driveLink": "https://drive.google.com/file/abc123"
    })

    data = result["data"]
    check("status is study_processed", data, data.get("status") == "study_processed")
    check("resource_analyzed in actions", data, "resource_analyzed" in data.get("actions", []))
    check("intervention_created in actions", data, "intervention_created" in data.get("actions", []))
    check("resource_response exists", data, data.get("resource_response") is not None)
    check("DB has intervention", db, len(db._interventions) >= 1)
    check("intervention trigger is RESOURCE_UPLOADED", db,
          any(i.get("trigger") == "RESOURCE_UPLOADED" for i in db._interventions))


def test_resource_ingestion_via_type():
    """Test resource_ingestion detected via type field."""
    print("\n📄 TEST: resource_ingestion (via type)")

    result, db, ctx = run_handler({
        "type": "resource_ingestion",
        "title": "Physics Lecture Slides"
    })

    data = result["data"]
    check("resource_analyzed in actions", data, "resource_analyzed" in data.get("actions", []))


def test_no_user_id():
    """Test error when no userId."""
    print("\n❌ TEST: no userId")

    ctx = MockContext()
    db = MockDBHelper()
    with patch("src.agents.study_brain.GeminiClient", MockGeminiClient):
        from src.agents.study_brain import run_study_agent
        result = run_study_agent(db, {}, ctx)

    data = result["data"]
    check("returns error", data, "error" in data)


def test_heartbeat_logged():
    """Test that heartbeat is always logged."""
    print("\n💓 TEST: heartbeat logged")

    result, db, ctx = run_handler({
        "status": "IN_PROGRESS",
        "focus_score": 80,
        "topic": "Calculus"
    })

    check("heartbeat logged", db, len(db._heartbeats) >= 1)


def test_state_cache_updated():
    """Test that state cache is always updated."""
    print("\n🔄 TEST: state cache updated")

    result, db, ctx = run_handler({
        "status": "IN_PROGRESS",
        "focus_score": 75,
        "topic": "Linear Algebra"
    })

    check("state cache has entry", db, USER_ID in db._state_cache)
    check("study_session in cache", db,
          "study_session" in db._state_cache.get(USER_ID, {}))


def test_debug_info():
    """Test debug info is always present."""
    print("\n🐛 TEST: debug info")

    result, db, ctx = run_handler({
        "status": "IN_PROGRESS",
        "focus_score": 50,
        "topic": "Chemistry"
    })

    data = result["data"]
    debug = data.get("debug", {})
    check("debug has user_id", debug, debug.get("user_id") == USER_ID)
    check("debug has status_resolved", debug, debug.get("status_resolved") == "IN_PROGRESS")
    check("debug has actions_count", debug, debug.get("actions_count") is not None)


# =============================================================================
# RUNNER
# =============================================================================

def run_all_tests():
    global PASS_COUNT, FAIL_COUNT
    PASS_COUNT = 0
    FAIL_COUNT = 0

    print("=" * 60)
    print("🎓 STUDY BRAIN - COGNITIVE SUPPLY CHAIN TEST SUITE")
    print(f"   User: {USER_ID}")
    print(f"   Timestamp: {datetime.now().isoformat()}")
    print("=" * 60)

    tests = [
        test_focus_drop_intervention,
        test_normal_focus_no_intervention,
        test_quiz_generation,
        test_resource_ingestion,
        test_resource_ingestion_via_type,
        test_no_user_id,
        test_heartbeat_logged,
        test_state_cache_updated,
        test_debug_info,
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
    print(f"🎓 STUDY BRAIN RESULTS: {PASS_COUNT} passed, {FAIL_COUNT} failed")
    print("=" * 60)

    if FAIL_COUNT == 0:
        print("🎉 ALL TESTS PASSED!")
    else:
        print(f"⚠️ {FAIL_COUNT} test(s) failed")

    return FAIL_COUNT == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
