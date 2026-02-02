"""
🌐 KAIRONEX BRAIN SERVER
========================
The main FastAPI server for the Kaironex backend.

Features:
- REST API for all agent interactions
- WebSocket for real-time communication
- Health checks and metrics
- Background task scheduling
"""

import os
import json
import asyncio
from typing import Dict, Any, Optional, List
from datetime import datetime
from contextlib import asynccontextmanager

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from dotenv import load_dotenv

# Load environment
load_dotenv()

# Import core components
from src.core.bicameral_engine import BicameralEngine, ReasoningRequest, ReasoningMode
from src.core.thought_manager import ThoughtManager
from src.core.marathon_runner import MarathonRunner, MarathonGoal
from src.core.event_bus import EventBus, EventType, Event
from src.utils.db_helper import KairoDB

# Import agents
from src.agents.campaign_agent import CampaignAgent
from src.agents.vitality_agent import VitalityAgent
from src.agents.radius_agent import RadiusAgent


# =============================================================================
# LIFECYCLE MANAGEMENT
# =============================================================================

class KaironexBrain:
    """Global brain state container."""
    
    def __init__(self):
        self._engine: Optional[BicameralEngine] = None
        self._thoughts: Optional[ThoughtManager] = None
        self._marathon: Optional[MarathonRunner] = None
        self._events: Optional[EventBus] = None
        self._db: Optional[KairoDB] = None
        
        self.agents: Dict[str, Any] = {}
        self.websocket_connections: Dict[str, List[WebSocket]] = {}
        self.started_at: Optional[datetime] = None
    
    @property
    def engine(self) -> BicameralEngine:
        """Get the bicameral engine (raises if not initialized)."""
        if self._engine is None:
            raise RuntimeError("Brain not started - engine not initialized")
        return self._engine
    
    @property
    def thoughts(self) -> ThoughtManager:
        """Get the thought manager (raises if not initialized)."""
        if self._thoughts is None:
            raise RuntimeError("Brain not started - thought manager not initialized")
        return self._thoughts
    
    @property
    def marathon(self) -> MarathonRunner:
        """Get the marathon runner (raises if not initialized)."""
        if self._marathon is None:
            raise RuntimeError("Brain not started - marathon runner not initialized")
        return self._marathon
    
    @property
    def events(self) -> EventBus:
        """Get the event bus (raises if not initialized)."""
        if self._events is None:
            raise RuntimeError("Brain not started - event bus not initialized")
        return self._events
    
    @property
    def db(self) -> KairoDB:
        """Get the database helper (raises if not initialized)."""
        if self._db is None:
            raise RuntimeError("Brain not started - database not initialized")
        return self._db
    
    async def startup(self):
        """Initialize all components."""
        print("🧠 KAIRONEX BRAIN STARTING...")
        
        # Initialize database helper
        try:
            self._db = KairoDB()
            print("✅ Database connected")
        except Exception as e:
            print(f"⚠️ Database connection failed: {e}")
            raise RuntimeError(f"Database connection required: {e}")
        
        # Initialize core components
        self._engine = BicameralEngine()
        print("✅ Bicameral Engine initialized")
        
        self._thoughts = ThoughtManager(self._db)
        print("✅ Thought Manager initialized")
        
        self._events = EventBus()
        print("✅ Event Bus initialized")
        
        self._marathon = MarathonRunner(self._engine, self._thoughts, self._db)
        print("✅ Marathon Runner initialized")
        
        # Initialize agents
        self.agents['campaign'] = CampaignAgent(
            self._engine, self._thoughts, self._db, self._events
        )
        self.agents['vitality'] = VitalityAgent(
            self._engine, self._thoughts, self._db, self._events
        )
        self.agents['radius'] = RadiusAgent(
            self._engine, self._thoughts, self._db, self._events
        )
        print("✅ Agents initialized")
        
        # Subscribe to events for WebSocket broadcast
        self._events.subscribe_all(self._broadcast_event)
        
        self.started_at = datetime.now()
        print("🟢 KAIRONEX BRAIN ONLINE")
    
    async def shutdown(self):
        """Cleanup on shutdown."""
        print("🧠 KAIRONEX BRAIN SHUTTING DOWN...")
        
        # Close WebSocket connections
        for user_id, connections in self.websocket_connections.items():
            for ws in connections:
                try:
                    await ws.close()
                except:
                    pass
        
        print("🔴 KAIRONEX BRAIN OFFLINE")
    
    async def _broadcast_event(self, event: Event):
        """Broadcast events to connected WebSocket clients."""
        if not event.user_id:
            return
        
        connections = self.websocket_connections.get(event.user_id, [])
        message = {
            "type": "event",
            "event": event.to_dict()
        }
        
        for ws in connections:
            try:
                await ws.send_json(message)
            except:
                pass


# Global brain instance
brain = KaironexBrain()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    await brain.startup()
    yield
    await brain.shutdown()


# =============================================================================
# FASTAPI APP
# =============================================================================

app = FastAPI(
    title="Kaironex Brain API",
    description="The intelligent backend for the Kaironex Student Life OS",
    version="2.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# =============================================================================
# REQUEST/RESPONSE MODELS
# =============================================================================

class TriggerRequest(BaseModel):
    userId: str
    type: str
    data: Dict[str, Any] = Field(default_factory=dict)


class QuickPromptRequest(BaseModel):
    userId: str
    prompt: str
    agent: str = "generic"
    mode: str = "reflex"


class MarathonCreateRequest(BaseModel):
    userId: str
    agent: str = "campaign"
    title: str
    description: str
    success_criteria: List[str]
    deadline: Optional[str] = None
    priority: int = 5


class InterventionResponse(BaseModel):
    userId: str
    interventionId: str
    response: str  # accept/snooze/dismiss
    feedback: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    uptime_seconds: float
    components: Dict[str, str]
    active_connections: int


# =============================================================================
# HEALTH & STATUS ENDPOINTS
# =============================================================================

@app.get("/", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    uptime = (datetime.now() - brain.started_at).total_seconds() if brain.started_at else 0
    
    return HealthResponse(
        status="NEURAL_CORE_ONLINE",
        uptime_seconds=uptime,
        components={
            "engine": "OK" if brain.engine else "OFFLINE",
            "database": "OK" if brain.db else "OFFLINE",
            "thoughts": "OK" if brain.thoughts else "OFFLINE",
            "marathon": "OK" if brain.marathon else "OFFLINE",
        },
        active_connections=sum(len(c) for c in brain.websocket_connections.values())
    )


@app.get("/api/v1/metrics")
async def get_metrics():
    """Get system metrics."""
    return {
        "event_bus": brain.events.get_stats() if brain.events else {},
        "active_marathons": len(brain.marathon._sessions) if brain.marathon else 0,
        "websocket_connections": sum(len(c) for c in brain.websocket_connections.values()),
        "uptime_hours": (datetime.now() - brain.started_at).total_seconds() / 3600 if brain.started_at else 0
    }


# =============================================================================
# AGENT TRIGGER ENDPOINTS
# =============================================================================

@app.post("/api/v1/brain/trigger")
async def universal_trigger(request: TriggerRequest, background_tasks: BackgroundTasks):
    """
    Universal event trigger endpoint.
    
    Routes to appropriate agent based on event type.
    """
    payload = {
        "userId": request.userId,
        "type": request.type,
        **request.data
    }
    
    # Determine agent based on event type
    agent_map = {
        "new_goal": "campaign",
        "schedule_update": "campaign",
        "quest_complete": "campaign",
        "simulacrum": "campaign",
        "sleep_log": "vitality",
        "activity_log": "vitality",
        "energy_check": "vitality",
        "regen_request": "vitality",
        "location_update": "radius",
        "local_scan": "radius",
        "slang_query": "radius",
        "housing_search": "radius",
        "visa_check": "radius",
    }
    
    agent_type = agent_map.get(request.type, "campaign")
    agent = brain.agents.get(agent_type)
    
    if not agent:
        raise HTTPException(status_code=400, detail=f"Unknown agent: {agent_type}")
    
    result = await agent.run(request.userId, payload, request.type)
    
    return result.to_dict()


@app.post("/api/v1/brain/campaign")
async def trigger_campaign(request: TriggerRequest):
    """Trigger the Campaign Agent."""
    agent = brain.agents.get('campaign')
    if not agent:
        raise HTTPException(status_code=500, detail="Campaign agent not initialized")
    
    payload = {
        "userId": request.userId,
        "type": request.type,
        **request.data
    }
    
    result = await agent.run(request.userId, payload, request.type)
    return result.to_dict()


@app.post("/api/v1/brain/vitality")
async def trigger_vitality(request: TriggerRequest):
    """Trigger the Vitality Agent."""
    agent = brain.agents.get('vitality')
    if not agent:
        raise HTTPException(status_code=500, detail="Vitality agent not initialized")
    
    payload = {
        "userId": request.userId,
        "type": request.type,
        **request.data
    }

    result = await agent.run(request.userId, payload, request.type)
    return result.to_dict()


@app.post("/api/v1/brain/radius")
async def trigger_radius(request: TriggerRequest):
    """Trigger the Radius Agent."""
    agent = brain.agents.get('radius')
    if not agent:
        raise HTTPException(status_code=500, detail="Radius agent not initialized")

    payload = {
        "userId": request.userId,
        "type": request.type,
        **request.data
    }
    
    result = await agent.run(request.userId, payload, request.type)
    return result.to_dict()


@app.post("/api/v1/brain/quick")
async def quick_prompt(request: QuickPromptRequest):
    """
    Quick prompt endpoint for simple AI responses.
    
    Uses reflex mode for fast responses.
    """
    mode = ReasoningMode.REFLEX if request.mode == "reflex" else ReasoningMode.DEEP
    
    response = await brain.engine.reason(ReasoningRequest(
        prompt=request.prompt,
        user_id=request.userId,
        agent=request.agent,
        mode=mode
    ))
    
    return {
        "response": response.content,
        "mode_used": response.mode_used.value,
        "latency_ms": response.latency_ms,
        "confidence": response.confidence
    }


# =============================================================================
# MARATHON ENDPOINTS
# =============================================================================

@app.post("/api/v1/marathon/create")
async def create_marathon(request: MarathonCreateRequest, background_tasks: BackgroundTasks):
    """Create a new marathon session."""
    
    deadline = None
    if request.deadline:
        try:
            deadline = datetime.fromisoformat(request.deadline)
        except:
            pass
    
    goal = MarathonGoal(
        title=request.title,
        description=request.description,
        success_criteria=request.success_criteria,
        deadline=deadline,
        priority=request.priority
    )
    
    session = await brain.marathon.create_marathon(
        user_id=request.userId,
        agent_type=request.agent,
        goal=goal
    )
    
    # Start marathon in background
    background_tasks.add_task(brain.marathon.run, session.session_id)
    
    return {
        "session_id": session.session_id,
        "status": session.status.value,
        "steps": len(session.steps),
        "estimated_completion": session.estimated_completion.isoformat() if session.estimated_completion else None
    }


@app.get("/api/v1/marathon/{session_id}")
async def get_marathon(session_id: str):
    """Get marathon session status."""
    session = await brain.marathon.get_session(session_id)
    
    if not session:
        raise HTTPException(status_code=404, detail="Marathon not found")
    
    return session.to_dict()


@app.get("/api/v1/marathon/user/{user_id}")
async def get_user_marathons(user_id: str):
    """Get all marathons for a user."""
    sessions = await brain.marathon.get_user_marathons(user_id)
    return [s.to_dict() for s in sessions]


@app.post("/api/v1/marathon/{session_id}/pause")
async def pause_marathon(session_id: str):
    """Pause a running marathon."""
    await brain.marathon.pause(session_id)
    return {"status": "paused"}


@app.post("/api/v1/marathon/{session_id}/resume")
async def resume_marathon(session_id: str, background_tasks: BackgroundTasks):
    """Resume a paused marathon."""
    await brain.marathon.resume(session_id)
    return {"status": "resumed"}


@app.post("/api/v1/marathon/{session_id}/cancel")
async def cancel_marathon(session_id: str):
    """Cancel a marathon."""
    await brain.marathon.cancel(session_id)
    return {"status": "cancelled"}


# =============================================================================
# STATE ENDPOINTS
# =============================================================================

@app.get("/api/v1/state/{user_id}")
async def get_user_state(user_id: str):
    """Get the full state for a user."""
    if not brain.db:
        raise HTTPException(status_code=500, detail="Database not connected")
    
    user_doc = brain.db.get_user_doc(user_id)
    
    if not user_doc:
        return {"user_id": user_id, "state": {}}
    
    state = {}
    try:
        state = json.loads(user_doc.get('studentState_json', '{}'))
    except:
        pass
    
    return {
        "user_id": user_id,
        "state": state,
        "profile": user_doc.get('studentprofile_json'),
        "last_updated": user_doc.get('$updatedAt')
    }


@app.get("/api/v1/thoughts/{user_id}")
async def get_user_thoughts(user_id: str, agent: Optional[str] = None):
    """Get recent thoughts for a user."""
    if agent:
        chain = await brain.thoughts.get_chain(user_id, agent)
        if chain:
            return {
                "chain_id": chain.chain_id,
                "depth": chain.depth,
                "total_confidence": chain.total_confidence,
                "thoughts": [t.to_dict() for t in chain.thoughts[-10:]]
            }
        return {"thoughts": []}
    
    # Get latest thought across all agents
    latest = await brain.thoughts.get_latest(user_id, "campaign")
    return {
        "latest": latest.to_dict() if latest else None
    }


# =============================================================================
# INTERVENTION ENDPOINTS
# =============================================================================

@app.post("/api/v1/intervention/respond")
async def respond_to_intervention(request: InterventionResponse):
    """Handle user response to an intervention."""
    
    # Update intervention in database
    if brain.db:
        try:
            from appwrite.query import Query
            from src.config import APPWRITE_DATABASE_ID, INTERVENTIONS_COL
            
            results = brain.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=INTERVENTIONS_COL,
                queries=[
                    Query.equal('interventionId', request.interventionId)
                ]
            )
            
            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                brain.db.db.update_row(
                    APPWRITE_DATABASE_ID,
                    INTERVENTIONS_COL,
                    doc_id,
                    {
                        'status': 'RESPONDED',
                        'user_response': request.response,
                        'outcome': request.feedback or request.response
                    }
                )
        except Exception as e:
            print(f"Intervention update error: {e}")
    
    # Emit event
    await brain.events.publish(Event.create(
        EventType.USER_INTERVENTION_RESPONSE,
        source="api",
        user_id=request.userId,
        data={
            "intervention_id": request.interventionId,
            "response": request.response,
            "feedback": request.feedback
        }
    ))
    
    return {"status": "recorded", "response": request.response}


# =============================================================================
# WEBSOCKET ENDPOINT
# =============================================================================

@app.websocket("/ws/brain/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    """
    WebSocket connection for real-time brain communication.
    
    Message Types (Client → Server):
    - {"type": "state_update", "domain": "vitality", "data": {...}}
    - {"type": "quick_prompt", "prompt": "...", "mode": "reflex"}
    - {"type": "ping"}
    
    Message Types (Server → Client):
    - {"type": "event", "event": {...}}
    - {"type": "thought_stream", "agent": "...", "thought": "..."}
    - {"type": "intervention", ...}
    - {"type": "pong"}
    """
    await websocket.accept()
    
    # Register connection
    if user_id not in brain.websocket_connections:
        brain.websocket_connections[user_id] = []
    brain.websocket_connections[user_id].append(websocket)
    
    print(f"🔌 WebSocket connected: {user_id}")
    
    try:
        while True:
            data = await websocket.receive_json()
            msg_type = data.get("type")
            
            if msg_type == "ping":
                await websocket.send_json({"type": "pong"})
            
            elif msg_type == "state_update":
                domain = data.get("domain", "generic")
                state_data = data.get("data", {})
                
                # Route to appropriate agent
                agent = brain.agents.get(domain)
                if agent:
                    payload = {"userId": user_id, "type": "state_update", **state_data}
                    result = await agent.run(user_id, payload, "websocket_update")
                    await websocket.send_json({
                        "type": "agent_response",
                        "agent": domain,
                        "result": result.to_dict()
                    })
            
            elif msg_type == "quick_prompt":
                prompt = data.get("prompt", "")
                mode = ReasoningMode.REFLEX if data.get("mode") == "reflex" else ReasoningMode.DEEP
                
                response = await brain.engine.reason(ReasoningRequest(
                    prompt=prompt,
                    user_id=user_id,
                    agent="websocket",
                    mode=mode
                ))
                
                await websocket.send_json({
                    "type": "quick_response",
                    "response": response.content,
                    "confidence": response.confidence
                })
            
            elif msg_type == "marathon_status":
                marathons = await brain.marathon.get_user_marathons(user_id)
                await websocket.send_json({
                    "type": "marathon_list",
                    "marathons": [m.to_dict() for m in marathons]
                })
    
    except WebSocketDisconnect:
        print(f"🔌 WebSocket disconnected: {user_id}")
    except Exception as e:
        print(f"WebSocket error for {user_id}: {e}")
    finally:
        # Unregister connection
        if user_id in brain.websocket_connections:
            brain.websocket_connections[user_id] = [
                ws for ws in brain.websocket_connections[user_id]
                if ws != websocket
            ]


# =============================================================================
# LEGACY COMPATIBILITY
# =============================================================================

@app.post("/simulate/study_log")
async def legacy_study_log(payload: Dict[str, Any]):
    """Legacy endpoint for study log simulation."""
    request = TriggerRequest(
        userId=payload.get("userId", "unknown"),
        type="study_log",
        data=payload
    )
    return await universal_trigger(request, BackgroundTasks())


@app.post("/simulate/vitality")
async def legacy_vitality(payload: Dict[str, Any]):
    """Legacy endpoint for vitality simulation."""
    request = TriggerRequest(
        userId=payload.get("userId", "unknown"),
        type=payload.get("type", "energy_check"),
        data=payload
    )
    return await trigger_vitality(request)


@app.post("/simulate/campaign")
async def legacy_campaign(payload: Dict[str, Any]):
    """Legacy endpoint for campaign simulation."""
    request = TriggerRequest(
        userId=payload.get("userId", "unknown"),
        type=payload.get("type", "new_goal"),
        data=payload
    )
    return await trigger_campaign(request)


# =============================================================================
# ENTRY POINT
# =============================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "server:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
