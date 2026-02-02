"""
📈 STUDENT GROWTH & ADAPTATION ENGINE
======================================
Learns who the student IS over time and adapts everything.

Features:
- Personality profiling (confidence, learning style, etc.)
- Historical performance tracking
- Pattern recognition (when do they study best? when do they fail?)
- Adaptive communication style
- Growth trajectory analysis

"We learn YOU so we can help YOU better."
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta, date
from enum import Enum
import statistics


class ConfidenceLevel(str, Enum):
    """Student confidence levels."""
    VERY_LOW = "very_low"       # Needs lots of encouragement
    LOW = "low"                 # Needs gentle support
    MODERATE = "moderate"       # Balanced approach
    HIGH = "high"               # Can handle direct feedback
    VERY_HIGH = "very_high"     # Thrives on challenge


class LearningStyle(str, Enum):
    """Primary learning styles."""
    VISUAL = "visual"           # Diagrams, videos, charts
    AUDITORY = "auditory"       # Audio, discussions, lectures
    READING = "reading"         # Text, articles, books
    KINESTHETIC = "kinesthetic" # Practice, hands-on, exercises


class StudyPattern(str, Enum):
    """Study behavior patterns."""
    EARLY_BIRD = "early_bird"       # Best in morning
    NIGHT_OWL = "night_owl"         # Best at night
    AFTERNOON = "afternoon"          # Best in afternoon
    SPORADIC = "sporadic"           # No clear pattern
    DEADLINE_DRIVEN = "deadline_driven"  # Only works near deadlines


class StressResponse(str, Enum):
    """How student responds to stress."""
    THRIVES = "thrives"         # Works better under pressure
    MANAGES = "manages"         # Handles it okay
    STRUGGLES = "struggles"     # Needs support during stress
    AVOIDS = "avoids"           # Procrastinates when stressed


class CommunicationStyle(str, Enum):
    """Preferred communication style."""
    ENCOURAGING = "encouraging"     # "You've got this!"
    DIRECT = "direct"               # "Here's what to do"
    ANALYTICAL = "analytical"       # "Data shows..."
    EMPATHETIC = "empathetic"       # "I understand..."
    PLAYFUL = "playful"             # Gamified, fun
    PROFESSIONAL = "professional"   # Business-like


@dataclass
class PerformanceSnapshot:
    """A snapshot of student performance."""
    date: date
    study_minutes: int
    sessions_completed: int
    sessions_missed: int
    mastery_scores: Dict[str, float]  # subject -> score
    goals_achieved: int
    distractions_detected: int
    mood_rating: Optional[int] = None  # 1-10
    energy_rating: Optional[int] = None  # 1-10
    notes: str = ""
    
    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        data['date'] = self.date.isoformat()
        return data


@dataclass
class StudentProfile:
    """Complete student profile that evolves over time."""
    user_id: str
    
    # Personality traits (learned over time)
    confidence_level: ConfidenceLevel = ConfidenceLevel.MODERATE
    primary_learning_style: LearningStyle = LearningStyle.READING
    secondary_learning_style: Optional[LearningStyle] = None
    study_pattern: StudyPattern = StudyPattern.SPORADIC
    stress_response: StressResponse = StressResponse.MANAGES
    preferred_communication: CommunicationStyle = CommunicationStyle.ENCOURAGING
    
    # Behavioral patterns (tracked)
    average_session_length_minutes: float = 45.0
    best_study_hour: int = 10  # 10 AM
    worst_study_hour: int = 22  # 10 PM
    average_weekly_study_hours: float = 10.0
    completion_rate: float = 0.7  # 70% of planned sessions completed
    
    # Strengths and struggles
    strong_subjects: List[str] = field(default_factory=list)
    struggling_subjects: List[str] = field(default_factory=list)
    
    # Response patterns
    responds_to_encouragement: float = 0.5  # 0-1 scale
    responds_to_challenge: float = 0.5
    responds_to_data: float = 0.5
    responds_to_social_proof: float = 0.5
    
    # Disobedience patterns (natural human behavior)
    typical_miss_rate: float = 0.2  # 20% of sessions missed
    recovery_speed: str = "moderate"  # How fast they bounce back
    excuse_patterns: List[str] = field(default_factory=list)  # Common excuses
    
    # Life context
    is_working_student: bool = False
    typical_work_hours: int = 0
    has_dependents: bool = False
    is_international: bool = False
    
    # Timestamps
    profile_created: datetime = field(default_factory=datetime.now)
    last_updated: datetime = field(default_factory=datetime.now)
    data_points_collected: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "confidence_level": self.confidence_level.value,
            "primary_learning_style": self.primary_learning_style.value,
            "secondary_learning_style": self.secondary_learning_style.value if self.secondary_learning_style else None,
            "study_pattern": self.study_pattern.value,
            "stress_response": self.stress_response.value,
            "preferred_communication": self.preferred_communication.value,
            "profile_created": self.profile_created.isoformat(),
            "last_updated": self.last_updated.isoformat()
        }


@dataclass
class GrowthMetric:
    """Tracks growth in a specific area."""
    metric_name: str
    start_value: float
    current_value: float
    target_value: float
    trend: str  # improving, declining, stable
    weekly_change: float
    
    @property
    def progress_percent(self) -> float:
        if self.target_value == self.start_value:
            return 100.0
        return ((self.current_value - self.start_value) / (self.target_value - self.start_value)) * 100


class StudentGrowthEngine:
    """
    Tracks and adapts to student growth over time.
    
    Features:
    1. Build student profile from behavior
    2. Track performance over time
    3. Detect patterns and preferences
    4. Adapt communication style
    5. Predict optimal interventions
    """
    
    def __init__(self, db=None):
        self.db = db
        self._profiles: Dict[str, StudentProfile] = {}
        self._history: Dict[str, List[PerformanceSnapshot]] = {}
        self._growth_metrics: Dict[str, List[GrowthMetric]] = {}
    
    def get_or_create_profile(self, user_id: str) -> StudentProfile:
        """Get existing profile or create new one."""
        if user_id not in self._profiles:
            self._profiles[user_id] = StudentProfile(user_id=user_id)
        return self._profiles[user_id]
    
    def record_session(
        self,
        user_id: str,
        session_data: Dict[str, Any]
    ):
        """Record a study session and update profile."""
        profile = self.get_or_create_profile(user_id)
        
        # Extract data
        duration = session_data.get("duration_minutes", 0)
        completed = session_data.get("completed", False)
        hour = session_data.get("start_hour", 12)
        mastery = session_data.get("mastery_score", 0)
        distractions = session_data.get("distractions", 0)
        
        # Update running averages
        profile.data_points_collected += 1
        n = profile.data_points_collected
        
        # Update average session length
        profile.average_session_length_minutes = (
            (profile.average_session_length_minutes * (n - 1) + duration) / n
        )
        
        # Track best/worst hours (simplified)
        if completed and duration >= 30:
            # Good session - might be a good hour
            if mastery > 70:
                profile.best_study_hour = hour
        elif not completed or distractions > 3:
            # Bad session - might be a bad hour
            profile.worst_study_hour = hour
        
        # Update completion rate
        completion_point = 1.0 if completed else 0.0
        profile.completion_rate = (
            (profile.completion_rate * (n - 1) + completion_point) / n
        )
        
        # Detect patterns
        self._detect_patterns(user_id, session_data)
        
        profile.last_updated = datetime.now()
    
    def record_missed_session(
        self,
        user_id: str,
        reason: Optional[str] = None
    ):
        """Record when student misses a session."""
        profile = self.get_or_create_profile(user_id)
        
        # Update miss rate
        n = profile.data_points_collected + 1
        profile.typical_miss_rate = (
            (profile.typical_miss_rate * (n - 1) + 1.0) / n
        )
        
        # Track excuse patterns
        if reason:
            if reason not in profile.excuse_patterns:
                profile.excuse_patterns.append(reason)
                # Keep only last 10 unique excuses
                profile.excuse_patterns = profile.excuse_patterns[-10:]
        
        profile.data_points_collected = n
        profile.last_updated = datetime.now()
    
    def update_confidence_assessment(
        self,
        user_id: str,
        signals: Dict[str, Any]
    ):
        """Update confidence level based on behavioral signals."""
        profile = self.get_or_create_profile(user_id)
        
        # Signals that indicate confidence level:
        # - Asking many questions = might be low confidence OR high engagement
        # - Skipping explanations = high confidence
        # - Requesting harder content = high confidence
        # - Needing reassurance = low confidence
        # - Self-reported anxiety = low confidence
        
        score = 0.5  # Start neutral
        
        if signals.get("requests_reassurance", False):
            score -= 0.2
        if signals.get("asks_many_questions", False):
            score -= 0.1  # Could go either way
        if signals.get("skips_explanations", False):
            score += 0.2
        if signals.get("requests_harder_content", False):
            score += 0.2
        if signals.get("self_reported_anxiety", False):
            score -= 0.3
        if signals.get("celebrates_achievements", False):
            score += 0.1
        
        # Map score to confidence level
        if score < 0.2:
            profile.confidence_level = ConfidenceLevel.VERY_LOW
        elif score < 0.4:
            profile.confidence_level = ConfidenceLevel.LOW
        elif score < 0.6:
            profile.confidence_level = ConfidenceLevel.MODERATE
        elif score < 0.8:
            profile.confidence_level = ConfidenceLevel.HIGH
        else:
            profile.confidence_level = ConfidenceLevel.VERY_HIGH
        
        profile.last_updated = datetime.now()
    
    def detect_learning_style(
        self,
        user_id: str,
        content_preferences: Dict[str, int]
    ):
        """Detect preferred learning style from content choices."""
        profile = self.get_or_create_profile(user_id)
        
        # Content type -> style mapping
        style_scores = {
            LearningStyle.VISUAL: content_preferences.get("videos", 0) + content_preferences.get("diagrams", 0),
            LearningStyle.AUDITORY: content_preferences.get("audio", 0) + content_preferences.get("podcasts", 0),
            LearningStyle.READING: content_preferences.get("articles", 0) + content_preferences.get("textbooks", 0),
            LearningStyle.KINESTHETIC: content_preferences.get("exercises", 0) + content_preferences.get("practice", 0)
        }
        
        # Sort by score
        sorted_styles = sorted(style_scores.items(), key=lambda x: x[1], reverse=True)
        
        if sorted_styles[0][1] > 0:
            profile.primary_learning_style = sorted_styles[0][0]
        if len(sorted_styles) > 1 and sorted_styles[1][1] > 0:
            profile.secondary_learning_style = sorted_styles[1][0]
        
        profile.last_updated = datetime.now()
    
    def get_optimal_communication_style(
        self,
        user_id: str,
        context: str = "general"
    ) -> CommunicationStyle:
        """Get best communication style for this student in this context."""
        profile = self.get_or_create_profile(user_id)
        
        # Base on confidence level
        if profile.confidence_level in [ConfidenceLevel.VERY_LOW, ConfidenceLevel.LOW]:
            base_style = CommunicationStyle.ENCOURAGING
        elif profile.confidence_level == ConfidenceLevel.VERY_HIGH:
            base_style = CommunicationStyle.DIRECT
        else:
            base_style = profile.preferred_communication
        
        # Adjust for context
        if context == "missed_session":
            if profile.confidence_level == ConfidenceLevel.VERY_LOW:
                return CommunicationStyle.EMPATHETIC
            else:
                return CommunicationStyle.ENCOURAGING
        
        elif context == "achievement":
            return CommunicationStyle.ENCOURAGING  # Everyone likes celebration
        
        elif context == "challenge":
            if profile.confidence_level in [ConfidenceLevel.HIGH, ConfidenceLevel.VERY_HIGH]:
                return CommunicationStyle.DIRECT
            else:
                return CommunicationStyle.ENCOURAGING
        
        elif context == "deadline":
            if profile.stress_response == StressResponse.THRIVES:
                return CommunicationStyle.DIRECT
            elif profile.stress_response == StressResponse.AVOIDS:
                return CommunicationStyle.EMPATHETIC
            else:
                return CommunicationStyle.PROFESSIONAL
        
        return base_style
    
    def generate_adapted_message(
        self,
        user_id: str,
        base_message: str,
        context: str = "general"
    ) -> str:
        """Generate message adapted to student's profile."""
        profile = self.get_or_create_profile(user_id)
        style = self.get_optimal_communication_style(user_id, context)
        
        # Adjust tone based on style
        if style == CommunicationStyle.ENCOURAGING:
            prefix = "You're doing great! "
            suffix = " I believe in you! 💪"
        elif style == CommunicationStyle.EMPATHETIC:
            prefix = "I understand this is tough. "
            suffix = " We're in this together. ❤️"
        elif style == CommunicationStyle.DIRECT:
            prefix = ""
            suffix = ""
        elif style == CommunicationStyle.ANALYTICAL:
            prefix = "Based on your patterns: "
            suffix = ""
        elif style == CommunicationStyle.PLAYFUL:
            prefix = "Hey champion! "
            suffix = " Let's crush it! 🎮"
        else:
            prefix = ""
            suffix = ""
        
        return f"{prefix}{base_message}{suffix}"
    
    def get_growth_report(
        self,
        user_id: str,
        period_days: int = 30
    ) -> Dict[str, Any]:
        """Generate growth report for student."""
        profile = self.get_or_create_profile(user_id)
        
        if user_id not in self._history or len(self._history[user_id]) < 2:
            return {
                "status": "insufficient_data",
                "message": "We need more data to show your growth. Keep studying!",
                "data_points": profile.data_points_collected
            }
        
        history = self._history[user_id]
        recent = [h for h in history if (date.today() - h.date).days <= period_days]
        
        if not recent:
            return {"status": "no_recent_data"}
        
        # Calculate trends
        report = {
            "period_days": period_days,
            "sessions_completed": sum(h.sessions_completed for h in recent),
            "sessions_missed": sum(h.sessions_missed for h in recent),
            "total_study_minutes": sum(h.study_minutes for h in recent),
            "completion_rate": profile.completion_rate * 100,
            "profile_summary": {
                "confidence": profile.confidence_level.value,
                "learning_style": profile.primary_learning_style.value,
                "study_pattern": profile.study_pattern.value,
                "best_time": f"{profile.best_study_hour}:00",
            },
            "strengths": profile.strong_subjects,
            "areas_for_growth": profile.struggling_subjects,
            "recommendations": self._generate_recommendations(profile)
        }
        
        return report
    
    def predict_session_success(
        self,
        user_id: str,
        planned_hour: int,
        subject: str,
        duration_minutes: int
    ) -> Dict[str, Any]:
        """Predict likelihood of session success."""
        profile = self.get_or_create_profile(user_id)
        
        success_probability = 0.5  # Start neutral
        factors = []
        
        # Time factor
        if planned_hour == profile.best_study_hour:
            success_probability += 0.2
            factors.append("Optimal study time for you")
        elif planned_hour == profile.worst_study_hour:
            success_probability -= 0.2
            factors.append("This isn't usually your best time")
        
        # Duration factor
        if duration_minutes > profile.average_session_length_minutes * 1.5:
            success_probability -= 0.15
            factors.append("Session is longer than your average")
        elif duration_minutes < profile.average_session_length_minutes * 0.7:
            success_probability += 0.1
            factors.append("Nice focused session length")
        
        # Subject factor
        if subject in profile.strong_subjects:
            success_probability += 0.15
            factors.append(f"You're strong in {subject}")
        elif subject in profile.struggling_subjects:
            success_probability -= 0.1
            factors.append(f"Extra support available for {subject}")
        
        # Historical completion rate
        success_probability = (success_probability + profile.completion_rate) / 2
        
        return {
            "success_probability": min(1.0, max(0.0, success_probability)),
            "factors": factors,
            "recommendation": self._get_session_recommendation(success_probability, factors)
        }
    
    def _detect_patterns(self, user_id: str, session_data: Dict[str, Any]):
        """Detect study patterns from session data."""
        profile = self.get_or_create_profile(user_id)
        
        hour = session_data.get("start_hour", 12)
        
        # Simple pattern detection
        if 5 <= hour <= 9:
            # Morning session
            if profile.study_pattern != StudyPattern.EARLY_BIRD:
                profile.study_pattern = StudyPattern.EARLY_BIRD
        elif 21 <= hour <= 2:
            # Night session
            if profile.study_pattern != StudyPattern.NIGHT_OWL:
                profile.study_pattern = StudyPattern.NIGHT_OWL
        elif 13 <= hour <= 17:
            profile.study_pattern = StudyPattern.AFTERNOON
    
    def _generate_recommendations(self, profile: StudentProfile) -> List[str]:
        """Generate personalized recommendations."""
        recs = []
        
        if profile.completion_rate < 0.5:
            recs.append("Try shorter, more frequent study sessions")
        
        if profile.confidence_level in [ConfidenceLevel.VERY_LOW, ConfidenceLevel.LOW]:
            recs.append("Celebrate small wins to build confidence")
        
        if profile.study_pattern == StudyPattern.DEADLINE_DRIVEN:
            recs.append("Try breaking big goals into smaller daily tasks")
        
        if profile.typical_miss_rate > 0.3:
            recs.append("Consider more flexible scheduling")
        
        if not recs:
            recs.append("Keep up the great work! Your patterns are strong.")
        
        return recs
    
    def _get_session_recommendation(
        self,
        probability: float,
        factors: List[str]
    ) -> str:
        """Get recommendation based on predicted success."""
        if probability >= 0.8:
            return "Great conditions for a productive session!"
        elif probability >= 0.6:
            return "Good setup. Stay focused and you'll do well."
        elif probability >= 0.4:
            return "Consider adjusting time or duration for better results."
        else:
            return "This might be challenging. Would you like to reschedule?"
