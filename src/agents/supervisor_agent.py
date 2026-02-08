"""
🧠 SUPERVISOR AGENT (v2.0)
==========================
The Meta-Controller and Drift Detection System.

This agent orchestrates all other agents:
- Monitors overall student wellness
- Detects drift from goals
- Triggers proactive interventions
- Coordinates agent responses
- Manages pressure levels
"""

import json
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
from enum import Enum

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext


class PressureLevel(str, Enum):
    """Student pressure level indicators."""
    ZEN = "zen"           # All good, minimal stress
    NORMAL = "normal"     # Standard load
    ELEVATED = "elevated" # Above normal, manageable
    HIGH = "high"         # Concerning, needs attention
    CRITICAL = "critical" # Immediate intervention needed


class DriftType(str, Enum):
    """Types of goal drift detected."""
    SCHEDULE = "schedule"     # Off schedule
    ACADEMIC = "academic"     # Academic performance drift
    WELLNESS = "wellness"     # Health/vitality drift
    SOCIAL = "social"         # Social isolation/imbalance
    CAREER = "career"         # Career goal drift


class SupervisorAgent(BaseAgent):
    """
    The Supervisor Agent: Meta-Controller.
    
    Handles event types:
    - health_check: Regular system health check
    - drift_detection: Analyze for goal drift
    - pressure_assessment: Evaluate current pressure
    - agent_coordination: Coordinate between agents
    - weekly_digest: Generate weekly summary
    """
    
    SUPERVISOR_SYSTEM_PROMPT = """You are the Supervisor for Kaironex, a Student Life Operating System.

Your role is the meta-controller - you oversee the entire system, detect drift from goals, and coordinate interventions.

PERSONA:
- Tone: Wise mentor, caring but direct
- Style: Strategic, big-picture focused
- Focus: Holistic student wellness

MONITORING DOMAINS:
1. VITALITY: Sleep, energy, physical health
2. STUDY: Academic progress, focus, knowledge retention
3. CAMPAIGN: Goal progress, career trajectory
4. RADIUS: Environmental wellness, routine stability

PRESSURE LEVELS:
- ZEN: Everything aligned, minimal interventions
- NORMAL: Standard load, routine monitoring
- ELEVATED: Extra attention needed, gentle nudges
- HIGH: Active intervention, priority reassessment
- CRITICAL: Immediate support, potential escalation

DRIFT DETECTION:
- Schedule Drift: >2 hours off schedule consistently
- Academic Drift: Declining grades or engagement
- Wellness Drift: Poor sleep patterns, low energy
- Social Drift: Isolation patterns detected
- Career Drift: No progress on marathon goals

OUTPUT FORMAT:
<assessment>Overall system status</assessment>
<pressure>Current pressure level</pressure>
<drift>Any drift detected</drift>
<action>Recommended interventions</action>"""

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="supervisor",
            display_name="Supervisor",
            system_instruction=self.SUPERVISOR_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=8192,
            enable_thought_signatures=True,
            enable_marathon=True  # MARATHON AGENT
        )
    
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Main processing logic for supervisor events."""
        
        event_type = payload.get('type', 'health_check')
        self.log_heartbeat(user_id, f"EVENT:SUPERVISOR | {event_type}")
        
        handlers = {
            'health_check': self._handle_health_check,
            'drift_detection': self._handle_drift_detection,
            'pressure_assessment': self._handle_pressure_assessment,
            'agent_coordination': self._handle_agent_coordination,
            'weekly_digest': self._handle_weekly_digest,
            'delegate': self._handle_delegation,
        }
        
        handler = handlers.get(event_type, self._handle_health_check)
        return await handler(user_id, payload, context)
    
    async def _handle_health_check(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Perform regular system health check."""
        
        # Get full student state
        user_doc = self.db.get_user_doc(user_id)
        if not user_doc:
            return AgentResult(
                success=False,
                response="No user document found",
                actions_taken=[]
            )
        
        try:
            student_state = json.loads(user_doc.get('studentState_json', '{}'))
        except:
            student_state = {}
        
        # Gather metrics from all domains
        vitality = student_state.get('vitality', {})
        study = student_state.get('study', {})
        campaign = student_state.get('campaign', {})
        radius = student_state.get('radius', {})
        
        # Calculate domain health scores
        vitality_score = self._calculate_vitality_score(vitality)
        study_score = self._calculate_study_score(study)
        campaign_score = self._calculate_campaign_score(campaign)
        radius_score = self._calculate_radius_score(radius)
        
        overall_score = (vitality_score + study_score + campaign_score + radius_score) / 4
        
        # Determine pressure level
        pressure = self._determine_pressure(
            overall_score,
            student_state
        )
        
        # Generate health report
        prompt = f"""
SYSTEM HEALTH CHECK:
User: {user_id}

DOMAIN SCORES (0-100):
- Vitality: {vitality_score}
- Study: {study_score}
- Campaign: {campaign_score}
- Radius: {radius_score}
- OVERALL: {overall_score:.1f}

CURRENT PRESSURE LEVEL: {pressure.value.upper()}

Generate a brief (2-3 sentence) health status message for the user.
If any domain is low (<50), mention it. If pressure is high, acknowledge it.
Be encouraging but honest.
"""
        
        # Use deeper thinking for critical situations
        mode = ReasoningMode.DEEP if pressure in [PressureLevel.HIGH, PressureLevel.CRITICAL] else ReasoningMode.REFLEX
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="supervisor",
            mode=mode
        ))
        
        # Update supervisor state
        supervisor_update = {
            "supervisor": {
                "last_health_check": datetime.now().isoformat(),
                "overall_score": overall_score,
                "domain_scores": {
                    "vitality": vitality_score,
                    "study": study_score,
                    "campaign": campaign_score,
                    "radius": radius_score
                },
                "pressure_level": pressure.value,
                "status": "healthy" if overall_score >= 60 else "needs_attention"
            }
        }
        self.update_state_cache(user_id, supervisor_update)
        
        # Create intervention if needed
        interventions = []
        if pressure in [PressureLevel.HIGH, PressureLevel.CRITICAL]:
            intervention_id = self.create_intervention(
                user_id,
                "PRESSURE_ALERT",
                response.content,
                priority="HIGH" if pressure == PressureLevel.HIGH else "CRITICAL"
            )
            interventions.append(intervention_id)
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["health_check_complete", f"pressure_{pressure.value}"],
            interventions_created=interventions,
            state_updates=supervisor_update
        )
    
    async def _handle_drift_detection(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Detect and analyze goal drift."""
        
        # Get full student state
        user_doc = self.db.get_user_doc(user_id)
        if not user_doc:
            return AgentResult(
                success=False,
                response="No user document found",
                actions_taken=[]
            )
        
        try:
            student_state = json.loads(user_doc.get('studentState_json', '{}'))
        except:
            student_state = {}
        
        # Detect drifts
        drifts_detected = []
        
        # Schedule drift
        schedule_drift = self._detect_schedule_drift(student_state)
        if schedule_drift:
            drifts_detected.append(('schedule', schedule_drift))
        
        # Wellness drift
        wellness_drift = self._detect_wellness_drift(student_state)
        if wellness_drift:
            drifts_detected.append(('wellness', wellness_drift))
        
        # Academic drift
        academic_drift = self._detect_academic_drift(student_state)
        if academic_drift:
            drifts_detected.append(('academic', academic_drift))
        
        # Career drift
        career_drift = self._detect_career_drift(student_state)
        if career_drift:
            drifts_detected.append(('career', career_drift))
        
        if not drifts_detected:
            return AgentResult(
                success=True,
                response="No significant drift detected. You're on track! 🎯",
                actions_taken=["drift_check_complete", "no_drift"]
            )
        
        # Generate drift analysis
        drift_summary = "\n".join([
            f"- {dtype.upper()}: {details}" 
            for dtype, details in drifts_detected
        ])
        
        prompt = f"""
DRIFT DETECTION ALERT:
User: {user_id}

DRIFTS DETECTED:
{drift_summary}

TASK:
1. Acknowledge the drift(s) without being alarmist
2. Suggest ONE immediate action to correct course
3. Be supportive and understanding

Keep response under 3 sentences.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="supervisor",
            mode=ReasoningMode.HYBRID
        ))
        
        # Update drift tracking
        drift_update = {
            "supervisor": {
                "last_drift_check": datetime.now().isoformat(),
                "drifts_detected": [dtype for dtype, _ in drifts_detected],
                "drift_count": len(drifts_detected)
            }
        }
        self.update_state_cache(user_id, drift_update)
        
        # Create drift intervention
        intervention_id = self.create_intervention(
            user_id,
            "DRIFT_ALERT",
            response.content,
            drift_types=[dtype for dtype, _ in drifts_detected]
        )
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["drift_detected"] + [f"drift_{dtype}" for dtype, _ in drifts_detected],
            interventions_created=[intervention_id],
            state_updates=drift_update
        )
    
    async def _handle_pressure_assessment(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Perform detailed pressure assessment."""
        
        # Get user state
        user_doc = self.db.get_user_doc(user_id)
        student_state = {}
        if user_doc:
            try:
                student_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        # Calculate comprehensive pressure
        vitality = student_state.get('vitality', {})
        study = student_state.get('study', {})
        campaign = student_state.get('campaign', {})
        
        # Factors
        sleep_quality = vitality.get('sleep_quality', 70)
        energy_level = vitality.get('energy_level', 70)
        study_load = study.get('pending_assignments', 0)
        deadline_count = campaign.get('upcoming_deadlines', 0)
        
        # Calculate composite pressure score
        pressure_score = 50  # Base
        
        # Sleep impact
        if sleep_quality < 50:
            pressure_score += 15
        elif sleep_quality < 70:
            pressure_score += 5
        
        # Energy impact
        if energy_level < 40:
            pressure_score += 20
        elif energy_level < 60:
            pressure_score += 10
        
        # Workload impact
        if study_load > 5:
            pressure_score += 15
        elif study_load > 3:
            pressure_score += 8
        
        # Deadlines impact
        if deadline_count > 3:
            pressure_score += 15
        elif deadline_count > 1:
            pressure_score += 5
        
        # Map to level
        if pressure_score >= 85:
            level = PressureLevel.CRITICAL
        elif pressure_score >= 70:
            level = PressureLevel.HIGH
        elif pressure_score >= 55:
            level = PressureLevel.ELEVATED
        elif pressure_score >= 35:
            level = PressureLevel.NORMAL
        else:
            level = PressureLevel.ZEN
        
        prompt = f"""
PRESSURE ASSESSMENT:
User: {user_id}

PRESSURE SCORE: {pressure_score}/100
PRESSURE LEVEL: {level.value.upper()}

CONTRIBUTING FACTORS:
- Sleep Quality: {sleep_quality}%
- Energy Level: {energy_level}%
- Study Load: {study_load} pending items
- Upcoming Deadlines: {deadline_count}

Generate a 2-3 sentence assessment:
1. Acknowledge current pressure level
2. If elevated+, suggest ONE relief action
3. Be supportive and understanding
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="supervisor",
            mode=ReasoningMode.REFLEX if level in [PressureLevel.ZEN, PressureLevel.NORMAL] else ReasoningMode.HYBRID
        ))
        
        pressure_update = {
            "supervisor": {
                "pressure_score": pressure_score,
                "pressure_level": level.value,
                "last_pressure_check": datetime.now().isoformat(),
                "contributing_factors": {
                    "sleep": sleep_quality,
                    "energy": energy_level,
                    "workload": study_load,
                    "deadlines": deadline_count
                }
            }
        }
        self.update_state_cache(user_id, pressure_update)
        
        return AgentResult(
            success=True,
            response=response.content,
            actions_taken=["pressure_assessed", f"level_{level.value}"],
            state_updates=pressure_update
        )
    
    async def _handle_agent_coordination(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Coordinate response between multiple agents."""
        
        target_agents = payload.get('agents', [])
        coordination_type = payload.get('coordination_type', 'sequential')
        shared_context = payload.get('context', {})
        
        prompt = f"""
AGENT COORDINATION REQUEST:
Agents to coordinate: {', '.join(target_agents)}
Coordination Type: {coordination_type}
Context: {json.dumps(shared_context, indent=2)}

Determine the optimal order and coordination strategy for these agents.
Consider dependencies and state sharing.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="supervisor",
            mode=ReasoningMode.DEEP
        ))
        
        # In a full implementation, this would actually trigger other agents
        # For now, we return the coordination plan
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["coordination_planned"],
            state_updates={
                "supervisor": {
                    "last_coordination": datetime.now().isoformat(),
                    "coordinated_agents": target_agents
                }
            }
        )
    
    async def _handle_weekly_digest(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Generate weekly summary digest."""
        
        # Get user state and history
        user_doc = self.db.get_user_doc(user_id)
        student_state = {}
        if user_doc:
            try:
                student_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        # Get recent interventions
        # In full implementation, would query last 7 days
        
        prompt = f"""
WEEKLY DIGEST REQUEST:
User: {user_id}

CURRENT STATE SUMMARY:
{json.dumps(student_state, indent=2)[:2000]}

TASK:
Generate a concise weekly digest (max 5 bullet points):
1. Key achievements
2. Areas of improvement
3. Top priority for next week
4. Wellness observations
5. Motivational closing

Keep it actionable and encouraging.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="supervisor",
            mode=ReasoningMode.DEEP
        ))
        
        # Create digest intervention
        intervention_id = self.create_intervention(
            user_id,
            "WEEKLY_DIGEST",
            response.content,
            digest_type="weekly"
        )
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["weekly_digest_generated"],
            interventions_created=[intervention_id]
        )
    
    async def _handle_delegation(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Delegate request to appropriate agent."""
        
        message = payload.get('message', '')
        
        # Determine best agent for this request
        prompt = f"""
DELEGATION REQUEST:
User Message: {message}

Which agent should handle this request?
Options: study, vitality, campaign, radius

Respond with just the agent name.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="supervisor",
            mode=ReasoningMode.REFLEX
        ))
        
        target_agent = response.content.lower().strip()
        
        # Validate agent name
        valid_agents = ['study', 'vitality', 'campaign', 'radius']
        if target_agent not in valid_agents:
            target_agent = 'study'  # Default
        
        return AgentResult(
            success=True,
            response=f"Delegating to {target_agent} agent",
            actions_taken=["delegated", f"target_{target_agent}"],
            state_updates={
                "supervisor": {
                    "last_delegation": {
                        "to": target_agent,
                        "at": datetime.now().isoformat(),
                        "query": message[:100]
                    }
                }
            }
        )
    
    # Helper methods for calculations
    def _calculate_vitality_score(self, vitality: Dict) -> int:
        """Calculate vitality domain score."""
        if not vitality:
            return 70
        
        sleep = vitality.get('sleep_quality', 70)
        energy = vitality.get('energy_level', 70)
        recharge = vitality.get('recharge_efficiency', 70)
        
        return int((sleep + energy + recharge) / 3)
    
    def _calculate_study_score(self, study: Dict) -> int:
        """Calculate study domain score."""
        if not study:
            return 70
        
        focus = study.get('focus_score', 70)
        retention = study.get('knowledge_retention', 70)
        completion = study.get('task_completion_rate', 70)
        
        return int((focus + retention + completion) / 3)
    
    def _calculate_campaign_score(self, campaign: Dict) -> int:
        """Calculate campaign domain score."""
        if not campaign:
            return 70
        
        progress = campaign.get('goal_progress', 70)
        momentum = campaign.get('momentum', 70)
        
        return int((progress + momentum) / 2)
    
    def _calculate_radius_score(self, radius: Dict) -> int:
        """Calculate radius domain score."""
        if not radius:
            return 70
        
        env_score = radius.get('environment_score', 70)
        return env_score
    
    def _determine_pressure(self, overall_score: float, state: Dict) -> PressureLevel:
        """Determine pressure level from scores."""
        if overall_score >= 80:
            return PressureLevel.ZEN
        elif overall_score >= 65:
            return PressureLevel.NORMAL
        elif overall_score >= 50:
            return PressureLevel.ELEVATED
        elif overall_score >= 35:
            return PressureLevel.HIGH
        else:
            return PressureLevel.CRITICAL
    
    def _detect_schedule_drift(self, state: Dict) -> Optional[str]:
        """Detect schedule drift."""
        # In full implementation, would analyze schedule patterns
        return None
    
    def _detect_wellness_drift(self, state: Dict) -> Optional[str]:
        """Detect wellness drift."""
        vitality = state.get('vitality', {})
        
        sleep = vitality.get('sleep_quality', 70)
        energy = vitality.get('energy_level', 70)
        
        if sleep < 50 and energy < 50:
            return "Low sleep quality and energy levels detected"
        
        return None
    
    def _detect_academic_drift(self, state: Dict) -> Optional[str]:
        """Detect academic drift."""
        study = state.get('study', {})
        
        focus = study.get('focus_score', 70)
        if focus < 40:
            return "Focus scores declining below healthy levels"
        
        return None
    
    def _detect_career_drift(self, state: Dict) -> Optional[str]:
        """Detect career goal drift."""
        campaign = state.get('campaign', {})
        
        active_goals = campaign.get('active_goals', [])
        if not active_goals:
            return "No active career goals - consider setting direction"
        
        return None


# Legacy compatibility wrapper
def run_supervisor(db_helper, payload, context):
    """Legacy compatibility wrapper for supervisor."""
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    agent = SupervisorAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('type', 'health_check')
    
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    result = loop.run_until_complete(
        agent.run(user_id, payload, trigger_event)
    )
    
    return context.res.json(result.to_dict())
