import os
import json
from .utils.db_helper import KairoDB
from .agents.main_brain import run_supervisor
from .agents.study_brain import run_study_agent
from .agents.vitality_brain import run_vitality_agent
from .agents.campaign_brain import run_campaign_agent
from .agents.radius_brain import run_radius_agent

def main(context):
    db_helper = KairoDB()
    
    # 1. GET TRIGGER INFO
    # Appwrite passes the event name (e.g., "databases...create")
    trigger_event = os.environ.get('APPWRITE_FUNCTION_EVENT', 'cron_schedule')
    
    # The data that changed (parsed safely)
    # Safe Payload Parsing
    try:
        if isinstance(context.req.body, str):
            payload = json.loads(context.req.body)
        else:
            payload = context.req.body # It's already a dict
    except Exception as e:
        context.error(f"Payload Error: {e}")
        payload = {}

    context.log(f"🧠 KAIRO AWAKE. Trigger: {trigger_event}")

    # 2. ROUTING LOGIC
    
    # --- A. EVENT DRIVEN (Real-Time) ---
    if 'study_logs' in trigger_event:
        return run_study_agent(db_helper, payload, context)
        
    elif 'vitality_state' in trigger_event:
        return run_vitality_agent(db_helper, payload, context)

    elif 'schedule' in trigger_event or 'profile' in trigger_event:
        return run_campaign_agent(db_helper, payload, context)

    elif 'radius_state' in trigger_event:
        return run_radius_agent(db_helper, payload, context)
        
    # --- B. CRON DRIVEN (Safety Net) ---
    else:
        # Runs every hour to check for drift
        return run_supervisor(db_helper, context)

    return context.res.json({"status": "no_action_needed"})
