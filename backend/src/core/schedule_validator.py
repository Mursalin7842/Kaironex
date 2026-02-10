"""
✅ SCHEDULE VALIDATOR
=====================
Validates user schedule changes and maintains schedule integrity.

"You propose, we validate, together we optimize."

Features:
- User manual schedule updates → Validation → Approve/Discard/Edit
- Conflict detection
- Feasibility checking
- Impact analysis
- Notification to user of decisions
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta, date, time
from enum import Enum
import uuid


class ValidationResult(str, Enum):
    """Result of schedule validation."""
    APPROVED = "approved"           # Change is valid, apply it
    APPROVED_WITH_WARNINGS = "approved_with_warnings"  # OK but has concerns
    MODIFIED = "modified"           # Changed the request slightly
    REJECTED = "rejected"           # Cannot accept this change
    NEEDS_DISCUSSION = "needs_discussion"  # Complex, need user input


class ConflictType(str, Enum):
    """Types of schedule conflicts."""
    TIME_OVERLAP = "time_overlap"
    INSUFFICIENT_REST = "insufficient_rest"
    DEADLINE_RISK = "deadline_risk"
    ENERGY_MISMATCH = "energy_mismatch"
    WORK_HOUR_VIOLATION = "work_hour_violation"
    TRAVEL_IMPOSSIBLE = "travel_impossible"
    TOO_LONG_SESSION = "too_long_session"


class ChangeType(str, Enum):
    """Types of schedule changes."""
    ADD = "add"
    REMOVE = "remove"
    MOVE = "move"
    RESIZE = "resize"
    SWAP = "swap"


@dataclass
class ScheduleBlock:
    """A block of time in the schedule."""
    block_id: str
    user_id: str
    title: str
    block_type: str  # study, work, class, personal, break
    start: datetime
    end: datetime
    location: Optional[str] = None
    is_flexible: bool = True
    priority: int = 5  # 1-10
    linked_deadline: Optional[str] = None
    
    @property
    def duration_minutes(self) -> int:
        return int((self.end - self.start).total_seconds() / 60)
    
    def overlaps_with(self, other: "ScheduleBlock") -> bool:
        return self.start < other.end and self.end > other.start
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "start": self.start.isoformat(),
            "end": self.end.isoformat(),
            "duration_minutes": self.duration_minutes
        }


@dataclass
class ScheduleChange:
    """A proposed change to the schedule."""
    change_id: str
    user_id: str
    change_type: ChangeType
    target_block_id: Optional[str]  # For edit/remove
    new_block: Optional[ScheduleBlock]  # For add/edit
    reason: Optional[str] = None
    proposed_at: datetime = field(default_factory=datetime.now)
    
    def to_dict(self) -> Dict[str, Any]:
        data = {
            "change_id": self.change_id,
            "change_type": self.change_type.value,
            "target_block_id": self.target_block_id,
            "reason": self.reason,
            "proposed_at": self.proposed_at.isoformat()
        }
        if self.new_block:
            data["new_block"] = self.new_block.to_dict()
        return data


@dataclass
class ValidationDecision:
    """Decision from the schedule validator."""
    decision_id: str
    change_id: str
    result: ValidationResult
    conflicts: List[Dict[str, Any]]
    warnings: List[str]
    suggestions: List[str]
    modified_block: Optional[ScheduleBlock]  # If we modified the request
    impact_analysis: Dict[str, Any]
    message_to_user: str
    requires_confirmation: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "decision_id": self.decision_id,
            "change_id": self.change_id,
            "result": self.result.value,
            "conflicts": self.conflicts,
            "warnings": self.warnings,
            "suggestions": self.suggestions,
            "modified_block": self.modified_block.to_dict() if self.modified_block else None,
            "impact_analysis": self.impact_analysis,
            "message_to_user": self.message_to_user,
            "requires_confirmation": self.requires_confirmation
        }


class ScheduleValidator:
    """
    Validates schedule changes and maintains integrity.
    
    When user wants to change their schedule:
    1. User proposes change
    2. Validator checks for conflicts
    3. Validator checks feasibility
    4. Validator returns: APPROVE / MODIFY / REJECT
    5. User is notified of decision with explanation
    """
    
    def __init__(self, db=None):
        self.db = db
        self._schedules: Dict[str, List[ScheduleBlock]] = {}
        self._pending_changes: Dict[str, ScheduleChange] = {}
        self._decisions: Dict[str, ValidationDecision] = {}
        
        # Validation rules
        self._min_break_between_sessions = 15  # minutes
        self._max_session_length = 180  # 3 hours max
        self._min_sleep_hours = 6
        self._max_daily_study_hours = 10
    
    def get_user_schedule(
        self,
        user_id: str,
        date_from: Optional[date] = None,
        date_to: Optional[date] = None
    ) -> List[ScheduleBlock]:
        """Get user's schedule, optionally filtered by date range."""
        if user_id not in self._schedules:
            return []
        
        blocks = self._schedules[user_id]
        
        if date_from:
            blocks = [b for b in blocks if b.start.date() >= date_from]
        if date_to:
            blocks = [b for b in blocks if b.start.date() <= date_to]
        
        return sorted(blocks, key=lambda b: b.start)
    
    async def validate_change(
        self,
        user_id: str,
        change: ScheduleChange,
        user_context: Optional[Dict[str, Any]] = None
    ) -> ValidationDecision:
        """
        Validate a proposed schedule change.
        
        Args:
            user_id: User identifier
            change: The proposed change
            user_context: Additional context (deadlines, energy, etc.)
        
        Returns:
            ValidationDecision with result and explanation
        """
        conflicts = []
        warnings = []
        suggestions = []
        
        # Get current schedule
        current_schedule = self.get_user_schedule(user_id)
        
        # Validate based on change type
        if change.change_type == ChangeType.ADD:
            conflicts, warnings = self._validate_add(change.new_block, current_schedule, user_context)
        
        elif change.change_type == ChangeType.MOVE:
            conflicts, warnings = self._validate_move(change, current_schedule, user_context)
        
        elif change.change_type == ChangeType.RESIZE:
            conflicts, warnings = self._validate_resize(change, current_schedule, user_context)
        
        elif change.change_type == ChangeType.REMOVE:
            conflicts, warnings = self._validate_remove(change, current_schedule, user_context)
        
        # Determine result
        result, modified_block = self._determine_result(change, conflicts, warnings)
        
        # Generate suggestions
        if conflicts:
            suggestions = self._generate_conflict_suggestions(conflicts, change, current_schedule)
        
        # Analyze impact
        impact = self._analyze_impact(change, current_schedule, user_context)
        
        # Generate user message
        message = self._generate_user_message(result, conflicts, warnings, suggestions)
        
        decision = ValidationDecision(
            decision_id=f"dec_{uuid.uuid4().hex[:8]}",
            change_id=change.change_id,
            result=result,
            conflicts=conflicts,
            warnings=warnings,
            suggestions=suggestions,
            modified_block=modified_block,
            impact_analysis=impact,
            message_to_user=message,
            requires_confirmation=(result == ValidationResult.MODIFIED or len(warnings) > 0)
        )
        
        self._decisions[decision.decision_id] = decision
        return decision
    
    async def apply_decision(
        self,
        user_id: str,
        decision_id: str,
        user_confirmed: bool = True
    ) -> Dict[str, Any]:
        """
        Apply a validation decision to the schedule.
        
        Args:
            user_id: User identifier
            decision_id: The decision to apply
            user_confirmed: Whether user confirmed (for modified decisions)
        
        Returns:
            Result of applying the change
        """
        if decision_id not in self._decisions:
            return {"success": False, "error": "Decision not found"}
        
        decision = self._decisions[decision_id]
        
        if decision.requires_confirmation and not user_confirmed:
            return {
                "success": False,
                "error": "User confirmation required",
                "awaiting_confirmation": True
            }
        
        if decision.result == ValidationResult.REJECTED:
            return {
                "success": False,
                "error": "Change was rejected",
                "reason": decision.message_to_user
            }
        
        # Get the change
        if decision.change_id not in self._pending_changes:
            return {"success": False, "error": "Change not found"}
        
        change = self._pending_changes[decision.change_id]
        
        # Apply the change
        if user_id not in self._schedules:
            self._schedules[user_id] = []
        
        schedule = self._schedules[user_id]
        
        if change.change_type == ChangeType.ADD:
            block_to_add = decision.modified_block or change.new_block
            if block_to_add:
                schedule.append(block_to_add)
        
        elif change.change_type == ChangeType.REMOVE:
            schedule = [b for b in schedule if b.block_id != change.target_block_id]
            self._schedules[user_id] = schedule
        
        elif change.change_type in [ChangeType.MOVE, ChangeType.RESIZE]:
            new_block = decision.modified_block or change.new_block
            if new_block:
                schedule = [b for b in schedule if b.block_id != change.target_block_id]
                schedule.append(new_block)
                self._schedules[user_id] = schedule
        
        # Clean up
        del self._pending_changes[change.change_id]
        
        return {
            "success": True,
            "applied": True,
            "result": decision.result.value,
            "message": "Schedule updated successfully!"
        }
    
    def propose_change(
        self,
        user_id: str,
        change_type: ChangeType,
        target_block_id: Optional[str] = None,
        new_data: Optional[Dict[str, Any]] = None,
        reason: Optional[str] = None
    ) -> ScheduleChange:
        """Create a new change proposal."""
        change_id = f"chg_{uuid.uuid4().hex[:8]}"
        
        new_block = None
        if new_data:
            block_id = new_data.get("block_id", f"blk_{uuid.uuid4().hex[:8]}")
            new_block = ScheduleBlock(
                block_id=block_id,
                user_id=user_id,
                title=new_data.get("title", "Untitled"),
                block_type=new_data.get("type", "study"),
                start=self._parse_datetime(new_data.get("start")),
                end=self._parse_datetime(new_data.get("end")),
                location=new_data.get("location"),
                is_flexible=new_data.get("is_flexible", True),
                priority=new_data.get("priority", 5)
            )
        
        change = ScheduleChange(
            change_id=change_id,
            user_id=user_id,
            change_type=change_type,
            target_block_id=target_block_id,
            new_block=new_block,
            reason=reason
        )
        
        self._pending_changes[change_id] = change
        return change
    
    # Validation methods
    
    def _validate_add(
        self,
        new_block: Optional[ScheduleBlock],
        current_schedule: List[ScheduleBlock],
        context: Optional[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[str]]:
        """Validate adding a new block."""
        conflicts = []
        warnings = []
        
        if not new_block:
            conflicts.append({
                "type": "invalid_data",
                "message": "No block data provided"
            })
            return conflicts, warnings
        
        # Check time overlaps
        for existing in current_schedule:
            if new_block.overlaps_with(existing):
                conflicts.append({
                    "type": ConflictType.TIME_OVERLAP.value,
                    "message": f"Overlaps with '{existing.title}' ({existing.start.strftime('%H:%M')}-{existing.end.strftime('%H:%M')})",
                    "conflicting_block": existing.to_dict()
                })
        
        # Check session length
        if new_block.duration_minutes > self._max_session_length:
            warnings.append(f"Session is {new_block.duration_minutes} min - consider breaking into smaller chunks")
        
        # Check if too late at night
        if new_block.end.hour >= 23 or (new_block.end.hour < 6 and new_block.end.hour > 0):
            warnings.append("Late night studying can affect sleep quality")
        
        # Check for sufficient breaks
        for existing in current_schedule:
            if existing.end <= new_block.start:
                gap = (new_block.start - existing.end).total_seconds() / 60
                if 0 < gap < self._min_break_between_sessions:
                    warnings.append(f"Only {int(gap)} min break after '{existing.title}' - consider more rest")
        
        # Check daily study hours
        same_day_blocks = [b for b in current_schedule 
                          if b.start.date() == new_block.start.date() 
                          and b.block_type == "study"]
        total_study_minutes = sum(b.duration_minutes for b in same_day_blocks)
        total_study_minutes += new_block.duration_minutes if new_block.block_type == "study" else 0
        
        if total_study_minutes > self._max_daily_study_hours * 60:
            warnings.append(f"This brings daily study to {total_study_minutes // 60}+ hours - quite intense!")
        
        # Check deadline proximity
        if context and context.get("deadlines"):
            # Check if this helps with upcoming deadlines
            pass  # Would implement deadline-aware logic
        
        return conflicts, warnings
    
    def _validate_move(
        self,
        change: ScheduleChange,
        current_schedule: List[ScheduleBlock],
        context: Optional[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[str]]:
        """Validate moving a block to new time."""
        # Similar to add, but excluding the original block
        if not change.new_block:
            return [{"type": "invalid_data", "message": "No new time provided"}], []
        
        schedule_without_target = [b for b in current_schedule if b.block_id != change.target_block_id]
        return self._validate_add(change.new_block, schedule_without_target, context)
    
    def _validate_resize(
        self,
        change: ScheduleChange,
        current_schedule: List[ScheduleBlock],
        context: Optional[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[str]]:
        """Validate resizing a block."""
        return self._validate_move(change, current_schedule, context)
    
    def _validate_remove(
        self,
        change: ScheduleChange,
        current_schedule: List[ScheduleBlock],
        context: Optional[Dict[str, Any]]
    ) -> Tuple[List[Dict[str, Any]], List[str]]:
        """Validate removing a block."""
        conflicts = []
        warnings = []
        
        # Find the block
        target_block = next((b for b in current_schedule if b.block_id == change.target_block_id), None)
        
        if not target_block:
            conflicts.append({"type": "not_found", "message": "Block not found in schedule"})
            return conflicts, warnings
        
        # Check if linked to deadline
        if target_block.linked_deadline:
            warnings.append(f"This session is linked to a deadline - removing may affect your prep")
        
        # Check if it's the only study session today
        same_day_study = [b for b in current_schedule 
                         if b.start.date() == target_block.start.date() 
                         and b.block_type == "study"
                         and b.block_id != target_block.block_id]
        if not same_day_study and target_block.block_type == "study":
            warnings.append("This is your only study session today")
        
        return conflicts, warnings
    
    def _determine_result(
        self,
        change: ScheduleChange,
        conflicts: List[Dict[str, Any]],
        warnings: List[str]
    ) -> Tuple[ValidationResult, Optional[ScheduleBlock]]:
        """Determine validation result and any modifications."""
        
        if not conflicts:
            if warnings:
                return ValidationResult.APPROVED_WITH_WARNINGS, None
            return ValidationResult.APPROVED, None
        
        # Check if we can auto-resolve conflicts
        overlap_conflicts = [c for c in conflicts if c["type"] == ConflictType.TIME_OVERLAP.value]
        
        if overlap_conflicts and change.new_block and change.new_block.is_flexible:
            # Try to find alternative time
            modified = self._find_alternative_time(change)
            if modified:
                return ValidationResult.MODIFIED, modified
        
        # Can't resolve
        if len(conflicts) > 2 or any(c["type"] == "invalid_data" for c in conflicts):
            return ValidationResult.REJECTED, None
        
        return ValidationResult.NEEDS_DISCUSSION, None
    
    def _find_alternative_time(
        self,
        change: ScheduleChange
    ) -> Optional[ScheduleBlock]:
        """Try to find alternative time slot for the block."""
        if not change.new_block:
            return None
        
        # Simple: try shifting by 1 hour
        original = change.new_block
        shifted_start = original.start + timedelta(hours=1)
        shifted_end = original.end + timedelta(hours=1)
        
        # Check if this works (simplified)
        if shifted_end.hour < 23:  # Not too late
            return ScheduleBlock(
                block_id=original.block_id,
                user_id=original.user_id,
                title=original.title,
                block_type=original.block_type,
                start=shifted_start,
                end=shifted_end,
                location=original.location,
                is_flexible=original.is_flexible,
                priority=original.priority
            )
        
        return None
    
    def _generate_conflict_suggestions(
        self,
        conflicts: List[Dict[str, Any]],
        change: ScheduleChange,
        schedule: List[ScheduleBlock]
    ) -> List[str]:
        """Generate suggestions to resolve conflicts."""
        suggestions = []
        
        for conflict in conflicts:
            if conflict["type"] == ConflictType.TIME_OVERLAP.value:
                suggestions.append("Try a different time slot")
                suggestions.append("Consider making the session shorter")
            elif conflict["type"] == ConflictType.TOO_LONG_SESSION.value:
                suggestions.append("Break into two 90-minute sessions with a break")
        
        return list(set(suggestions))  # Remove duplicates
    
    def _analyze_impact(
        self,
        change: ScheduleChange,
        schedule: List[ScheduleBlock],
        context: Optional[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """Analyze impact of the change."""
        impact = {
            "affected_deadlines": [],
            "weekly_balance_change": "neutral",
            "energy_impact": "neutral"
        }
        
        if change.change_type == ChangeType.REMOVE:
            impact["weekly_balance_change"] = "less_study"
        elif change.change_type == ChangeType.ADD:
            impact["weekly_balance_change"] = "more_study"
        
        return impact
    
    def _generate_user_message(
        self,
        result: ValidationResult,
        conflicts: List[Dict[str, Any]],
        warnings: List[str],
        suggestions: List[str]
    ) -> str:
        """Generate message to show the user."""
        if result == ValidationResult.APPROVED:
            return "✅ Schedule updated! Looking good."
        
        elif result == ValidationResult.APPROVED_WITH_WARNINGS:
            return f"✅ Schedule updated, but heads up: {warnings[0] if warnings else 'minor concerns'}"
        
        elif result == ValidationResult.MODIFIED:
            return "📝 I adjusted the time slightly to avoid conflicts. See if this works?"
        
        elif result == ValidationResult.REJECTED:
            conflict_msg = conflicts[0]["message"] if conflicts else "multiple conflicts"
            return f"❌ Can't do this: {conflict_msg}. {suggestions[0] if suggestions else ''}"
        
        else:  # NEEDS_DISCUSSION
            return "🤔 This change has some complications. Let's figure out the best approach together."
    
    def _parse_datetime(self, dt_val: Any) -> datetime:
        """Parse datetime from various formats."""
        if isinstance(dt_val, datetime):
            return dt_val
        if isinstance(dt_val, str):
            try:
                return datetime.fromisoformat(dt_val)
            except ValueError:
                pass
        return datetime.now()
