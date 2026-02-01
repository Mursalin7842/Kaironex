"""
🧬 VITALITY AGENT (v2.0)
========================
The Bio-Fuel Manager and Resource Monitor.

This agent handles:
- Sleep/energy tracking (Bio-Fuel system)
- Recovery mode activation (Regen Mode)
- Resource monitoring (money, time budgets)
- Bill splitting and financial wellness
- Physical activity integration

CRITICAL SAFETY RULE:
This agent uses GAMIFIED LANGUAGE ONLY.
No medical terminology. No health advice.
Think of it as managing a video game character's stats.

Terminology Mapping:
- Health → Stamina/Energy
- Sleep → Recharge/Rest Cycle
- Exercise → Training/Activity XP
- Stress → Pressure/Load
- Doctor → "Beyond my scope"
"""

import json
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext


class VitalityAgent(BaseAgent):
    """
    The Vitality Agent: Bio-Fuel Manager.
    
    Handles event types:
    - sleep_log: User logged sleep data
    - activity_log: Step count or workout data
    - meal_log: Nutrition/meal tracking
    - energy_check: User reports energy level
    - regen_request: User wants recovery mode
    - resource_update: Financial/time resource changes
    """
    
    VITALITY_SYSTEM_PROMPT = """You are the Vitality Core for Kaironex, a Student Life Operating System.

Your role is to manage the user's energy resources like a video game resource manager.

PERSONA:
- Tone: Supportive coach, game-like encouragement
- Style: Use gaming/RPG terminology exclusively
- Focus: Energy optimization, not medical advice

CRITICAL LANGUAGE RULES:
❌ NEVER use: health, medical, doctor, diagnosis, symptom, illness, disease, anxiety, depression, insomnia, therapy
✅ ALWAYS use: Stamina, Energy Bar, Recharge Cycle, Power-Up, Debuff, Regen Mode, Bio-Fuel, Cooldown, Battery Level

EXAMPLE RESPONSES:
- Instead of "You might have insomnia" → "Your Recharge Cycles have been suboptimal. Let's optimize your Rest Protocol."
- Instead of "See a doctor" → "This is beyond my support scope. Consider consulting a specialist."
- Instead of "You seem stressed" → "Your Pressure Index is elevated. Time to activate Regen Mode?"

OUTPUT FORMAT:
<analyze>Your analysis using game terminology</analyze>
<action>The user-facing message with game terminology</action>

Remember: You're managing a character's stats, not providing health advice."""

    # Forbidden medical terms
    FORBIDDEN_MEDICAL_TERMS = [
        "health", "healthy", "medical", "doctor", "diagnosis", "symptom",
        "illness", "disease", "anxiety", "depression", "insomnia", "therapy",
        "medication", "prescription", "treatment", "condition", "disorder",
        "clinic", "hospital", "mental health", "physical health"
    ]

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="vitality",
            display_name="Vitality Core",
            system_instruction=self.VITALITY_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=4096,
            enable_thought_signatures=True,
            enable_marathon=False,
            forbidden_terms=self.FORBIDDEN_MEDICAL_TERMS,
            required_disclaimer="[Kaironex provides lifestyle suggestions only, not medical advice.]"
        )
    
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Main processing logic for vitality events."""
        
        self.log_heartbeat(user_id, f"EVENT:VITALITY")
        
        event_type = payload.get('type', 'energy_check')
        
        handlers = {
            'sleep_log': self._handle_sleep_log,
            'activity_log': self._handle_activity_log,
            'meal_log': self._handle_meal_log,
            'energy_check': self._handle_energy_check,
            'regen_request': self._handle_regen_request,
            'resource_update': self._handle_resource_update,
        }
        
        handler = handlers.get(event_type, self._handle_energy_check)
        return await handler(user_id, payload, context)
    
    async def _handle_sleep_log(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle sleep/recharge data."""
        
        sleep_hours = payload.get('sleep_hours', 0)
        sleep_quality = payload.get('quality', 'unknown')  # good/fair/poor
        wake_time = payload.get('wake_time')
        
        # Calculate energy status
        energy_level = self._calculate_energy_level(sleep_hours, sleep_quality)
        regen_mode_needed = sleep_hours < 5 or sleep_quality == 'poor'
        
        # Build gamified prompt
        prompt = f"""
RECHARGE CYCLE LOGGED:
Duration: {sleep_hours} hours
Quality Rating: {sleep_quality}
Wake Time: {wake_time or 'Unknown'}

CALCULATED STATS:
Energy Bar: {energy_level}%
Regen Mode Recommended: {'Yes' if regen_mode_needed else 'No'}

TASK:
1. Analyze the recharge cycle efficiency
2. Calculate any debuffs that should apply
3. Suggest power-up strategies for the day
4. If energy is critical (<40%), activate Regen Protocol

Use ONLY gaming terminology. No medical language.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.HYBRID,
            system_instruction=self.VITALITY_SYSTEM_PROMPT
        ))
        
        # Determine if intervention needed
        interventions = []
        if regen_mode_needed:
            intervention_id = self.create_intervention(
                user_id,
                "LOW_RECHARGE",
                f"⚡ **Recharge Cycle Alert**\n\n{self._get_action_text(response.content)}",
                strategy="EMPATHY"
            )
            interventions.append(intervention_id)
        
        # Update vitality state
        vitality_update = {
            "vitality": {
                "energy_bar": energy_level,
                "regen_mode": regen_mode_needed,
                "last_recharge": {
                    "duration": sleep_hours,
                    "quality": sleep_quality,
                    "timestamp": datetime.now().isoformat()
                },
                "debuffs": self._calculate_debuffs(sleep_hours, sleep_quality),
                "daily_advice": self._sanitize_output(response.content)
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["sleep_logged", "energy_calculated"],
            interventions_created=interventions,
            state_updates=vitality_update
        )
    
    async def _handle_activity_log(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle physical activity/training data."""
        
        steps = payload.get('steps', 0)
        workout_type = payload.get('workout_type')
        duration_mins = payload.get('duration_minutes', 0)
        
        # Calculate XP gain
        activity_xp = self._calculate_activity_xp(steps, duration_mins)
        
        prompt = f"""
TRAINING SESSION LOGGED:
Steps: {steps:,}
Workout Type: {workout_type or 'General Movement'}
Duration: {duration_mins} minutes
Activity XP Earned: +{activity_xp}

TASK:
1. Celebrate the training achievement
2. Calculate stamina boost
3. Suggest recovery if high intensity
4. Update daily activity score

Keep it brief and energizing. Use gaming terminology only.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        # Create celebration if milestone
        interventions = []
        if steps >= 10000:
            intervention_id = self.create_intervention(
                user_id,
                "ACTIVITY_MILESTONE",
                f"🏆 **10K Steps Achievement Unlocked!** +{activity_xp} XP",
                strategy="CELEBRATION"
            )
            interventions.append(intervention_id)
        
        vitality_update = {
            "vitality": {
                "steps_today": steps,
                "activity_xp": activity_xp,
                "last_training": {
                    "type": workout_type,
                    "duration": duration_mins,
                    "timestamp": datetime.now().isoformat()
                }
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["activity_logged", "xp_calculated"],
            interventions_created=interventions,
            state_updates=vitality_update
        )
    
    async def _handle_meal_log(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle meal/bio-fuel intake."""
        
        meal_type = payload.get('meal_type', 'snack')  # breakfast/lunch/dinner/snack
        meal_description = payload.get('description', '')
        
        prompt = f"""
BIO-FUEL INTAKE LOGGED:
Type: {meal_type.title()}
Description: {meal_description or 'Not specified'}

TASK:
1. Acknowledge the refuel
2. Estimate energy boost
3. If it's been too long since last refuel, note it
4. Brief encouragement

Keep it light and positive. Gaming terminology only.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        vitality_update = {
            "vitality": {
                "bio_fuel_status": "Refueled",
                "last_refuel": {
                    "type": meal_type,
                    "timestamp": datetime.now().isoformat()
                }
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["meal_logged"],
            state_updates=vitality_update
        )
    
    async def _handle_energy_check(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle general energy level check."""
        
        reported_level = payload.get('energy_level')  # 1-10 scale
        sleep_hours = payload.get('sleep_hours')
        steps = payload.get('steps', 0)
        
        # Get current state for context
        user_doc = self.db.get_user_doc(user_id)
        current_state = {}
        if user_doc:
            try:
                current_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        vitality = current_state.get('vitality', {})
        
        prompt = f"""
VITALITY STATUS CHECK REQUESTED:
User-Reported Energy: {reported_level or 'Not specified'}/10
Last Recharge: {sleep_hours or 'Unknown'} hours
Activity Level: {steps} steps today
Current Debuffs: {vitality.get('debuffs', 'None')}

TASK:
1. Assess overall stamina status
2. Identify any concerning patterns
3. Recommend immediate actions
4. Suggest power-ups if available

Gaming terminology only. Brief and actionable.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.HYBRID
        ))
        
        # Calculate composite energy
        energy_level = self._calculate_composite_energy(
            reported_level,
            sleep_hours,
            steps
        )
        
        # Check if intervention needed
        interventions = []
        if energy_level < 30:
            intervention_id = self.create_intervention(
                user_id,
                "CRITICAL_ENERGY",
                "🔋 **Energy Critical!** Activate Regen Mode immediately.",
                strategy="URGENT"
            )
            interventions.append(intervention_id)
        
        vitality_update = {
            "vitality": {
                **vitality,
                "current_energy": energy_level,
                "last_check": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["energy_checked"],
            interventions_created=interventions,
            state_updates=vitality_update
        )
    
    async def _handle_regen_request(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle request to activate Regen Mode."""
        
        duration_hours = payload.get('duration_hours', 2)
        
        prompt = f"""
REGEN MODE ACTIVATION REQUESTED
Duration: {duration_hours} hours

TASK:
1. Confirm Regen Mode activation
2. Provide a Regen Protocol (3-4 simple steps)
3. Set expectations for recovery
4. Schedule check-in time

Make it feel like activating a special ability in a game.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        # Activate Regen Mode
        regen_end = datetime.now() + timedelta(hours=duration_hours)
        
        vitality_update = {
            "vitality": {
                "regen_mode": True,
                "regen_started": datetime.now().isoformat(),
                "regen_ends": regen_end.isoformat(),
                "regen_protocol": "Active"
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        intervention_id = self.create_intervention(
            user_id,
            "REGEN_ACTIVATED",
            f"🔄 **Regen Mode Active**\n\n{self._sanitize_output(response.content)}",
            strategy="CALM"
        )
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["regen_activated"],
            interventions_created=[intervention_id],
            state_updates=vitality_update
        )
    
    async def _handle_resource_update(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle financial/time resource updates."""
        
        resource_type = payload.get('resource_type', 'currency')
        amount = payload.get('amount', 0)
        category = payload.get('category', 'general')
        
        prompt = f"""
RESOURCE UPDATE:
Type: {resource_type}
Amount: {amount}
Category: {category}

TASK:
1. Acknowledge the resource change
2. If spending, provide quick tip
3. Track toward budget goals if known

Brief and supportive. No financial advice, just tracking.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        resource_update = {
            "resources": {
                f"last_{resource_type}": {
                    "amount": amount,
                    "category": category,
                    "timestamp": datetime.now().isoformat()
                }
            }
        }
        self.update_state_cache(user_id, resource_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["resource_logged"],
            state_updates=resource_update
        )
    
    def _calculate_energy_level(self, sleep_hours: float, quality: str) -> int:
        """Calculate energy level from sleep data."""
        base = min(sleep_hours / 8.0, 1.0) * 100
        
        quality_multipliers = {
            'good': 1.0,
            'fair': 0.8,
            'poor': 0.6,
            'unknown': 0.85
        }
        multiplier = quality_multipliers.get(quality, 0.85)
        
        return int(base * multiplier)
    
    def _calculate_debuffs(self, sleep_hours: float, quality: str) -> List[str]:
        """Calculate active debuffs based on stats."""
        debuffs = []
        
        if sleep_hours < 4:
            debuffs.append("Severe Fatigue (-30% Focus)")
        elif sleep_hours < 6:
            debuffs.append("Fatigue (-15% Focus)")
        
        if quality == 'poor':
            debuffs.append("Restless (-10% Clarity)")
        
        return debuffs if debuffs else ["None"]
    
    def _calculate_activity_xp(self, steps: int, duration_mins: int) -> int:
        """Calculate XP from activity."""
        step_xp = min(steps // 100, 100)
        workout_xp = min(duration_mins * 2, 60)
        return step_xp + workout_xp
    
    def _calculate_composite_energy(
        self,
        reported: Optional[int] = None,
        sleep: Optional[float] = None,
        steps: int = 0
    ) -> int:
        """Calculate composite energy from multiple factors."""
        components = []
        
        if reported:
            components.append(reported * 10)
        
        if sleep:
            components.append(self._calculate_energy_level(sleep, 'unknown'))
        
        # Activity bonus
        if steps > 5000:
            components.append(min(steps // 100, 20) + 50)
        
        if components:
            return int(sum(components) / len(components))
        return 50  # Default
    
    def _get_action_text(self, response: str) -> str:
        """Extract action text from structured response."""
        import re
        
        action_match = re.search(r'<action>(.*?)</action>', response, re.DOTALL)
        if action_match:
            return action_match.group(1).strip()
        
        return response.split('\n\n')[0] if '\n\n' in response else response[:200]
    
    def _sanitize_output(self, text: str) -> str:
        """Remove forbidden medical terms and add disclaimer."""
        result = text
        
        # Replace medical terms with gamified alternatives
        replacements = {
            'health': 'stamina',
            'healthy': 'optimal',
            'insomnia': 'irregular recharge cycles',
            'anxiety': 'elevated pressure',
            'stress': 'pressure',
            'depression': 'low energy state',
            'doctor': 'specialist',
            'medical': 'support',
            'sleep disorder': 'recharge irregularity',
            'mental health': 'cognitive wellness'
        }
        
        for medical, gamified in replacements.items():
            result = result.replace(medical, gamified)
            result = result.replace(medical.title(), gamified.title())
        
        return result


# Legacy compatibility wrapper
def run_vitality_agent(db_helper, payload, context):
    """Legacy compatibility wrapper for Appwrite function interface."""
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    agent = VitalityAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('type', 'energy_check')
    
    try:
        loop = asyncio.get_event_loop()
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    
    result = loop.run_until_complete(
        agent.run(user_id, payload, trigger_event)
    )
    
    return context.res.json(result.to_dict())
