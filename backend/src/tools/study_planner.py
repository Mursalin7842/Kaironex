"""
📅 STUDY PLANNER & SCHEDULER
=============================
Intelligent study scheduling with calendar integration.

Features:
- Optimal study slot calculation based on user patterns
- Deadline-aware scheduling
- Energy level optimization (via Vitality data)
- Integration with Radius for location awareness
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta, time
from enum import Enum
import math


class StudyBlockType(str, Enum):
    """Types of study blocks."""
    DEEP_FOCUS = "deep_focus"       # 90-min deep work
    REVIEW = "review"               # 30-min review
    PRACTICE = "practice"           # 45-min problem solving
    LIGHT_STUDY = "light_study"     # 25-min pomodoro
    CRAM = "cram"                   # Intensive before exam


class EnergyLevel(str, Enum):
    """Energy levels from Vitality agent."""
    PEAK = "peak"           # Best for deep focus
    HIGH = "high"           # Good for learning
    MODERATE = "moderate"   # OK for review
    LOW = "low"             # Light tasks only
    DEPLETED = "depleted"   # Rest needed


@dataclass
class TimeSlot:
    """A potential study time slot."""
    start: datetime
    end: datetime
    predicted_energy: EnergyLevel = EnergyLevel.MODERATE
    location_context: str = "unknown"  # From Radius
    is_blocked: bool = False
    block_reason: Optional[str] = None
    
    @property
    def duration_minutes(self) -> int:
        return int((self.end - self.start).total_seconds() / 60)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "start": self.start.isoformat(),
            "end": self.end.isoformat(),
            "duration_minutes": self.duration_minutes,
            "predicted_energy": self.predicted_energy.value,
            "location_context": self.location_context,
            "is_blocked": self.is_blocked,
            "block_reason": self.block_reason
        }


@dataclass
class StudyBlock:
    """A scheduled study block."""
    block_id: str
    subject: str
    block_type: StudyBlockType
    start: datetime
    end: datetime
    topics: List[str] = field(default_factory=list)
    goals: List[str] = field(default_factory=list)
    priority: int = 5  # 1-10
    is_confirmed: bool = False
    reminder_set: bool = False
    
    @property
    def duration_minutes(self) -> int:
        return int((self.end - self.start).total_seconds() / 60)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "block_type": self.block_type.value,
            "start": self.start.isoformat(),
            "end": self.end.isoformat(),
            "duration_minutes": self.duration_minutes
        }


@dataclass
class Deadline:
    """A study deadline (exam, assignment, etc.)."""
    deadline_id: str
    subject: str
    title: str
    due_date: datetime
    estimated_prep_hours: float = 5.0
    priority: int = 5
    completed_hours: float = 0.0
    topics: List[str] = field(default_factory=list)
    
    @property
    def hours_remaining(self) -> float:
        return max(0, self.estimated_prep_hours - self.completed_hours)
    
    @property
    def time_until_due(self) -> timedelta:
        return self.due_date - datetime.now()
    
    @property
    def urgency_score(self) -> float:
        """Calculate urgency (higher = more urgent)."""
        hours_left = self.time_until_due.total_seconds() / 3600
        if hours_left <= 0:
            return 100.0  # Past due!
        
        # Urgency increases as deadline approaches
        return (self.hours_remaining / hours_left) * 10 * self.priority
    
    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        data['due_date'] = self.due_date.isoformat()
        data['hours_remaining'] = self.hours_remaining
        data['urgency_score'] = self.urgency_score
        return data


class StudyPlanner:
    """
    Intelligent study scheduler with deadline awareness.
    
    Features:
    1. Optimal slot finding based on energy/location
    2. Deadline-driven scheduling
    3. Spaced practice scheduling
    4. Integration with Vitality and Radius agents
    """
    
    def __init__(self, gemini_client=None, db=None):
        self.gemini = gemini_client
        self.db = db
        self._user_schedules: Dict[str, List[StudyBlock]] = {}
        self._user_deadlines: Dict[str, List[Deadline]] = {}
        
        # Default productive hours (can be personalized)
        self._default_productive_hours = {
            "morning_peak": (time(9, 0), time(12, 0)),
            "afternoon": (time(14, 0), time(17, 0)),
            "evening": (time(19, 0), time(22, 0))
        }
    
    def get_tool_definition(self) -> Dict[str, Any]:
        """Return the function calling definition for Gemini."""
        return {
            "name": "schedule_study",
            "description": "Find optimal study times and create study blocks. Considers energy levels, deadlines, and location.",
            "parameters": {
                "type": "object",
                "properties": {
                    "subject": {
                        "type": "string",
                        "description": "Subject to study"
                    },
                    "duration_minutes": {
                        "type": "integer",
                        "description": "Desired study duration"
                    },
                    "deadline": {
                        "type": "string",
                        "description": "ISO format deadline if applicable"
                    },
                    "preferred_time": {
                        "type": "string",
                        "enum": ["morning", "afternoon", "evening", "any"],
                        "description": "Preferred time of day"
                    },
                    "block_type": {
                        "type": "string",
                        "enum": ["deep_focus", "review", "practice", "light_study", "cram"],
                        "description": "Type of study session"
                    }
                },
                "required": ["subject"]
            }
        }
    
    async def find_optimal_slots(
        self,
        user_id: str,
        duration_minutes: int = 60,
        block_type: StudyBlockType = StudyBlockType.DEEP_FOCUS,
        preferred_time: str = "any",
        days_ahead: int = 7,
        vitality_data: Optional[Dict[str, Any]] = None,
        radius_data: Optional[Dict[str, Any]] = None
    ) -> List[TimeSlot]:
        """
        Find optimal study slots based on various factors.
        
        Args:
            user_id: User identifier
            duration_minutes: Required duration
            block_type: Type of study session
            preferred_time: morning/afternoon/evening/any
            days_ahead: How many days to look ahead
            vitality_data: Energy predictions from Vitality agent
            radius_data: Location context from Radius agent
        
        Returns:
            List of ranked TimeSlot options
        """
        slots = []
        now = datetime.now()
        
        for day_offset in range(days_ahead):
            day = now + timedelta(days=day_offset)
            day_slots = self._get_day_slots(day, duration_minutes, preferred_time)
            
            for slot in day_slots:
                # Apply vitality data if available
                if vitality_data:
                    slot.predicted_energy = self._predict_energy(
                        slot.start.time(),
                        vitality_data
                    )
                
                # Apply radius data if available
                if radius_data:
                    slot.location_context = self._predict_location(
                        slot.start,
                        radius_data
                    )
                
                # Check if blocked by existing schedule
                if self._is_slot_blocked(user_id, slot):
                    slot.is_blocked = True
                    slot.block_reason = "Existing commitment"
                
                slots.append(slot)
        
        # Rank slots by suitability
        slots = self._rank_slots(slots, block_type)
        
        return slots
    
    async def create_study_plan(
        self,
        user_id: str,
        deadline: Deadline,
        available_slots: Optional[List[TimeSlot]] = None
    ) -> List[StudyBlock]:
        """
        Create a study plan for a deadline.
        
        Args:
            user_id: User identifier
            deadline: Deadline to prepare for
            available_slots: Pre-calculated available slots
        
        Returns:
            List of StudyBlock for the deadline
        """
        import uuid
        
        # Calculate required study sessions
        hours_needed = deadline.hours_remaining
        sessions_needed = math.ceil(hours_needed / 1.5)  # 90-min blocks
        
        # Get available slots if not provided
        if not available_slots:
            days_until = max(1, deadline.time_until_due.days)
            available_slots = await self.find_optimal_slots(
                user_id,
                duration_minutes=90,
                block_type=StudyBlockType.DEEP_FOCUS,
                days_ahead=days_until
            )
        
        # Filter out blocked slots
        available_slots = [s for s in available_slots if not s.is_blocked]
        
        # Distribute sessions with spaced practice
        study_blocks = []
        topics_per_session = len(deadline.topics) // max(1, sessions_needed)
        
        for i, slot in enumerate(available_slots[:sessions_needed]):
            # Determine block type based on proximity to deadline
            days_left = (deadline.due_date - slot.start).days
            if days_left <= 1:
                block_type = StudyBlockType.CRAM
            elif days_left <= 3:
                block_type = StudyBlockType.PRACTICE
            else:
                block_type = StudyBlockType.DEEP_FOCUS
            
            # Assign topics
            start_idx = i * topics_per_session
            end_idx = start_idx + topics_per_session
            session_topics = deadline.topics[start_idx:end_idx] if deadline.topics else []
            
            block = StudyBlock(
                block_id=f"sb_{uuid.uuid4().hex[:8]}",
                subject=deadline.subject,
                block_type=block_type,
                start=slot.start,
                end=slot.end,
                topics=session_topics,
                goals=[f"Study for {deadline.title}"],
                priority=deadline.priority
            )
            study_blocks.append(block)
        
        return study_blocks
    
    async def schedule_spaced_reviews(
        self,
        user_id: str,
        subject: str,
        topics: List[str],
        start_date: Optional[datetime] = None
    ) -> List[StudyBlock]:
        """
        Schedule spaced repetition reviews.
        
        Uses optimal spacing: Day 1, Day 3, Day 7, Day 14, Day 30
        """
        import uuid
        
        if start_date is None:
            start_date = datetime.now()
        
        spacing_days = [1, 3, 7, 14, 30]
        review_blocks = []
        
        for day_offset in spacing_days:
            review_date = start_date + timedelta(days=day_offset)
            
            # Find a good slot on that day
            slots = await self.find_optimal_slots(
                user_id,
                duration_minutes=30,
                block_type=StudyBlockType.REVIEW,
                preferred_time="any",
                days_ahead=1
            )
            
            if slots:
                slot = slots[0]
                
                block = StudyBlock(
                    block_id=f"sr_{uuid.uuid4().hex[:8]}",
                    subject=subject,
                    block_type=StudyBlockType.REVIEW,
                    start=review_date.replace(
                        hour=slot.start.hour,
                        minute=slot.start.minute
                    ),
                    end=review_date.replace(
                        hour=slot.end.hour,
                        minute=slot.end.minute
                    ),
                    topics=topics,
                    goals=[f"Spaced review #{spacing_days.index(day_offset) + 1}"],
                    priority=6
                )
                review_blocks.append(block)
        
        return review_blocks
    
    def add_deadline(self, user_id: str, deadline: Deadline):
        """Add a deadline for tracking."""
        if user_id not in self._user_deadlines:
            self._user_deadlines[user_id] = []
        self._user_deadlines[user_id].append(deadline)
    
    def get_urgent_deadlines(self, user_id: str, threshold: float = 5.0) -> List[Deadline]:
        """Get deadlines above urgency threshold."""
        if user_id not in self._user_deadlines:
            return []
        
        urgent = [d for d in self._user_deadlines[user_id] if d.urgency_score >= threshold]
        urgent.sort(key=lambda d: d.urgency_score, reverse=True)
        return urgent
    
    def get_today_schedule(self, user_id: str) -> List[StudyBlock]:
        """Get today's study blocks."""
        if user_id not in self._user_schedules:
            return []
        
        today = datetime.now().date()
        return [
            block for block in self._user_schedules[user_id]
            if block.start.date() == today
        ]
    
    def get_week_overview(self, user_id: str) -> Dict[str, Any]:
        """Get weekly study overview."""
        if user_id not in self._user_schedules:
            return {"total_hours": 0, "blocks": [], "by_subject": {}}
        
        now = datetime.now()
        week_end = now + timedelta(days=7)
        
        week_blocks = [
            block for block in self._user_schedules[user_id]
            if now <= block.start <= week_end
        ]
        
        total_minutes = sum(block.duration_minutes for block in week_blocks)
        
        by_subject = {}
        for block in week_blocks:
            if block.subject not in by_subject:
                by_subject[block.subject] = 0
            by_subject[block.subject] += block.duration_minutes
        
        return {
            "total_hours": total_minutes / 60,
            "blocks": [b.to_dict() for b in week_blocks],
            "by_subject": {k: v / 60 for k, v in by_subject.items()}
        }
    
    # Private helper methods
    
    def _get_day_slots(
        self,
        day: datetime,
        duration_minutes: int,
        preferred_time: str
    ) -> List[TimeSlot]:
        """Get potential slots for a day."""
        slots = []
        
        # Filter by preference
        time_ranges = self._default_productive_hours.copy()
        if preferred_time == "morning":
            time_ranges = {"morning_peak": time_ranges["morning_peak"]}
        elif preferred_time == "afternoon":
            time_ranges = {"afternoon": time_ranges["afternoon"]}
        elif preferred_time == "evening":
            time_ranges = {"evening": time_ranges["evening"]}
        
        for period_name, (start_time, end_time) in time_ranges.items():
            period_start = day.replace(
                hour=start_time.hour,
                minute=start_time.minute,
                second=0,
                microsecond=0
            )
            period_end = day.replace(
                hour=end_time.hour,
                minute=end_time.minute,
                second=0,
                microsecond=0
            )
            
            # Only add if slot fits
            if (period_end - period_start).total_seconds() / 60 >= duration_minutes:
                slots.append(TimeSlot(
                    start=period_start,
                    end=period_start + timedelta(minutes=duration_minutes)
                ))
        
        return slots
    
    def _predict_energy(self, slot_time: time, vitality_data: Dict[str, Any]) -> EnergyLevel:
        """Predict energy level from Vitality data."""
        # This would use actual vitality patterns
        hour = slot_time.hour
        
        if 9 <= hour <= 11:
            return EnergyLevel.PEAK
        elif 14 <= hour <= 16:
            return EnergyLevel.HIGH
        elif 19 <= hour <= 21:
            return EnergyLevel.MODERATE
        else:
            return EnergyLevel.LOW
    
    def _predict_location(self, slot_time: datetime, radius_data: Dict[str, Any]) -> str:
        """Predict location from Radius data."""
        # Would use actual Radius patterns
        weekday = slot_time.weekday()
        hour = slot_time.hour
        
        if weekday < 5 and 8 <= hour <= 17:
            return "campus"
        elif weekday < 5:
            return "home"
        else:
            return "home"
    
    def _is_slot_blocked(self, user_id: str, slot: TimeSlot) -> bool:
        """Check if slot conflicts with existing blocks."""
        if user_id not in self._user_schedules:
            return False
        
        for block in self._user_schedules[user_id]:
            if block.start < slot.end and block.end > slot.start:
                return True
        
        return False
    
    def _rank_slots(
        self,
        slots: List[TimeSlot],
        block_type: StudyBlockType
    ) -> List[TimeSlot]:
        """Rank slots by suitability for the block type."""
        
        def score_slot(slot: TimeSlot) -> float:
            score = 0.0
            
            # Energy match
            energy_scores = {
                EnergyLevel.PEAK: 100,
                EnergyLevel.HIGH: 80,
                EnergyLevel.MODERATE: 50,
                EnergyLevel.LOW: 20,
                EnergyLevel.DEPLETED: 0
            }
            score += energy_scores.get(slot.predicted_energy, 50)
            
            # Block type preferences
            if block_type == StudyBlockType.DEEP_FOCUS:
                if slot.predicted_energy == EnergyLevel.PEAK:
                    score += 50
            elif block_type == StudyBlockType.REVIEW:
                if slot.predicted_energy == EnergyLevel.MODERATE:
                    score += 30
            
            # Location bonus
            if slot.location_context == "campus":
                score += 20
            elif slot.location_context == "home":
                score += 10
            
            # Penalty for blocked
            if slot.is_blocked:
                score -= 1000
            
            return score
        
        slots.sort(key=score_slot, reverse=True)
        return slots
