import json
from ..utils.gemini_client import GeminiClient
from ..config import *

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

    if event_type == 'new_goal':
        goal_title = payload.get('goal_title', 'New Project')
        
        prompt = f"User set a goal: {goal_title}. Give 1 strategic first step."
        strategy = ai.generate_response(prompt)
        
        db_helper.create_intervention(
            user_id, "NEW_GOAL", strategy, strategy="PROACTIVE"
        )
        
        campaign_data["active_quest"] = goal_title
        campaign_data["current_strategy"] = strategy

    elif event_type == 'schedule_update':
        # Logic to re-optimize schedule could go here
        campaign_data["schedule_status"] = "Optimized"

    # --- SYNC: UPDATE CACHE ---
    if campaign_data:
        update_payload = {"campaign": campaign_data}
        db_helper.update_state_cache(user_id, update_payload)

    return context.res.json({"status": "campaign_synced"})
