"""
🏃 MARATHON RUNNER
==================
Orchestrates long-running agent tasks that span hours or days.

A Marathon is a multi-step goal that requires:
- Persistent state across server restarts
- Progress tracking and checkpointing
- Self-correction and adaptation
- Real-time progress streaming

Examples:
- "Get a job interview at Google" (72-hour campaign)
- "Complete my thesis outline" (week-long study marathon)
- "Optimize my sleep schedule" (ongoing vitality marathon)
"""

import asyncio
import json
import uuid
from enum import Enum
from typing import Optional, Dict, Any, List, Callable, Awaitable
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from abc import ABC, abstractmethod

from .bicameral_engine import BicameralEngine, ReasoningRequest, ReasoningMode, ThoughtSignature
from .thought_manager import ThoughtManager


class MarathonStatus(Enum):
    PENDING = "pending"
    RUNNING = "running"
    PAUSED = "paused"
    WAITING = "waiting"      # Waiting for user input
    THINKING = "thinking"    # Deep reasoning in progress
    ACTING = "acting"        # Executing an action
    COMPLETE = "complete"
    FAILED = "failed"
    CANCELLED = "cancelled"


@dataclass
class MarathonStep:
    """A single step in a marathon."""
    step_id: str
    title: str
    description: str
    status: MarathonStatus
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    thought_id: Optional[str] = None
    result: Optional[str] = None
    error: Optional[str] = None
    
    @property
    def duration_seconds(self) -> float:
        if not self.started_at:
            return 0
        end = self.completed_at or datetime.now()
        return (end - self.started_at).total_seconds()


@dataclass
class MarathonGoal:
    """The goal definition for a marathon."""
    title: str
    description: str
    success_criteria: List[str]
    deadline: Optional[datetime] = None
    priority: int = 5  # 1-10 scale
    context: Dict[str, Any] = field(default_factory=dict)
    
    def to_prompt(self) -> str:
        criteria_str = "\n".join(f"  - {c}" for c in self.success_criteria)
        deadline_str = f"\nDeadline: {self.deadline.isoformat()}" if self.deadline else ""
        
        return f"""
MARATHON GOAL: {self.title}
{self.description}

Success Criteria:
{criteria_str}
{deadline_str}
Priority: {self.priority}/10

Context: {json.dumps(self.context, indent=2)}
"""


@dataclass
class MarathonSession:
    """
    A marathon session tracks a long-running goal.
    
    This is the primary state object that gets persisted
    and restored across server restarts.
    """
    session_id: str
    user_id: str
    agent_type: str
    goal: MarathonGoal
    status: MarathonStatus
    steps: List[MarathonStep] = field(default_factory=list)
    current_step_index: int = 0
    progress: float = 0.0  # 0.0 to 1.0
    thought_chain: List[str] = field(default_factory=list)  # Thought IDs
    created_at: datetime = field(default_factory=datetime.now)
    last_active_at: datetime = field(default_factory=datetime.now)
    estimated_completion: Optional[datetime] = None
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    @property
    def current_step(self) -> Optional[MarathonStep]:
        if 0 <= self.current_step_index < len(self.steps):
            return self.steps[self.current_step_index]
        return None
    
    @property
    def is_active(self) -> bool:
        return self.status in [
            MarathonStatus.RUNNING,
            MarathonStatus.THINKING,
            MarathonStatus.ACTING,
            MarathonStatus.WAITING
        ]
    
    @property
    def duration_hours(self) -> float:
        return (datetime.now() - self.created_at).total_seconds() / 3600
    
    def add_step(self, title: str, description: str) -> MarathonStep:
        step = MarathonStep(
            step_id=f"step_{len(self.steps) + 1}_{uuid.uuid4().hex[:6]}",
            title=title,
            description=description,
            status=MarathonStatus.PENDING
        )
        self.steps.append(step)
        return step
    
    def advance(self):
        """Move to the next step."""
        if self.current_step:
            self.current_step.status = MarathonStatus.COMPLETE
            self.current_step.completed_at = datetime.now()
        
        self.current_step_index += 1
        self.progress = self.current_step_index / max(len(self.steps), 1)
        self.last_active_at = datetime.now()
        
        if self.current_step_index >= len(self.steps):
            self.status = MarathonStatus.COMPLETE
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "session_id": self.session_id,
            "user_id": self.user_id,
            "agent_type": self.agent_type,
            "goal": {
                "title": self.goal.title,
                "description": self.goal.description,
                "success_criteria": self.goal.success_criteria,
                "deadline": self.goal.deadline.isoformat() if self.goal.deadline else None,
                "priority": self.goal.priority,
                "context": self.goal.context
            },
            "status": self.status.value,
            "steps": [
                {
                    "step_id": s.step_id,
                    "title": s.title,
                    "description": s.description,
                    "status": s.status.value,
                    "started_at": s.started_at.isoformat() if s.started_at else None,
                    "completed_at": s.completed_at.isoformat() if s.completed_at else None,
                    "thought_id": s.thought_id,
                    "result": s.result,
                    "error": s.error
                }
                for s in self.steps
            ],
            "current_step_index": self.current_step_index,
            "progress": self.progress,
            "thought_chain": self.thought_chain,
            "created_at": self.created_at.isoformat(),
            "last_active_at": self.last_active_at.isoformat(),
            "estimated_completion": self.estimated_completion.isoformat() if self.estimated_completion else None,
            "metadata": self.metadata
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'MarathonSession':
        goal_data = data["goal"]
        goal = MarathonGoal(
            title=goal_data["title"],
            description=goal_data["description"],
            success_criteria=goal_data.get("success_criteria", []),
            deadline=datetime.fromisoformat(goal_data["deadline"]) if goal_data.get("deadline") else None,
            priority=goal_data.get("priority", 5),
            context=goal_data.get("context", {})
        )
        
        steps = [
            MarathonStep(
                step_id=s["step_id"],
                title=s["title"],
                description=s["description"],
                status=MarathonStatus(s["status"]),
                started_at=datetime.fromisoformat(s["started_at"]) if s.get("started_at") else None,
                completed_at=datetime.fromisoformat(s["completed_at"]) if s.get("completed_at") else None,
                thought_id=s.get("thought_id"),
                result=s.get("result"),
                error=s.get("error")
            )
            for s in data.get("steps", [])
        ]
        
        return cls(
            session_id=data["session_id"],
            user_id=data["user_id"],
            agent_type=data["agent_type"],
            goal=goal,
            status=MarathonStatus(data["status"]),
            steps=steps,
            current_step_index=data.get("current_step_index", 0),
            progress=data.get("progress", 0.0),
            thought_chain=data.get("thought_chain", []),
            created_at=datetime.fromisoformat(data["created_at"]),
            last_active_at=datetime.fromisoformat(data["last_active_at"]),
            estimated_completion=datetime.fromisoformat(data["estimated_completion"]) if data.get("estimated_completion") else None,
            metadata=data.get("metadata", {})
        )


class MarathonCallback(ABC):
    """Callback interface for marathon events."""
    
    @abstractmethod
    async def on_step_start(self, session: MarathonSession, step: MarathonStep):
        pass
    
    @abstractmethod
    async def on_step_complete(self, session: MarathonSession, step: MarathonStep):
        pass
    
    @abstractmethod
    async def on_thought(self, session: MarathonSession, thought: ThoughtSignature):
        pass
    
    @abstractmethod
    async def on_progress(self, session: MarathonSession, progress: float):
        pass
    
    @abstractmethod
    async def on_complete(self, session: MarathonSession):
        pass
    
    @abstractmethod
    async def on_error(self, session: MarathonSession, error: str):
        pass


class MarathonRunner:
    """
    Orchestrates marathon sessions across multiple agents.
    
    The runner is responsible for:
    1. Creating and decomposing goals into steps
    2. Executing steps with the bicameral engine
    3. Managing checkpoints and recovery
    4. Streaming progress to clients
    
    Usage:
        runner = MarathonRunner(engine, thought_manager, db_helper)
        
        session = await runner.create_marathon(
            user_id="user_123",
            agent_type="campaign",
            goal=MarathonGoal(
                title="Get a job at Google",
                description="...",
                success_criteria=["Received interview invite"]
            )
        )
        
        # Run in background
        asyncio.create_task(runner.run(session.session_id))
    """
    
    # Configuration
    STEP_TIMEOUT_SECONDS = 300  # 5 minutes per step
    MAX_RETRIES = 3
    CHECKPOINT_INTERVAL_STEPS = 5
    
    def __init__(
        self,
        engine: BicameralEngine,
        thought_manager: ThoughtManager,
        db_helper=None
    ):
        self.engine = engine
        self.thoughts = thought_manager
        self.db = db_helper
        
        self._sessions: Dict[str, MarathonSession] = {}
        self._callbacks: Dict[str, List[MarathonCallback]] = {}
        self._running: Dict[str, asyncio.Task] = {}
    
    async def create_marathon(
        self,
        user_id: str,
        agent_type: str,
        goal: MarathonGoal
    ) -> MarathonSession:
        """
        Create a new marathon session.
        
        This will:
        1. Use deep reasoning to decompose the goal into steps
        2. Create the session object
        3. Persist to database
        """
        session_id = f"marathon_{user_id}_{agent_type}_{uuid.uuid4().hex[:8]}"
        
        # Use deep reasoning to decompose the goal
        decomposition_prompt = f"""
You are planning a marathon goal for a student.

{goal.to_prompt()}

TASK: Decompose this goal into 5-15 concrete, actionable steps.

OUTPUT FORMAT:
Return a JSON array of steps:
[
  {{"title": "Step title", "description": "What to do"}},
  ...
]

Each step should be:
- Specific and actionable
- Completable in 30-60 minutes
- Have a clear success indicator
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=decomposition_prompt,
            user_id=user_id,
            agent=agent_type,
            mode=ReasoningMode.DEEP,
            context=goal.context
        ))
        
        # Parse steps from response
        steps_data = self._parse_steps(response.content)
        
        # Create session
        session = MarathonSession(
            session_id=session_id,
            user_id=user_id,
            agent_type=agent_type,
            goal=goal,
            status=MarathonStatus.PENDING
        )
        
        # Add parsed steps
        for step_data in steps_data:
            session.add_step(
                title=step_data.get("title", "Unnamed Step"),
                description=step_data.get("description", "")
            )
        
        # Store initial thought
        if response.thought_signature:
            session.thought_chain.append(response.thought_signature.thought_id)
            await self.thoughts.store(response.thought_signature)
        
        # Estimate completion
        session.estimated_completion = datetime.now() + timedelta(
            hours=len(session.steps) * 0.5  # Rough estimate
        )
        
        # Cache session
        self._sessions[session_id] = session
        
        # Persist to database
        await self._persist_session(session)
        
        return session
    
    def _parse_steps(self, content: str) -> List[Dict[str, str]]:
        """Parse step definitions from AI response."""
        import re
        
        # Try to extract JSON array
        json_match = re.search(r'\[[\s\S]*\]', content)
        if json_match:
            try:
                return json.loads(json_match.group())
            except json.JSONDecodeError:
                pass
        
        # Fallback: parse numbered list
        steps = []
        lines = content.split('\n')
        current_step = None
        
        for line in lines:
            # Match "1.", "Step 1:", etc.
            if re.match(r'^(\d+[\.\):]|Step \d+)', line.strip()):
                if current_step:
                    steps.append(current_step)
                current_step = {"title": line.strip(), "description": ""}
            elif current_step and line.strip():
                current_step["description"] += line.strip() + " "
        
        if current_step:
            steps.append(current_step)
        
        # Fallback: create generic steps
        if not steps:
            steps = [
                {"title": "Analyze the goal", "description": "Understand what needs to be done"},
                {"title": "Plan the approach", "description": "Create a strategy"},
                {"title": "Execute the plan", "description": "Take action"},
                {"title": "Review and iterate", "description": "Check progress and adjust"},
                {"title": "Complete and reflect", "description": "Finish and learn from the experience"}
            ]
        
        return steps
    
    async def run(
        self,
        session_id: str,
        callback: Optional[MarathonCallback] = None
    ):
        """
        Run a marathon session.
        
        This is the main execution loop that:
        1. Iterates through steps
        2. Executes each step with deep reasoning
        3. Handles errors and retries
        4. Reports progress
        """
        session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"Session not found: {session_id}")
        
        if callback:
            if session_id not in self._callbacks:
                self._callbacks[session_id] = []
            self._callbacks[session_id].append(callback)
        
        session.status = MarathonStatus.RUNNING
        
        try:
            while session.current_step_index < len(session.steps):
                step = session.current_step
                if not step:
                    break
                
                # Notify: step starting
                await self._notify_step_start(session, step)
                
                step.status = MarathonStatus.THINKING
                step.started_at = datetime.now()
                session.status = MarathonStatus.THINKING
                
                # Execute the step
                success = await self._execute_step(session, step)
                
                if success:
                    session.advance()
                    await self._notify_step_complete(session, step)
                    await self._notify_progress(session, session.progress)
                else:
                    # Step failed
                    session.status = MarathonStatus.FAILED
                    await self._notify_error(session, step.error or "Step execution failed")
                    break
                
                # Checkpoint periodically
                if session.current_step_index % self.CHECKPOINT_INTERVAL_STEPS == 0:
                    await self._persist_session(session)
            
            # Marathon complete
            if session.status == MarathonStatus.RUNNING:
                session.status = MarathonStatus.COMPLETE
            
            await self._notify_complete(session)
            await self._persist_session(session)
            
        except asyncio.CancelledError:
            session.status = MarathonStatus.CANCELLED
            await self._persist_session(session)
            raise
            
        except Exception as e:
            session.status = MarathonStatus.FAILED
            await self._notify_error(session, str(e))
            await self._persist_session(session)
            raise
    
    async def _execute_step(
        self,
        session: MarathonSession,
        step: MarathonStep,
        retry_count: int = 0
    ) -> bool:
        """Execute a single marathon step."""
        try:
            # Get reasoning context from previous thoughts
            reasoning_context = await self.thoughts.get_reasoning_context(
                session.user_id,
                session.agent_type
            )
            
            # Build step execution prompt
            step_prompt = f"""
{reasoning_context}

MARATHON: {session.goal.title}
CURRENT STEP: {step.title}
STEP DESCRIPTION: {step.description}

Progress: {session.progress * 100:.1f}% ({session.current_step_index + 1}/{len(session.steps)} steps)

PREVIOUS STEPS COMPLETED:
{chr(10).join(f"✓ {s.title}" for s in session.steps[:session.current_step_index])}

TASK: Execute this step. Provide:
1. Your analysis of what needs to be done
2. The specific actions you're taking
3. The outcome/result of this step
4. What the next step should focus on

Be thorough but concise.
"""
            
            session.status = MarathonStatus.THINKING
            
            response = await self.engine.reason(ReasoningRequest(
                prompt=step_prompt,
                user_id=session.user_id,
                agent=session.agent_type,
                mode=ReasoningMode.MARATHON,
                parent_thought=session.thought_chain[-1] if session.thought_chain else None,
                context={
                    "marathon_id": session.session_id,
                    "step_id": step.step_id,
                    "goal": session.goal.title,
                    "progress": session.progress
                }
            ))
            
            session.status = MarathonStatus.ACTING
            
            # Store thought
            if response.thought_signature:
                step.thought_id = response.thought_signature.thought_id
                session.thought_chain.append(response.thought_signature.thought_id)
                await self.thoughts.store(response.thought_signature)
                await self._notify_thought(session, response.thought_signature)
            
            step.result = response.content
            step.status = MarathonStatus.COMPLETE
            
            return True
            
        except Exception as e:
            if retry_count < self.MAX_RETRIES:
                await asyncio.sleep(2 ** retry_count)  # Exponential backoff
                return await self._execute_step(session, step, retry_count + 1)
            
            step.error = str(e)
            step.status = MarathonStatus.FAILED
            return False
    
    async def pause(self, session_id: str):
        """Pause a running marathon."""
        session = self._sessions.get(session_id)
        if session and session.is_active:
            session.status = MarathonStatus.PAUSED
            
            # Cancel the running task
            if session_id in self._running:
                self._running[session_id].cancel()
                del self._running[session_id]
            
            await self._persist_session(session)
    
    async def resume(self, session_id: str):
        """Resume a paused marathon."""
        session = self._sessions.get(session_id)
        if session and session.status == MarathonStatus.PAUSED:
            session.status = MarathonStatus.RUNNING
            
            # Restart the run task
            task = asyncio.create_task(self.run(session_id))
            self._running[session_id] = task
    
    async def cancel(self, session_id: str):
        """Cancel a marathon."""
        await self.pause(session_id)
        session = self._sessions.get(session_id)
        if session:
            session.status = MarathonStatus.CANCELLED
            await self._persist_session(session)
    
    async def get_session(self, session_id: str) -> Optional[MarathonSession]:
        """Get a session by ID."""
        return self._sessions.get(session_id)
    
    async def get_user_marathons(self, user_id: str) -> List[MarathonSession]:
        """Get all marathons for a user."""
        return [
            session for session in self._sessions.values()
            if session.user_id == user_id
        ]
    
    async def _persist_session(self, session: MarathonSession):
        """Persist session to database."""
        if not self.db:
            return
        
        # TODO: Implement Appwrite persistence for marathon_sessions table
        pass
    
    async def _load_session(self, session_id: str) -> Optional[MarathonSession]:
        """Load session from database."""
        # TODO: Implement Appwrite loading
        return self._sessions.get(session_id)
    
    # Notification helpers
    async def _notify_step_start(self, session: MarathonSession, step: MarathonStep):
        for cb in self._callbacks.get(session.session_id, []):
            await cb.on_step_start(session, step)
    
    async def _notify_step_complete(self, session: MarathonSession, step: MarathonStep):
        for cb in self._callbacks.get(session.session_id, []):
            await cb.on_step_complete(session, step)
    
    async def _notify_thought(self, session: MarathonSession, thought: ThoughtSignature):
        for cb in self._callbacks.get(session.session_id, []):
            await cb.on_thought(session, thought)
    
    async def _notify_progress(self, session: MarathonSession, progress: float):
        for cb in self._callbacks.get(session.session_id, []):
            await cb.on_progress(session, progress)
    
    async def _notify_complete(self, session: MarathonSession):
        for cb in self._callbacks.get(session.session_id, []):
            await cb.on_complete(session)
    
    async def _notify_error(self, session: MarathonSession, error: str):
        for cb in self._callbacks.get(session.session_id, []):
            await cb.on_error(session, error)
