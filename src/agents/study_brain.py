import json
# Import the client we just updated
from ..utils.gemini_client import GeminiClient

def run_study_agent(db_helper, payload, context):
    user_id = payload.get('userId')
    
    # 1. Log the Action
    action_log = f"EVENT:STUDY_LOG | Duration: {payload.get('duration_seconds')}s | Focus: {payload.get('focus_score')}"
    db_helper.log_heartbeat(user_id, action_log)
    
    context.log(f"🎓 Study Brain processing for {user_id}")

    # 2. Initialize AI (Gemini 3 Flash)
    ai = GeminiClient()

    # 3. Logic: Analyze Focus
    focus_score = payload.get('focus_score', 0)
    
    if focus_score > 0 and focus_score < 40:
        # --- NEW: ASK GEMINI TO WRITE THE MESSAGE ---
        
        prompt = f"""
        The student is struggling.
        Current Focus Score: {focus_score}/100.
        Context: They have been studying for {payload.get('duration_seconds', 0) // 60} minutes.
        
        Task: Write a short, empathetic, but firm 1-sentence voice message suggesting a 5-minute break.
        """
        
        # Generate dynamic response
        ai_message = ai.generate_response(prompt)
        
        # Trigger Voice Intervention with AI text
        db_helper.create_intervention(
            user_id,
            "LOW_FOCUS_DETECTED",
            ai_message, # <--- Now sending real AI text
            strategy="NEGOTIATION"
        )
        return context.res.json({"status": "intervention_sent", "ai_reply": ai_message})

    return context.res.json({"status": "log_analyzed_normal"})
