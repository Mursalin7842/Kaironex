import datetime
import hashlib
import uuid
from appwrite.query import Query
from ..utils.gemini_client import GeminiClient
from ..config import *

def _create_thought_signature(user_id, agent, prompt, response, db_helper, context=None):
    """Create and store thought signature - shared helper."""
    try:
        thought_id = f"thought_{uuid.uuid4().hex[:12]}"
        timestamp = datetime.datetime.now().isoformat()
        context_hash = hashlib.sha256(f"{user_id}:{prompt[:200]}:{timestamp}".encode()).hexdigest()[:16]
        
        thought_data = {
            "thought_id": thought_id, "timestamp": timestamp, "agent": agent,
            "context_hash": context_hash,
            "reasoning_trace": [f"Prompt: {prompt[:100]}...", f"Response: {response[:200]}..."],
            "confidence": 0.85, "tool_calls": [], "action_output": response[:500], "parent_signature": ""
        }
        
        db_helper.create_thought_signature(user_id, thought_data)
        db_helper.update_agent_memory_full(user_id, thought_sig_dict=thought_data, active_agents=agent, reasoning_mode='SYSTEM')
        
        if context: context.log(f"🧠 Thought signature stored: {thought_id}")
        return thought_id
    except Exception as e:
        print(f"❌ Thought signature error: {e}")
        return None

def run_supervisor(db_helper, context):
    """
    Supervisor: Checks for 'Drift' (User silence) and runs audits.
    Run via CRON.
    """
    context.log("🛡️ Supervisor: Running Safety Checks...")
    
    # 1. Fetch active users (via Agent Memory)
    # Limit 100 for safety
    try:
        memories = db_helper.db.list_rows(
            database_id=APPWRITE_DATABASE_ID, table_id=AGENT_MEMORY_COL, queries=[Query.limit(100)]
        )
    except Exception as e:
        context.log(f"❌ Failed to fetch memories: {e}")
        return context.res.json({"error": str(e)})

    ai = GeminiClient()
    interventions_triggered = 0
    thoughts_created = 0
    now = datetime.datetime.now(datetime.timezone.utc)

    for mem in memories.get('documents', memories.get('rows', [])):
        user_id = mem.get('userId')
        if not user_id:
            continue
            
        last_active_str = mem.get('last_active')
        
        # Check Drift
        if last_active_str:
            try:
                # Basic parsing, handle TZ
                last_active = datetime.datetime.fromisoformat(last_active_str)
                if last_active.tzinfo is None:
                    last_active = last_active.replace(tzinfo=datetime.timezone.utc)
                
                diff_mins = (now - last_active).total_seconds() / 60
                
                # Logic: If silent > 4 hours, check in
                if diff_mins > 240:
                    context.log(f"User {user_id} drift detected ({int(diff_mins)}m).")
                    
                    prompt = f"User has been silent for {int(diff_mins)} minutes. Write a gentle check-in."
                    msg = ai.generate_response(prompt)
                    
                    # Create thought signature for this AI reasoning
                    thought_id = _create_thought_signature(user_id, "supervisor", prompt, msg, db_helper, context)
                    if thought_id:
                        thoughts_created += 1
                    
                    db_helper.create_intervention(
                        user_id, "DRIFT_CHECK", msg, strategy="WARM"
                    )
                    interventions_triggered += 1
            except Exception as e:
                context.log(f"Date parse error for {user_id}: {e}")

    context.log(f"✅ Supervisor complete. Interventions: {interventions_triggered}, Thoughts: {thoughts_created}")
    return context.res.json({
        "status": "supervisor_complete",
        "interventions": interventions_triggered,
        "thoughts_created": thoughts_created
    })
