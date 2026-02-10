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
from .agents.vitality_brain_v2 import run_vitality_agent  # v2.0 Survival & Growth Protocol
from .agents.campaign_agent import run_campaign_agent
from .agents.radius_agent import run_radius_agent
from .config import validate_config


async def main(context):
    """
    Main entry point for Appwrite Functions.
    Routes requests to appropriate agent based on trigger event.
    """
    # The runtime expects an async function if we want to await internal calls.
    # By making this async, the runtime's "await userModule.main(context)" will work correctly.
    return await _async_main(context)

async def _async_main(context):
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
    
    collection_id = payload.get('$collectionId', '')
    
    # --- DATABASE EVENT TRIGGERS (High Priority) ---
    
    # 1. PROFILE UPDATE
    if 'users' in trigger_event or collection_id == 'users':
        user_id = payload.get('userId') or payload.get('$id')
        resource_count = db.count_user_resources(user_id)
        if resource_count > 0:
            context.log(f"✅ Profile ready + {resource_count} Files detected. Triggering Campaign Agent.")
            return await run_campaign_agent(db, payload, context)
        else:
            context.log("⏳ Profile updated, but waiting for files.")
            return context.res.json({"status": "waiting_for_files"})

    # 2. RESOURCES UPDATE
    if 'resources' in trigger_event or collection_id == 'resources':
        context.log("📄 Resource uploaded found in payload.")
        
        # Batch processing handling
        study_result = {"success": True, "message": "Ingestion skipped for batch processing."}
        
        context.log(f"✅ Resource processing initiated.")
        return study_result

    # 3. OTHER STATE UPDATES
    if 'study_logs' in trigger_event or collection_id == 'study_logs':
        context.log("📚 Routing to Study Agent")
        return await run_study_agent(db, payload, context)
    
    if 'vitality_state' in trigger_event or collection_id == 'vitality_state':
        context.log("⚡ Routing to Vitality Agent")
        return await run_vitality_agent(db, payload, context)
    
    if 'radius_state' in trigger_event or collection_id == 'radius_state':
        context.log("🌍 Routing to Radius Agent")
        return await run_radius_agent(db, payload, context)
    
    if any(x in trigger_event for x in ['schedule', 'campaign']):
        if 'cron' not in trigger_event:
            context.log("⚔️ Routing to Campaign Agent")
            return await run_campaign_agent(db, payload, context)

    # --- HTTP API ROUTES ---
    if request_path and request_path != '/':
        return await _handle_http_request(db, context, request_path, request_method, payload)
    
    # --- MANUAL TEST ---
    if request_path in ['/', ''] and payload:
        event_type = payload.get('type', payload.get('trigger', ''))
        context.log(f"🧪 Console test / Generic trigger detected | Event: {event_type}")
        
        if event_type in ['session_start', 'session_end', 'focus_update', 'quiz_request', 'IN_PROGRESS', 'COMPLETED', 'resource_ingestion', 'schedule_request']:
            context.log("📚 Routing to Study Agent (console test)")
            return await run_study_agent(db, payload, context)
        elif event_type in ['sleep_update', 'meal_logged', 'health_check']:
            context.log("⚡ Routing to Vitality Agent (console test)")
            return await run_vitality_agent(db, payload, context)
        elif event_type in ['location_update', 'social_event']:
            context.log("🌍 Routing to Radius Agent (console test)")
            return await run_radius_agent(db, payload, context)
        elif event_type in ['campaign_calibration', 'campaign_init']:
            context.log("⚔️ Routing to Campaign Agent (Calibration)")
            return await run_campaign_agent(db, payload, context)
        elif event_type == 'cron_schedule':
            context.log("🛡️ Running Supervisor Check (console test)")
            return await run_supervisor(db, context) # Supervisor is now async
    
    # --- CRON SCHEDULE ---
    if 'cron' in trigger_event or not trigger_event:
        context.log(f"🛡️ Safety Net: Running Supervisor")
        return await run_supervisor(db, context)
    
    context.log(f"⚠️ Unhandled trigger: {trigger_event}")
    return context.res.json({"status": "no_action", "trigger": trigger_event})

def _parse_payload(context) -> Dict[str, Any]:
    try:
        body = context.req.body
        if not body: return {}
        if isinstance(body, str): return json.loads(body)
        if isinstance(body, dict): return body
        return {}
    except Exception as e:
        context.log(f"⚠️ Payload error: {e}")
        return {}

async def _handle_http_request(db: KairoDB, context, path: str, method: str, payload: Dict) -> Any:
    # Brain trigger endpoint
    if path == '/brain/deep' and method == 'POST':
        return await _handle_deep_request(db, context, payload)
        
    # Campaign Agent endpoint
    if path == '/campaign' and method == 'POST':
        context.log(f"⚔️ Campaign request received: {payload.get('type')}")
        return await run_campaign_agent(db, payload, context)
        
    # State sync endpoint
    if path == '/state' and method == 'GET':
        return _handle_state_request(db, context, payload)
        
    # Health check
    if path == '/health':
        return context.res.json({"status": "DEEP_BRAIN_ONLINE", "version": "2.0.0"})
        
    return context.res.json({"error": "Unknown endpoint"}, 404)

async def _handle_deep_request(db: KairoDB, context, payload: Dict) -> Any:
    user_id = payload.get('userId')
    if not user_id: return context.res.json({"error": "userId required"}, 400)
    
    agent = payload.get('agent', 'campaign')
    prompt = payload.get('prompt', '')
    
    context.log(f"🧠 Deep request from Reflex | User: {user_id} | Agent: {agent}")
    
    agent_payload = {
        'userId': user_id, 'type': 'deep_request', 'prompt': prompt, **payload.get('data', {})
    }
    
    if agent == 'study':
        return await run_study_agent(db, agent_payload, context)
    elif agent == 'vitality':
        return await run_vitality_agent(db, agent_payload, context)
    elif agent == 'radius':
        return await run_radius_agent(db, agent_payload, context)
    else:
        return await run_campaign_agent(db, agent_payload, context)

def _handle_state_request(db: KairoDB, context, payload: Dict) -> Any:
    user_id = payload.get('userId') or context.req.query.get('userId')
    if not user_id: return context.res.json({"error": "userId required"}, 400)
    
    user_doc = db.get_user_doc(user_id)
    if not user_doc: return context.res.json({"error": "User not found"}, 404)
    
    state_json = user_doc.get('studentState_json', '{}')
    try: state = json.loads(state_json)
    except: state = {}
    
    return context.res.json({
        "userId": user_id, "state": state,
        "profile": user_doc.get('studentprofile_json'),
        "last_updated": user_doc.get('$updatedAt')
    })
