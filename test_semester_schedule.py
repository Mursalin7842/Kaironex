
import asyncio
import os
import sys
from datetime import datetime
from dotenv import load_dotenv

# Setup paths (Adjust based on your local rig)
SRC_PATH = r"D:\Kaironex-Brain"
sys.path.append(SRC_PATH)
load_dotenv(os.path.join(SRC_PATH, '.env'))

from src.utils.db_helper import KairoDB
from src.agents.study_agent import StudyAgent
from src.core.bicameral_engine import BicameralEngine
from src.core.thought_manager import ThoughtManager

async def test_semester_schedule():
    print("🚀 Starting Semester Schedule Test...")
    
    # 1. Initialize
    db = KairoDB()
    engine = BicameralEngine() # Will check GEMINI_API_KEY
    thought_manager = ThoughtManager(db)
    agent = StudyAgent(engine, thought_manager, db)
    
    # 2. Setup Test User
    # Using the user ID from your previous runs or fetching one
    user_id = "67980554002e3b2e95a9" 
    
    # Verify User Exists
    user = db.get_user_doc(user_id)
    if not user:
        print(f"❌ User {user_id} not found. Using fallback logic or exiting.")
        # Fallback: Find any user
        users = db.db.list_rows(db.APPWRITE_DATABASE_ID, 'users', queries=[db.Query.limit(1)])
        if users['total'] > 0:
            user_id = users['rows'][0]['userId']
            print(f"👤 Switched to User ID: {user_id}")
        else:
            print("❌ No users found in DB. Ingest some data first.")
            return

    print(f"👤 Testing for User: {user_id}")
    
    # 3. Simulate Schedule Request Payload
    payload = {
        "status": "schedule_request",
        "duration_days": 120, # Semester length ~4 months
        "resource_ids": []    # Empty list = Fetch all/recent
    }
    
    # 4. Execute Agent Logic
    print("\n⏳ Agent Thinking (Deep Mode)... This may take 30-60s...")
    result = await agent.process(user_id, payload, {})
    
    # 5. Output Results
    print("\n" + "="*50)
    print("✅ AGENT RESULT:")
    print("="*50)
    print(f"Success: {result.success}")
    print(f"Response: {result.response}")
    if result.actions_taken:
        print(f"Actions: {result.actions_taken}")
        
    # 6. Verify DB Entries (Progressive Check)
    print("\n🔍 Verifying Progressive Schedule...")
    schedules = db.get_schedule(user_id, limit=200) # Fetch more to see progression
    if schedules:
        print(f"found {len(schedules)}+ tasks.")
        
        # Sort by time
        schedules.sort(key=lambda x: x['startTime'])
        
        # Check Day 1 vs Day 30
        print("\n--- Week 1 Sample ---")
        for t in schedules[:5]:
             print(f"[{t['startTime'][:10]}] {t['title']} ({t['type']})")
             
        print("\n--- Week 5 Sample (Should be different topics) ---")
        week_5_tasks = [t for t in schedules if "Week 5" in t.get('description', '') or datetime.fromisoformat(t['startTime']) > datetime.now().astimezone() + timedelta(days=28)]
        # Just grab tasks from index 40-45
        if len(schedules) > 50:
             for t in schedules[40:45]:
                 print(f"[{t['startTime'][:10]}] {t['title']} ({t['type']})")
    else:
        print("⚠️ No schedule tasks found in DB.")

if __name__ == "__main__":
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
    asyncio.run(test_semester_schedule())
