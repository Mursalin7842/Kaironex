"""
🚨 INTERVENTION ENGINE
======================
The "Unprompted AI" capability - proactive interventions without user request.

Features:
- Drift Detection (off-track from goals)
- Pressure Detection (stress/overload)
- Opportunity Detection (optimal moments)
- Gentle Intervention Generation
"""

import json
from typing import Dict, Any, List, Optional, Callable, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from enum import Enum
import random


class InterventionType(str, Enum):
    """Types of proactive interventions."""
    DRIFT = "drift"               # Off-track from goals
    PRESSURE = "pressure"         # Stress/overload detected
    OPPORTUNITY = "opportunity"   # Good moment for action
    REMINDER = "reminder"         # Gentle reminder
    CELEBRATION = "celebration"   # Acknowledge achievement
    SUGGESTION = "suggestion"     # Proactive suggestion


class InterventionUrgency(str, Enum):
    """Urgency levels for interventions."""
    CRITICAL = "critical"     # Immediate attention needed
    HIGH = "high"             # Soon
    MEDIUM = "medium"         # When convenient
    LOW = "low"               # FYI


class InterventionChannel(str, Enum):
    """Delivery channels for interventions."""
    NOTIFICATION = "notification"  # Push notification
    WIDGET = "widget"              # Dashboard widget update
    VOICE = "voice"                # Voice prompt
    FULL_SCREEN = "full_screen"    # Full screen takeover (rare)


@dataclass
class InterventionTrigger:
    """A condition that triggers an intervention."""
    trigger_id: str
    name: str
    check_function: Optional[Callable] = None
    threshold: float = 0.5
    cooldown_minutes: int = 60  # Don't re-trigger within this time
    last_triggered: Optional[datetime] = None
    
    def can_trigger(self) -> bool:
        """Check if cooldown has passed."""
        if self.last_triggered is None:
            return True
        elapsed = datetime.now() - self.last_triggered
        return elapsed.total_seconds() / 60 >= self.cooldown_minutes


@dataclass
class Intervention:
    """A generated intervention."""
    intervention_id: str
    intervention_type: InterventionType
    urgency: InterventionUrgency
    channel: InterventionChannel
    title: str
    message: str
    action_label: Optional[str] = None
    action_callback: Optional[str] = None  # Action ID to execute
    context: Dict[str, Any] = field(default_factory=dict)
    created_at: datetime = field(default_factory=datetime.now)
    expires_at: Optional[datetime] = None
    delivered: bool = False
    dismissed: bool = False
    acted_upon: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "intervention_id": self.intervention_id,
            "type": self.intervention_type.value,
            "urgency": self.urgency.value,
            "channel": self.channel.value,
            "title": self.title,
            "message": self.message,
            "action_label": self.action_label,
            "action_callback": self.action_callback,
            "created_at": self.created_at.isoformat(),
            "expires_at": self.expires_at.isoformat() if self.expires_at else None,
            "delivered": self.delivered,
            "dismissed": self.dismissed,
            "acted_upon": self.acted_upon
        }


class InterventionEngine:
    """
    The Unprompted AI Engine - generates proactive interventions.
    
    This is THE differentiating feature for Kaironex:
    - Monitors user state continuously
    - Detects opportunities and risks
    - Generates timely, contextual interventions
    - Respects user attention/preferences
    """
    
    def __init__(
        self,
        gemini_client=None,
        db=None,
        event_bus=None
    ):
        self.gemini = gemini_client
        self.db = db
        self.event_bus = event_bus
        
        # Registered triggers
        self._triggers: Dict[str, InterventionTrigger] = {}
        
        # Active interventions
        self._active_interventions: Dict[str, List[Intervention]] = {}
        
        # User preferences
        self._user_preferences: Dict[str, Dict[str, Any]] = {}
        
        # Initialize default triggers
        self._register_default_triggers()
    
    def _register_default_triggers(self):
        """Register built-in intervention triggers."""
        
        # Drift Detection
        self._triggers["goal_drift"] = InterventionTrigger(
            trigger_id="goal_drift",
            name="Goal Drift Detection",
            threshold=0.6,
            cooldown_minutes=120
        )
        
        # Pressure Detection
        self._triggers["high_pressure"] = InterventionTrigger(
            trigger_id="high_pressure",
            name="High Pressure Detection",
            threshold=0.75,
            cooldown_minutes=60
        )
        
        # Study Break
        self._triggers["study_break"] = InterventionTrigger(
            trigger_id="study_break",
            name="Study Break Reminder",
            threshold=0.5,
            cooldown_minutes=90
        )
        
        # Energy Dip
        self._triggers["energy_dip"] = InterventionTrigger(
            trigger_id="energy_dip",
            name="Energy Dip Detection",
            threshold=0.4,
            cooldown_minutes=180
        )
        
        # Celebration
        self._triggers["achievement"] = InterventionTrigger(
            trigger_id="achievement",
            name="Achievement Celebration",
            threshold=0.8,
            cooldown_minutes=30
        )
    
    async def check_triggers(
        self,
        user_id: str,
        context: Dict[str, Any]
    ) -> List[Intervention]:
        """
        Check all triggers and generate interventions.
        
        Args:
            user_id: User identifier
            context: Current user context including:
                - current_activity: What user is doing
                - energy_level: From Vitality
                - location: From Radius
                - goals: Active Marathon goals
                - pressure_score: Calculated pressure
                - study_session: Current study state
        
        Returns:
            List of generated interventions
        """
        interventions = []
        
        # Check goal drift
        if self._triggers["goal_drift"].can_trigger():
            drift_intervention = await self._check_goal_drift(user_id, context)
            if drift_intervention:
                interventions.append(drift_intervention)
                self._triggers["goal_drift"].last_triggered = datetime.now()
        
        # Check pressure
        if self._triggers["high_pressure"].can_trigger():
            pressure_intervention = await self._check_pressure(user_id, context)
            if pressure_intervention:
                interventions.append(pressure_intervention)
                self._triggers["high_pressure"].last_triggered = datetime.now()
        
        # Check study break
        if self._triggers["study_break"].can_trigger():
            break_intervention = await self._check_study_break(user_id, context)
            if break_intervention:
                interventions.append(break_intervention)
                self._triggers["study_break"].last_triggered = datetime.now()
        
        # Check energy
        if self._triggers["energy_dip"].can_trigger():
            energy_intervention = await self._check_energy(user_id, context)
            if energy_intervention:
                interventions.append(energy_intervention)
                self._triggers["energy_dip"].last_triggered = datetime.now()
        
        # Check achievements
        if self._triggers["achievement"].can_trigger():
            achievement_intervention = await self._check_achievement(user_id, context)
            if achievement_intervention:
                interventions.append(achievement_intervention)
                self._triggers["achievement"].last_triggered = datetime.now()
        
        # Store active interventions
        if user_id not in self._active_interventions:
            self._active_interventions[user_id] = []
        self._active_interventions[user_id].extend(interventions)
        
        return interventions
    
    async def _check_goal_drift(
        self,
        user_id: str,
        context: Dict[str, Any]
    ) -> Optional[Intervention]:
        """Check if user has drifted from their goals."""
        import uuid
        
        goals = context.get("goals", [])
        current_activity = context.get("current_activity", "")
        
        if not goals or not current_activity:
            return None
        
        # In production, use Gemini to assess alignment
        # For now, simulate drift detection
        drift_score = context.get("drift_score", 0.0)
        
        if drift_score >= self._triggers["goal_drift"].threshold:
            return Intervention(
                intervention_id=f"int_{uuid.uuid4().hex[:8]}",
                intervention_type=InterventionType.DRIFT,
                urgency=InterventionUrgency.MEDIUM,
                channel=InterventionChannel.NOTIFICATION,
                title="📍 Gentle Navigation Check",
                message=self._generate_drift_message(goals, current_activity),
                action_label="Back to Focus",
                action_callback="return_to_focus",
                context={"goals": goals, "drift_score": drift_score}
            )
        
        return None
    
    async def _check_pressure(
        self,
        user_id: str,
        context: Dict[str, Any]
    ) -> Optional[Intervention]:
        """Check if user is under high pressure."""
        import uuid
        
        pressure_score = context.get("pressure_score", 0.0)
        
        if pressure_score >= self._triggers["high_pressure"].threshold:
            urgency = InterventionUrgency.HIGH if pressure_score > 0.9 else InterventionUrgency.MEDIUM
            
            return Intervention(
                intervention_id=f"int_{uuid.uuid4().hex[:8]}",
                intervention_type=InterventionType.PRESSURE,
                urgency=urgency,
                channel=InterventionChannel.NOTIFICATION,
                title="🌊 Pressure Release Check",
                message=self._generate_pressure_message(pressure_score),
                action_label="Breathe & Reset",
                action_callback="start_breathing_exercise",
                context={"pressure_score": pressure_score}
            )
        
        return None
    
    async def _check_study_break(
        self,
        user_id: str,
        context: Dict[str, Any]
    ) -> Optional[Intervention]:
        """Check if user needs a study break."""
        import uuid
        
        study_session = context.get("study_session", {})
        session_minutes = study_session.get("duration_minutes", 0)
        
        # Suggest break after 90 minutes of deep focus
        if session_minutes >= 90:
            return Intervention(
                intervention_id=f"int_{uuid.uuid4().hex[:8]}",
                intervention_type=InterventionType.SUGGESTION,
                urgency=InterventionUrgency.LOW,
                channel=InterventionChannel.WIDGET,
                title="⏰ Strategic Pause",
                message=f"You've been crushing it for {session_minutes} minutes! A 15-minute break will actually improve retention. Your brain needs to consolidate.",
                action_label="Start Break",
                action_callback="start_break_timer",
                context={"session_minutes": session_minutes},
                expires_at=datetime.now() + timedelta(minutes=30)
            )
        
        return None
    
    async def _check_energy(
        self,
        user_id: str,
        context: Dict[str, Any]
    ) -> Optional[Intervention]:
        """Check for energy dips."""
        import uuid
        
        energy_level = context.get("energy_level", 0.5)
        
        if energy_level <= self._triggers["energy_dip"].threshold:
            return Intervention(
                intervention_id=f"int_{uuid.uuid4().hex[:8]}",
                intervention_type=InterventionType.SUGGESTION,
                urgency=InterventionUrgency.LOW,
                channel=InterventionChannel.NOTIFICATION,
                title="⚡ Energy Recharge Detected",
                message=self._generate_energy_message(energy_level),
                action_label="Quick Recharge",
                action_callback="show_recharge_options",
                context={"energy_level": energy_level}
            )
        
        return None
    
    async def _check_achievement(
        self,
        user_id: str,
        context: Dict[str, Any]
    ) -> Optional[Intervention]:
        """Check for achievements to celebrate."""
        import uuid
        
        recent_achievement = context.get("recent_achievement")
        
        if recent_achievement:
            return Intervention(
                intervention_id=f"int_{uuid.uuid4().hex[:8]}",
                intervention_type=InterventionType.CELEBRATION,
                urgency=InterventionUrgency.LOW,
                channel=InterventionChannel.NOTIFICATION,
                title="🎉 Victory Lap!",
                message=f"You just crushed it: {recent_achievement['title']}! That's {recent_achievement.get('streak', 1)} wins in a row.",
                action_label="View Progress",
                action_callback="show_achievements",
                context={"achievement": recent_achievement}
            )
        
        return None
    
    # Message generation helpers
    
    def _generate_drift_message(
        self,
        goals: List[str],
        current_activity: str
    ) -> str:
        """Generate a drift intervention message."""
        messages = [
            f"I noticed you're on '{current_activity}' - totally valid detour! Just checking: ready to return to '{goals[0]}' or is this intentional exploration?",
            f"Quick vibe check: '{current_activity}' is interesting, but '{goals[0]}' is still waiting. Want me to bookmark this for later?",
            f"No judgment, but we've drifted a bit from '{goals[0]}'. Sometimes tangents lead to gold - should we track this one?",
        ]
        return random.choice(messages)
    
    def _generate_pressure_message(self, pressure_score: float) -> str:
        """Generate a pressure intervention message."""
        if pressure_score > 0.9:
            return "Red alert on pressure levels! This is your brain's way of saying 'please help.' Let's do a 2-minute reset before continuing."
        else:
            return "Pressure is climbing. You're capable of handling this, but let's not white-knuckle it. Quick breathing exercise?"
    
    def _generate_energy_message(self, energy_level: float) -> str:
        """Generate an energy intervention message."""
        messages = [
            "Your energy tank is running low. Quick 5-minute walk or a power snack could give you another 2 hours of focus.",
            "Brain fuel is depleting. This is science, not weakness. What sounds good: movement, food, or a power nap?",
            "Energy dip detected. Fun fact: even a 10-second stretch can boost alertness by 30%. Worth a try?",
        ]
        return random.choice(messages)
    
    # Intervention management
    
    def mark_delivered(self, intervention_id: str, user_id: str):
        """Mark intervention as delivered."""
        if user_id in self._active_interventions:
            for intervention in self._active_interventions[user_id]:
                if intervention.intervention_id == intervention_id:
                    intervention.delivered = True
                    break
    
    def mark_dismissed(self, intervention_id: str, user_id: str):
        """Mark intervention as dismissed."""
        if user_id in self._active_interventions:
            for intervention in self._active_interventions[user_id]:
                if intervention.intervention_id == intervention_id:
                    intervention.dismissed = True
                    break
    
    def mark_acted_upon(self, intervention_id: str, user_id: str):
        """Mark intervention as acted upon."""
        if user_id in self._active_interventions:
            for intervention in self._active_interventions[user_id]:
                if intervention.intervention_id == intervention_id:
                    intervention.acted_upon = True
                    break
    
    def get_pending_interventions(self, user_id: str) -> List[Intervention]:
        """Get pending (undelivered) interventions."""
        if user_id not in self._active_interventions:
            return []
        
        now = datetime.now()
        return [
            i for i in self._active_interventions[user_id]
            if not i.delivered
            and not i.dismissed
            and (i.expires_at is None or i.expires_at > now)
        ]
    
    def get_intervention_stats(self, user_id: str) -> Dict[str, Any]:
        """Get intervention statistics."""
        if user_id not in self._active_interventions:
            return {"total": 0, "delivered": 0, "dismissed": 0, "acted_upon": 0}
        
        interventions = self._active_interventions[user_id]
        
        return {
            "total": len(interventions),
            "delivered": sum(1 for i in interventions if i.delivered),
            "dismissed": sum(1 for i in interventions if i.dismissed),
            "acted_upon": sum(1 for i in interventions if i.acted_upon),
            "by_type": self._count_by_type(interventions)
        }
    
    def _count_by_type(self, interventions: List[Intervention]) -> Dict[str, int]:
        """Count interventions by type."""
        counts = {}
        for i in interventions:
            if i.intervention_type.value not in counts:
                counts[i.intervention_type.value] = 0
            counts[i.intervention_type.value] += 1
        return counts
    
    def set_user_preferences(self, user_id: str, preferences: Dict[str, Any]):
        """Set user intervention preferences."""
        self._user_preferences[user_id] = preferences
    
    def get_user_preferences(self, user_id: str) -> Dict[str, Any]:
        """Get user intervention preferences."""
        return self._user_preferences.get(user_id, {
            "quiet_hours_start": "22:00",
            "quiet_hours_end": "08:00",
            "max_daily_interventions": 10,
            "preferred_channel": "notification",
            "enabled_types": list(InterventionType)
        })
