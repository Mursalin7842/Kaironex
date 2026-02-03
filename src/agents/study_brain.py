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
    if not user_id: 
        context.error("❌ No userId in payload!")
        return context.res.json({"error": "No userId"})

    context.log(f"🎓 Study Brain processing for {user_id}")
    context.log(f"📋 Full payload keys: {list(payload.keys())}")
    
    # 1. Parse Data
    focus_score = payload.get('focus_score', 100)
    duration = payload.get('duration_seconds', 0)
    topic = payload.get('topic', 'General Study')
    status = payload.get('status', payload.get('type', 'IN_PROGRESS'))
    
    context.log(f"📊 Status resolved to: {status}")

    db_helper.log_heartbeat(user_id, f"EVENT:STUDY | Focus: {focus_score} | Status: {status}")
    
    ai = GeminiClient()
    response_actions = []
    resource_response = None
    quiz_data = None

    # --- LOGIC A: FAILURE MOMENT (Drift) ---
    if status == 'IN_PROGRESS' and focus_score < 40:
        context.log("⚠️ Focus drop detected, generating intervention...")
        prompt = f"""
        Student studying '{topic}' has lost focus (Score: {focus_score}/100).
        Write a 1-sentence empathetic nudge to help them refocus or take a break.
        """
        try:
            ai_msg = ai.generate_response(prompt)
            context.log(f"🤖 Focus intervention: {ai_msg[:100]}...")
            db_helper.create_intervention(
                user_id, "FOCUS_DROP", ai_msg, strategy="NEGOTIATION"
            )
            response_actions.append("intervention_sent")
        except Exception as e:
            context.error(f"❌ Focus intervention error: {e}")

    # --- LOGIC B: KNOWLEDGE GATEKEEPER ---
    if status == 'REQUEST_UNLOCK':
        context.log("🔐 Quiz requested, generating question...")
        prompt = f"""
        Generate a multiple choice question about '{topic}' to verify study.
        Return JSON: {{ "question": "...", "options": ["A", "B", "C"], "correct": "A" }}
        """
        try:
            raw_quiz = ai.generate_response(prompt)
            context.log(f"🤖 Raw quiz response: {raw_quiz[:200]}...")
            raw_quiz = raw_quiz.replace('```json', '').replace('```', '')
            quiz_data = json.loads(raw_quiz)
            response_actions.append("quiz_generated")
        except Exception as e:
            context.error(f"❌ Quiz generation error: {e}")
            quiz_data = {"question": "What did you learn?", "options": ["Everything", "Nothing"], "correct": "Everything"}

    # --- LOGIC C: RESOURCE INGESTION (File Upload) ---
    if status == 'resource_ingestion' or payload.get('type') == 'resource_ingestion':
        title = payload.get('title', 'Untitled Resource')
        resource_type = payload.get('resourceType', payload.get('resource_type', 'document'))
        drive_link = payload.get('driveLink', payload.get('url', ''))
        
        context.log(f"📄 Processing resource: {title}")
        context.log(f"📄 Type: {resource_type}, Link: {drive_link[:50] if drive_link else 'Local'}")
        
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
        try:
            context.log("🤖 Calling Gemini for resource analysis...")
            resource_response = ai.generate_response(prompt)
            context.log(f"✅ Gemini response received: {len(resource_response)} chars")
            context.log(f"🤖 AI Response: {resource_response[:300]}...")
            response_actions.append("resource_analyzed")
            
            # CRITICAL: Create intervention so app can see the response!
            context.log("💾 Creating intervention in database...")
            db_helper.create_intervention(
                user_id, 
                "RESOURCE_UPLOADED", 
                resource_response,
                status="PENDING",
                strategy="CELEBRATION"
            )
            context.log("✅ Intervention created successfully!")
            response_actions.append("intervention_created")
            
        except Exception as e:
            context.error(f"❌ Resource processing error: {str(e)}")
            resource_response = f"Error: {str(e)}"
            response_actions.append("error")

    # --- SYNC: UPDATE CACHE ---
    context.log("🔄 Updating state cache...")
    state_update = {
        "study_session": {
            "is_active": (status == 'IN_PROGRESS'),
            "current_topic": topic,
            "current_focus": focus_score,
            "last_updated": datetime.datetime.now().isoformat()
        }
    }
    
    if resource_response:
        state_update["last_resource_analysis"] = resource_response[:500]
    
    db_helper.update_state_cache(user_id, state_update)
    context.log(f"✅ State cache updated. Actions: {response_actions}")

    return context.res.json({
        "status": "study_processed", 
        "actions": response_actions,
        "quiz": quiz_data,
        "resource_response": resource_response,
        "debug": {
            "user_id": user_id,
            "status_resolved": status,
            "actions_count": len(response_actions)
        }
    })
