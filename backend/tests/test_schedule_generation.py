#!/usr/bin/env python3
"""
🧪 TEST SCHEDULE GENERATION
===========================
Tests the full Study Agent pipeline with the new rich task format.

Run: python test_schedule_generation.py
"""

import sys
import json
import asyncio
from pathlib import Path
from datetime import datetime, timedelta

# Load environment variables FIRST
from dotenv import load_dotenv
load_dotenv(Path(__file__).parent.parent / ".env")

# Add project root to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.utils.db_helper import KairoDB
from src.core.bicameral_engine import BicameralEngine
from src.core.thought_manager import ThoughtManager
from src.core.state_machine import StateContext
from src.agents.study_agent import StudyAgent


# =============================================================================
# CONFIGURATION - SET YOUR TEST USER ID HERE
# =============================================================================
TEST_USER_ID = "67a3eb680026f0ea2e4f"  # <-- CHANGE THIS to your real user ID


class MockContext:
    """Mock Appwrite context for local testing."""
    def __init__(self):
        self.logs = []
        self.errors = []
    
    def log(self, msg):
        print(f"📋 LOG: {msg}")
        self.logs.append(msg)
    
    def error(self, msg):
        print(f"❌ ERROR: {msg}")
        self.errors.append(msg)
    
    class res:
        @staticmethod
        def json(data, status=200):
            print(f"\n📤 RESPONSE ({status}):")
            print(json.dumps(data, indent=2))
            return data


async def test_schedule_generation():
    """Test the full schedule generation pipeline."""
    
    print("\n" + "="*60)
    print("🧪 SCHEDULE GENERATION TEST")
    print("="*60)
    print(f"📅 Test Date: {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    print(f"👤 User ID: {TEST_USER_ID}")
    print("="*60 + "\n")
    
    # Initialize components
    try:
        db = KairoDB()
        engine = BicameralEngine()
        thought_manager = ThoughtManager(db)
        agent = StudyAgent(engine, thought_manager, db)
        print("✅ All components initialized\n")
    except Exception as e:
        print(f"❌ Failed to initialize: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    # Step 1: Check if user exists with profile
    print("STEP 1: Checking user profile...")
    print("-" * 50)
    user_doc = db.get_user_doc(TEST_USER_ID)
    if not user_doc:
        print(f"❌ User {TEST_USER_ID} not found!")
        print("   Make sure you set a valid user ID at the top of this file.")
        return False
    
    profile_json = user_doc.get('studentprofile_json', '{}')
    try:
        profile = json.loads(profile_json)
        print(f"✅ Profile found!")
        print(f"   - Has job: {profile.get('hasJob', 'Not specified')}")
        print(f"   - Target CGPA: {profile.get('targetCgpa', 'Not specified')}")
        print(f"   - Energy preference: {profile.get('energyPreference', 'Not specified')}")
        print(f"   - Non-negotiables: {profile.get('nonNegotiables', 'None')[:50]}...")
    except Exception as e:
        print(f"⚠️ Could not parse profile: {e}")
        profile = {}
    
    # Step 2: Check resources
    print("\nSTEP 2: Checking academic resources...")
    print("-" * 50)
    resources = db.get_user_resources(TEST_USER_ID)
    if not resources:
        print("⚠️ No resources found!")
        print("   Upload a syllabus PDF first, or the schedule won't generate.")
        print("   Continuing anyway to test error handling...")
    else:
        print(f"✅ Found {len(resources)} resources:")
        for r in resources[:5]:
            title = r.get('title', 'Untitled')
            file_id = r.get('fileId', 'N/A')
            summary = r.get('summaryText', '')
            mined = r.get('mined_data', '')
            content_len = len(summary) if summary else (len(mined) if mined else 0)
            print(f"   - {title}")
            print(f"     FileId: {file_id[:20] if file_id else 'None'}...")
            print(f"     Content: {content_len} chars {'✅' if content_len > 0 else '❌ EMPTY'}")
    
    # Step 3: Execute schedule generation
    print("\nSTEP 3: Generating schedule (this may take 30-60 seconds)...")
    print("-" * 50)
    
    payload = {
        "userId": TEST_USER_ID,
        "type": "schedule_request",
        "trigger": "test_script"
    }
    
    context = MockContext()
    state = StateContext(user_id=TEST_USER_ID, agent_type="study", trigger_event="schedule_request")
    
    try:
        result = await agent.run(TEST_USER_ID, payload, "schedule_request")
        
        print("\n" + "="*60)
        print("📊 RESULT")
        print("="*60)
        print(f"Success: {result.success}")
        print(f"Response: {result.response}")
        print(f"Actions: {result.actions_taken}")
        if result.error:
            print(f"Error: {result.error}")
            
    except Exception as e:
        print(f"❌ Schedule generation failed: {e}")
        import traceback
        traceback.print_exc()
        return False
    
    # Step 4: Verify schedule was created
    print("\nSTEP 4: Verifying schedule in database...")
    print("-" * 50)
    
    schedule = db.get_schedule(TEST_USER_ID, limit=10)
    if schedule:
        print(f"✅ Found {len(schedule)} tasks in schedule!")
        print("\n📅 Sample tasks:")
        for task in schedule[:3]:
            print(f"\n   📌 {task.get('title', 'Untitled')}")
            print(f"      Time: {task.get('startTime', '?')} - {task.get('endTime', '?')}")
            print(f"      Type: {task.get('type', '?')}")
            print(f"      Subject: {task.get('subject', 'N/A')}")
            print(f"      Content Mode: {task.get('content_mode', 'N/A')}")
            print(f"      Difficulty: {task.get('difficulty', 'N/A')}")
            topics = task.get('topics', '')
            if topics:
                print(f"      Topics: {topics[:100]}...")
            metadata = task.get('metadata_json', '')
            if metadata:
                print(f"      Has metadata: Yes ({len(metadata)} chars)")
    else:
        print("⚠️ No schedule tasks found in database")
    
    # Step 5: Check monthly plans
    print("\nSTEP 5: Checking monthly plans...")
    print("-" * 50)
    
    plans = db.get_monthly_plans(TEST_USER_ID)
    if plans:
        print(f"✅ Found {len(plans)} monthly plans!")
        total_goals_chars = 0
        for plan in plans:
            goals_text = plan.get('goals_context', '')
            goals_len = len(goals_text)
            total_goals_chars += goals_len
            print(f"\n   📆 Month {plan.get('month_index', '?')} ({plan.get('status', '?')})")
            print(f"      Date Range: {plan.get('start_date', '?')[:10]} to {plan.get('end_date', '?')[:10]}")
            print(f"      Goals Length: {goals_len:,} chars {'✅ RICH' if goals_len > 3000 else '⚠️ SHORT'}")
            # Show first 300 chars of goals
            if goals_text:
                print(f"      Preview: {goals_text[:300]}...")
        print(f"\n   📊 Total goals content: {total_goals_chars:,} chars")
    else:
        print("⚠️ No monthly plans found")
    
    print("\n" + "="*60)
    print("✅ TEST COMPLETE")
    print("="*60)
    return True


async def test_proactive_content():
    """Test proactive content engine with new task format."""
    
    print("\n" + "="*60)
    print("🎯 PROACTIVE CONTENT ENGINE TEST")
    print("="*60 + "\n")
    
    from src.tools.proactive_content_engine import ProactiveContentEngine
    
    db = KairoDB()
    engine = ProactiveContentEngine(db=db)
    
    # Get schedule
    schedule = db.get_schedule(TEST_USER_ID, limit=5)
    
    if not schedule:
        print("⚠️ No schedule found. Run schedule generation first.")
        return
    
    print(f"📅 Found {len(schedule)} upcoming tasks\n")
    
    # Test content preparation for first task
    task = schedule[0]
    print(f"Testing content preparation for: {task.get('title')}")
    print(f"Content mode: {task.get('content_mode', 'deep_dive')}")
    
    try:
        prepared = await engine.prepare_content_for_rich_task(
            user_id=TEST_USER_ID,
            task=task,
            mastery_data=None
        )
        
        if prepared:
            print(f"\n✅ Content prepared!")
            print(f"   Content ID: {prepared.content_id}")
            print(f"   Subject: {prepared.subject}")
            print(f"   Duration: {prepared.duration_minutes} min")
            print(f"   Content types: {list(prepared.content_data.keys())}")
        else:
            print("⚠️ No content prepared (task may be blocked type)")
            
    except Exception as e:
        print(f"❌ Content preparation failed: {e}")
        import traceback
        traceback.print_exc()


def list_users():
    """Helper to list users in the database."""
    print("\n" + "="*60)
    print("👥 LISTING USERS (to find your user ID)")
    print("="*60 + "\n")
    
    db = KairoDB()
    
    try:
        from src.config import APPWRITE_DATABASE_ID, USERS_COL
        from appwrite.query import Query
        
        results = db.db.list_rows(
            database_id=APPWRITE_DATABASE_ID,
            table_id=USERS_COL,
            queries=[Query.limit(10)]
        )
        
        if results.get('total', 0) == 0:
            print("No users found in database.")
            return
        
        print(f"Found {results['total']} users:\n")
        for user in results.get('rows', []):
            user_id = user.get('userId', user.get('$id', '?'))
            email = user.get('email', 'N/A')
            name = user.get('name', 'N/A')
            has_profile = bool(user.get('studentprofile_json'))
            print(f"   ID: {user_id}")
            print(f"   Email: {email}")
            print(f"   Name: {name}")
            print(f"   Has Profile: {'✅' if has_profile else '❌'}")
            print()
            
    except Exception as e:
        print(f"❌ Failed to list users: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Test Kaironex Schedule Generation")
    parser.add_argument("--list-users", action="store_true", help="List users to find user ID")
    parser.add_argument("--content", action="store_true", help="Test proactive content engine")
    parser.add_argument("--user", type=str, help="Override test user ID")
    args = parser.parse_args()
    
    if args.user:
        TEST_USER_ID = args.user
    
    if args.list_users:
        list_users()
    elif args.content:
        asyncio.run(test_proactive_content())
    else:
        asyncio.run(test_schedule_generation())
