"""
🤖 BASE AGENT
=============
Abstract base class for all Kaironex agents.

All agents inherit from this class to ensure:
- Consistent lifecycle management
- State machine integration
- Thought signature handling
- Event bus communication
- Error handling patterns
"""

from abc import ABC, abstractmethod
from typing import Optional, Dict, Any, List
from dataclasses import dataclass, field
from datetime import datetime
import asyncio
import uuid

from ..core.bicameral_engine import (
    BicameralEngine, 
    ReasoningRequest, 
    ReasoningResponse,
    ReasoningMode
)
from ..core.thought_manager import ThoughtManager
from ..core.state_machine import AgentStateMachine, AgentState, StateContext
from ..core.event_bus import EventBus, Event, EventType, emit_agent_started, emit_agent_completed
from ..utils.db_helper import KairoDB


@dataclass
class AgentConfig:
    """Configuration for an agent."""
    agent_type: str
    display_name: str
    system_instruction: str
    default_reasoning_mode: ReasoningMode = ReasoningMode.HYBRID
    max_thinking_tokens: int = 8192
    enable_thought_signatures: bool = True
    enable_marathon: bool = False
    
    # Safety rules
    forbidden_terms: List[str] = field(default_factory=list)
    required_disclaimer: Optional[str] = None


@dataclass
class AgentResult:
    """Result from an agent execution."""
    success: bool
    response: str
    thought_id: Optional[str] = None
    actions_taken: List[str] = field(default_factory=list)
    interventions_created: List[str] = field(default_factory=list)
    state_updates: Dict[str, Any] = field(default_factory=dict)
    latency_ms: float = 0.0
    error: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "success": self.success,
            "response": self.response,
            "thought_id": self.thought_id,
            "actions_taken": self.actions_taken,
            "interventions_created": self.interventions_created,
            "state_updates": self.state_updates,
            "latency_ms": self.latency_ms,
            "error": self.error
        }


class BaseAgent(ABC):
    """
    Abstract base class for all Kaironex agents.
    
    Subclasses must implement:
    - get_config(): Return agent configuration
    - process(): Main processing logic
    
    Optional overrides:
    - on_initialize(): Called before processing
    - on_complete(): Called after successful processing
    - on_error(): Called on processing error
    """
    
    def __init__(
        self,
        engine: BicameralEngine,
        thought_manager: ThoughtManager,
        db: KairoDB,
        event_bus: Optional[EventBus] = None
    ):
        self.engine = engine
        self.thoughts = thought_manager
        self.db = db
        self.events = event_bus or EventBus()
        
        self._config = self.get_config()
        self._state_machine = AgentStateMachine(self._config.agent_type)
        self._setup_state_handlers()
    
    @abstractmethod
    def get_config(self) -> AgentConfig:
        """Return the configuration for this agent."""
        pass
    
    @abstractmethod
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Main processing logic for the agent.
        
        This is called during the ACTING state.
        """
        pass
    
    def _setup_state_handlers(self):
        """Set up state machine handlers."""
        self._state_machine.on_enter(AgentState.THINKING, self._on_thinking)
        self._state_machine.on_enter(AgentState.ACTING, self._on_acting)
        self._state_machine.on_enter(AgentState.OBSERVING, self._on_observing)
        self._state_machine.on_enter(AgentState.REFLECTING, self._on_reflecting)
    
    async def _on_thinking(self, context: StateContext) -> Any:
        """Handle THINKING state - deep reasoning."""
        # Get previous thought for context chaining
        parent_thought = await self.thoughts.get_latest(
            context.user_id,
            self._config.agent_type
        )
        
        # Build the reasoning request
        reasoning_context = await self.thoughts.get_reasoning_context(
            context.user_id,
            self._config.agent_type
        )
        
        prompt = self._build_thinking_prompt(context, reasoning_context)
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=context.user_id,
            agent=self._config.agent_type,
            context=context.payload,
            mode=self._config.default_reasoning_mode,
            parent_thought=parent_thought.thought_id if parent_thought else None,
            system_instruction=self._config.system_instruction,
            max_thinking_tokens=self._config.max_thinking_tokens
        ))
        
        context.reasoning_result = response.content
        
        if response.thought_signature:
            context.thought_id = response.thought_signature.thought_id
            await self.thoughts.store(response.thought_signature)
        
        return response
    
    async def _on_acting(self, context: StateContext) -> Any:
        """Handle ACTING state - execute agent logic."""
        result = await self.process(
            context.user_id,
            context.payload,
            context
        )
        context.action_result = result
        return result
    
    async def _on_observing(self, context: StateContext) -> Any:
        """Handle OBSERVING state - process results."""
        result = context.action_result
        
        if isinstance(result, AgentResult):
            context.observation = (
                f"Agent completed with success={result.success}. "
                f"Actions: {result.actions_taken}. "
                f"Interventions: {result.interventions_created}."
            )
        else:
            context.observation = str(result)
        
        return context.observation
    
    async def _on_reflecting(self, context: StateContext) -> Any:
        """Handle REFLECTING state - learn from outcome."""
        # Create a policy episode for learning
        if context.action_result and isinstance(context.action_result, AgentResult):
            await self._create_policy_episode(context)
        
        context.reflection = "Reflection complete."
        return context.reflection
    
    def _build_thinking_prompt(
        self,
        context: StateContext,
        reasoning_context: str
    ) -> str:
        """Build the prompt for the thinking phase."""
        return f"""
{reasoning_context}

EVENT: {context.trigger_event}
USER: {context.user_id}

PAYLOAD:
{context.payload}

Analyze this situation and determine the best course of action.
"""
    
    async def _create_policy_episode(self, context: StateContext):
        """Create a policy episode for reinforcement learning."""
        try:
            import hashlib
            import json
            
            result = context.action_result
            if result is None:
                return
            
            context_hash = hashlib.sha256(
                json.dumps(context.payload, sort_keys=True, default=str).encode()
            ).hexdigest()[:64]
            
            # Calculate reward based on success
            reward = 100 if result.success else -50
            
            # Get database ID safely
            db_id = getattr(self.db.db, '_database_id', None) or 'default'
            
            self.db.db.create_row(
                db_id,
                'policy_episodes',
                'unique()',
                {
                    'episodeId': f"ep_{uuid.uuid4().hex[:12]}",
                    'userId': context.user_id,
                    'context_hash': context_hash,
                    'action_taken': json.dumps(result.to_dict())[:99999],
                    'reward': reward,
                    'timestamp': datetime.now().isoformat()
                }
            )
        except Exception as e:
            print(f"Policy episode creation error: {e}")
    
    async def run(
        self,
        user_id: str,
        payload: Dict[str, Any],
        trigger_event: str
    ) -> AgentResult:
        """
        Run the agent through its full lifecycle.
        
        This is the main entry point for agent execution.
        """
        import time
        start_time = time.time()
        
        correlation_id = f"corr_{uuid.uuid4().hex[:8]}"
        
        # Emit start event
        await emit_agent_started(
            self.events,
            self._config.agent_type,
            user_id,
            correlation_id
        )
        
        # Create state context
        context = StateContext(
            user_id=user_id,
            agent_type=self._config.agent_type,
            trigger_event=trigger_event,
            payload=payload
        )
        
        try:
            # Run through state machine
            await self._state_machine.start(context)
            
            # Get result
            if isinstance(context.action_result, AgentResult):
                result = context.action_result
            else:
                result = AgentResult(
                    success=True,
                    response=str(context.action_result) if context.action_result else "Completed",
                    thought_id=context.thought_id
                )
            
            result.latency_ms = (time.time() - start_time) * 1000
            
            # Emit completion event
            await emit_agent_completed(
                self.events,
                self._config.agent_type,
                user_id,
                result.to_dict(),
                correlation_id
            )
            
            return result
            
        except Exception as e:
            latency_ms = (time.time() - start_time) * 1000
            
            # Emit error event
            await self.events.publish(Event.create(
                EventType.AGENT_ERROR,
                source=f"{self._config.agent_type}_agent",
                user_id=user_id,
                data={"error": str(e)},
                correlation_id=correlation_id
            ))
            
            return AgentResult(
                success=False,
                response="An error occurred during processing.",
                error=str(e),
                latency_ms=latency_ms
            )
    
    def _sanitize_output(self, text: str) -> str:
        """Remove forbidden terms from output."""
        result = text
        for term in self._config.forbidden_terms:
            result = result.replace(term, "[REDACTED]")
        
        if self._config.required_disclaimer:
            result = f"{result}\n\n{self._config.required_disclaimer}"
        
        return result
    
    async def quick_response(
        self,
        user_id: str,
        prompt: str
    ) -> str:
        """Generate a quick reflex response without full lifecycle."""
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent=self._config.agent_type,
            mode=ReasoningMode.REFLEX,
            system_instruction=self._config.system_instruction
        ))
        
        return self._sanitize_output(response.content)
    
    def log_heartbeat(self, user_id: str, details: str):
        """Log agent activity heartbeat."""
        self.db.log_heartbeat(user_id, f"{self._config.agent_type.upper()}: {details}")
    
    def create_intervention(
        self,
        user_id: str,
        trigger: str,
        message: str,
        strategy: str = "NEUTRAL",
        **kwargs
    ) -> str:
        """
        Create an intervention for the user.
        
        Additional kwargs are stored as metadata:
        - priority: Intervention priority level
        - drift_types: Types of drift detected
        - digest_type: Type of digest (weekly, daily, etc.)
        """
        intervention_id = f"int_{uuid.uuid4().hex[:8]}"
        
        self.db.create_intervention(
            user_id,
            trigger,
            self._sanitize_output(message),
            strategy=strategy
        )
        
        return intervention_id
    
    def update_state_cache(self, user_id: str, state_data: Dict[str, Any]):
        """Update the user's state cache."""
        self.db.update_state_cache(user_id, state_data)
