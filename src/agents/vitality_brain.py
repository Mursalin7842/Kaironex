import json
from ..utils.gemini_client import GeminiClient
from ..config import *

def run_vitality_agent(db_helper, payload, context):
    """
    Bio-Manager: Monitors health/energy.
    """
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "No userId"})

    context.log(f"🧬 Vitality Brain processing for {user_id}")
    db_helper.log_heartbeat(user_id, f"EVENT:VITALITY")
    
    ai = GeminiClient()
    
    # Inputs
    sleep_hours = payload.get('sleep_hours')
    steps = payload.get('steps', 0)
    
    # Determine Regen Mode
    regen_mode_active = False
    ai_advice = "Systems nominal."

    # Logic: Low Sleep
    if sleep_hours is not None and sleep_hours < 5:
        regen_mode_active = True
        prompt = f"User slept {sleep_hours}h. 1 sentence advice for low energy."
        ai_advice = ai.generate_response(prompt)
        
        db_helper.create_intervention(
            user_id, "LOW_SLEEP", ai_advice, strategy="EMPATHY"
        )
    
    # Logic: High Activity
    elif steps > 8000:
        prompt = f"User walked {steps} steps. 1 sentence hype message."
        ai_advice = ai.generate_response(prompt)
        # No voice intervention, just silent log/cache update
    
    # --- SYNC: UPDATE CACHE ---
    # Updates the 'vitality' key in users.studentState_json
    vitality_update = {
        "vitality": {
            "regen_mode": regen_mode_active,
            "bio_fuel_status": "Calculating...", # Placeholder for food logic
            "last_sleep": sleep_hours,
            "steps_today": steps,
            "daily_advice": ai_advice
        }
    }
    db_helper.update_state_cache(user_id, vitality_update)

    return context.res.json({"status": "vitality_synced", "advice": ai_advice})
