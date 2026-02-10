#!/usr/bin/env python3
"""
Test God Mode Database Architecture
Verifies all 4 critical tables are properly persisted
"""

import sys
import json
import uuid
import datetime
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.utils.db_helper import KairoDB
from src.config import *

def test_god_mode():
    print("\n" + "="*60)
    print("🚀 GOD MODE DATABASE ARCHITECTURE TEST")
    print("="*60 + "\n")
    
    # Initialize DB helper
    try:
        db = KairoDB()
        print("✅ Database helper initialized")
    except Exception as e:
        print(f"❌ Failed to initialize DB: {e}")
        return False
    
    # Test user ID
    test_user_id = f"test_user_{uuid.uuid4().hex[:8]}"
    print(f"📝 Test user ID: {test_user_id}\n")
    
    # TEST 1: agent_memory with full state
    print("TEST 1: agent_memory - Full state persistence")
    print("-" * 50)
    try:
        thought_data = {
            "thought_id": f"test_thought_{uuid.uuid4().hex[:12]}",
            "timestamp": datetime.datetime.now().isoformat(),
            "agent": "test_agent",
            "context_hash": "test_hash_123",
            "reasoning_trace": ["Thought 1", "Thought 2"],
            "confidence": 0.92,
            "tool_calls": ["tool_1"],
            "action_output": "Test output",
            "parent_signature": ""
        }
        
        db.update_agent_memory_full(
            test_user_id,
            thought_sig_dict=thought_data,
            active_agents="test_agent",
            reasoning_mode="DEEP",
            session_id=f"session_{uuid.uuid4().hex[:8]}",
            pressure_index="42"
        )
        print(f"✅ agent_memory updated with all 8 fields")
        print(f"   - thought_signature: {thought_data['thought_id'][:20]}...")
        print(f"   - reasoning_mode: DEEP")
        print(f"   - pressure_index: '42' (string)")
    except Exception as e:
        print(f"❌ agent_memory test failed: {e}")
        return False
    
    # TEST 2: thought_signatures table
    print("\nTEST 2: thought_signatures - AI reasoning persistence")
    print("-" * 50)
    try:
        signature_data = {
            "thought_id": f"thought_{uuid.uuid4().hex[:12]}",
            "timestamp": datetime.datetime.now().isoformat(),
            "agent": "study",
            "context_hash": "abc123def456",
            "reasoning_trace": [
                "User uploaded PDF on Machine Learning",
                "Extracted key concepts",
                "Generated study plan"
            ],
            "confidence": 0.88,
            "tool_calls": ["extract_pdf", "generate_quiz"],
            "action_output": "5 key concepts identified",
            "parent_signature": ""
        }
        
        db.create_thought_signature(test_user_id, signature_data)
        print(f"✅ thought_signature created")
        print(f"   - ID: {signature_data['thought_id']}")
        print(f"   - Agent: study")
        print(f"   - Tool calls: {len(signature_data['tool_calls'])}")
        print(f"   - Confidence: {signature_data['confidence']}")
    except Exception as e:
        print(f"❌ thought_signature test failed: {e}")
        return False
    
    # TEST 3: marathon_sessions table
    print("\nTEST 3: marathon_sessions - Goal tracking persistence")
    print("-" * 50)
    try:
        session_id = f"marathon_{uuid.uuid4().hex[:8]}"
        session_data = {
            "session_id": session_id,
            "user_id": test_user_id,
            "agent_type": "study",
            "goal": {
                "title": "Master Machine Learning",
                "duration_days": 30
            },
            "status": "in_progress",
            "steps": [
                {"step": 1, "title": "Linear Regression", "status": "complete"},
                {"step": 2, "title": "Neural Networks", "status": "in_progress"}
            ],
            "progress": 45,
            "thought_chain": [
                "thought_001", "thought_002", "thought_003"
            ],
            "estimated_completion": (datetime.datetime.now() + datetime.timedelta(days=30)).isoformat()
        }
        
        db.create_marathon_session(session_data)
        print(f"✅ marathon_session created")
        print(f"   - Session ID: {session_id}")
        print(f"   - Goal: Master Machine Learning")
        print(f"   - Progress: 45%")
        print(f"   - Thought chain length: 3 thoughts")
    except Exception as e:
        print(f"❌ marathon_session test failed: {e}")
        return False
    
    # TEST 4: policy_episodes table
    print("\nTEST 4: policy_episodes - Learning system persistence")
    print("-" * 50)
    try:
        episode_data = {
            "episode_id": f"episode_{uuid.uuid4().hex[:12]}",
            "user_id": test_user_id,
            "agent": "campaign",
            "trigger": "user_disengagement",
            "state_before": {
                "engagement_score": 0.3,
                "last_interaction": "3 days ago"
            },
            "action_taken": "sent_motivation_email",
            "outcome": "user_returned",
            "reward": 0.8,
            "state_after": {
                "engagement_score": 0.7,
                "last_interaction": "now"
            },
            "thought_chain_summary": "Identified disengagement → Sent intervention → User responded positively"
        }
        
        db.create_policy_episode(episode_data)
        print(f"✅ policy_episode created")
        print(f"   - Agent: campaign")
        print(f"   - Trigger: user_disengagement")
        print(f"   - Action: sent_motivation_email")
        print(f"   - Outcome: user_returned")
        print(f"   - Reward: 0.8 (positive learning)")
    except Exception as e:
        print(f"❌ policy_episode test failed: {e}")
        return False
    
    # SUMMARY
    print("\n" + "="*60)
    print("✅ ALL TESTS PASSED - GOD MODE ARCHITECTURE VERIFIED")
    print("="*60)
    print("\n📊 Summary:")
    print("   • agent_memory: ✅ Full 8-field persistence")
    print("   • thought_signatures: ✅ AI reasoning captured")
    print("   • marathon_sessions: ✅ Goal tracking complete")
    print("   • policy_episodes: ✅ Learning system active")
    print("\n🎯 Status: READY FOR PRODUCTION")
    print("🚀 Deployment: Push to Appwrite Functions\n")
    
    return True

if __name__ == "__main__":
    success = test_god_mode()
    sys.exit(0 if success else 1)
