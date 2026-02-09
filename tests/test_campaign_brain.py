"""
⚔️ TEST: CAMPAIGN BRAIN - Career Strategist
=============================================
Tests for the resume/interview/skill-tree career agent.

Tests key event types:
1. analyze_resume - ATS analysis
2. analyze_resume - Missing resume error
3. generate_resume - Resume creation
4. generate_resume - Missing JD error
5. design_interview - Mock interview designer
6. design_interview - Missing JD error
7. generate_skill_tree - Skill tree creation
8. new_goal - Legacy goal setting
9. unknown event - Fallback
10. no userId - Error

User: demo_user_001
"""

import sys
import os
import json
import asyncio
from datetime import datetime
from unittest.mock import patch, MagicMock

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from test_mocks import (
    MockDBHelper, MockContext, MockGeminiClient,
    MockBicameralEngine, MockReasoningResult
)

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


def run_handler(event_type, data=None, extra_payload=None):
    """Run campaign brain handler with mocked dependencies."""
    db = MockDBHelper()
    ctx = MockContext()

    payload = {"userId": USER_ID, "type": event_type, "data": data or {}}
    if extra_payload:
        payload.update(extra_payload)

    with patch("src.agents.campaign_brain.GeminiClient", MockGeminiClient), \
         patch("src.agents.campaign_brain.BicameralEngine", MockBicameralEngine):
        from src.agents.campaign_brain import run_campaign_agent
        result = run_campaign_agent(db, payload, ctx)

    return result, db, ctx


# =============================================================================
# TESTS
# =============================================================================

def test_analyze_resume():
    """Test ATS resume analysis."""
    print("\n📝 TEST: analyze_resume")

    result, db, ctx = run_handler("analyze_resume", {
        "resumeText": "John Doe\nSoftware Engineer\n5 years Python, React, AWS\nBuilt REST APIs serving 10K requests/day",
        "jobDesc": "Looking for a Python developer with AWS experience"
    })

    data = result["data"]
    check("success is True", data, data.get("success") == True)
    check("data has ats_score", data, data.get("data", {}).get("ats_score") is not None)
    check("data has score_breakdown", data, data.get("data", {}).get("score_breakdown") is not None)


def test_analyze_resume_no_text():
    """Test ATS analysis without resume text."""
    print("\n📝 TEST: analyze_resume (no text)")

    result, db, ctx = run_handler("analyze_resume", {
        "jobDesc": "Python developer"
    })

    data = result["data"]
    check("returns error", data, "error" in data)


def test_generate_resume():
    """Test tailored resume generation."""
    print("\n📄 TEST: generate_resume")

    result, db, ctx = run_handler("generate_resume", {
        "jobDesc": "Full-stack developer with React and Node.js experience",
        "projects": [
            {"name": "E-commerce Platform", "description": "Built full-stack e-commerce", "technologies": "React, Node.js", "impact": "Served 1000+ users"},
            {"name": "ML Pipeline", "description": "Data processing pipeline", "technologies": "Python, TensorFlow", "impact": "95% accuracy"}
        ],
        "skills": ["Python", "React", "JavaScript"],
        "education": "BS Computer Science",
        "includeDocx": False
    })

    data = result["data"]
    check("success is True", data, data.get("success") == True)
    check("data has resume", data, data.get("data", {}).get("resume") is not None)
    check("data has suggestions", data, data.get("data", {}).get("suggestions") is not None)


def test_generate_resume_no_jd():
    """Test resume generation without job description."""
    print("\n📄 TEST: generate_resume (no JD)")

    result, db, ctx = run_handler("generate_resume", {
        "projects": [{"name": "Test"}]
    })

    data = result["data"]
    check("returns error", data, "error" in data)


def test_generate_resume_no_projects():
    """Test resume generation without projects."""
    print("\n📄 TEST: generate_resume (no projects)")

    result, db, ctx = run_handler("generate_resume", {
        "jobDesc": "Python developer"
    })

    data = result["data"]
    check("returns error", data, "error" in data)


def test_design_interview():
    """Test mock interview design."""
    print("\n🎭 TEST: design_interview")

    result, db, ctx = run_handler("design_interview", {
        "jobDesc": "Senior Python developer at a fintech startup",
        "resumeText": "5 years Python experience, built financial APIs",
        "interviewType": "technical",
        "difficulty": "medium"
    })

    data = result["data"]
    check("success is True", data, data.get("success") == True)
    check("data has interview_id", data, data.get("data", {}).get("interview_id") is not None)
    check("data has question_bank", data, data.get("data", {}).get("question_bank") is not None)


def test_design_interview_no_jd():
    """Test interview design without JD."""
    print("\n🎭 TEST: design_interview (no JD)")

    result, db, ctx = run_handler("design_interview", {})

    data = result["data"]
    check("returns error", data, "error" in data)


def test_generate_skill_tree():
    """Test skill tree generation."""
    print("\n🌳 TEST: generate_skill_tree")

    result, db, ctx = run_handler("generate_skill_tree", {
        "targetRole": "Machine Learning Engineer",
        "skills": ["Python", "Statistics"],
        "education": "BS Mathematics"
    })

    data = result["data"]
    check("success is True", data, data.get("success") == True)
    check("data has skill_tree", data, data.get("data", {}).get("skill_tree") is not None)
    check("data has recommended_path", data, data.get("data", {}).get("recommended_path") is not None)
    check("data has time_to_job_ready", data, data.get("data", {}).get("time_to_job_ready") is not None)


def test_new_goal():
    """Test legacy goal setting."""
    print("\n🎯 TEST: new_goal")

    result, db, ctx = run_handler("new_goal", extra_payload={
        "goal_title": "Become a Full-Stack Developer"
    })

    data = result["data"]
    check("status is goal_set", data, data.get("status") == "goal_set")
    check("strategy exists", data, len(data.get("strategy", "")) > 3)
    check("DB has intervention", db, len(db._interventions) >= 1)


def test_unknown_event():
    """Test unknown event type returns status."""
    print("\n🔀 TEST: unknown event")

    result, db, ctx = run_handler("some_random_event")
    data = result["data"]
    check("status is unknown_event", data, data.get("status") == "unknown_event")


def test_no_user_id():
    """Test error when no userId."""
    print("\n❌ TEST: no userId")

    ctx = MockContext()
    db = MockDBHelper()
    with patch("src.agents.campaign_brain.GeminiClient", MockGeminiClient), \
         patch("src.agents.campaign_brain.BicameralEngine", MockBicameralEngine):
        from src.agents.campaign_brain import run_campaign_agent
        result = run_campaign_agent(db, {"type": "analyze_resume"}, ctx)

    data = result["data"]
    check("returns error", data, "error" in data)


def test_campaign_calibration():
    """Test full campaign calibration (skill tree + armory + quest board)."""
    print("\n🎮 TEST: campaign_calibration")

    result, db, ctx = run_handler("campaign_calibration", {
        "targetRole": "Backend Engineer",
        "skills": ["Python", "SQL"],
        "education": "BS CS"
    })

    data = result["data"]
    check("success is True", data, data.get("success") == True)
    check("data has skill_tree", data, data.get("data", {}).get("skill_tree") is not None)
    check("data has armory", data, data.get("data", {}).get("armory") is not None)
    check("data has quest_board", data, data.get("data", {}).get("quest_board") is not None)
    check("DB has campaign state", db, USER_ID in db._campaign_state)


# =============================================================================
# RUNNER
# =============================================================================

def run_all_tests():
    global PASS_COUNT, FAIL_COUNT
    PASS_COUNT = 0
    FAIL_COUNT = 0

    print("=" * 60)
    print("⚔️ CAMPAIGN BRAIN - CAREER STRATEGIST TEST SUITE")
    print(f"   User: {USER_ID}")
    print(f"   Timestamp: {datetime.now().isoformat()}")
    print("=" * 60)

    tests = [
        test_analyze_resume,
        test_analyze_resume_no_text,
        test_generate_resume,
        test_generate_resume_no_jd,
        test_generate_resume_no_projects,
        test_design_interview,
        test_design_interview_no_jd,
        test_generate_skill_tree,
        test_campaign_calibration,
        test_new_goal,
        test_unknown_event,
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
    print(f"⚔️ CAMPAIGN BRAIN RESULTS: {PASS_COUNT} passed, {FAIL_COUNT} failed")
    print("=" * 60)

    if FAIL_COUNT == 0:
        print("🎉 ALL TESTS PASSED!")
    else:
        print(f"⚠️ {FAIL_COUNT} test(s) failed")

    return FAIL_COUNT == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
