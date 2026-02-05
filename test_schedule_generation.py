
import os
import json
import logging
import sys
from datetime import datetime

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Mock Context
class MockContext:
    def __init__(self):
        self.req = self
        self.path = '/'
        self.method = 'POST'
        self.body = {}
        
    def log(self, msg):
        logger.info(msg)
    
    def error(self, msg):
        logger.error(msg)
        
    class Res:
        def json(self, data, status=200):
            print(f"RESPONSE [{status}]: {json.dumps(data, indent=2)}")
            return data
            
    res = Res()

# Load Env
from dotenv import load_dotenv
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
load_dotenv(os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env'))

# Import Agent
try:
    from src.utils.db_helper import KairoDB
    from src.agents.study_agent import run_study_agent
except ImportError as e:
    print(f"Import Error: {e}")
    sys.exit(1)

def main():
    print("🚀 Starting Local Debug for Schedule Logic...")
    
    # 1. Init DB
    try:
        db = KairoDB()
        print("✅ Database Connected")
    except Exception as e:
        print(f"❌ DB Connection Failed: {e}")
        return

    # 2. Get User
    user_id = "67980554002e3b2e95a9" 
    try:
        # Try to find a user with resources
        users = db.db.list_rows(db.APPWRITE_DATABASE_ID, 'users', queries=[db.Query.limit(5)])
        found_user = False
        
        for user in users.get('rows', []):
            uid = user['userId']
            # Check if user has resources
            res_count = db.count_user_resources(uid)
            if res_count > 0:
                user_id = uid
                print(f"👤 Found User with {res_count} resources: {user_id}")
                found_user = True
                break
        
        if not found_user and users.get('rows'):
            user_id = users['rows'][0]['userId']
            print(f"⚠️ No user with resources found, falling back to: {user_id}")
            
    except Exception as e:
        print(f"❌ Failed to fetch users: {e}")

    # 3. Trigger Schedule Request
    payload = {
        "userId": user_id,
        "type": "schedule_request",
        "duration_days": 7, # Short duration for test
        "resource_ids": [] # Let it pick automatically
    }
    
    context = MockContext()
    context.req.body = payload
    
    print(f"\n--- RUNNING STUDY AGENT (Schedule Request) for {user_id} ---\n")
    try:
        result = run_study_agent(db, payload, context)
        print("\n✅ Execution Finished")
        # Check if tasks were created?
        # Manually:
        tasks = db.get_schedule(user_id, limit=5)
        print(f"\nCreated Schedule Tasks (Top 5):")
        for t in tasks:
            print(f"- [{t['startTime']}] {t['title']}")
            
    except Exception as e:
        print(f"\n❌ Execution Failed: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
