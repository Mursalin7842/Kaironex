import json
from ..utils.gemini_client import GeminiClient

def run_vitality_agent(db_helper, payload, context):
    """
    The Bio-Manager.
    Triggered by: 'vitality_state'
    Purpose: Monitor health, sleep, and energy levels.
    """
    user_id = payload.get('userId')
    context.log(f"🧬 Vitality Brain processing for {user_id}")
    
    # 1. Log the Event
    vitality_summary = f"EVENT:VITALITY | Data: {str(payload)[:100]}"
    db_helper.log_heartbeat(user_id, vitality_summary)
    
    # 2. Initialize AI
    ai = GeminiClient()
    
    # 3. Logic: Sleep & Energy Analysis
    sleep_hours = payload.get('sleep_hours')
    steps = payload.get('steps', 0)
    
    # Scenario A: Low Sleep (Needs Empathy)
    if sleep_hours is not None and sleep_hours < 5:
        prompt = f"""
        User Data: Sleep: {sleep_hours} hours (Very Low). Steps: {steps}.
        Context: It is morning. The user is likely exhausted.
        Task: Write a short, warm, empathetic voice message (1 sentence). Suggest a slow start or rescheduling high-focus tasks.
        """
        ai_message = ai.generate_response(prompt)
        
        db_helper.create_intervention(
            user_id,
            "LOW_SLEEP_DETECTED",
            ai_message,
            strategy="EMPATHY"
        )
        return context.res.json({"status": "intervention_sent", "ai_reply": ai_message})

    # Scenario B: High Activity (Needs Hype)
    if steps > 8000:
        prompt = f"""
        User Data: Steps: {steps}. The user is active and moving.
        Task: Write a short, high-energy hype message (1 sentence). Congratulate them on hitting momentum.
        """
        ai_message = ai.generate_response(prompt)
        
        db_helper.create_intervention(
            user_id,
            "HIGH_ACTIVITY_DETECTED",
            ai_message,
            strategy="ENERGETIC"
        )
        return context.res.json({"status": "intervention_sent", "ai_reply": ai_message})

    return context.res.json({"status": "vitality_logged"})
