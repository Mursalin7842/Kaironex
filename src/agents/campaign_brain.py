import json
from ..utils.gemini_client import GeminiClient

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
    
    # 2. Initialize AI
    ai = GeminiClient()
    
    # 3. Logic: Analyze Goal/Schedule Updates
    event_type = payload.get('type')
    
    if event_type == 'new_goal':
        goal_title = payload.get('goal_title', 'Unknown Goal')
        
        # Ask Gemini to be a Strategist
        prompt = f"""
        User Context: The user just set a new high-level goal: '{goal_title}'.
        Role: You are an elite career strategist and productivity coach.
        Task: Write a 1-sentence, punchy, motivating response. Validate the goal and propose an immediate first step (e.g., breaking it down or scheduling a deep work block).
        """
        
        ai_message = ai.generate_response(prompt)
        
        db_helper.create_intervention(
            user_id,
            "NEW_GOAL_SET",
            ai_message,
            strategy="PROACTIVE"
        )
        return context.res.json({"status": "goal_acknowledged", "ai_reply": ai_message})

    return context.res.json({"status": "campaign_processed"})
