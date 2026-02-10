"""
📦 PYDANTIC MODELS
==================
Data models for API requests and responses.
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum


# =============================================================================
# ENUMS
# =============================================================================

class AgentType(str, Enum):
    STUDY = "study"
    VITALITY = "vitality"
    CAMPAIGN = "campaign"
    RADIUS = "radius"
    SUPERVISOR = "supervisor"


class ReasoningModeType(str, Enum):
    REFLEX = "reflex"
    DEEP = "deep"
    HYBRID = "hybrid"
    MARATHON = "marathon"


class MarathonStatusType(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    PAUSED = "paused"
    WAITING = "waiting"
    THINKING = "thinking"
    ACTING = "acting"
    COMPLETE = "complete"
    FAILED = "failed"
    CANCELLED = "cancelled"


class InterventionStrategy(str, Enum):
    PROACTIVE = "PROACTIVE"
    EMPATHY = "EMPATHY"
    NEGOTIATION = "NEGOTIATION"
    CELEBRATION = "CELEBRATION"
    URGENT = "URGENT"
    CALM = "CALM"
    WARM = "WARM"
    NEUTRAL = "NEUTRAL"


# =============================================================================
# USER MODELS
# =============================================================================

class UserProfile(BaseModel):
    """User profile data."""
    userId: str
    name: Optional[str] = None
    email: Optional[str] = None
    timezone: str = "UTC"
    preferences: Dict[str, Any] = Field(default_factory=dict)


class UserState(BaseModel):
    """Complete user state snapshot."""
    userId: str
    vitality: Optional[Dict[str, Any]] = None
    campaign: Optional[Dict[str, Any]] = None
    radius: Optional[Dict[str, Any]] = None
    study_session: Optional[Dict[str, Any]] = None
    last_updated: Optional[datetime] = None


# =============================================================================
# EVENT MODELS
# =============================================================================

class EventPayload(BaseModel):
    """Generic event payload."""
    userId: str
    type: str
    data: Dict[str, Any] = Field(default_factory=dict)
    timestamp: Optional[datetime] = None
    
    class Config:
        extra = "allow"


class StudyEvent(BaseModel):
    """Study session event."""
    userId: str
    topic: str
    duration_seconds: int = 0
    focus_score: int = 100
    status: str = "IN_PROGRESS"
    taskId: Optional[str] = None


class VitalityEvent(BaseModel):
    """Vitality/energy event."""
    userId: str
    type: str = "energy_check"
    sleep_hours: Optional[float] = None
    sleep_quality: Optional[str] = None
    steps: int = 0
    energy_level: Optional[int] = None
    workout_type: Optional[str] = None
    workout_duration: Optional[int] = None


class CampaignEvent(BaseModel):
    """Campaign/goal event."""
    userId: str
    type: str = "new_goal"
    goal_title: Optional[str] = None
    description: Optional[str] = None
    deadline: Optional[str] = None
    priority: int = 5
    quest_id: Optional[str] = None


class RadiusEvent(BaseModel):
    """Location/context event."""
    userId: str
    user_location: str
    trigger_voice: bool = False
    context: Dict[str, Any] = Field(default_factory=dict)


# =============================================================================
# THOUGHT MODELS
# =============================================================================

class ThoughtSignatureModel(BaseModel):
    """Thought signature data."""
    thought_id: str
    timestamp: datetime
    user_id: str
    agent: str
    context_hash: str
    reasoning_trace: List[str]
    confidence: float
    tool_calls: List[str]
    action_output: str
    parent_signature: Optional[str] = None


class ThoughtChainModel(BaseModel):
    """Chain of linked thoughts."""
    chain_id: str
    user_id: str
    agent: str
    depth: int
    total_confidence: float
    thoughts: List[ThoughtSignatureModel]
    created_at: datetime
    last_active: datetime
    status: str


# =============================================================================
# MARATHON MODELS
# =============================================================================

class MarathonGoalModel(BaseModel):
    """Marathon goal definition."""
    title: str
    description: str
    success_criteria: List[str]
    deadline: Optional[datetime] = None
    priority: int = Field(default=5, ge=1, le=10)
    context: Dict[str, Any] = Field(default_factory=dict)


class MarathonStepModel(BaseModel):
    """A single marathon step."""
    step_id: str
    title: str
    description: str
    status: MarathonStatusType
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    thought_id: Optional[str] = None
    result: Optional[str] = None
    error: Optional[str] = None


class MarathonSessionModel(BaseModel):
    """Full marathon session."""
    session_id: str
    user_id: str
    agent_type: str
    goal: MarathonGoalModel
    status: MarathonStatusType
    steps: List[MarathonStepModel]
    current_step_index: int
    progress: float
    thought_chain: List[str]
    created_at: datetime
    last_active_at: datetime
    estimated_completion: Optional[datetime] = None


# =============================================================================
# INTERVENTION MODELS
# =============================================================================

class InterventionModel(BaseModel):
    """AI intervention/nudge."""
    intervention_id: str
    user_id: str
    trigger_event: str
    message: str
    strategy: InterventionStrategy
    status: str = "PENDING"
    user_response: Optional[str] = None
    outcome: Optional[str] = None
    created_at: Optional[datetime] = None


class InterventionAction(BaseModel):
    """Possible action for an intervention."""
    id: str
    label: str
    style: str = "default"  # default, primary, danger


class InterventionWithActions(InterventionModel):
    """Intervention with action buttons."""
    actions: List[InterventionAction] = Field(default_factory=list)


# =============================================================================
# AGENT RESULT MODELS
# =============================================================================

class AgentResultModel(BaseModel):
    """Result from agent processing."""
    success: bool
    response: str
    thought_id: Optional[str] = None
    actions_taken: List[str] = Field(default_factory=list)
    interventions_created: List[str] = Field(default_factory=list)
    state_updates: Dict[str, Any] = Field(default_factory=dict)
    latency_ms: float = 0.0
    error: Optional[str] = None


class ReasoningResponseModel(BaseModel):
    """Response from the bicameral engine."""
    content: str
    mode_used: ReasoningModeType
    thought_signature: Optional[ThoughtSignatureModel] = None
    latency_ms: float = 0.0
    confidence: float = 1.0


# =============================================================================
# WEBSOCKET MODELS
# =============================================================================

class WebSocketMessage(BaseModel):
    """Generic WebSocket message."""
    type: str
    data: Dict[str, Any] = Field(default_factory=dict)


class ThoughtStreamMessage(BaseModel):
    """Real-time thought streaming message."""
    type: str = "thought_stream"
    agent: str
    thought: str
    state: str
    confidence: float = 0.0
    progress: float = 0.0


class InterventionMessage(BaseModel):
    """Intervention notification message."""
    type: str = "intervention"
    intervention_id: str
    message: str
    strategy: str
    actions: List[InterventionAction]


class MarathonUpdateMessage(BaseModel):
    """Marathon progress update message."""
    type: str = "marathon_update"
    session_id: str
    agent: str
    progress: float
    current_step: str
    eta_minutes: Optional[int] = None
    thought_signature: Optional[str] = None


# =============================================================================
# API REQUEST/RESPONSE MODELS
# =============================================================================

class TriggerRequest(BaseModel):
    """Universal trigger request."""
    userId: str
    type: str
    data: Dict[str, Any] = Field(default_factory=dict)


class QuickPromptRequest(BaseModel):
    """Quick prompt request."""
    userId: str
    prompt: str
    agent: str = "generic"
    mode: ReasoningModeType = ReasoningModeType.REFLEX


class MarathonCreateRequest(BaseModel):
    """Create marathon request."""
    userId: str
    agent: AgentType = AgentType.CAMPAIGN
    title: str
    description: str
    success_criteria: List[str]
    deadline: Optional[str] = None
    priority: int = Field(default=5, ge=1, le=10)


class InterventionResponseRequest(BaseModel):
    """User response to intervention."""
    userId: str
    interventionId: str
    response: str  # accept/snooze/dismiss
    feedback: Optional[str] = None


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    uptime_seconds: float
    components: Dict[str, str]
    active_connections: int


class MetricsResponse(BaseModel):
    """System metrics response."""
    event_bus: Dict[str, Any]
    active_marathons: int
    websocket_connections: int
    uptime_hours: float
