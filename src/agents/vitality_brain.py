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

def run_vitality_agent(db_helper, payload, context):
    """
    Bio-Manager: Monitors health/energy.
    """
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "No userId"})

    context.log(f"🧬 Vitality Brain processing for {user_id}")
    db_helper.log_heartbeat(user_id, f"EVENT:VITALITY")
    
    ai = GeminiClient()
    response_actions = []
    
    # Inputs
    sleep_hours = payload.get('sleep_hours')
    steps = payload.get('steps', 0)
    
    # Determine Regen Mode
    regen_mode_active = False
    ai_advice = "Systems nominal."
    prompt_used = None

    # Logic: Low Sleep
    if sleep_hours is not None and sleep_hours < 5:
        regen_mode_active = True
        prompt_used = f"User slept {sleep_hours}h. 1 sentence advice for low energy."
        ai_advice = ai.generate_response(prompt_used)
        
        # Create thought signature
        thought_id = _create_thought_signature(user_id, "vitality", prompt_used, ai_advice, db_helper, context)
        if thought_id:
            response_actions.append(f"thought:{thought_id}")
        
        db_helper.create_intervention(
            user_id, "LOW_SLEEP", ai_advice, strategy="EMPATHY"
        )
        response_actions.append("intervention_created")
    
    # Logic: High Activity
    elif steps > 8000:
        prompt_used = f"User walked {steps} steps. 1 sentence hype message."
        ai_advice = ai.generate_response(prompt_used)
        
        # Create thought signature
        thought_id = _create_thought_signature(user_id, "vitality", prompt_used, ai_advice, db_helper, context)
        if thought_id:
            response_actions.append(f"thought:{thought_id}")
    
    # --- SYNC: UPDATE CACHE ---
    vitality_update = {
        "vitality": {
            "regen_mode": regen_mode_active,
            "bio_fuel_status": "Calculating...",
            "last_sleep": sleep_hours,
            "steps_today": steps,
            "daily_advice": ai_advice
        }
    }
    db_helper.update_state_cache(user_id, vitality_update)

    context.log(f"✅ Vitality processed. Actions: {response_actions}")
    return context.res.json({"status": "vitality_synced", "advice": ai_advice, "actions": response_actions})
