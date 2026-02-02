"""
🎭 KAIRONEX ORCHESTRATOR
=========================
The Main Brain that coordinates all agents, tools, and engines.

This is the single entry point for all Kaironex operations.
Perfect for demo and testing!

"One brain to rule them all."
"""

import os
import json
import asyncio
from typing import Dict, Any, List, Optional, Callable
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum

from .core.bicameral_engine import BicameralEngine, ReasoningRequest, ReasoningResponse, ReasoningMode
from .core.marathon_runner import MarathonRunner, MarathonGoal
from .core.thought_manager import ThoughtManager
from .core.event_bus import EventBus, EventType, Event
from .core.intervention_engine import InterventionEngine
from .core.student_growth_engine import StudentGrowthEngine
from .core.life_event_handler import LifeEventHandler
from .core.schedule_validator import ScheduleValidator

from .tools.tool_executor import ToolExecutor
from .tools.deep_research import DeepResearchEngine, ResearchType


class RequestType(str, Enum):
    """Types of requests the orchestrator can handle."""
    CHAT = "chat"                       # Simple conversation
    COMMAND = "command"                 # Direct command (schedule, remind, etc.)
    RESEARCH = "research"               # Deep research task
    MARATHON = "marathon"               # Long-running goal
    LIFE_EVENT = "life_event"           # Life disruption
    SCHEDULE_CHANGE = "schedule_change" # Schedule modification
    VOICE = "voice"                     # Voice interaction


@dataclass
class OrchestratorRequest:
    """A request to the orchestrator."""
    user_id: str
    message: str
    request_type: RequestType = RequestType.CHAT
    context: Dict[str, Any] = field(default_factory=dict)
    session_id: Optional[str] = None
    voice_audio: Optional[bytes] = None  # For voice requests


@dataclass
class OrchestratorResponse:
    """Response from the orchestrator."""
    message: str
    request_type: RequestType
    success: bool = True
    data: Dict[str, Any] = field(default_factory=dict)
    tool_calls: List[str] = field(default_factory=list)
    thinking_trace: Optional[str] = None
    latency_ms: float = 0.0
    follow_up_actions: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "message": self.message,
            "request_type": self.request_type.value,
            "success": self.success,
            "data": self.data,
            "tool_calls": self.tool_calls,
            "thinking_trace": self.thinking_trace,
            "latency_ms": self.latency_ms,
            "follow_up_actions": self.follow_up_actions
        }


class KaironexOrchestrator:
    """
    The unified orchestrator that coordinates all Kaironex systems.
    
    Features:
    - Routes requests to appropriate handlers
    - Manages conversation context
    - Coordinates multi-agent workflows
    - Handles tool execution
    - Tracks student growth
    
    Usage:
        orchestrator = KaironexOrchestrator()
        response = await orchestrator.process(OrchestratorRequest(
            user_id="user_123",
            message="Help me plan my week"
        ))
    """
    
    def __init__(self, db=None):
        self.db = db
        
        # Initialize core engines
        self.brain = BicameralEngine(use_tools=True)
        self.thoughts = ThoughtManager(db)
        self.marathon = MarathonRunner(self.brain, self.thoughts, db)
        self.events = EventBus()
        self.interventions = InterventionEngine(self.brain, db)
        self.student_growth = StudentGrowthEngine(db)
        self.life_events = LifeEventHandler(db)
        self.schedule = ScheduleValidator(db)
        self.research = DeepResearchEngine(db=db)
        self.tools = ToolExecutor(db)
        
        # User context cache
        self._user_contexts: Dict[str, Dict[str, Any]] = {}
        
        # Request handlers
        self._handlers = {
            RequestType.CHAT: self._handle_chat,
            RequestType.COMMAND: self._handle_command,
            RequestType.RESEARCH: self._handle_research,
            RequestType.MARATHON: self._handle_marathon,
            RequestType.LIFE_EVENT: self._handle_life_event,
            RequestType.SCHEDULE_CHANGE: self._handle_schedule_change,
            RequestType.VOICE: self._handle_voice,
        }
        
        # Setup event subscriptions
        self._setup_event_handlers()
    
    def _setup_event_handlers(self):
        """Setup internal event handlers."""
        
        async def on_intervention(event: Event):
            # Log intervention for demo
            print(f"🚨 INTERVENTION: {event.data}")
        
        async def on_marathon_update(event: Event):
            # Log marathon progress
            print(f"🏃 MARATHON UPDATE: {event.data}")
        
        self.events.subscribe(EventType.INTERVENTION_CREATED, on_intervention)
        self.events.subscribe(EventType.MARATHON_PROGRESS, on_marathon_update)
    
    async def process(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """
        Process a request through the orchestrator.
        
        This is the main entry point for all Kaironex operations.
        """
        import time
        start_time = time.time()
        
        # Auto-detect request type if CHAT
        if request.request_type == RequestType.CHAT:
            request.request_type = self._detect_request_type(request.message)
        
        # Get or create user context
        user_context = await self._get_user_context(request.user_id)
        request.context.update(user_context)
        
        # Route to appropriate handler
        handler = self._handlers.get(request.request_type, self._handle_chat)
        
        try:
            response = await handler(request)
            response.latency_ms = (time.time() - start_time) * 1000
            
            # Update user context with this interaction
            await self._update_user_context(request.user_id, request, response)
            
            return response
            
        except Exception as e:
            return OrchestratorResponse(
                message=f"I encountered an error: {str(e)}. Let me try a different approach.",
                request_type=request.request_type,
                success=False,
                latency_ms=(time.time() - start_time) * 1000
            )
    
    def _detect_request_type(self, message: str) -> RequestType:
        """Auto-detect the type of request from the message."""
        message_lower = message.lower()
        
        # Life events
        life_keywords = ["sick", "wedding", "funeral", "emergency", "hospital", "overtime", "family"]
        if any(kw in message_lower for kw in life_keywords):
            return RequestType.LIFE_EVENT
        
        # Schedule changes
        schedule_keywords = ["reschedule", "move my", "change my schedule", "cancel", "add to schedule"]
        if any(kw in message_lower for kw in schedule_keywords):
            return RequestType.SCHEDULE_CHANGE
        
        # Research
        research_keywords = ["research", "find out about", "look up", "what is", "syllabus", "course info"]
        if any(kw in message_lower for kw in research_keywords):
            return RequestType.RESEARCH
        
        # Marathon goals
        marathon_keywords = ["long term", "this semester", "by graduation", "career goal", "72 hours"]
        if any(kw in message_lower for kw in marathon_keywords):
            return RequestType.MARATHON
        
        # Commands
        command_keywords = ["set reminder", "create", "add", "schedule", "plan my"]
        if any(kw in message_lower for kw in command_keywords):
            return RequestType.COMMAND
        
        return RequestType.CHAT
    
    async def _get_user_context(self, user_id: str) -> Dict[str, Any]:
        """Get or load user context."""
        if user_id in self._user_contexts:
            return self._user_contexts[user_id]
        
        # Load from database or create new
        context = {
            "user_id": user_id,
            "loaded_at": datetime.now().isoformat(),
            "interaction_count": 0,
            "last_topics": [],
            "current_mood": "neutral",
            "active_goals": []
        }
        
        # Try to load student profile for personalization
        try:
            profile = self.student_growth.get_or_create_profile(user_id)
            if profile:
                context["confidence_level"] = profile.confidence_level.value
                context["learning_style"] = profile.primary_learning_style.value
                context["communication_style"] = profile.preferred_communication.value
        except Exception:
            pass
        
        self._user_contexts[user_id] = context
        return context
    
    async def _update_user_context(
        self,
        user_id: str,
        request: OrchestratorRequest,
        response: OrchestratorResponse
    ):
        """Update user context after interaction."""
        if user_id not in self._user_contexts:
            self._user_contexts[user_id] = {}
        
        context = self._user_contexts[user_id]
        context["interaction_count"] = context.get("interaction_count", 0) + 1
        context["last_interaction"] = datetime.now().isoformat()
        
        # Track topics
        topics = context.get("last_topics", [])
        topics.insert(0, request.message[:50])
        context["last_topics"] = topics[:5]
    
    # =========================================================================
    # REQUEST HANDLERS
    # =========================================================================
    
    async def _handle_chat(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a general chat request."""
        
        # Use bicameral engine with tools
        response = await self.brain.reason_with_tools(ReasoningRequest(
            prompt=request.message,
            user_id=request.user_id,
            agent="main_brain",
            context=request.context,
            mode=ReasoningMode.HYBRID
        ))
        
        return OrchestratorResponse(
            message=response.content,
            request_type=RequestType.CHAT,
            success=True,
            tool_calls=[tc["tool_name"] for tc in response.tool_results] if response.tool_results else [],
            thinking_trace=response.thinking_content,
            data={"reasoning_mode": response.mode_used.value}
        )
    
    async def _handle_command(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a direct command."""
        
        # Use deep reasoning for commands
        response = await self.brain.reason_with_tools(ReasoningRequest(
            prompt=f"Execute this command for the user: {request.message}",
            user_id=request.user_id,
            agent="main_brain",
            context=request.context,
            mode=ReasoningMode.DEEP,
            system_instruction="""You are Kaironex's command processor.
Execute the user's command by calling the appropriate tools.
Be efficient and confirm what you did."""
        ))
        
        return OrchestratorResponse(
            message=response.content,
            request_type=RequestType.COMMAND,
            success=True,
            tool_calls=[tc["tool_name"] for tc in response.tool_results] if response.tool_results else [],
            thinking_trace=response.thinking_content
        )
    
    async def _handle_research(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a deep research request."""
        from .tools.deep_research import ResearchQuery
        import uuid
        
        # Determine research type
        message_lower = request.message.lower()
        
        if "syllabus" in message_lower or "course" in message_lower:
            research_type = ResearchType.SYLLABUS_ANALYSIS
        elif "career" in message_lower or "job" in message_lower:
            research_type = ResearchType.CAREER_RESEARCH
        elif "exam" in message_lower or "test" in message_lower:
            research_type = ResearchType.EXAM_PREP
        elif "internship" in message_lower:
            research_type = ResearchType.INTERNSHIP_HUNT
        else:
            research_type = ResearchType.TOPIC_DEEP_DIVE
        
        # Extract topic from message
        topic = request.message  # In production, would use NLP to extract
        
        query = ResearchQuery(
            query_id=f"research_{uuid.uuid4().hex[:8]}",
            user_id=request.user_id,
            research_type=research_type,
            topic=topic,
            context=request.context
        )
        
        # Execute research
        report = await self.research.research(query)
        
        # Format response
        response_message = f"""
📚 **Research Complete: {report.topic}**

{report.summary}

**Key Findings:**
{chr(10).join(f"• {f}" for f in report.key_findings[:5])}

**Recommendations:**
{chr(10).join(f"• {r}" for r in report.recommendations[:3])}

**Sources Analyzed:** {report.total_sources_analyzed}
**Research Time:** {report.research_duration_seconds:.1f}s
"""
        
        return OrchestratorResponse(
            message=response_message,
            request_type=RequestType.RESEARCH,
            success=report.status.value == "completed",
            data=report.to_dict(),
            thinking_trace=report.thinking_summaries[0] if report.thinking_summaries else None
        )
    
    async def _handle_marathon(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a long-running marathon goal."""
        
        # Create marathon goal
        goal = MarathonGoal(
            title=f"Marathon Goal {datetime.now().strftime('%Y%m%d%H%M%S')}",
            description=request.message,
            success_criteria=["Goal achieved"],
            priority=5,
            context={"user_id": request.user_id, "agent_type": "campaign"}
        )
        
        # Start marathon session using create_marathon
        session = await self.marathon.create_marathon(
            user_id=request.user_id,
            agent_type="campaign",
            goal=goal
        )
        
        return OrchestratorResponse(
            message=f"""
🏃 **Marathon Goal Started!**

I've created a long-running mission to: {request.message}

**Session ID:** {session.session_id}
**Status:** {session.status.value}

I'll be working on this in the background and will update you on progress.
Check back anytime to see how we're doing!
""",
            request_type=RequestType.MARATHON,
            success=True,
            data={"session_id": session.session_id, "status": session.status.value},
            follow_up_actions=["Monitor progress", "Check in after 24 hours"]
        )
    
    async def _handle_life_event(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a life event disruption."""
        from .core.life_event_handler import LifeEvent, EventCategory, EventSeverity
        import uuid
        
        # Parse event from message
        message_lower = request.message.lower()
        
        # Detect category
        if "sick" in message_lower or "ill" in message_lower:
            category = EventCategory.HEALTH
            severity = EventSeverity.MODERATE
        elif "wedding" in message_lower or "funeral" in message_lower or "family" in message_lower:
            category = EventCategory.FAMILY
            severity = EventSeverity.MAJOR
        elif "overtime" in message_lower or "work" in message_lower:
            category = EventCategory.WORK
            severity = EventSeverity.MODERATE
        elif "emergency" in message_lower:
            category = EventCategory.EMERGENCY
            severity = EventSeverity.CRITICAL
        else:
            category = EventCategory.SOCIAL
            severity = EventSeverity.MINOR
        
        # Process the life event directly (LifeEventHandler.process_event handles creation)
        result = await self.life_events.process_event(
            user_id=request.user_id,
            event_description=request.message,
            affects_from=datetime.now(),
            source="user"
        )
        
        return OrchestratorResponse(
            message=f"""
💙 **I understand - life happens!**

{result.get("message", "I've adjusted things for you.")}

**What I did:**
{chr(10).join(f"• {a}" for a in result.get("adjustments", ["Noted the event"]))}

Take care of yourself. I'll keep things running smoothly here.
""",
            request_type=RequestType.LIFE_EVENT,
            success=True,
            data=result
        )
    
    async def _handle_schedule_change(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a schedule change request."""
        from .core.schedule_validator import ChangeType
        
        # Parse the change request
        message_lower = request.message.lower()
        
        if "cancel" in message_lower or "remove" in message_lower:
            change_type = ChangeType.REMOVE
        elif "move" in message_lower or "reschedule" in message_lower:
            change_type = ChangeType.MOVE
        else:
            change_type = ChangeType.ADD
        
        # Create change proposal
        change = self.schedule.propose_change(
            user_id=request.user_id,
            change_type=change_type,
            reason=request.message
        )
        
        # Validate the change
        decision = await self.schedule.validate_change(
            user_id=request.user_id,
            change=change,
            user_context=request.context
        )
        
        return OrchestratorResponse(
            message=decision.message_to_user,
            request_type=RequestType.SCHEDULE_CHANGE,
            success=decision.result.value in ["approved", "approved_with_warnings", "modified"],
            data=decision.to_dict(),
            follow_up_actions=decision.suggestions
        )
    
    async def _handle_voice(self, request: OrchestratorRequest) -> OrchestratorResponse:
        """Handle a voice interaction."""
        # For now, treat voice as chat with transcribed text
        # In production, would use Gemini Live API
        
        return await self._handle_chat(request)
    
    # =========================================================================
    # CONVENIENCE METHODS
    # =========================================================================
    
    async def quick_chat(self, user_id: str, message: str) -> str:
        """Quick chat for simple interactions."""
        response = await self.process(OrchestratorRequest(
            user_id=user_id,
            message=message,
            request_type=RequestType.CHAT
        ))
        return response.message
    
    async def run_demo(self, user_id: str = "demo_user") -> List[Dict[str, Any]]:
        """
        Run a demo sequence showing all Kaironex capabilities.
        
        Perfect for hackathon video!
        """
        demo_interactions = [
            ("Hi Kairo! I'm stressed about my calculus exam next week.", RequestType.CHAT),
            ("Can you research the syllabus for CS 201 Data Structures at Stanford?", RequestType.RESEARCH),
            ("I have a wedding this Saturday - I won't be able to study.", RequestType.LIFE_EVENT),
            ("Move my Friday study session to Sunday evening.", RequestType.SCHEDULE_CHANGE),
            ("I want to land a software engineering internship by summer.", RequestType.MARATHON),
        ]
        
        results = []
        
        for message, request_type in demo_interactions:
            print(f"\n{'='*60}")
            print(f"👤 USER: {message}")
            print(f"{'='*60}")
            
            response = await self.process(OrchestratorRequest(
                user_id=user_id,
                message=message,
                request_type=request_type
            ))
            
            print(f"\n🤖 KAIRO: {response.message}")
            print(f"\n📊 Type: {response.request_type.value} | Tools: {response.tool_calls} | Time: {response.latency_ms:.0f}ms")
            
            results.append({
                "user": message,
                "response": response.to_dict()
            })
            
            await asyncio.sleep(1)  # Pause for demo effect
        
        return results


# =============================================================================
# SINGLETON INSTANCE
# =============================================================================

_orchestrator: Optional[KaironexOrchestrator] = None


def get_orchestrator(db=None) -> KaironexOrchestrator:
    """Get or create the singleton orchestrator instance."""
    global _orchestrator
    if _orchestrator is None:
        _orchestrator = KaironexOrchestrator(db)
    return _orchestrator


async def quick_process(user_id: str, message: str) -> str:
    """Quick process a message through the orchestrator."""
    orchestrator = get_orchestrator()
    response = await orchestrator.quick_chat(user_id, message)
    return response
