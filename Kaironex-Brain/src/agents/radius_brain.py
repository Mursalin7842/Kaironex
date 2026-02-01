import json
from ..utils.gemini_client import GeminiClient
from ..config import *

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

    # If location changed significantly, maybe trigger a voice welcome
    if payload.get('trigger_voice', False):
        prompt = f"User arrived at {location}. Write a 1-sentence welcome."
        msg = ai.generate_response(prompt)
        db_helper.create_intervention(user_id, "LOCATION_CHANGE", msg, strategy=strategy)

    # --- SYNC: UPDATE CACHE ---
    radius_update = {
        "radius": {
            "current_location": location,
            "active_mode": mode,
            "safehouse_status": "SECURE"
        }
    }
    db_helper.update_state_cache(user_id, radius_update)

    return context.res.json({"status": "radius_synced", "mode": mode})
