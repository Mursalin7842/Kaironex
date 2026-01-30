import json
from ..utils.gemini_client import GeminiClient

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
    
    # 2. Initialize AI
    ai = GeminiClient()
    
    # 3. Logic: Location Context
    if location:
        prompt = f"""
        User Status: Arrived at location '{location}'.
        Role: Context-aware AI Assistant.
        Task: Write a 1-sentence message suitable for this environment.
        - If Gym: High energy, hype them up for the workout.
        - If Library/Office: Calm, suggest focus mode.
        - If Home: Welcoming, suggest rest or preparation for tomorrow.
        """
        
        ai_message = ai.generate_response(prompt)
        
        # Determine strategy based on location for the Voice Engine
        strategy = "NEUTRAL"
        if "gym" in location.lower(): strategy = "ENERGETIC"
        elif "library" in location.lower(): strategy = "WHISPER"
        elif "home" in location.lower(): strategy = "WARM"
        
        db_helper.create_intervention(
            user_id,
            f"LOCATION_{location.upper()}",
            ai_message,
            strategy=strategy
        )
        return context.res.json({"status": "location_adapted", "ai_reply": ai_message})

    return context.res.json({"status": "radius_processed"})
