"""
📡 EVENT BUS
============
Internal pub/sub system for agent coordination.

The event bus enables:
- Loose coupling between agents
- Cross-agent communication
- Event-driven architecture
- Audit logging of all system events
"""

import asyncio
import json
from enum import Enum
from typing import Optional, Dict, Any, List, Callable, Awaitable, Set
from dataclasses import dataclass, field
from datetime import datetime
import uuid


class EventType(Enum):
    """System-wide event types."""
    
    # Agent lifecycle
    AGENT_STARTED = "agent.started"
    AGENT_COMPLETED = "agent.completed"
    AGENT_ERROR = "agent.error"
    
    # State changes
    STATE_TRANSITION = "state.transition"
    STATE_UPDATED = "state.updated"
    
    # Reasoning
    THOUGHT_CREATED = "thought.created"
    THOUGHT_VALIDATED = "thought.validated"
    
    # Marathon
    MARATHON_CREATED = "marathon.created"
    MARATHON_STEP_START = "marathon.step.start"
    MARATHON_STEP_COMPLETE = "marathon.step.complete"
    MARATHON_PROGRESS = "marathon.progress"
    MARATHON_COMPLETE = "marathon.complete"
    MARATHON_FAILED = "marathon.failed"
    
    # User events
    USER_ACTION = "user.action"
    USER_INTERVENTION_RESPONSE = "user.intervention.response"
    USER_STUDY_SESSION = "user.study.session"
    USER_LOCATION_CHANGE = "user.location.change"
    
    # Interventions
    INTERVENTION_CREATED = "intervention.created"
    INTERVENTION_SENT = "intervention.sent"
    INTERVENTION_ACCEPTED = "intervention.accepted"
    INTERVENTION_DISMISSED = "intervention.dismissed"
    
    # System
    SYSTEM_HEARTBEAT = "system.heartbeat"
    SYSTEM_HEALTH_CHECK = "system.health"
    SYSTEM_ERROR = "system.error"
    
    # Pressure/Alerting
    PRESSURE_SPIKE = "pressure.spike"
    DRIFT_DETECTED = "drift.detected"


@dataclass
class Event:
    """A system event."""
    event_id: str
    event_type: EventType
    source: str  # Agent or component that emitted
    timestamp: datetime
    user_id: Optional[str] = None
    data: Dict[str, Any] = field(default_factory=dict)
    correlation_id: Optional[str] = None  # For event chaining
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "event_id": self.event_id,
            "event_type": self.event_type.value,
            "source": self.source,
            "timestamp": self.timestamp.isoformat(),
            "user_id": self.user_id,
            "data": self.data,
            "correlation_id": self.correlation_id
        }
    
    def to_json(self) -> str:
        return json.dumps(self.to_dict(), default=str)
    
    @classmethod
    def create(
        cls,
        event_type: EventType,
        source: str,
        user_id: Optional[str] = None,
        data: Optional[Dict[str, Any]] = None,
        correlation_id: Optional[str] = None
    ) -> 'Event':
        return cls(
            event_id=f"evt_{uuid.uuid4().hex[:12]}",
            event_type=event_type,
            source=source,
            timestamp=datetime.now(),
            user_id=user_id,
            data=data or {},
            correlation_id=correlation_id
        )


# Type alias for event handlers
EventHandler = Callable[[Event], Awaitable[None]]


class EventBus:
    """
    Central event bus for the Kaironex system.
    
    Usage:
        bus = EventBus()
        
        # Subscribe to events
        async def on_thought(event: Event):
            print(f"New thought: {event.data}")
        
        bus.subscribe(EventType.THOUGHT_CREATED, on_thought)
        
        # Publish events
        await bus.publish(Event.create(
            EventType.THOUGHT_CREATED,
            source="campaign_agent",
            user_id="user_123",
            data={"thought_id": "ts_001"}
        ))
    """
    
    _instance: Optional['EventBus'] = None
    
    def __new__(cls):
        """Singleton pattern - one event bus per process."""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        
        self._subscribers: Dict[EventType, List[EventHandler]] = {}
        self._global_subscribers: List[EventHandler] = []
        self._event_history: List[Event] = []
        self._max_history = 1000
        self._paused = False
        self._event_queue: asyncio.Queue = asyncio.Queue()
        self._processing = False
        self._initialized = True
    
    def subscribe(
        self,
        event_type: EventType,
        handler: EventHandler
    ) -> Callable[[], None]:
        """
        Subscribe to a specific event type.
        
        Returns an unsubscribe function.
        """
        if event_type not in self._subscribers:
            self._subscribers[event_type] = []
        
        self._subscribers[event_type].append(handler)
        
        def unsubscribe():
            self._subscribers[event_type].remove(handler)
        
        return unsubscribe
    
    def subscribe_all(self, handler: EventHandler) -> Callable[[], None]:
        """
        Subscribe to all events.
        
        Returns an unsubscribe function.
        """
        self._global_subscribers.append(handler)
        
        def unsubscribe():
            self._global_subscribers.remove(handler)
        
        return unsubscribe
    
    def subscribe_pattern(
        self,
        pattern: str,
        handler: EventHandler
    ) -> Callable[[], None]:
        """
        Subscribe to events matching a pattern (e.g., "marathon.*").
        
        Returns an unsubscribe function.
        """
        matching_types = [
            et for et in EventType
            if et.value.startswith(pattern.replace("*", ""))
        ]
        
        unsubscribes = []
        for event_type in matching_types:
            unsub = self.subscribe(event_type, handler)
            unsubscribes.append(unsub)
        
        def unsubscribe_all():
            for unsub in unsubscribes:
                unsub()
        
        return unsubscribe_all
    
    async def publish(self, event: Event):
        """
        Publish an event to all subscribers.
        
        Events are processed asynchronously.
        """
        if self._paused:
            return
        
        # Add to history
        self._event_history.append(event)
        if len(self._event_history) > self._max_history:
            self._event_history = self._event_history[-self._max_history:]
        
        # Get all applicable handlers
        handlers: List[EventHandler] = []
        handlers.extend(self._global_subscribers)
        handlers.extend(self._subscribers.get(event.event_type, []))
        
        # Execute handlers concurrently
        if handlers:
            await asyncio.gather(
                *[self._safe_execute(handler, event) for handler in handlers],
                return_exceptions=True
            )
    
    async def _safe_execute(self, handler: EventHandler, event: Event):
        """Execute a handler with error handling."""
        try:
            await handler(event)
        except Exception as e:
            # Emit an error event (but don't recurse infinitely)
            if event.event_type != EventType.SYSTEM_ERROR:
                error_event = Event.create(
                    EventType.SYSTEM_ERROR,
                    source="event_bus",
                    data={
                        "original_event": event.event_id,
                        "handler": str(handler),
                        "error": str(e)
                    }
                )
                # Just log, don't publish (avoid recursion)
                print(f"Event handler error: {error_event.to_json()}")
    
    def pause(self):
        """Pause event processing."""
        self._paused = True
    
    def resume(self):
        """Resume event processing."""
        self._paused = False
    
    def get_history(
        self,
        event_type: Optional[EventType] = None,
        user_id: Optional[str] = None,
        limit: int = 100
    ) -> List[Event]:
        """Get event history with optional filtering."""
        events = self._event_history
        
        if event_type:
            events = [e for e in events if e.event_type == event_type]
        
        if user_id:
            events = [e for e in events if e.user_id == user_id]
        
        return events[-limit:]
    
    def get_correlation_chain(self, correlation_id: str) -> List[Event]:
        """Get all events in a correlation chain."""
        return [
            e for e in self._event_history
            if e.correlation_id == correlation_id
        ]
    
    def clear_history(self):
        """Clear event history."""
        self._event_history.clear()
    
    def get_subscriber_count(self, event_type: EventType) -> int:
        """Get the number of subscribers for an event type."""
        return len(self._subscribers.get(event_type, []))
    
    def get_stats(self) -> Dict[str, Any]:
        """Get event bus statistics."""
        event_counts = {}
        for event in self._event_history:
            key = event.event_type.value
            event_counts[key] = event_counts.get(key, 0) + 1
        
        return {
            "total_events": len(self._event_history),
            "subscriber_count": sum(len(h) for h in self._subscribers.values()),
            "global_subscriber_count": len(self._global_subscribers),
            "event_types_seen": len(event_counts),
            "event_counts": event_counts,
            "paused": self._paused
        }


# Convenience functions for common event patterns

async def emit_agent_started(
    bus: EventBus,
    agent_type: str,
    user_id: str,
    correlation_id: Optional[str] = None
):
    """Emit an agent started event."""
    await bus.publish(Event.create(
        EventType.AGENT_STARTED,
        source=f"{agent_type}_agent",
        user_id=user_id,
        data={"agent_type": agent_type},
        correlation_id=correlation_id
    ))


async def emit_agent_completed(
    bus: EventBus,
    agent_type: str,
    user_id: str,
    result: Dict[str, Any],
    correlation_id: Optional[str] = None
):
    """Emit an agent completed event."""
    await bus.publish(Event.create(
        EventType.AGENT_COMPLETED,
        source=f"{agent_type}_agent",
        user_id=user_id,
        data={"agent_type": agent_type, "result": result},
        correlation_id=correlation_id
    ))


async def emit_thought(
    bus: EventBus,
    agent_type: str,
    user_id: str,
    thought_id: str,
    confidence: float,
    correlation_id: Optional[str] = None
):
    """Emit a thought created event."""
    await bus.publish(Event.create(
        EventType.THOUGHT_CREATED,
        source=f"{agent_type}_agent",
        user_id=user_id,
        data={
            "thought_id": thought_id,
            "confidence": confidence
        },
        correlation_id=correlation_id
    ))


async def emit_intervention(
    bus: EventBus,
    agent_type: str,
    user_id: str,
    intervention_id: str,
    trigger: str,
    strategy: str,
    correlation_id: Optional[str] = None
):
    """Emit an intervention created event."""
    await bus.publish(Event.create(
        EventType.INTERVENTION_CREATED,
        source=f"{agent_type}_agent",
        user_id=user_id,
        data={
            "intervention_id": intervention_id,
            "trigger": trigger,
            "strategy": strategy
        },
        correlation_id=correlation_id
    ))


async def emit_pressure_spike(
    bus: EventBus,
    user_id: str,
    pressure_index: int,
    trigger: str
):
    """Emit a pressure spike event."""
    await bus.publish(Event.create(
        EventType.PRESSURE_SPIKE,
        source="supervisor",
        user_id=user_id,
        data={
            "pressure_index": pressure_index,
            "trigger": trigger
        }
    ))
