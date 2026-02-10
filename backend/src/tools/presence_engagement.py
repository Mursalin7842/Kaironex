"""
👁️ PRESENCE & ENGAGEMENT ENGINE
=================================
Detects when user is/isn't present and takes action.

Features:
- Detect absence during scheduled study time
- Gentle persuasion to return
- Distraction detection
- Respectful engagement (like a caring tutor)
- Understand and convince without being pushy

"We call you back, but we respect you."
"""

import json
from typing import Dict, Any, List, Optional, Callable
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from enum import Enum
import random


class PresenceState(str, Enum):
    """User's presence state."""
    ACTIVE = "active"           # Currently engaged in app
    IDLE = "idle"               # App open but no activity
    BACKGROUND = "background"   # App in background
    ABSENT = "absent"           # Not in app during scheduled time
    EXPECTED = "expected"       # Scheduled time but not arrived yet
    EXCUSED = "excused"         # User indicated they can't make it


class EngagementLevel(str, Enum):
    """Level of engagement."""
    DEEP_FOCUS = "deep_focus"   # Highly engaged
    ENGAGED = "engaged"         # Normal engagement
    DISTRACTED = "distracted"   # Showing signs of distraction
    DISENGAGED = "disengaged"   # Very low engagement
    AWAY = "away"               # Not present


class PersuasionStyle(str, Enum):
    """Style of gentle persuasion."""
    ENCOURAGING = "encouraging"   # You've got this!
    EMPATHETIC = "empathetic"     # I understand it's hard
    PRACTICAL = "practical"       # Here's what we can do
    MOTIVATIONAL = "motivational" # Think of your goals
    LIGHT = "light"              # No pressure, just checking


@dataclass
class PresenceEvent:
    """An event related to user presence."""
    event_type: str  # arrival, departure, activity, idle_start, idle_end
    timestamp: datetime
    context: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "timestamp": self.timestamp.isoformat()
        }


@dataclass
class EngagementSession:
    """A study/engagement session."""
    session_id: str
    user_id: str
    scheduled_start: datetime
    scheduled_end: datetime
    actual_start: Optional[datetime] = None
    actual_end: Optional[datetime] = None
    presence_state: PresenceState = PresenceState.EXPECTED
    engagement_level: EngagementLevel = EngagementLevel.ENGAGED
    distractions_detected: int = 0
    nudges_sent: int = 0
    
    @property
    def is_late(self) -> bool:
        if self.actual_start:
            return False
        return datetime.now() > self.scheduled_start + timedelta(minutes=5)
    
    @property
    def minutes_late(self) -> int:
        if self.actual_start or datetime.now() <= self.scheduled_start:
            return 0
        return int((datetime.now() - self.scheduled_start).total_seconds() / 60)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "session_id": self.session_id,
            "scheduled_start": self.scheduled_start.isoformat(),
            "scheduled_end": self.scheduled_end.isoformat(),
            "actual_start": self.actual_start.isoformat() if self.actual_start else None,
            "is_late": self.is_late,
            "minutes_late": self.minutes_late,
            "presence_state": self.presence_state.value,
            "engagement_level": self.engagement_level.value,
            "distractions_detected": self.distractions_detected
        }


class PresenceEngagementEngine:
    """
    Manages user presence detection and gentle engagement.
    
    Features:
    1. Detect when user should be present but isn't
    2. Send gentle, respectful nudges
    3. Track engagement levels
    4. Detect and help with distractions
    5. Celebrate returns and completions
    """
    
    def __init__(self, notification_handler: Optional[Callable] = None):
        self.notification_handler = notification_handler
        self._sessions: Dict[str, EngagementSession] = {}
        self._presence_history: Dict[str, List[PresenceEvent]] = {}
        self._user_preferences: Dict[str, Dict[str, Any]] = {}
        self._nudge_cooldown_minutes = 10
        self._last_nudge: Dict[str, datetime] = {}
    
    def create_session(
        self,
        user_id: str,
        session_id: str,
        scheduled_start: datetime,
        scheduled_end: datetime
    ) -> EngagementSession:
        """Create a new study session to monitor."""
        session = EngagementSession(
            session_id=session_id,
            user_id=user_id,
            scheduled_start=scheduled_start,
            scheduled_end=scheduled_end
        )
        self._sessions[session_id] = session
        return session
    
    def record_presence(
        self,
        user_id: str,
        session_id: str,
        event_type: str,
        context: Optional[Dict[str, Any]] = None
    ):
        """Record a presence event."""
        event = PresenceEvent(
            event_type=event_type,
            timestamp=datetime.now(),
            context=context or {}
        )
        
        if user_id not in self._presence_history:
            self._presence_history[user_id] = []
        self._presence_history[user_id].append(event)
        
        # Update session if exists
        if session_id in self._sessions:
            session = self._sessions[session_id]
            
            if event_type == "arrival":
                session.actual_start = event.timestamp
                session.presence_state = PresenceState.ACTIVE
            elif event_type == "departure":
                session.presence_state = PresenceState.ABSENT
            elif event_type == "idle_start":
                session.presence_state = PresenceState.IDLE
            elif event_type == "activity":
                session.presence_state = PresenceState.ACTIVE
    
    def check_session_status(
        self,
        session_id: str
    ) -> Dict[str, Any]:
        """Check status of a session and determine if action needed."""
        if session_id not in self._sessions:
            return {"status": "not_found"}
        
        session = self._sessions[session_id]
        now = datetime.now()
        
        # Not time yet
        if now < session.scheduled_start:
            return {
                "status": "upcoming",
                "starts_in_minutes": int((session.scheduled_start - now).total_seconds() / 60),
                "action": None
            }
        
        # Session should be active
        if session.scheduled_start <= now <= session.scheduled_end:
            if session.presence_state == PresenceState.ACTIVE:
                return {
                    "status": "active",
                    "engagement": session.engagement_level.value,
                    "action": None
                }
            elif session.is_late:
                return {
                    "status": "absent",
                    "minutes_late": session.minutes_late,
                    "action": "send_nudge" if self._can_send_nudge(session.user_id) else "wait"
                }
        
        # Session ended
        if now > session.scheduled_end:
            return {
                "status": "completed",
                "actual_duration": self._calculate_duration(session),
                "action": "send_summary"
            }
        
        return {"status": "unknown"}
    
    def generate_nudge(
        self,
        user_id: str,
        session_id: str,
        style: Optional[PersuasionStyle] = None
    ) -> Dict[str, Any]:
        """Generate a gentle nudge to bring user back."""
        if session_id not in self._sessions:
            return {"error": "Session not found"}
        
        session = self._sessions[session_id]
        
        # Choose style based on history and preferences
        if style is None:
            style = self._choose_nudge_style(user_id, session)
        
        nudge = {
            "type": "presence_nudge",
            "style": style.value,
            "title": self._get_nudge_title(style),
            "message": self._get_nudge_message(style, session),
            "action_options": self._get_nudge_actions(session),
            "respect_decline": True,  # Always offer way to decline
            "timestamp": datetime.now().isoformat()
        }
        
        session.nudges_sent += 1
        self._last_nudge[user_id] = datetime.now()
        
        return nudge
    
    def detect_distraction(
        self,
        user_id: str,
        session_id: str,
        activity_pattern: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Detect if user is distracted based on activity patterns."""
        # Patterns that suggest distraction:
        # - Long pauses in activity
        # - Rapid app switching
        # - Scrolling without interaction
        
        indicators = []
        distraction_score = 0
        
        idle_seconds = activity_pattern.get("idle_seconds", 0)
        if idle_seconds > 120:  # 2 minutes idle
            indicators.append("extended_idle")
            distraction_score += 30
        
        app_switches = activity_pattern.get("app_switches_last_5min", 0)
        if app_switches > 5:
            indicators.append("frequent_app_switching")
            distraction_score += 40
        
        scroll_without_action = activity_pattern.get("scroll_without_tap", 0)
        if scroll_without_action > 10:
            indicators.append("passive_scrolling")
            distraction_score += 20
        
        is_distracted = distraction_score >= 50
        
        if is_distracted and session_id in self._sessions:
            self._sessions[session_id].distractions_detected += 1
        
        return {
            "is_distracted": is_distracted,
            "distraction_score": distraction_score,
            "indicators": indicators,
            "intervention": self._get_distraction_intervention(indicators) if is_distracted else None
        }
    
    def generate_return_celebration(
        self,
        user_id: str,
        session_id: str,
        was_absent_minutes: int
    ) -> Dict[str, Any]:
        """Generate a warm welcome back when user returns."""
        messages = [
            "Welcome back! Let's pick up where we left off. 🎯",
            "Hey, you're back! Every minute counts. Ready to crush it?",
            "Great to see you! Your materials are right where you left them.",
            "You made it back! That takes strength. Let's make this count.",
            "Welcome back, champion. Ready when you are. 💪"
        ]
        
        return {
            "type": "return_welcome",
            "message": random.choice(messages),
            "context_restore": {
                "last_topic": "We were working on...",
                "suggested_action": "Continue from where you left off?"
            },
            "no_guilt": True  # We don't make them feel bad for the absence
        }
    
    def _can_send_nudge(self, user_id: str) -> bool:
        """Check if enough time has passed to send another nudge."""
        if user_id not in self._last_nudge:
            return True
        
        elapsed = datetime.now() - self._last_nudge[user_id]
        return elapsed.total_seconds() / 60 >= self._nudge_cooldown_minutes
    
    def _choose_nudge_style(
        self,
        user_id: str,
        session: EngagementSession
    ) -> PersuasionStyle:
        """Choose appropriate nudge style based on context."""
        if session.nudges_sent == 0:
            return PersuasionStyle.LIGHT
        elif session.nudges_sent == 1:
            return PersuasionStyle.ENCOURAGING
        elif session.nudges_sent == 2:
            return PersuasionStyle.EMPATHETIC
        else:
            return PersuasionStyle.PRACTICAL
    
    def _get_nudge_title(self, style: PersuasionStyle) -> str:
        """Get nudge title based on style."""
        titles = {
            PersuasionStyle.LIGHT: "Hey there! 👋",
            PersuasionStyle.ENCOURAGING: "You've got this! 💪",
            PersuasionStyle.EMPATHETIC: "Just checking in ❤️",
            PersuasionStyle.PRACTICAL: "Quick thought 💡",
            PersuasionStyle.MOTIVATIONAL: "Remember why 🎯"
        }
        return titles[style]
    
    def _get_nudge_message(
        self,
        style: PersuasionStyle,
        session: EngagementSession
    ) -> str:
        """Get nudge message based on style."""
        messages = {
            PersuasionStyle.LIGHT: [
                f"Your study session started {session.minutes_late} min ago. No pressure - just a friendly reminder!",
                "Your study materials are ready and waiting. Swing by when you can!"
            ],
            PersuasionStyle.ENCOURAGING: [
                "Starting is the hardest part - once you begin, you'll find your flow!",
                "Even 15 minutes of focused study makes a difference. You can do this!"
            ],
            PersuasionStyle.EMPATHETIC: [
                "I know starting can feel hard sometimes. Would a shorter session work better today?",
                "It's okay if today is tough. Maybe we can adjust the plan together?"
            ],
            PersuasionStyle.PRACTICAL: [
                "Your exam is coming up. What if we do a quick 20-minute review instead?",
                "I can prepare a lighter session if the full one feels like too much."
            ],
            PersuasionStyle.MOTIVATIONAL: [
                "Think about how good you'll feel after making progress today!",
                "Future you will thank present you. Let's make them proud!"
            ]
        }
        return random.choice(messages[style])
    
    def _get_nudge_actions(self, session: EngagementSession) -> List[Dict[str, str]]:
        """Get action options for nudge."""
        return [
            {"id": "start_now", "label": "Start Now", "type": "primary"},
            {"id": "start_in_10", "label": "In 10 minutes", "type": "secondary"},
            {"id": "shorter_session", "label": "Shorter session", "type": "secondary"},
            {"id": "reschedule", "label": "Not today", "type": "tertiary"}
        ]
    
    def _get_distraction_intervention(
        self,
        indicators: List[str]
    ) -> Dict[str, Any]:
        """Get appropriate intervention for distraction."""
        if "frequent_app_switching" in indicators:
            return {
                "type": "focus_mode",
                "message": "Noticed some app-hopping! Want to enable focus mode?",
                "suggestion": "Block distracting apps for the next 30 minutes"
            }
        elif "extended_idle" in indicators:
            return {
                "type": "check_in",
                "message": "You've been quiet. Need a quick break or a different approach?",
                "suggestion": "Take a 5-minute stretch break"
            }
        else:
            return {
                "type": "gentle_redirect",
                "message": "Getting a bit distracted? Happens to everyone!",
                "suggestion": "Let's refocus on one small goal"
            }
    
    def _calculate_duration(self, session: EngagementSession) -> int:
        """Calculate actual session duration in minutes."""
        if not session.actual_start:
            return 0
        
        end = session.actual_end or datetime.now()
        return int((end - session.actual_start).total_seconds() / 60)
