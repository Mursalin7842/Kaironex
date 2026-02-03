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

def run_radius_agent(db_helper, payload, context):
    """
    Habitat Manager: Location & Environment.
    """
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "No userId"})

    context.log(f"🧭 Radius Brain processing for {user_id}")
    
    location = payload.get('user_location', 'Unknown')
    db_helper.log_heartbeat(user_id, f"EVENT:RADIUS | {location}")
    
    ai = GeminiClient()
    response_actions = []
    
    # Determine Mode based on location string
    mode = "STANDARD"
    strategy = "NEUTRAL"
    
    if "library" in location.lower():
        mode = "DEEP_FOCUS"
        strategy = "WHISPER"
    elif "gym" in location.lower():
        mode = "WORKOUT"
        strategy = "ENERGETIC"
    elif "home" in location.lower():
        mode = "REST"
        strategy = "WARM"

    # If location changed significantly, trigger a voice welcome
    if payload.get('trigger_voice', False):
        prompt = f"User arrived at {location}. Write a 1-sentence welcome."
        msg = ai.generate_response(prompt)
        
        # Create thought signature
        thought_id = _create_thought_signature(user_id, "radius", prompt, msg, db_helper, context)
        if thought_id:
            response_actions.append(f"thought:{thought_id}")
        
        db_helper.create_intervention(user_id, "LOCATION_CHANGE", msg, strategy=strategy)
        response_actions.append("intervention_created")

    # --- SYNC: UPDATE CACHE ---
    radius_update = {
        "radius": {
            "current_location": location,
            "active_mode": mode,
            "safehouse_status": "SECURE"
        }
    }
    db_helper.update_state_cache(user_id, radius_update)

    context.log(f"✅ Radius processed. Mode: {mode}, Actions: {response_actions}")
    return context.res.json({"status": "radius_synced", "mode": mode, "actions": response_actions})
