import json
import datetime
import hashlib
import uuid
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
        db_helper.update_agent_memory_full(user_id, thought_sig_dict=thought_data, active_agents=agent, reasoning_mode='DEEP')
        
        if context: context.log(f"🧠 Thought signature stored: {thought_id}")
        return thought_id
    except Exception as e:
        print(f"❌ Thought signature error: {e}")
        return None

def run_campaign_agent(db_helper, payload, context):
    """
    Career Strategist: Goals & Schedule.
    """
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "No userId"})

    context.log(f"⚔️ Campaign Brain processing for {user_id}")
    db_helper.log_heartbeat(user_id, f"EVENT:CAMPAIGN | {payload.get('type')}")
    
    ai = GeminiClient()
    event_type = payload.get('type')
    
    campaign_data = {}
    response_actions = []

    if event_type == 'new_goal':
        goal_title = payload.get('goal_title', 'New Project')
        
        prompt = f"User set a goal: {goal_title}. Give 1 strategic first step."
        strategy = ai.generate_response(prompt)
        
        # Create thought signature
        thought_id = _create_thought_signature(user_id, "campaign", prompt, strategy, db_helper, context)
        if thought_id:
            response_actions.append(f"thought:{thought_id}")
        
        db_helper.create_intervention(
            user_id, "NEW_GOAL", strategy, strategy="PROACTIVE"
        )
        response_actions.append("intervention_created")
        
        campaign_data["active_quest"] = goal_title
        campaign_data["current_strategy"] = strategy

    elif event_type == 'schedule_update':
        campaign_data["schedule_status"] = "Optimized"

    # --- SYNC: UPDATE CACHE ---
    if campaign_data:
        update_payload = {"campaign": campaign_data}
        db_helper.update_state_cache(user_id, update_payload)

    context.log(f"✅ Campaign processed. Actions: {response_actions}")
    return context.res.json({"status": "campaign_synced", "actions": response_actions})
