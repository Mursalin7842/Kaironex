"""
🎓 STUDY AGENT (v2.0)
=====================
The Cognitive Supply Chain Manager.

This agent handles:
- Real-time study session monitoring
- Focus tracking and intervention
- Knowledge gatekeeper (quiz validation)
- Learning pattern analysis
- Pressure map generation
- Deep research integration
"""

import json
from typing import Dict, Any, List
from datetime import datetime

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext


class StudyAgent(BaseAgent):
    """
    The Study Agent: Cognitive Supply Chain Manager.
    
    Handles event types:
    - session_start: New study session beginning
    - session_end: Study session completed
    - focus_update: Focus score changed
    - quiz_request: Knowledge gatekeeper challenge
    - pressure_check: Analyze current pressure levels
    - resource_ingestion: New learning resource added
    """
    
    STUDY_SYSTEM_PROMPT = """You are the Study Guardian for Kaironex, a Student Life Operating System.

Your role is to protect the student's cognitive supply chain - ensuring knowledge flows smoothly and focus remains sharp.

PERSONA:
- Tone: Encouraging academic coach
- Style: Metacognitive guidance (help them think about thinking)
- Focus: Learning efficiency, not just time spent

RULES:
1. Celebrate focus milestones (30min, 1hr, 2hr streaks)
2. When focus drops, offer OPTIONS not commands
3. Generate quizzes that test understanding, not memorization
4. Track learning patterns over time
5. Never shame for breaks - they're part of the process

OUTPUT FORMAT:
<analyze>Your analysis of their learning state</analyze>
<action>The user-facing guidance</action>

Remember: You're optimizing for long-term retention and sustainable study habits."""

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="study",
            display_name="Study Guardian",
            system_instruction=self.STUDY_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=8192,
            enable_thought_signatures=True,
            enable_marathon=False
        )
    
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Main processing logic for study events."""
        
        self.log_heartbeat(user_id, f"EVENT:STUDY | {payload.get('status', 'unknown')}")
        
        event_type = payload.get('type', payload.get('status', 'focus_update'))
        
        handlers = {
            'session_start': self._handle_session_start,
            'IN_PROGRESS': self._handle_in_progress,
            'session_end': self._handle_session_end,
            'COMPLETED': self._handle_session_end,
            'focus_update': self._handle_focus_update,
            'REQUEST_UNLOCK': self._handle_quiz_request,
            'quiz_request': self._handle_quiz_request,
            'quiz_answer': self._handle_quiz_answer,
            'pressure_check': self._handle_pressure_check,
        }
        
        handler = handlers.get(event_type, self._handle_focus_update)
        return await handler(user_id, payload, context)
    
    async def _handle_session_start(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle study session start."""
        
        topic = payload.get('topic', 'General Study')
        planned_duration = payload.get('planned_duration_minutes', 60)
        
        prompt = f"""
NEW STUDY SESSION STARTING:
Topic: {topic}
Planned Duration: {planned_duration} minutes

TASK:
1. Send a brief motivational start message
2. Set expectations for the session
3. Offer a focus tip for this topic type

Keep it brief (2-3 sentences) and energizing.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.REFLEX
        ))
        
        # Update state
        study_update = {
            "study_session": {
                "is_active": True,
                "topic": topic,
                "started_at": datetime.now().isoformat(),
                "planned_duration": planned_duration,
                "current_focus": 100,
                "focus_history": []
            }
        }
        self.update_state_cache(user_id, study_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["session_started"],
            state_updates=study_update
        )
    
    async def _handle_in_progress(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle in-progress session update."""
        
        focus_score = payload.get('focus_score', 100)
        topic = payload.get('topic', 'Unknown')
        duration = payload.get('duration_seconds', 0)
        
        # Check if intervention needed
        interventions = []
        
        if focus_score < 40:
            prompt = f"""
FOCUS DROP DETECTED:
Topic: {topic}
Current Focus: {focus_score}/100
Session Duration: {duration // 60} minutes

TASK:
Write a 1-sentence empathetic nudge. Options to offer:
- Take a short break
- Switch to active recall
- Try the Pomodoro technique
- Push through for 5 more minutes

Be supportive, not judgmental.
"""
            
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.REFLEX,
                system_instruction=self.STUDY_SYSTEM_PROMPT
            ))
            
            intervention_id = self.create_intervention(
                user_id,
                "FOCUS_DROP",
                response.content,
                strategy="NEGOTIATION"
            )
            interventions.append(intervention_id)
        
        # Update state
        study_update = {
            "study_session": {
                "is_active": True,
                "topic": topic,
                "current_focus": focus_score,
                "session_duration": duration,
                "last_updated": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, study_update)
        
        return AgentResult(
            success=True,
            response=response.content if interventions else "Focus tracked.",
            actions_taken=["focus_tracked"] + (["intervention_sent"] if interventions else []),
            interventions_created=interventions,
            state_updates=study_update
        )
    
    async def _handle_session_end(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle study session completion."""
        
        topic = payload.get('topic', 'Study Session')
        duration = payload.get('duration_seconds', 0)
        avg_focus = payload.get('average_focus', 70)
        
        duration_mins = duration // 60
        
        prompt = f"""
STUDY SESSION COMPLETED:
Topic: {topic}
Duration: {duration_mins} minutes
Average Focus: {avg_focus}/100

TASK:
1. Celebrate the completion
2. Assess session quality based on focus
3. Suggest what to do next (rest, review, or continue)

Keep it brief and positive.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.REFLEX
        ))
        
        # Create celebration for long sessions
        interventions = []
        if duration_mins >= 60:
            intervention_id = self.create_intervention(
                user_id,
                "SESSION_COMPLETE",
                f"🎉 **{duration_mins}-Minute Session Complete!**\n\n{response.content}",
                strategy="CELEBRATION"
            )
            interventions.append(intervention_id)
        
        # Update state
        study_update = {
            "study_session": {
                "is_active": False,
                "last_topic": topic,
                "last_duration": duration_mins,
                "last_focus": avg_focus,
                "completed_at": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, study_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["session_completed", "stats_recorded"],
            interventions_created=interventions,
            state_updates=study_update
        )
    
    async def _handle_focus_update(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle generic focus update."""
        # Delegate to in_progress handler
        return await self._handle_in_progress(user_id, payload, context)
    
    async def _handle_quiz_request(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Generate a quiz question (Knowledge Gatekeeper)."""
        
        topic = payload.get('topic', 'the current subject')
        difficulty = payload.get('difficulty', 'medium')
        
        prompt = f"""
KNOWLEDGE GATEKEEPER QUIZ:
Topic: {topic}
Difficulty: {difficulty}

Generate a thoughtful multiple-choice question that tests UNDERSTANDING, not just memorization.

Return as JSON:
{{
    "question": "The question text",
    "options": ["A. Option 1", "B. Option 2", "C. Option 3", "D. Option 4"],
    "correct": "A",
    "explanation": "Why this is the correct answer"
}}
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.DEEP  # Use deep for better questions
        ))
        
        # Parse quiz data
        quiz_data = self._parse_quiz(response.content)
        
        return AgentResult(
            success=True,
            response=json.dumps(quiz_data),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["quiz_generated"],
            state_updates={"pending_quiz": quiz_data}
        )
    
    async def _handle_quiz_answer(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Process quiz answer and provide feedback."""
        
        user_answer = payload.get('answer')
        correct_answer = payload.get('correct')
        question = payload.get('question', '')
        explanation = payload.get('explanation', '')
        
        is_correct = user_answer == correct_answer
        
        if is_correct:
            prompt = f"User answered correctly! Generate a brief (1 sentence) celebration."
        else:
            prompt = f"""
User answered '{user_answer}' but the correct answer was '{correct_answer}'.

Question: {question}
Explanation: {explanation}

Generate a supportive response that:
1. Doesn't shame them
2. Explains why the correct answer is right
3. Encourages them to try again or continue studying
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.REFLEX
        ))
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["quiz_answered", f"result_{'correct' if is_correct else 'incorrect'}"],
            state_updates={"last_quiz_result": is_correct}
        )
    
    async def _handle_pressure_check(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Analyze current academic pressure."""
        
        # Get user state
        user_doc = self.db.get_user_doc(user_id)
        user_state = {}
        if user_doc:
            try:
                user_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        prompt = f"""
PRESSURE MAP ANALYSIS:
User State: {json.dumps(user_state, indent=2)[:1000]}

TASK:
1. Identify top 3 pressure sources
2. Estimate overall pressure level (1-100)
3. Suggest one immediate action to reduce pressure

Be specific and actionable.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.DEEP
        ))
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["pressure_analyzed"]
        )
    
    def _parse_quiz(self, content: str) -> Dict[str, Any]:
        """Parse quiz JSON from AI response."""
        import re
        
        # Try to extract JSON
        json_match = re.search(r'\{[\s\S]*\}', content)
        if json_match:
            try:
                return json.loads(json_match.group())
            except json.JSONDecodeError:
                pass
        
        # Fallback
        return {
            "question": "What did you learn in this session?",
            "options": ["A. Everything", "B. Something", "C. A little", "D. Need review"],
            "correct": "A",
            "explanation": "Reflect on your learning!"
        }


# Legacy compatibility wrapper
def run_study_agent(db_helper, payload, context):
    """Legacy compatibility wrapper."""
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    agent = StudyAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('status', payload.get('type', 'focus_update'))
    
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    result = loop.run_until_complete(
        agent.run(user_id, payload, trigger_event)
    )
    
    return context.res.json(result.to_dict())
