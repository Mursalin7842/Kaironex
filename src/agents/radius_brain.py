def run_radius_agent(db_helper, payload, context):
    """
    The Guide.
    Triggered by: 'radius_state' (Location/Context changes)
    Purpose: Context-aware assistance based on where the user is (Gym, Office, Home).
    """
    user_id = payload.get('userId')
    context.log(f"🧭 Radius Brain processing for {user_id}")
    
    # 1. Log the Event
    location = payload.get('location', 'Unknown')
    radius_summary = f"EVENT:RADIUS | Location: {location}"
    db_helper.log_heartbeat(user_id, radius_summary)
    
    # 2. Logic (Placeholder)
    # Example: Arrived at Gym
    if location.lower() == 'gym':
        db_helper.create_intervention(
            user_id,
            "LOCATION_GYM",
            "You're at the gym! Power mode activated. Want me to queue your workout playlist?",
            strategy="ENERGETIC"
        )
        return context.res.json({"status": "gym_mode_activated"})

    return context.res.json({"status": "radius_processed"})
