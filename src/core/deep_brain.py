"""
🧠 DEEP BRAIN
=============
The core reasoning engine for complex, thoughtful tasks.

This is the cloud-side brain that handles:
- Long-form planning and strategy
- Marathon sessions (multi-step goals)
- Deep research with Google Search grounding
- Complex reasoning that requires HIGH thinking

The Reflex Agent lives in the mobile app - NOT here.
Communication happens via Appwrite Database.
"""

import os
import json
import uuid
import hashlib
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
from dataclasses import dataclass, field
from enum import Enum

from google import genai
from google.genai import types

from ..config import (
    GEMINI_API_KEY,
    GEMINI_3_FLASH,
    THINKING_LEVEL_DEEP,
    APPWRITE_DATABASE_ID,
    MARATHON_SESSIONS_COL,
    THOUGHT_SIGNATURES_COL,
    AGENT_MEMORY_COL
)
from ..utils.db_helper import KairoDB


class ThinkingLevel(Enum):
    """Thinking levels for Gemini 3 Flash."""
    MINIMAL = "MINIMAL"  # Fast, pattern-matching (for Reflex in app)
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"        # Full reasoning (for Deep Brain)


@dataclass
class ThoughtSignature:
    """Cryptographic chain of reasoning for verification."""
    thought_id: str
    user_id: str
    agent: str
    timestamp: datetime
    context_hash: str
    reasoning_trace: List[str]
    confidence: float
    action_output: str
    parent_id: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "thoughtId": self.thought_id,
            "userId": self.user_id,
            "agentType": self.agent,
            "signature_json": json.dumps({
                "context_hash": self.context_hash,
                "reasoning_trace": self.reasoning_trace,
                "confidence": self.confidence
            }),
            "parent_thought_id": self.parent_id,
            "reasoning_trace": "\n".join(self.reasoning_trace),
            "confidence": self.confidence,
            "created_at": self.timestamp.isoformat()
        }


@dataclass
class MarathonSession:
    """A long-running goal with multiple steps."""
    session_id: str
    user_id: str
    agent_type: str
    title: str
    description: str
    success_criteria: List[str]
    status: str  # pending, running, paused, complete, failed
    steps: List[Dict[str, Any]] = field(default_factory=list)
    current_step: int = 0
    progress: float = 0.0
    thought_chain: List[str] = field(default_factory=list)
    created_at: datetime = field(default_factory=datetime.now)
    last_active: datetime = field(default_factory=datetime.now)
    deadline: Optional[datetime] = None
    priority: int = 5
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "sessionId": self.session_id,
            "userId": self.user_id,
            "agentType": self.agent_type,
            "goal_json": json.dumps({
                "title": self.title,
                "description": self.description,
                "success_criteria": self.success_criteria,
                "deadline": self.deadline.isoformat() if self.deadline else None,
                "priority": self.priority
            }),
            "state_json": json.dumps({
                "steps": self.steps,
                "current_step": self.current_step
            }),
            "thought_chain_json": json.dumps(self.thought_chain),
            "progress": self.progress,
            "status": self.status,
            "started_at": self.created_at.isoformat(),
            "last_active_at": self.last_active.isoformat(),
            "estimated_completion": None
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'MarathonSession':
        goal = json.loads(data.get('goal_json', '{}'))
        state = json.loads(data.get('state_json', '{}'))
        thoughts = json.loads(data.get('thought_chain_json', '[]'))
        
        return cls(
            session_id=data['sessionId'],
            user_id=data['userId'],
            agent_type=data.get('agentType', 'campaign'),
            title=goal.get('title', ''),
            description=goal.get('description', ''),
            success_criteria=goal.get('success_criteria', []),
            status=data.get('status', 'pending'),
            steps=state.get('steps', []),
            current_step=state.get('current_step', 0),
            progress=data.get('progress', 0.0),
            thought_chain=thoughts,
            deadline=datetime.fromisoformat(goal['deadline']) if goal.get('deadline') else None,
            priority=goal.get('priority', 5)
        )


class DeepBrain:
    """
    The Deep Brain handles complex reasoning tasks.
    
    This is the cloud-side component that:
    - Plans multi-step goals (marathons)
    - Performs deep research
    - Makes strategic decisions
    - Generates thought signatures for verification
    
    The Reflex Agent (in the app) handles:
    - Quick responses
    - Pattern matching
    - Caching
    - User-facing interactions
    """
    
    def __init__(self, db: KairoDB):
        self.db = db
        self.client = genai.Client(
            api_key=GEMINI_API_KEY,
            http_options={'api_version': 'v1beta'}
        )
        self.model = GEMINI_3_FLASH
    
    # =========================================================================
    # CORE REASONING
    # =========================================================================
    
    def reason(
        self,
        prompt: str,
        user_id: str,
        agent: str = "deep",
        context: Optional[Dict[str, Any]] = None,
        use_search: bool = False
    ) -> Dict[str, Any]:
        """
        Perform deep reasoning with HIGH thinking level.
        
        Args:
            prompt: The reasoning prompt
            user_id: User identifier
            agent: Agent type for context
            context: Additional context data
            use_search: Whether to use Google Search grounding
        
        Returns:
            Dict with response, thought_id, confidence
        """
        start_time = datetime.now()
        
        # Build context hash for signature
        context_str = json.dumps(context or {}, sort_keys=True)
        context_hash = hashlib.sha256(
            f"{user_id}:{agent}:{prompt}:{context_str}".encode()
        ).hexdigest()[:16]
        
        # Build config with HIGH thinking
        config = types.GenerateContentConfig(
            thinking_config=types.ThinkingConfig(
                thinking_budget=8192  # HIGH thinking
            ),
            temperature=0.7,
            max_output_tokens=4096
        )
        
        # Add Google Search if requested
        tools = None
        if use_search:
            tools = [types.Tool(google_search=types.GoogleSearch())]
        
        # Generate response
        try:
            response = self.client.models.generate_content(
                model=self.model,
                contents=prompt,
                config=config
            )
            
            content = response.text
            
            # Extract reasoning trace from thinking
            reasoning_trace = []
            if hasattr(response, 'candidates') and response.candidates:
                for part in response.candidates[0].content.parts:
                    if hasattr(part, 'thought') and part.thought:
                        reasoning_trace.append(part.text)
            
            # Calculate confidence based on response quality
            confidence = self._calculate_confidence(content, reasoning_trace)
            
        except Exception as e:
            error_msg = str(e)
            print(f"❌ Deep Brain Error: {error_msg}")
            
            # Fallback response
            content = f"I encountered an issue processing this request: {error_msg}"
            reasoning_trace = [f"Error: {error_msg}"]
            confidence = 0.0
        
        # Create thought signature
        thought = ThoughtSignature(
            thought_id=f"thought_{uuid.uuid4().hex[:12]}",
            user_id=user_id,
            agent=agent,
            timestamp=datetime.now(),
            context_hash=context_hash,
            reasoning_trace=reasoning_trace,
            confidence=confidence,
            action_output=content[:500]
        )
        
        # Persist thought signature
        self._save_thought(thought)
        
        latency_ms = (datetime.now() - start_time).total_seconds() * 1000
        
        return {
            "response": content,
            "thought_id": thought.thought_id,
            "confidence": confidence,
            "reasoning_trace": reasoning_trace,
            "latency_ms": latency_ms
        }
    
    def _calculate_confidence(self, content: str, reasoning: List[str]) -> float:
        """Calculate confidence score based on response quality."""
        score = 0.5  # Base score
        
        # Longer, more detailed responses are more confident
        if len(content) > 500:
            score += 0.1
        if len(content) > 1000:
            score += 0.1
        
        # More reasoning steps = more confident
        if len(reasoning) >= 3:
            score += 0.1
        if len(reasoning) >= 5:
            score += 0.1
        
        # Cap at 0.95
        return min(score, 0.95)
    
    def _save_thought(self, thought: ThoughtSignature):
        """Persist thought signature to database."""
        try:
            self.db.db.create_row(
                APPWRITE_DATABASE_ID,
                THOUGHT_SIGNATURES_COL,
                'unique()',
                thought.to_dict()
            )
        except Exception as e:
            print(f"⚠️ Failed to save thought: {e}")
    
    # =========================================================================
    # MARATHON MANAGEMENT
    # =========================================================================
    
    def create_marathon(
        self,
        user_id: str,
        agent_type: str,
        title: str,
        description: str,
        success_criteria: List[str],
        deadline: Optional[str] = None,
        priority: int = 5
    ) -> Dict[str, Any]:
        """
        Create a new marathon session.
        
        A marathon is a multi-step goal that spans time.
        """
        session_id = f"marathon_{uuid.uuid4().hex[:12]}"
        
        # Parse deadline if provided
        deadline_dt = None
        if deadline:
            try:
                deadline_dt = datetime.fromisoformat(deadline)
            except:
                pass
        
        session = MarathonSession(
            session_id=session_id,
            user_id=user_id,
            agent_type=agent_type,
            title=title,
            description=description,
            success_criteria=success_criteria,
            status="pending",
            deadline=deadline_dt,
            priority=priority
        )
        
        # Plan the marathon steps
        steps = self._plan_marathon_steps(session)
        session.steps = steps
        
        # Persist to database
        self._save_marathon(session)
        
        return {
            "session_id": session_id,
            "title": title,
            "steps": len(steps),
            "status": "pending"
        }
    
    def _plan_marathon_steps(self, session: MarathonSession) -> List[Dict[str, Any]]:
        """Use AI to plan marathon steps."""
        prompt = f"""
You are planning a multi-step goal for a student.

GOAL: {session.title}
DESCRIPTION: {session.description}
SUCCESS CRITERIA:
{chr(10).join(f'- {c}' for c in session.success_criteria)}

Create 3-7 concrete, actionable steps to achieve this goal.
For each step, provide:
1. A clear title (max 50 chars)
2. A description of what to do
3. Estimated time in minutes

Respond in JSON format:
[
  {{"title": "Step 1 Title", "description": "What to do", "estimated_minutes": 30}},
  ...
]
"""
        
        try:
            response = self.reason(
                prompt=prompt,
                user_id=session.user_id,
                agent="marathon_planner"
            )
            
            # Parse JSON from response
            content = response['response']
            
            # Find JSON array in response
            start = content.find('[')
            end = content.rfind(']') + 1
            if start >= 0 and end > start:
                steps = json.loads(content[start:end])
                return [
                    {
                        "step_id": f"step_{i+1}",
                        "title": s.get('title', f'Step {i+1}'),
                        "description": s.get('description', ''),
                        "estimated_minutes": s.get('estimated_minutes', 30),
                        "status": "pending"
                    }
                    for i, s in enumerate(steps)
                ]
        except Exception as e:
            print(f"⚠️ Step planning error: {e}")
        
        # Fallback: create basic steps
        return [
            {"step_id": "step_1", "title": "Start", "description": "Begin the task", "status": "pending"},
            {"step_id": "step_2", "title": "Execute", "description": "Complete the main work", "status": "pending"},
            {"step_id": "step_3", "title": "Finish", "description": "Finalize and verify", "status": "pending"}
        ]
    
    def _save_marathon(self, session: MarathonSession):
        """Persist marathon session to database."""
        try:
            self.db.db.create_row(
                APPWRITE_DATABASE_ID,
                MARATHON_SESSIONS_COL,
                'unique()',
                session.to_dict()
            )
            print(f"💾 Marathon saved: {session.session_id}")
        except Exception as e:
            print(f"⚠️ Failed to save marathon: {e}")
    
    def _update_marathon(self, session: MarathonSession):
        """Update existing marathon in database."""
        try:
            # Find existing document
            from appwrite.query import Query
            results = self.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=MARATHON_SESSIONS_COL,
                queries=[Query.equal('sessionId', session.session_id)]
            )
            
            if results['total'] > 0:
                doc_id = results['rows'][0]['$id']
                self.db.db.update_row(
                    APPWRITE_DATABASE_ID,
                    MARATHON_SESSIONS_COL,
                    doc_id,
                    session.to_dict()
                )
        except Exception as e:
            print(f"⚠️ Failed to update marathon: {e}")
    
    def get_marathon(self, session_id: str) -> Optional[Dict[str, Any]]:
        """Get marathon session by ID."""
        try:
            from appwrite.query import Query
            results = self.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=MARATHON_SESSIONS_COL,
                queries=[Query.equal('sessionId', session_id)]
            )
            
            if results['total'] > 0:
                data = results['rows'][0]
                session = MarathonSession.from_dict(data)
                return session.to_dict()
            
            return None
        except Exception as e:
            print(f"⚠️ Failed to get marathon: {e}")
            return None
    
    def execute_marathon_step(self, session_id: str) -> Dict[str, Any]:
        """Execute the next step in a marathon."""
        try:
            from appwrite.query import Query
            results = self.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=MARATHON_SESSIONS_COL,
                queries=[Query.equal('sessionId', session_id)]
            )
            
            if results['total'] == 0:
                return {"error": "Marathon not found"}
            
            session = MarathonSession.from_dict(results['rows'][0])
            
            if session.current_step >= len(session.steps):
                session.status = "complete"
                self._update_marathon(session)
                return {"status": "complete", "message": "All steps completed"}
            
            # Get current step
            step = session.steps[session.current_step]
            
            # Execute step with AI
            prompt = f"""
You are executing step {session.current_step + 1} of a marathon goal.

GOAL: {session.title}
STEP: {step['title']}
DESCRIPTION: {step['description']}

Provide a concrete action or output for this step.
Be specific and actionable.
"""
            
            result = self.reason(
                prompt=prompt,
                user_id=session.user_id,
                agent=session.agent_type
            )
            
            # Update step status
            step['status'] = 'complete'
            step['result'] = result['response'][:500]
            step['thought_id'] = result['thought_id']
            
            # Add to thought chain
            session.thought_chain.append(result['thought_id'])
            
            # Advance to next step
            session.current_step += 1
            session.progress = session.current_step / len(session.steps)
            session.last_active = datetime.now()
            
            if session.current_step >= len(session.steps):
                session.status = "complete"
            else:
                session.status = "running"
            
            # Save updated session
            self._update_marathon(session)
            
            return {
                "status": session.status,
                "step_completed": step['title'],
                "result": step['result'],
                "progress": session.progress,
                "thought_id": result['thought_id']
            }
            
        except Exception as e:
            print(f"❌ Marathon step error: {e}")
            return {"error": str(e)}
    
    # =========================================================================
    # PRESSURE INDEX
    # =========================================================================
    
    def update_pressure(self, user_id: str, delta: int = 0, absolute: Optional[int] = None):
        """Update user's pressure index."""
        try:
            from appwrite.query import Query
            results = self.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=AGENT_MEMORY_COL,
                queries=[Query.equal('userId', user_id)]
            )
            
            if results['total'] > 0:
                doc = results['rows'][0]
                current = doc.get('pressure_index', 50)
                
                if absolute is not None:
                    new_pressure = max(0, min(100, absolute))
                else:
                    new_pressure = max(0, min(100, current + delta))
                
                self.db.db.update_row(
                    APPWRITE_DATABASE_ID,
                    AGENT_MEMORY_COL,
                    doc['$id'],
                    {'pressure_index': new_pressure}
                )
        except Exception as e:
            print(f"⚠️ Pressure update error: {e}")
