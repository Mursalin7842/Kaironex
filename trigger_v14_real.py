
import os
import sys
import json
import asyncio
import logging
from dotenv import load_dotenv

# Setup paths
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env'))

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Mock Context for Appwrite Function
class MockContext:
    def __init__(self):
        self.req = self
        self.res = self
        self.body = {}
        
    def json(self, data, status=200):
        print(f"\n[RESPONSE {status}] {json.dumps(data, indent=2)}")
        return data

    def log(self, msg): print(f"[LOG] {msg}")
    def error(self, msg): print(f"[ERROR] {msg}")

async def main():
    print("🚀 Triggering StudyAgent v14.0 (REAL DATABASE)...")
    
    try:
        from src.utils.db_helper import KairoDB
        from src.agents.study_agent import run_study_agent
    except ImportError as e:
        print(f"❌ Import Error: {e}")
        return

    # 1. Connect to DB
    print("🔌 Connecting to Appwrite...")
    try:
        db = KairoDB()
        print("✅ Connected.")
    except Exception as e:
        print(f"❌ Connection Failed: {e}")
        return

    # 2. Find Real User
    print("🔍 Using specific user: demo_user_001")
    user_id = "demo_user_001"

    if not user_id:
        print("❌ Could not find any user ID to test with.")
        return

    # 3. Trigger v14.0 Logic
    print(f"\n🎬 ACTION: Schedule Request for User {user_id}")
    
    payload = {
        "userId": user_id,
        "type": "schedule_request",
        "duration_days": 0, # 0 = Full Semester (Dynamic)
    }
    
    context = MockContext()
    
    try:
        # Run the async entry point
        # Note: run_study_agent is async, so we await it
        await run_study_agent(db, payload, context)
        
        print("\n✅ Script Finished Successfully.")
        
    except Exception as e:
        print(f"\n❌ Execution Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(main())
