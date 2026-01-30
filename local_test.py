import os
import sys
import json
from dotenv import load_dotenv

# 1. Load Environment Variables from .env
load_dotenv()

# 2. Add 'src' to python path so relative imports work
current_dir = os.path.dirname(os.path.abspath(__file__))
sys.path.append(current_dir)

# Now we can import the function
from src.main import main

# 3. Mock the Appwrite Context Object
class MockRequest:
    def __init__(self, body_json=None):
        self.body = json.dumps(body_json) if body_json else ""
        self.body_json = body_json

class MockResponse:
    def json(self, data):
        print("\n--- 🟢 FUNCTION RESPONSE ---")
        print(json.dumps(data, indent=2))
        return data

    def send(self, text, status=200):
        print(f"\n--- 🟢 FUNCTION TEXT RESPONSE ({status}) ---")
        print(text)

class MockContext:
    def __init__(self, payload=None):
        self.req = MockRequest(payload)
        self.res = MockResponse()

    def log(self, message):
        print(f"ℹ️ [LOG]: {message}")

    def error(self, message):
        print(f"❌ [ERROR]: {message}")

# 4. Define Test Scenarios
def test_cron_trigger():
    print("\n🧪 TESTING SCENARIO: CRON JOB (Safety Net)")
    # Simulate a Cron Trigger
    os.environ['APPWRITE_FUNCTION_EVENT'] = 'cron_schedule'
    
    context = MockContext(payload={})
    main(context)

def test_study_log_trigger():
    print("\n🧪 TESTING SCENARIO: STUDY LOG EVENT")
    # Simulate a Study Log Event
    os.environ['APPWRITE_FUNCTION_EVENT'] = 'databases.kaironex_db.collections.study_logs.documents.create'
    
    # Payload similar to what Appwrite sends
    mock_payload = {
        "userId": "test_user_id_123",
        "duration_seconds": 1800,
        "focus_score": 35,  # Low score to trigger intervention
        "$id": "log_123"
    }
    
    
    context = MockContext(payload=mock_payload)
    main(context)

def test_vitality_trigger():
    print("\n🧪 TESTING SCENARIO: VITALITY BRAIN")
    os.environ['APPWRITE_FUNCTION_EVENT'] = 'vitality_state'
    mock_payload = {
        "userId": "test_user_id_123",
        "sleep_hours": 4, # Low sleep to trigger empathy
        "steps": 2000
    }
    context = MockContext(payload=mock_payload)
    main(context)

def test_campaign_trigger():
    print("\n🧪 TESTING SCENARIO: CAMPAIGN BRAIN")
    os.environ['APPWRITE_FUNCTION_EVENT'] = 'schedule.updated' # triggers 'schedule' check
    mock_payload = {
        "userId": "test_user_id_123",
        "type": "new_goal",
        "goal_title": "Run a Marathon"
    }
    context = MockContext(payload=mock_payload)
    main(context)

def test_radius_trigger():
    print("\n🧪 TESTING SCENARIO: RADIUS BRAIN")
    os.environ['APPWRITE_FUNCTION_EVENT'] = 'radius_state'
    mock_payload = {
        "userId": "test_user_id_123",
        "location": "gym"
    }
    context = MockContext(payload=mock_payload)
    main(context)

# 5. Run Tests
if __name__ == "__main__":
    # Uncomment the scenario you want to test:
    
    # A. Test the Cron Job
    # test_cron_trigger()

    # B. Test the Study Brain
    # test_study_log_trigger()
    
    # C. Test Vitality (Sleep Check)
    test_vitality_trigger()
    
    # D. Test Campaign (New Goal)
    # test_campaign_trigger()
    
    # E. Test Radius (Location: Gym)
    # test_radius_trigger()
