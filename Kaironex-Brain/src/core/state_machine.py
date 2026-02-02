"""
🔄 AGENT STATE MACHINE
======================
Finite state machine for agent execution lifecycle.

Each agent operates through a defined sequence of states:
DORMANT → THINKING → ACTING → OBSERVING → REFLECTING → WAITING → (repeat)

This ensures consistent behavior, proper error handling,
and the ability to resume from any state after failures.
"""

from enum import Enum, auto
from typing import Optional, Dict, Any, Callable, Awaitable, List
from dataclasses import dataclass, field
from datetime import datetime
import asyncio


class AgentState(Enum):
    """The possible states an agent can be in."""
    DORMANT = auto()       # Inactive, waiting for trigger
    INITIALIZING = auto()  # Setting up context
    THINKING = auto()      # Deep reasoning in progress
    ACTING = auto()        # Executing an action
    OBSERVING = auto()     # Processing action results
    REFLECTING = auto()    # Learning from outcome
    WAITING = auto()       # Waiting for next cycle/input
    ERROR = auto()         # Error state
    TERMINATED = auto()    # Permanently stopped


@dataclass
class StateTransition:
    """A transition between states."""
    from_state: AgentState
    to_state: AgentState
    trigger: str
    timestamp: datetime = field(default_factory=datetime.now)
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class StateContext:
    """Context data passed between states."""
    user_id: str
    agent_type: str
    trigger_event: str
    payload: Dict[str, Any] = field(default_factory=dict)
    reasoning_result: Optional[str] = None
    action_result: Optional[Any] = None
    observation: Optional[str] = None
    reflection: Optional[str] = None
    thought_id: Optional[str] = None
    error: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "user_id": self.user_id,
            "agent_type": self.agent_type,
            "trigger_event": self.trigger_event,
            "payload": self.payload,
            "reasoning_result": self.reasoning_result,
            "action_result": str(self.action_result) if self.action_result else None,
            "observation": self.observation,
            "reflection": self.reflection,
            "thought_id": self.thought_id,
            "error": self.error
        }


class AgentStateMachine:
    """
    Manages the state lifecycle of an agent.
    
    Usage:
        sm = AgentStateMachine("study")
        
        sm.on_enter(AgentState.THINKING, async_thinking_handler)
        sm.on_enter(AgentState.ACTING, async_action_handler)
        
        context = StateContext(user_id="123", agent_type="study", trigger_event="focus_drop")
        await sm.start(context)
    """
    
    # Valid state transitions
    VALID_TRANSITIONS = {
        AgentState.DORMANT: [AgentState.INITIALIZING],
        AgentState.INITIALIZING: [AgentState.THINKING, AgentState.ERROR],
        AgentState.THINKING: [AgentState.ACTING, AgentState.WAITING, AgentState.ERROR],
        AgentState.ACTING: [AgentState.OBSERVING, AgentState.ERROR],
        AgentState.OBSERVING: [AgentState.REFLECTING, AgentState.ACTING, AgentState.ERROR],
        AgentState.REFLECTING: [AgentState.WAITING, AgentState.THINKING, AgentState.DORMANT],
        AgentState.WAITING: [AgentState.THINKING, AgentState.DORMANT, AgentState.TERMINATED],
        AgentState.ERROR: [AgentState.DORMANT, AgentState.WAITING, AgentState.TERMINATED],
        AgentState.TERMINATED: []
    }
    
    def __init__(self, agent_type: str):
        self.agent_type = agent_type
        self._state = AgentState.DORMANT
        self._context: Optional[StateContext] = None
        self._history: List[StateTransition] = []
        
        # State handlers
        self._enter_handlers: Dict[AgentState, Callable[[StateContext], Awaitable[Any]]] = {}
        self._exit_handlers: Dict[AgentState, Callable[[StateContext], Awaitable[None]]] = {}
        
        # Callbacks
        self._transition_callbacks: List[Callable[[StateTransition], Awaitable[None]]] = []
    
    @property
    def state(self) -> AgentState:
        return self._state
    
    @property
    def context(self) -> Optional[StateContext]:
        return self._context
    
    @property
    def history(self) -> List[StateTransition]:
        return self._history.copy()
    
    def on_enter(
        self,
        state: AgentState,
        handler: Callable[[StateContext], Awaitable[Any]]
    ):
        """Register a handler to run when entering a state."""
        self._enter_handlers[state] = handler
    
    def on_exit(
        self,
        state: AgentState,
        handler: Callable[[StateContext], Awaitable[None]]
    ):
        """Register a handler to run when exiting a state."""
        self._exit_handlers[state] = handler
    
    def on_transition(
        self,
        callback: Callable[[StateTransition], Awaitable[None]]
    ):
        """Register a callback for any state transition."""
        self._transition_callbacks.append(callback)
    
    def can_transition(self, to_state: AgentState) -> bool:
        """Check if transition to given state is valid."""
        valid = self.VALID_TRANSITIONS.get(self._state, [])
        return to_state in valid
    
    async def transition(
        self,
        to_state: AgentState,
        trigger: str = "manual",
        metadata: Optional[Dict[str, Any]] = None
    ) -> bool:
        """
        Transition to a new state.
        
        Returns True if transition was successful.
        """
        if not self.can_transition(to_state):
            return False
        
        # Record transition
        transition = StateTransition(
            from_state=self._state,
            to_state=to_state,
            trigger=trigger,
            metadata=metadata or {}
        )
        self._history.append(transition)
        
        # Exit current state
        if self._state in self._exit_handlers and self._context:
            try:
                await self._exit_handlers[self._state](self._context)
            except Exception as e:
                print(f"Exit handler error: {e}")
        
        # Update state
        old_state = self._state
        self._state = to_state
        
        # Notify callbacks
        for callback in self._transition_callbacks:
            try:
                await callback(transition)
            except Exception as e:
                print(f"Transition callback error: {e}")
        
        # Enter new state
        if to_state in self._enter_handlers and self._context:
            try:
                result = await self._enter_handlers[to_state](self._context)
                return result
            except Exception as e:
                self._context.error = str(e)
                await self.transition(AgentState.ERROR, f"enter_{to_state.name}_failed")
                return False
        
        return True
    
    async def start(self, context: StateContext) -> StateContext:
        """
        Start the state machine with given context.
        
        Runs through the full cycle:
        DORMANT → INITIALIZING → THINKING → ACTING → OBSERVING → REFLECTING → WAITING
        """
        self._context = context
        self._state = AgentState.DORMANT
        
        # Run through the standard cycle
        transitions = [
            (AgentState.INITIALIZING, "start"),
            (AgentState.THINKING, "initialized"),
            (AgentState.ACTING, "thought_complete"),
            (AgentState.OBSERVING, "action_complete"),
            (AgentState.REFLECTING, "observation_complete"),
            (AgentState.WAITING, "reflection_complete")
        ]
        
        for target_state, trigger in transitions:
            success = await self.transition(target_state, trigger)
            
            if not success or self._state == AgentState.ERROR:
                break
            
            # Check for early exit conditions
            if self._state == AgentState.TERMINATED:
                break
        
        return self._context
    
    async def run_cycle(self) -> bool:
        """
        Run one complete cycle from current state.
        
        Returns True if cycle completed successfully.
        """
        if self._state == AgentState.DORMANT:
            return False
        
        if self._state == AgentState.WAITING:
            # Start new thinking cycle
            await self.transition(AgentState.THINKING, "cycle_start")
        
        # Continue from current state
        cycle_map = {
            AgentState.THINKING: (AgentState.ACTING, "thought_complete"),
            AgentState.ACTING: (AgentState.OBSERVING, "action_complete"),
            AgentState.OBSERVING: (AgentState.REFLECTING, "observation_complete"),
            AgentState.REFLECTING: (AgentState.WAITING, "reflection_complete")
        }
        
        while self._state in cycle_map:
            next_state, trigger = cycle_map[self._state]
            success = await self.transition(next_state, trigger)
            
            if not success:
                return False
        
        return self._state == AgentState.WAITING
    
    def reset(self):
        """Reset the state machine to DORMANT."""
        self._state = AgentState.DORMANT
        self._context = None
        self._history.clear()
    
    def get_duration(self) -> float:
        """Get total duration of current execution in seconds."""
        if not self._history:
            return 0.0
        
        first = self._history[0].timestamp
        last = self._history[-1].timestamp
        return (last - first).total_seconds()
    
    def get_state_durations(self) -> Dict[AgentState, float]:
        """Get time spent in each state."""
        durations = {state: 0.0 for state in AgentState}
        
        for i, transition in enumerate(self._history[:-1]):
            next_transition = self._history[i + 1]
            duration = (next_transition.timestamp - transition.timestamp).total_seconds()
            durations[transition.to_state] += duration
        
        return durations
    
    def to_dict(self) -> Dict[str, Any]:
        """Serialize state machine to dictionary."""
        return {
            "agent_type": self.agent_type,
            "current_state": self._state.name,
            "context": self._context.to_dict() if self._context else None,
            "history": [
                {
                    "from": t.from_state.name,
                    "to": t.to_state.name,
                    "trigger": t.trigger,
                    "timestamp": t.timestamp.isoformat(),
                    "metadata": t.metadata
                }
                for t in self._history
            ],
            "duration_seconds": self.get_duration()
        }


class AgentStateMachineFactory:
    """Factory for creating pre-configured state machines."""
    
    @staticmethod
    def create_study_agent() -> AgentStateMachine:
        """Create a state machine for the study agent."""
        sm = AgentStateMachine("study")
        # Handlers would be registered by the agent itself
        return sm
    
    @staticmethod
    def create_campaign_agent() -> AgentStateMachine:
        """Create a state machine for the campaign agent."""
        sm = AgentStateMachine("campaign")
        return sm
    
    @staticmethod
    def create_vitality_agent() -> AgentStateMachine:
        """Create a state machine for the vitality agent."""
        sm = AgentStateMachine("vitality")
        return sm
    
    @staticmethod
    def create_radius_agent() -> AgentStateMachine:
        """Create a state machine for the radius agent."""
        sm = AgentStateMachine("radius")
        return sm
