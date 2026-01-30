def run_campaign_agent(db_helper, payload, context):
    """
    The Career Coach.
    Triggered by: 'schedule' or 'profile' updates (or custom 'campaign_logs')
    Purpose: Manage long-term goals, career progression, and daily schedule alignment.
    """
    user_id = payload.get('userId')
    context.log(f"⚔️ Campaign Brain processing for {user_id}")
    
    # 1. Log the Event
    campaign_summary = f"EVENT:CAMPAIGN | Type: {payload.get('type', 'unknown')}"
    db_helper.log_heartbeat(user_id, campaign_summary)
    
    # 2. Logic (Placeholder)
    # Example: If a new big goal is added, offer a breakdown
    event_type = payload.get('type')
    if event_type == 'new_goal':
        goal_title = payload.get('goal_title', 'Unknown Goal')
        db_helper.create_intervention(
            user_id,
            "NEW_GOAL_SET",
            f"That's a great goal: '{goal_title}'. Do you want me to draft a weekly plan for it?",
            strategy="PROACTIVE"
        )
        return context.res.json({"status": "goal_acknowledged"})

    return context.res.json({"status": "campaign_processed"})
