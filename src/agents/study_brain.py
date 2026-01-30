def run_study_agent(db_helper, payload, context):
    user_id = payload.get('userId')
    
    # 1. Log the Action
    action_log = f"EVENT:STUDY_LOG | Duration: {payload.get('duration_seconds')}s | Focus: {payload.get('focus_score')}"
    db_helper.log_heartbeat(user_id, action_log)
    
    context.log(f"🎓 Study Brain processing for {user_id}")

    # 2. Logic: Analyze Focus
    focus_score = payload.get('focus_score', 0)
    
    if focus_score > 0 and focus_score < 40:
        # Trigger Voice Intervention
        db_helper.create_intervention(
            user_id,
            "LOW_FOCUS_DETECTED",
            "I noticed your focus dropped below 40%. Do you want to try a 5-minute breathing box exercise?",
            strategy="NEGOTIATION"
        )
        return context.res.json({"status": "intervention_sent"})

    return context.res.json({"status": "log_analyzed_normal"})
