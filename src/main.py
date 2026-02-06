"""
🧠 KAIRONEX DEEP BRAIN
======================
Appwrite Functions Entry Point

Architecture:
- This is the DEEP BRAIN - complex reasoning, planning, marathon tasks
- REFLEX is handled by the mobile app (not here)
- Communicates with app via Appwrite Database (no WebSocket needed)

Triggers:
- Database events (study_logs, vitality_state, etc.)
- CRON schedule (supervisor checks)
- HTTP requests (marathon tasks)
"""

import os
import json
from typing import Dict, Any, Optional

from .utils.db_helper import KairoDB
from .agents.main_brain import run_supervisor
from .agents.study_agent import run_study_agent
from .agents.vitality_agent import run_vitality_agent
from .agents.campaign_agent import run_campaign_agent
from .agents.radius_agent import run_radius_agent
from .config import validate_config


def main(context):
    """
    Main entry point for Appwrite Functions.
    
    Routes requests to appropriate agent based on trigger event.
    """
    
    # Validate configuration
    is_valid, errors = validate_config()
    if not is_valid:
        context.error(f"❌ Configuration errors: {errors}")
        return context.res.json({
            "status": "error",
            "message": "Configuration invalid",
            "errors": errors
        }, 500)
    
    # Initialize database helper
    try:
        db = KairoDB()
    except Exception as e:
        context.error(f"❌ Database connection failed: {e}")
        return context.res.json({
            "status": "error",
            "message": f"Database connection failed: {str(e)}"
        }, 500)
    
    # Parse trigger event
    trigger_event = os.environ.get('APPWRITE_FUNCTION_EVENT', '')
    request_path = context.req.path if hasattr(context.req, 'path') else ''
    request_method = context.req.method if hasattr(context.req, 'method') else 'POST'
    
    # Parse payload safely
    payload = _parse_payload(context)
    
    context.log(f"🧠 DEEP BRAIN ACTIVE | Trigger: {trigger_event} | Path: {request_path}")
    context.log(f"📦 Payload: {json.dumps(payload)[:500]}")
    
    # ==========================================================================
    # ROUTING LOGIC
    # ==========================================================================
    
    # ==========================================================================
    # ROUTING LOGIC
    # ==========================================================================
    
    collection_id = payload.get('$collectionId', '')
    
    # --- DATABASE EVENT TRIGGERS (High Priority) ---
    
    # 1. PROFILE UPDATE (User filled intake form)
    if 'users' in trigger_event or collection_id == 'users':
        user_id = payload.get('userId') or payload.get('$id')
        resource_count = db.count_user_resources(user_id)
        if resource_count > 0:
            context.log(f"✅ Profile ready + {resource_count} Files detected. Triggering Campaign Agent.")
            return run_campaign_agent(db, payload, context)
        else:
            context.log("⏳ Profile updated, but waiting for academic files (Google Drive/Uploads) before Deep Brain trigger.")
            return context.res.json({"status": "waiting_for_files"})

    # 2. RESOURCES UPDATE (User uploaded files)
    if 'resources' in trigger_event or collection_id == 'resources':
        context.log("📄 Resource uploaded found in payload.")
        user_id = payload.get('userId')
        title = payload.get('title', 'Unknown')
        
        # Ingest the resource first (Study Brain)
        payload['type'] = 'resource_ingestion'
        study_result = run_study_agent(db, payload, context)
        
        # Extract the resource response from study_result
        # study_result is a Response object, we need to get its data
        
        # Ingest the resource first (Study Brain)
        payload['type'] = 'resource_ingestion'
        # DISABLING PER-FILE TRIGGER to avoid double-cost. 
        # The Schedule Agent will process these files in batch when user clicks "Finish".
        # study_result = run_study_agent(db, payload, context)
        study_result = {"success": True, "message": "Ingestion skipped for batch processing."}
        
        # Note: We rely on the Study Agent to process the file.
        # We do NOT trigger Campaign Agent automatically here to prevent:
        # 1. Race conditions on multiple file uploads
        # 2. Redundant token usage (CampaignAgent has no 'resource_ingestion' handler)
        # 3. Accidental data resets logic
        
        context.log(f"✅ Resource processing initiated for: {title}")
        return study_result

    # 3. OTHER STATE UPDATES
    if 'study_logs' in trigger_event or collection_id == 'study_logs':
        context.log("📚 Routing to Study Agent")
        return run_study_agent(db, payload, context)
    
    if 'vitality_state' in trigger_event or collection_id == 'vitality_state':
        context.log("⚡ Routing to Vitality Agent")
        return run_vitality_agent(db, payload, context)
    
    if 'radius_state' in trigger_event or collection_id == 'radius_state':
        context.log("🌍 Routing to Radius Agent")
        return run_radius_agent(db, payload, context)
    
    if any(x in trigger_event for x in ['schedule', 'campaign']):
        if 'cron' not in trigger_event:
            context.log("⚔️ Routing to Campaign Agent")
            return run_campaign_agent(db, payload, context)

    # --- HTTP API ROUTES (for app to call directly) ---
    if request_path and request_path != '/':
        return _handle_http_request(db, context, request_path, request_method, payload)
    
    # --- MANUAL TEST FROM CONSOLE / GENERIC FALLBACK ---
    if request_path in ['/', ''] and payload:
        event_type = payload.get('type', payload.get('trigger', ''))
        context.log(f"🧪 Console test / Generic trigger detected | Event: {event_type}")
        
        # Route based on payload type
        if event_type in ['session_start', 'session_end', 'focus_update', 'quiz_request', 'IN_PROGRESS', 'COMPLETED', 'resource_ingestion', 'schedule_request']:
            context.log("📚 Routing to Study Agent (console test)")
            return run_study_agent(db, payload, context)
        elif event_type in ['sleep_update', 'meal_logged', 'health_check']:
            context.log("⚡ Routing to Vitality Agent (console test)")
            return run_vitality_agent(db, payload, context)
        elif event_type in ['location_update', 'social_event']:
            context.log("🌍 Routing to Radius Agent (console test)")
            return run_radius_agent(db, payload, context)
        elif event_type in ['campaign_calibration', 'campaign_init']:
            context.log("⚔️ Routing to Campaign Agent (Calibration)")
            return run_campaign_agent(db, payload, context)
        elif event_type == 'cron_schedule':
            context.log("🛡️ Running Supervisor Check (console test)")
            return run_supervisor(db, context)
    
    # --- CRON SCHEDULE (Supervisor Safety Net) ---
    if 'cron' in trigger_event or not trigger_event:
        context.log(f"🛡️ Safety Net: Unknown event '{trigger_event}' - Running Supervisor")
        return run_supervisor(db, context)
    
    # --- FALLBACK ---
    context.log(f"⚠️ Unhandled trigger: {trigger_event}")
    return context.res.json({
        "status": "no_action",
        "trigger": trigger_event
    })


def _parse_payload(context) -> Dict[str, Any]:
    """Safely parse request payload."""
    try:
        body = context.req.body
        if not body:
            return {}
        if isinstance(body, str):
            return json.loads(body)
        if isinstance(body, dict):
            return body
        return {}
    except json.JSONDecodeError as e:
        context.log(f"⚠️ Payload parse error: {e}")
        return {}
    except Exception as e:
        context.log(f"⚠️ Payload error: {e}")
        return {}


def _handle_http_request(db: KairoDB, context, path: str, method: str, payload: Dict) -> Any:
    """Handle direct HTTP API requests from the app."""
    
    # Marathon endpoints
    if path.startswith('/marathon'):
        return _handle_marathon_request(db, context, path, method, payload)
    
    # Brain trigger endpoint (for Reflex to escalate to Deep)
    if path == '/brain/deep' and method == 'POST':
        return _handle_deep_request(db, context, payload)
    
    # State sync endpoint
    if path == '/state' and method == 'GET':
        return _handle_state_request(db, context, payload)
    
    # Health check
    if path == '/health':
        return context.res.json({
            "status": "DEEP_BRAIN_ONLINE",
            "version": "2.0.0"
        })
    
    return context.res.json({"error": "Unknown endpoint"}, 404)


def _handle_deep_request(db: KairoDB, context, payload: Dict) -> Any:
    """
    Handle escalation from Reflex Agent to Deep Brain.
    
    The app's Reflex Agent calls this when:
    - Task is too complex for quick response
    - User explicitly requests deep thinking
    - Pressure index requires careful planning
    """
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "userId required"}, 400)
    
    prompt = payload.get('prompt', '')
    agent = payload.get('agent', 'campaign')
    
    context.log(f"🧠 Deep request from Reflex | User: {user_id} | Agent: {agent}")
    
    # Route to appropriate agent
    agent_payload = {
        'userId': user_id,
        'type': 'deep_request',
        'prompt': prompt,
        **payload.get('data', {})
    }
    
    if agent == 'study':
        return run_study_agent(db, agent_payload, context)
    elif agent == 'vitality':
        return run_vitality_agent(db, agent_payload, context)
    elif agent == 'radius':
        return run_radius_agent(db, agent_payload, context)
    else:
        return run_campaign_agent(db, agent_payload, context)


def _handle_marathon_request(db: KairoDB, context, path: str, method: str, payload: Dict) -> Any:
    """Handle marathon session management."""
    from .core.deep_brain import DeepBrain
    
    brain = DeepBrain(db)
    
    # POST /marathon/create - Create new marathon
    if path == '/marathon/create' and method == 'POST':
        user_id = payload.get('userId')
        if not user_id:
            return context.res.json({"error": "userId required"}, 400)
        
        session = brain.create_marathon(
            user_id=user_id,
            agent_type=payload.get('agent', 'campaign'),
            title=payload.get('title', 'New Goal'),
            description=payload.get('description', ''),
            success_criteria=payload.get('success_criteria', []),
            deadline=payload.get('deadline'),
            priority=payload.get('priority', 5)
        )
        
        return context.res.json({
            "status": "marathon_created",
            "session_id": session['session_id'],
            "steps": session.get('steps', 0)
        })
    
    # GET /marathon/{session_id} - Get marathon status
    if path.startswith('/marathon/') and method == 'GET':
        session_id = path.split('/')[-1]
        session = brain.get_marathon(session_id)
        
        if not session:
            return context.res.json({"error": "Marathon not found"}, 404)
        
        return context.res.json(session)
    
    # POST /marathon/{session_id}/step - Execute next step
    if path.endswith('/step') and method == 'POST':
        session_id = path.split('/')[-2]
        result = brain.execute_marathon_step(session_id)
        
        return context.res.json(result)
    
    return context.res.json({"error": "Unknown marathon endpoint"}, 404)


def _handle_state_request(db: KairoDB, context, payload: Dict) -> Any:
    """Get user state for app sync."""
    user_id = payload.get('userId') or context.req.query.get('userId')
    
    if not user_id:
        return context.res.json({"error": "userId required"}, 400)
    
    user_doc = db.get_user_doc(user_id)
    if not user_doc:
        return context.res.json({"error": "User not found"}, 404)
    
    # Parse the cached state
    state_json = user_doc.get('studentState_json', '{}')
    try:
        state = json.loads(state_json)
    except:
        state = {}
    
    return context.res.json({
        "userId": user_id,
        "state": state,
        "profile": user_doc.get('studentprofile_json'),
        "last_updated": user_doc.get('$updatedAt')
    })
