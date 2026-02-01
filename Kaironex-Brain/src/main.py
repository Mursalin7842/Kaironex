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
    trigger_event = os.environ.get('APPWRITE_FUNCTION_EVENT', 'cron_schedule')
    
    # Safe Payload Parsing
    try:
        if not context.req.body:
            payload = {}
        elif isinstance(context.req.body, str):
            payload = json.loads(context.req.body)
        else:
            payload = context.req.body
    except Exception as e:
        context.log(f"Payload Note: {e}")
        payload = {}

    context.log(f"🧠 KAIRO AWAKE. Trigger: {trigger_event}")

    # 2. ROUTING LOGIC
    
    # --- A. EVENT DRIVEN ---
    if 'study_logs' in trigger_event:
        return run_study_agent(db_helper, payload, context)
        
    elif 'vitality_state' in trigger_event:
        return run_vitality_agent(db_helper, payload, context)

    elif 'radius_state' in trigger_event:
        return run_radius_agent(db_helper, payload, context)

    # Catches 'schedule' updates OR 'campaign' triggers
    # Ensures CRON jobs don't accidentally trigger this agent
    elif ('schedule' in trigger_event and 'cron' not in trigger_event) or \
         'profile' in trigger_event or 'campaign' in trigger_event:
        return run_campaign_agent(db_helper, payload, context)
        
    # --- B. CRON DRIVEN (Safety Net) ---
    else:
        # Default fallback for 'cron_schedule' or unknown triggers
        return run_supervisor(db_helper, context)

    return context.res.json({"status": "no_action_needed"})
