def run_vitality_agent(db_helper, payload, context):
    """
    The Bio-Manager.
    Triggered by: 'vitality_state'
    Purpose: Monitor health, sleep, and energy levels.
    """
    user_id = payload.get('userId')
    context.log(f"🧬 Vitality Brain processing for {user_id}")
    
    # 1. Log the Event
    # payload might contain: steps, sleep_hours, energetic_state
    vitality_summary = f"EVENT:VITALITY | Data: {str(payload)[:100]}"
    db_helper.log_heartbeat(user_id, vitality_summary)
    
    # 2. Logic (Placeholder)
    # If sleep is low, trigger intervention
    sleep_hours = payload.get('sleep_hours')
    if sleep_hours is not None and sleep_hours < 5:
        db_helper.create_intervention(
            user_id,
            "LOW_SLEEP_DETECTED",
            "I see you didn't get much sleep. Should we lighten the schedule for this morning?",
            strategy="EMPATHY"
        )
        return context.res.json({"status": "intervention_sent"})

    return context.res.json({"status": "vitality_logged"})
