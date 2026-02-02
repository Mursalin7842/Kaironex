"""
🌊 LIFE EVENT HANDLER
=====================
Handles real-world disruptions and life events.

"Life happens. We adapt."

Features:
- Sudden event processing (illness, overtime, emergencies)
- Event classification and escalation
- Automatic schedule adjustment recommendations
- Reflex → Main Brain escalation
- Recovery planning after disruptions
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta, date
from enum import Enum


class EventSeverity(str, Enum):
    """Severity of life events."""
    MINOR = "minor"           # Small adjustment needed
    MODERATE = "moderate"     # Notable impact
    MAJOR = "major"           # Significant disruption
    CRITICAL = "critical"     # Emergency - full reschedule


class EventCategory(str, Enum):
    """Categories of life events."""
    HEALTH = "health"                 # Illness, doctor visits
    WORK = "work"                     # Overtime, new shifts
    FAMILY = "family"                 # Family events, obligations
    SOCIAL = "social"                 # Weddings, parties, gatherings
    ACADEMIC = "academic"             # Surprise tests, deadlines
    EMERGENCY = "emergency"           # Urgent unexpected situations
    MOOD = "mood"                     # Mental health, motivation
    SCHEDULE_CONFLICT = "schedule_conflict"


class EscalationLevel(str, Enum):
    """Where to handle the event."""
    REFLEX = "reflex"         # Handle locally, quick adjustment
    MAIN_BRAIN = "main_brain" # Needs deeper analysis
    USER_DECISION = "user"    # User must decide


@dataclass
class LifeEvent:
    """A real-world event that affects the student."""
    event_id: str
    user_id: str
    category: EventCategory
    severity: EventSeverity
    description: str
    reported_at: datetime
    affects_from: datetime
    affects_until: Optional[datetime]
    user_reported: bool = True  # Did user tell us, or did we detect?
    requires_escalation: bool = False
    escalation_level: EscalationLevel = EscalationLevel.REFLEX
    resolution_status: str = "pending"  # pending, processing, resolved
    adjustments_made: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "category": self.category.value,
            "severity": self.severity.value,
            "escalation_level": self.escalation_level.value,
            "reported_at": self.reported_at.isoformat(),
            "affects_from": self.affects_from.isoformat(),
            "affects_until": self.affects_until.isoformat() if self.affects_until else None
        }


@dataclass
class DisruptionPattern:
    """Pattern of disruptions for a user."""
    user_id: str
    total_events: int = 0
    events_by_category: Dict[str, int] = field(default_factory=dict)
    average_recovery_days: float = 1.0
    common_excuses: List[str] = field(default_factory=list)
    peak_disruption_days: List[str] = field(default_factory=list)  # weekday names


@dataclass
class RecoveryPlan:
    """Plan to recover after a disruption."""
    plan_id: str
    user_id: str
    event_id: str
    missed_items: List[str]
    recovery_actions: List[Dict[str, Any]]
    estimated_recovery_days: int
    created_at: datetime
    status: str = "active"
    
    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        data['created_at'] = self.created_at.isoformat()
        return data


class LifeEventHandler:
    """
    Processes real-world events and determines appropriate response.
    
    The Reflex agent receives life events and this handler:
    1. Classifies the event
    2. Determines if escalation to Main Brain needed
    3. Suggests schedule adjustments
    4. Creates recovery plans
    """
    
    def __init__(self, db=None, main_brain_callback=None):
        self.db = db
        self.main_brain_callback = main_brain_callback
        self._events: Dict[str, List[LifeEvent]] = {}
        self._patterns: Dict[str, DisruptionPattern] = {}
        self._recovery_plans: Dict[str, RecoveryPlan] = {}
    
    async def process_event(
        self,
        user_id: str,
        event_description: str,
        affects_from: Optional[datetime] = None,
        affects_until: Optional[datetime] = None,
        source: str = "user"  # user, detection, schedule
    ) -> Dict[str, Any]:
        """
        Process a life event and determine response.
        
        Args:
            user_id: User identifier
            event_description: What happened/will happen
            affects_from: When impact starts
            affects_until: When impact ends
            source: How we learned about this
        
        Returns:
            Processing result with recommendations
        """
        import uuid
        
        # Parse and classify the event
        category, severity = self._classify_event(event_description)
        
        # Determine escalation
        escalation = self._determine_escalation(category, severity, event_description)
        
        # Create event record
        event = LifeEvent(
            event_id=f"evt_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            category=category,
            severity=severity,
            description=event_description,
            reported_at=datetime.now(),
            affects_from=affects_from or datetime.now(),
            affects_until=affects_until,
            user_reported=(source == "user"),
            requires_escalation=(escalation != EscalationLevel.REFLEX),
            escalation_level=escalation
        )
        
        # Store event
        if user_id not in self._events:
            self._events[user_id] = []
        self._events[user_id].append(event)
        
        # Update disruption patterns
        self._update_patterns(user_id, event)
        
        # Generate response
        response = {
            "event": event.to_dict(),
            "acknowledged": True,
            "message": self._generate_acknowledgment(event),
            "escalated_to": escalation.value,
        }
        
        # Handle based on escalation level
        if escalation == EscalationLevel.REFLEX:
            # Handle locally with quick adjustments
            adjustments = self._generate_quick_adjustments(event)
            response["adjustments"] = adjustments
            response["action"] = "auto_adjust"
            event.adjustments_made = [a["action"] for a in adjustments]
            
        elif escalation == EscalationLevel.MAIN_BRAIN:
            # Escalate to main brain for deeper analysis
            response["action"] = "escalated"
            response["escalation_reason"] = self._get_escalation_reason(event)
            
            if self.main_brain_callback:
                # Send to main brain
                brain_response = await self.main_brain_callback({
                    "type": "life_event",
                    "event": event.to_dict(),
                    "user_context": self._get_user_context(user_id)
                })
                response["brain_analysis"] = brain_response
            
        else:
            # User decision needed
            response["action"] = "user_decision"
            response["options"] = self._generate_user_options(event)
        
        # Check if recovery plan needed
        if severity in [EventSeverity.MAJOR, EventSeverity.CRITICAL]:
            recovery = self._create_recovery_plan(user_id, event)
            response["recovery_plan"] = recovery.to_dict()
        
        return response
    
    def process_user_disobedience(
        self,
        user_id: str,
        disobedience_type: str,
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Handle when user doesn't follow the plan.
        
        Types:
        - missed_session: Didn't show up
        - early_quit: Left before session ended
        - ignored_nudge: Didn't respond to reminder
        - did_other_activity: Did something else during study time
        - mood_off: Expressed low motivation
        """
        response = {
            "type": disobedience_type,
            "understood": True,
            "judged": False,  # We don't judge - we adapt
        }
        
        if disobedience_type == "missed_session":
            response["message"] = "No worries - life happens. Want to reschedule or adjust today's plan?"
            response["options"] = [
                {"id": "reschedule", "label": "Reschedule for later"},
                {"id": "shorter", "label": "Do a shorter session now"},
                {"id": "skip", "label": "Skip today, catch up tomorrow"},
                {"id": "talk", "label": "Tell me what's going on"}
            ]
            
        elif disobedience_type == "early_quit":
            response["message"] = "Got it - sometimes we need to stop. We saved your progress."
            response["options"] = [
                {"id": "break", "label": "Taking a break, will return"},
                {"id": "done", "label": "Done for today"},
                {"id": "struggling", "label": "Content is too hard"},
                {"id": "distracted", "label": "Can't focus right now"}
            ]
            
        elif disobedience_type == "ignored_nudge":
            response["message"] = "I see you're busy. I'll check in again later."
            response["next_check_minutes"] = 30
            response["reduce_nudge_frequency"] = True
            
        elif disobedience_type == "did_other_activity":
            activity = context.get("activity", "something else")
            response["message"] = f"Noticed you're on {activity}. Intentional break or got distracted?"
            response["options"] = [
                {"id": "intentional", "label": "Intentional - I needed this"},
                {"id": "distracted", "label": "Oops, got distracted"},
                {"id": "priority", "label": "This is more important right now"},
                {"id": "reschedule", "label": "Let's reschedule study time"}
            ]
            
        elif disobedience_type == "mood_off":
            response["message"] = "I hear you. We all have those days. What would help right now?"
            response["options"] = [
                {"id": "lighter", "label": "Lighter workload today"},
                {"id": "different", "label": "Try a different subject"},
                {"id": "break", "label": "Take a real break first"},
                {"id": "talk", "label": "I want to talk about it"},
                {"id": "push", "label": "No, push me to do it anyway"}
            ]
            response["empathy_mode"] = True
        
        return response
    
    def detect_pattern_from_events(
        self,
        user_id: str
    ) -> Dict[str, Any]:
        """Analyze patterns in user's life events."""
        if user_id not in self._patterns:
            return {"status": "insufficient_data"}
        
        pattern = self._patterns[user_id]
        
        insights = []
        
        # Check for common categories
        if pattern.events_by_category:
            top_category = max(pattern.events_by_category.items(), key=lambda x: x[1])
            if top_category[1] >= 3:
                insights.append(f"Frequent {top_category[0]} events detected")
        
        # Check recovery time
        if pattern.average_recovery_days > 2:
            insights.append("Takes time to recover after disruptions - plan buffer days")
        
        # Check for day patterns
        if pattern.peak_disruption_days:
            insights.append(f"More disruptions on: {', '.join(pattern.peak_disruption_days)}")
        
        return {
            "total_events": pattern.total_events,
            "by_category": pattern.events_by_category,
            "average_recovery_days": pattern.average_recovery_days,
            "insights": insights,
            "recommendations": self._generate_pattern_recommendations(pattern)
        }
    
    # Private methods
    
    def _classify_event(
        self,
        description: str
    ) -> Tuple[EventCategory, EventSeverity]:
        """Classify event from description."""
        desc_lower = description.lower()
        
        # Category detection
        if any(word in desc_lower for word in ["sick", "ill", "doctor", "hospital", "fever", "pain"]):
            category = EventCategory.HEALTH
        elif any(word in desc_lower for word in ["work", "shift", "overtime", "boss", "job"]):
            category = EventCategory.WORK
        elif any(word in desc_lower for word in ["wedding", "party", "birthday", "funeral", "family"]):
            category = EventCategory.FAMILY
        elif any(word in desc_lower for word in ["test", "exam", "quiz", "assignment", "deadline", "class"]):
            category = EventCategory.ACADEMIC
        elif any(word in desc_lower for word in ["emergency", "urgent", "accident"]):
            category = EventCategory.EMERGENCY
        elif any(word in desc_lower for word in ["tired", "exhausted", "stressed", "anxious", "depressed", "mood"]):
            category = EventCategory.MOOD
        else:
            category = EventCategory.SOCIAL
        
        # Severity detection
        if any(word in desc_lower for word in ["emergency", "hospital", "accident", "critical"]):
            severity = EventSeverity.CRITICAL
        elif any(word in desc_lower for word in ["sick", "overtime", "exam tomorrow", "deadline"]):
            severity = EventSeverity.MAJOR
        elif any(word in desc_lower for word in ["tired", "busy", "appointment"]):
            severity = EventSeverity.MODERATE
        else:
            severity = EventSeverity.MINOR
        
        return category, severity
    
    def _determine_escalation(
        self,
        category: EventCategory,
        severity: EventSeverity,
        description: str
    ) -> EscalationLevel:
        """Determine where to handle this event."""
        
        # Critical always goes to main brain
        if severity == EventSeverity.CRITICAL:
            return EscalationLevel.MAIN_BRAIN
        
        # Emergency category
        if category == EventCategory.EMERGENCY:
            return EscalationLevel.MAIN_BRAIN
        
        # Major academic events (exams, deadlines)
        if category == EventCategory.ACADEMIC and severity == EventSeverity.MAJOR:
            return EscalationLevel.MAIN_BRAIN
        
        # Health issues that affect multiple days
        if category == EventCategory.HEALTH and severity in [EventSeverity.MAJOR, EventSeverity.MODERATE]:
            return EscalationLevel.MAIN_BRAIN
        
        # Minor stuff handled by reflex
        return EscalationLevel.REFLEX
    
    def _generate_acknowledgment(self, event: LifeEvent) -> str:
        """Generate appropriate acknowledgment message."""
        acks = {
            EventCategory.HEALTH: "I hope you feel better soon. Let's adjust your schedule.",
            EventCategory.WORK: "Understood - work commitments come first. We'll adapt.",
            EventCategory.FAMILY: "Family is important. I'll reschedule your study time.",
            EventCategory.SOCIAL: "Got it! I'll work around your social plans.",
            EventCategory.ACADEMIC: "Important academic event noted. Let's prepare accordingly.",
            EventCategory.EMERGENCY: "I understand this is urgent. Take care of what matters.",
            EventCategory.MOOD: "I hear you. We'll take it easy.",
        }
        return acks.get(event.category, "Understood. Let me adjust things for you.")
    
    def _generate_quick_adjustments(
        self,
        event: LifeEvent
    ) -> List[Dict[str, Any]]:
        """Generate quick schedule adjustments."""
        adjustments = []
        
        if event.severity == EventSeverity.MINOR:
            adjustments.append({
                "action": "shift_sessions",
                "details": "Move affected sessions by 1-2 hours",
                "automatic": True
            })
        elif event.severity == EventSeverity.MODERATE:
            adjustments.append({
                "action": "reduce_today",
                "details": "Reduce today's study load by 50%",
                "automatic": True
            })
            adjustments.append({
                "action": "redistribute",
                "details": "Spread remaining work across week",
                "automatic": True
            })
        
        return adjustments
    
    def _get_escalation_reason(self, event: LifeEvent) -> str:
        """Get reason for escalation."""
        return f"{event.category.value} event with {event.severity.value} severity requires full schedule re-optimization."
    
    def _generate_user_options(
        self,
        event: LifeEvent
    ) -> List[Dict[str, str]]:
        """Generate options for user to choose from."""
        return [
            {"id": "accept_adjustment", "label": "Accept suggested adjustments"},
            {"id": "custom", "label": "I want to customize the plan"},
            {"id": "ignore", "label": "Keep original schedule (I'll manage)"},
            {"id": "discuss", "label": "Let's discuss options"}
        ]
    
    def _create_recovery_plan(
        self,
        user_id: str,
        event: LifeEvent
    ) -> RecoveryPlan:
        """Create plan to recover from major disruption."""
        import uuid
        
        # Calculate missed items (simplified)
        missed = ["Scheduled study sessions", "Daily goals"]
        
        # Create recovery actions
        actions = [
            {
                "day": 1,
                "action": "Light review of missed topics",
                "duration_minutes": 30
            },
            {
                "day": 2,
                "action": "Catch-up session on priority items",
                "duration_minutes": 60
            },
            {
                "day": 3,
                "action": "Resume normal schedule with adjustments",
                "duration_minutes": 45
            }
        ]
        
        plan = RecoveryPlan(
            plan_id=f"rp_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            event_id=event.event_id,
            missed_items=missed,
            recovery_actions=actions,
            estimated_recovery_days=3,
            created_at=datetime.now()
        )
        
        self._recovery_plans[user_id] = plan
        return plan
    
    def _get_user_context(self, user_id: str) -> Dict[str, Any]:
        """Get user context for main brain escalation."""
        pattern = self._patterns.get(user_id, DisruptionPattern(user_id=user_id))
        recent_events = self._events.get(user_id, [])[-5:]  # Last 5 events
        
        return {
            "user_id": user_id,
            "disruption_pattern": asdict(pattern),
            "recent_events": [e.to_dict() for e in recent_events]
        }
    
    def _update_patterns(self, user_id: str, event: LifeEvent):
        """Update disruption patterns for user."""
        if user_id not in self._patterns:
            self._patterns[user_id] = DisruptionPattern(user_id=user_id)
        
        pattern = self._patterns[user_id]
        pattern.total_events += 1
        
        cat_name = event.category.value
        pattern.events_by_category[cat_name] = pattern.events_by_category.get(cat_name, 0) + 1
        
        # Track day of week
        day_name = event.reported_at.strftime("%A")
        if day_name not in pattern.peak_disruption_days:
            pattern.peak_disruption_days.append(day_name)
    
    def _generate_pattern_recommendations(
        self,
        pattern: DisruptionPattern
    ) -> List[str]:
        """Generate recommendations based on patterns."""
        recs = []
        
        if pattern.total_events > 10:
            recs.append("Consider building more buffer time into your schedule")
        
        if EventCategory.WORK.value in pattern.events_by_category:
            if pattern.events_by_category[EventCategory.WORK.value] >= 3:
                recs.append("Work seems to frequently conflict - consider adjusting study times")
        
        if pattern.average_recovery_days > 2:
            recs.append("Plan lighter days after disruptions to ease back in")
        
        return recs
