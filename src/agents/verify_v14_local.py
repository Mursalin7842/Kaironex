
import asyncio
import sys
import os
import json
from unittest.mock import MagicMock, AsyncMock
from datetime import datetime, timedelta

# Add src parent to path to allow imports
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))

# Mock external dependencies that aren't present locally
sys.modules['src.core.bicameral_engine'] = MagicMock()
sys.modules['src.core.thought_manager'] = MagicMock()
sys.modules['src.core.state_machine'] = MagicMock()
sys.modules['src.agents.base_agent'] = MagicMock()

# Define ReasoningMode and AgentResult mocks
class ReasoningMode:
    FAST = "FAST"
    DEEP = "DEEP"
    HYBRID = "HYBRID"
    REFLEX = "REFLEX"

class ReasoningRequest:
    def __init__(self, **kwargs):
        pass

class AgentResult:
    def __init__(self, success, response, actions_taken=None, error=None):
        self.success = success
        self.response = response
        self.actions_taken = actions_taken or []
        self.error = error
    
    def to_dict(self):
        return {"success": self.success, "response": self.response}

class AgentConfig:
    def __init__(self, **kwargs):
        pass

# Mock BaseAgent
class BaseAgent:
    def __init__(self, engine, thought_manager, db_helper):
        self.engine = engine
        self.thought_manager = thought_manager
        self.db = db_helper
    
    def log_heartbeat(self, *args):
        print(f"[HEARTBEAT] {args}")
        
    async def run(self, user_id, payload, trigger_event):
        return await self.process(user_id, payload, None)

# Apply mocks to modules
sys.modules['src.core.bicameral_engine'].ReasoningMode = ReasoningMode
sys.modules['src.core.bicameral_engine'].ReasoningRequest = ReasoningRequest
sys.modules['src.agents.base_agent'].BaseAgent = BaseAgent
sys.modules['src.agents.base_agent'].AgentConfig = AgentConfig
sys.modules['src.agents.base_agent'].AgentResult = AgentResult

# Import the agent to test
from src.agents.study_agent import StudyAgent

# --- SIMULATION DATA ---
MOCK_USER_ID = "test_user_123"

MOCK_PROFILE = {
    "name": "Test Student",
    "jobSchedule": "Mon-Wed 9-5",
    "nonNegotiables": "Gym at 6am",
    "commuteTime": "30 mins"
}

MOCK_RESOURCES = [
    {
        "title": "Spring 2026 Routine.pdf",
        "resource_type": "PDF",
        "mined_data": "Monday: Class CS450 Room 304 10:00-11:30. Wednesday: Class MAT301 Room 101 14:00-15:30."
    },
    {
        "title": "Syllabus_CS450.pdf",
        "resource_type": "PDF",
        "mined_data": "Course: Machine Learning (CS450). Week 1: Introduction. Week 5: Neural Networks."
    }
]

# --- MOCK ENGINE RESPONSES ---
# 1. Timeline
MOCK_TIMELINE_JSON = json.dumps({
    "start_date": "2026-01-15",
    "end_date": "2026-05-15",
    "exam_weeks": [{"start": "2026-03-10", "end": "2026-03-17"}]
})

# 2. Blocked Slots (Constraints)
MOCK_CONSTRAINTS_JSON = json.dumps([
    {"day_offset": 0, "startTime": "10:00", "endTime": "11:30", "title": "Class: CS450", "location": "Room 304", "type": "blocked"},
    {"day_offset": 2, "startTime": "14:00", "endTime": "15:30", "title": "Class: MAT301", "location": "Room 101", "type": "blocked"}
])

# 3. The Schedule Plan (Director)
MOCK_PLAN_JSON = json.dumps({
    "phases": [
        {
            "phase_name": "Early Semester",
            "start_week": 1,
            "end_week": 8,
            "routine_template": [
                {
                    "day_offset": 0, "startTime": "19:00", "endTime": "21:00",
                    "title": "Deep Work: CS450", "type": "study", "subject": "CS450",
                    "priority": 8, "location": "Library",
                    "topics": "Read Ch 2. Implement Gradient Descent."
                }
            ]
        }
    ],
    "syllabus_map": {"CS450": ["Intro", "Linear Regression", "Gradient Descent"]},
    "deadlines": []
})

async def run_simulation():
    print("🚀 STARTING V14.0 LOCAL SIMULATION...")
    
    # Mock Helper
    db_helper = MagicMock()
    db_helper.get_user_doc.return_value = {"studentprofile_json": json.dumps(MOCK_PROFILE)}
    db_helper.get_user_resources.return_value = MOCK_RESOURCES
    db_helper.batch_create_schedule_tasks.return_value = 5 # Fake task count
    
    # Mock Engine
    engine = MagicMock()
    # Chain of reponses: Timeline -> Constraints -> Director
    # logic: The agent calls reason() 3 times. We'll return them in order.
    
    async def mock_reason(request):
        prompt = request.prompt
        content = "{}"
        if "Extract Academic Timeline" in prompt:
            print("   -> Engine: Extracting Timeline...")
            content = MOCK_TIMELINE_JSON
        elif "Extract blocked time slots" in prompt:
            print("   -> Engine: Extracting Constraints...")
            content = MOCK_CONSTRAINTS_JSON
        elif "ACT AS: The Semester Director" in prompt:
            print("   -> Engine: Creating Semester Plan...")
            # Verify subject autodiscovery worked by checking prompt
            if "DETECTED SUBJECTS: CS450" in prompt or "CS450" in prompt:
                print("   ✅ PASS: Subject 'CS450' was auto-discovered from constraints!")
            else:
                print("   ❌ FAIL: Subject 'CS450' NOT found in prompt!")
            content = MOCK_PLAN_JSON
        
        response_mock = MagicMock()
        response_mock.content = content
        return response_mock
        
    engine.reason = AsyncMock(side_effect=mock_reason)
    
    # Initialize Agent
    agent = StudyAgent(engine, MagicMock(), db_helper)
    
    # Payload
    payload = {
        "type": "schedule_request",
        "duration_days": 14
    }
    
    # Run
    try:
        result = await agent.process(MOCK_USER_ID, payload, None)
        print(f"\n🏁 SIMULATION RESULT: {result.success}")
        print(f"📝 Response: {result.response}")
        print(f"🛠️ Actions: {result.actions_taken}")
        
    except Exception as e:
        print(f"❌ CRITICAL ERROR: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(run_simulation())
