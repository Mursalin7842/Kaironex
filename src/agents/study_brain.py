import json
import datetime
from ..utils.gemini_client import GeminiClient
from ..config import *

def run_study_agent(db_helper, payload, context):
    """
    Cognitive Supply Chain & Control.
    Handles 'Real-World Study Session' & 'Knowledge Gatekeeper'.
    """
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "No userId"})

    context.log(f"🎓 Study Brain processing for {user_id}")
    
    # 1. Parse Data
    focus_score = payload.get('focus_score', 100)
    duration = payload.get('duration_seconds', 0)
    topic = payload.get('topic', 'General Study')
    status = payload.get('status', 'IN_PROGRESS')

    db_helper.log_heartbeat(user_id, f"EVENT:STUDY | Focus: {focus_score} | Status: {status}")
    
    ai = GeminiClient()
    response_actions = []

    # --- LOGIC A: FAILURE MOMENT (Drift) ---
    if status == 'IN_PROGRESS' and focus_score < 40:
        prompt = f"""
        Student studying '{topic}' has lost focus (Score: {focus_score}/100).
        Write a 1-sentence empathetic nudge to help them refocus or take a break.
        """
        ai_msg = ai.generate_response(prompt)
        
        db_helper.create_intervention(
            user_id, "FOCUS_DROP", ai_msg, strategy="NEGOTIATION"
        )
        response_actions.append("intervention_sent")

    # --- LOGIC B: KNOWLEDGE GATEKEEPER ---
    quiz_data = None
    if status == 'REQUEST_UNLOCK':
        prompt = f"""
        Generate a multiple choice question about '{topic}' to verify study.
        Return JSON: {{ "question": "...", "options": ["A", "B", "C"], "correct": "A" }}
        """
        raw_quiz = ai.generate_response(prompt)
        try:
            # Clean up potential markdown formatting
            raw_quiz = raw_quiz.replace('```json', '').replace('```', '')
            quiz_data = json.loads(raw_quiz)
        except:
            quiz_data = {"question": "What did you learn?", "options": ["Everything", "Nothing"], "correct": "Everything"}
        
        response_actions.append("quiz_generated")

    # --- LOGIC C: RESOURCE INGESTION (File Upload) ---
    resource_response = None
    if status == 'resource_ingestion' or payload.get('type') == 'resource_ingestion':
        title = payload.get('title', 'Untitled Resource')
        resource_type = payload.get('resourceType', 'document')
        drive_link = payload.get('driveLink', '')
        
        prompt = f"""
NEW LEARNING RESOURCE UPLOADED:
Title: {title}
Type: {resource_type}
Link: {drive_link[:100] if drive_link else 'Local upload'}

TASK:
1. Acknowledge the resource upload briefly
2. Suggest how this resource might fit into study planning
3. Provide one tip for using this type of resource effectively

Keep response to 2-3 sentences max. Be encouraging.
"""
        resource_response = ai.generate_response(prompt)
        response_actions.append("resource_ingested")
        context.log(f"📄 Resource processed: {title}")

    # --- SYNC: UPDATE CACHE ---
    state_update = {
        "study_session": {
            "is_active": (status == 'IN_PROGRESS'),
            "current_topic": topic,
            "current_focus": focus_score,
            "last_updated": datetime.datetime.now().isoformat()
        }
    }
    db_helper.update_state_cache(user_id, state_update)

    return context.res.json({
        "status": "study_processed", 
        "actions": response_actions,
        "quiz": quiz_data,
        "resource_response": resource_response
    })
