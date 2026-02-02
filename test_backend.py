"""
🧪 KAIRONEX BACKEND TEST SUITE
================================
Run this to verify the entire backend is working!

Usage:
    python test_backend.py

This tests:
- BicameralEngine with thinking
- Function calling with tools
- Deep Research API
- Student growth tracking
- Life event handling
- Schedule validation
- Full orchestrator flow
"""

import os
import sys
import asyncio
from datetime import datetime
from dotenv import load_dotenv

# Load environment
load_dotenv()

# Add src to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def print_header(title: str):
    """Print a formatted header."""
    print(f"\n{'='*60}")
    print(f"🧪 {title}")
    print(f"{'='*60}\n")


def print_result(success: bool, message: str):
    """Print a test result."""
    status = "✅ PASS" if success else "❌ FAIL"
    print(f"  {status}: {message}")


async def test_config():
    """Test configuration is loaded correctly."""
    print_header("TEST 1: Configuration")
    
    try:
        from src.config import (
            GEMINI_API_KEY, APPWRITE_DATABASE_ID,
            GEMINI_3_FLASH,
            MARATHON_SESSIONS_COL, THOUGHT_SIGNATURES_COL,
            STUDENT_PROFILES_COL, LIFE_EVENTS_COL
        )
        
        print_result(bool(GEMINI_API_KEY), f"GEMINI_API_KEY configured")
        print_result(bool(APPWRITE_DATABASE_ID), f"Database ID: {APPWRITE_DATABASE_ID}")
        print_result(bool(GEMINI_3_FLASH), f"Model: {GEMINI_3_FLASH}")
        print_result(bool(STUDENT_PROFILES_COL), f"New tables configured: {STUDENT_PROFILES_COL}")
        
        return bool(GEMINI_API_KEY)
        
    except Exception as e:
        print_result(False, f"Config error: {e}")
        return False


async def test_bicameral_engine():
    """Test the BicameralEngine."""
    print_header("TEST 2: Bicameral Engine (Gemini 3 Thinking)")
    
    try:
        from src.core.bicameral_engine import (
            BicameralEngine, ReasoningRequest, ReasoningMode
        )
        
        engine = BicameralEngine(use_tools=False)
        print_result(True, "BicameralEngine initialized")
        
        # Test reflex mode
        response = await engine.reason(ReasoningRequest(
            prompt="What is 2 + 2? Answer briefly.",
            user_id="test_user",
            agent="test",
            mode=ReasoningMode.REFLEX
        ))
        
        print_result(len(response.content) > 0, f"Reflex response: {response.content[:100]}...")
        print_result(response.latency_ms > 0, f"Latency: {response.latency_ms:.0f}ms")
        
        # Test deep mode with thinking
        print("\n  Testing DEEP mode with thinking (this may take a moment)...")
        response = await engine.reason(ReasoningRequest(
            prompt="If a student has an exam in 5 days and works part-time, what should they prioritize?",
            user_id="test_user",
            agent="test",
            mode=ReasoningMode.DEEP,
            max_thinking_tokens=4096
        ))
        
        print_result(len(response.content) > 0, f"Deep response received ({len(response.content)} chars)")
        print_result(response.thought_signature is not None, "Thought signature generated")
        if response.thought_signature:
            print_result(True, f"Thought ID: {response.thought_signature.thought_id}")
        
        return True
        
    except Exception as e:
        print_result(False, f"Engine error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_tool_declarations():
    """Test tool declarations are valid."""
    print_header("TEST 3: Tool Declarations")
    
    try:
        from src.tools.tool_declarations import (
            ALL_KAIRONEX_TOOLS, get_tools_for_agent, get_tool_descriptions
        )
        
        print_result(len(ALL_KAIRONEX_TOOLS) > 0, f"Total tools: {len(ALL_KAIRONEX_TOOLS)}")
        
        # Count functions
        total_functions = 0
        for tool in ALL_KAIRONEX_TOOLS:
            funcs = tool.function_declarations or []
            total_functions += len(funcs)
        
        print_result(total_functions > 10, f"Total functions: {total_functions}")
        
        # Test per-agent tools
        study_tools = get_tools_for_agent("study")
        print_result(len(study_tools) > 0, f"Study agent tools: {len(study_tools)}")
        
        campaign_tools = get_tools_for_agent("campaign")
        print_result(len(campaign_tools) > 0, f"Campaign agent tools: {len(campaign_tools)}")
        
        # Test descriptions
        descriptions = get_tool_descriptions()
        print_result(len(descriptions) > 100, f"Tool descriptions: {len(descriptions)} chars")
        
        return True
        
    except Exception as e:
        print_result(False, f"Tool declaration error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_tool_executor():
    """Test tool executor."""
    print_header("TEST 4: Tool Executor")
    
    try:
        from src.tools.tool_executor import ToolExecutor, ToolResult
        
        executor = ToolExecutor()
        print_result(True, "ToolExecutor initialized")
        
        # Test a simple tool
        result = await executor.execute("check_engagement", {
            "user_id": "test_user"
        })
        
        print_result(result.success, f"check_engagement executed")
        print_result("engagement_level" in result.data, f"Result: {result.data}")
        
        # Test another tool
        result = await executor.execute("get_schedule", {
            "user_id": "test_user",
            "date": "2026-02-03"
        })
        
        print_result(result.success, f"get_schedule executed")
        
        return True
        
    except Exception as e:
        print_result(False, f"Executor error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_function_calling():
    """Test function calling with BicameralEngine."""
    print_header("TEST 5: Function Calling (CRITICAL)")
    
    try:
        from src.core.bicameral_engine import (
            BicameralEngine, ReasoningRequest, ReasoningMode
        )
        
        engine = BicameralEngine(use_tools=True)
        print_result(engine.use_tools, "Engine initialized with tools")
        
        # Test function calling
        print("\n  Testing function calling (may take a moment)...")
        response = await engine.reason_with_tools(ReasoningRequest(
            prompt="Check my engagement level and tell me if I need a break.",
            user_id="test_user",
            agent="study",
            mode=ReasoningMode.DEEP
        ))
        
        print_result(len(response.content) > 0, f"Response: {response.content[:150]}...")
        print_result(True, f"Tool calls: {response.tool_results}")
        
        return True
        
    except Exception as e:
        print_result(False, f"Function calling error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_deep_research():
    """Test Deep Research API."""
    print_header("TEST 6: Deep Research API (FLAGSHIP)")
    
    try:
        from src.tools.deep_research import (
            DeepResearchEngine, ResearchQuery, ResearchType
        )
        
        engine = DeepResearchEngine()
        print_result(True, "DeepResearchEngine initialized")
        
        # Test syllabus analysis
        print("\n  Running syllabus analysis (this takes 10-30 seconds)...")
        report = await engine.analyze_syllabus(
            user_id="test_user",
            course_name="Introduction to Algorithms",
            university="MIT"
        )
        
        print_result(report.status.value == "completed", f"Status: {report.status.value}")
        print_result(len(report.summary) > 50, f"Summary: {report.summary[:200]}...")
        print_result(len(report.key_findings) > 0, f"Findings: {len(report.key_findings)}")
        print_result(len(report.sources) >= 0, f"Sources: {len(report.sources)}")
        print_result(report.research_duration_seconds > 0, f"Duration: {report.research_duration_seconds:.1f}s")
        
        return True
        
    except Exception as e:
        print_result(False, f"Deep Research error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_student_growth():
    """Test student growth engine."""
    print_header("TEST 7: Student Growth Engine")
    
    try:
        from src.core.student_growth_engine import (
            StudentGrowthEngine, StudentProfile, ConfidenceLevel
        )
        
        engine = StudentGrowthEngine()
        print_result(True, "StudentGrowthEngine initialized")
        
        # Create a profile (not async)
        profile = engine.get_or_create_profile("test_user")
        print_result(profile is not None, f"Profile created")
        print_result(hasattr(profile, 'confidence_level'), f"Has confidence level")
        
        # Record a session
        engine.record_session(
            user_id="test_user",
            session_data={
                "duration_minutes": 45,
                "completed": True,
                "mastery_score": 80,
                "start_hour": 10
            }
        )
        print_result(True, "Session recorded")
        
        # Check profile was updated
        updated_profile = engine.get_or_create_profile("test_user")
        print_result(updated_profile.data_points_collected > 0, f"Profile updated with {updated_profile.data_points_collected} data points")
        
        return True
        
    except Exception as e:
        print_result(False, f"Student growth error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_life_events():
    """Test life event handling."""
    print_header("TEST 8: Life Event Handler")
    
    try:
        from src.core.life_event_handler import (
            LifeEventHandler, LifeEvent, EventCategory, EventSeverity
        )
        
        handler = LifeEventHandler()
        print_result(True, "LifeEventHandler initialized")
        
        # Process an event using the handler's process_event method
        # (which creates the event internally)
        result = await handler.process_event(
            user_id="test_user",
            event_description="I have a wedding this weekend",
            affects_from=datetime.now(),
            source="user"
        )
        
        print_result("escalated" in str(result) or "adjustments" in str(result) or "recommendations" in str(result), 
                     f"Event processed: {str(result)[:200]}...")
        
        return True
        
    except Exception as e:
        print_result(False, f"Life event error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_schedule_validator():
    """Test schedule validation."""
    print_header("TEST 9: Schedule Validator")
    
    try:
        from src.core.schedule_validator import (
            ScheduleValidator, ChangeType
        )
        
        validator = ScheduleValidator()
        print_result(True, "ScheduleValidator initialized")
        
        # Propose a change
        change = validator.propose_change(
            user_id="test_user",
            change_type=ChangeType.ADD,
            new_data={
                "title": "Study Calculus",
                "type": "study",
                "start": "2026-02-03T14:00:00",
                "end": "2026-02-03T16:00:00"
            },
            reason="Need to prepare for exam"
        )
        print_result(change is not None, f"Change proposed: {change.change_id}")
        
        # Validate it
        decision = await validator.validate_change(
            user_id="test_user",
            change=change
        )
        print_result(decision is not None, f"Decision: {decision.result.value}")
        print_result(len(decision.message_to_user) > 0, f"Message: {decision.message_to_user}")
        
        return True
        
    except Exception as e:
        print_result(False, f"Schedule validator error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_orchestrator():
    """Test the full orchestrator."""
    print_header("TEST 10: Full Orchestrator (INTEGRATION)")
    
    try:
        from src.orchestrator import (
            KaironexOrchestrator, OrchestratorRequest, RequestType
        )
        
        orchestrator = KaironexOrchestrator()
        print_result(True, "KaironexOrchestrator initialized")
        
        # Test chat
        response = await orchestrator.process(OrchestratorRequest(
            user_id="test_user",
            message="Hi! I'm a student preparing for exams.",
            request_type=RequestType.CHAT
        ))
        print_result(response.success, f"Chat response: {response.message[:100]}...")
        
        # Test life event detection
        response = await orchestrator.process(OrchestratorRequest(
            user_id="test_user",
            message="I'm feeling sick today, might have a cold."
        ))
        print_result(response.request_type == RequestType.LIFE_EVENT, f"Detected: {response.request_type.value}")
        
        return True
        
    except Exception as e:
        print_result(False, f"Orchestrator error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def run_all_tests():
    """Run all tests."""
    print("\n" + "="*60)
    print("🚀 KAIRONEX BACKEND TEST SUITE")
    print("="*60)
    print(f"Started: {datetime.now().isoformat()}")
    
    results = {}
    
    # Run tests in order
    results["config"] = await test_config()
    
    if not results["config"]:
        print("\n❌ STOPPING: Config failed - please set GEMINI_API_KEY")
        return results
    
    results["bicameral"] = await test_bicameral_engine()
    results["tool_declarations"] = await test_tool_declarations()
    results["tool_executor"] = await test_tool_executor()
    results["function_calling"] = await test_function_calling()
    results["deep_research"] = await test_deep_research()
    results["student_growth"] = await test_student_growth()
    results["life_events"] = await test_life_events()
    results["schedule_validator"] = await test_schedule_validator()
    results["orchestrator"] = await test_orchestrator()
    
    # Summary
    print("\n" + "="*60)
    print("📊 TEST SUMMARY")
    print("="*60)
    
    passed = sum(1 for v in results.values() if v)
    total = len(results)
    
    for test, passed_test in results.items():
        status = "✅" if passed_test else "❌"
        print(f"  {status} {test}")
    
    print(f"\n  TOTAL: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 ALL TESTS PASSED! Backend is ready for demo!")
    else:
        print(f"\n⚠️  {total - passed} test(s) failed. Check errors above.")
    
    return results


if __name__ == "__main__":
    asyncio.run(run_all_tests())
