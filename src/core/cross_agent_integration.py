"""
🔄 CROSS-AGENT INTEGRATION
===========================
The Context Engine that connects all Kaironex agents.

This module provides the glue that allows agents to share state and make
coordinated decisions:

1. FINANCIAL DEFCON SYSTEM (Vitality ↔ Campaign)
   - Campaign reports employment status and financial pressure
   - Vitality adjusts Defcon level and spending strategy

2. TIME-ENERGY MATRIX (Vitality ↔ Study)
   - Study reports schedule pressure and exam mode
   - Vitality adjusts food strategy (convenience vs cooking)

3. LOCATION TRIGGERS (Vitality ↔ Radius)
   - Radius reports location changes (near grocery store)
   - Vitality triggers shopping alerts when needed

4. VICTORY FEAST (Campaign → Vitality)
   - Campaign reports career wins
   - Vitality unlocks reward budget

This is the "brain" that makes all agents work together as one system.
"""

from typing import Dict, Any, Optional, List
from datetime import datetime
from dataclasses import dataclass, field

from .survival_protocol import (
    SurvivalState, DefconLevel,
    extract_campaign_financial_context,
    extract_study_schedule_context,
    extract_radius_location_context
)


@dataclass
class CrossAgentContext:
    """
    Unified context for cross-agent decision making.
    
    This is the "shared brain state" that all agents can read and write to.
    """
    user_id: str
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    
    # Vitality State
    defcon_level: int = 3
    daily_budget: float = 20.0
    energy_level: int = 50
    food_days_remaining: int = 3
    needs_shopping: bool = False
    reward_unlocked: bool = False
    
    # Campaign State
    employment_status: str = "student"
    job_hunting: bool = False
    urgent_job_hunt: bool = False
    recent_wins: List[str] = field(default_factory=list)
    financial_pressure: str = "normal"
    
    # Study State
    pressure_index: int = 50
    exam_mode: bool = False
    free_time_mins: int = 60
    study_session_active: bool = False
    schedule_pressure: str = "normal"
    
    # Radius State
    current_location: str = "unknown"
    near_grocery_store: bool = False
    location_mode: str = "STANDARD"
    
    # Sync Metadata
    last_sync: Optional[str] = None
    sync_sources: List[str] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "user_id": self.user_id,
            "timestamp": self.timestamp,
            "vitality": {
                "defcon_level": self.defcon_level,
                "daily_budget": self.daily_budget,
                "energy_level": self.energy_level,
                "food_days_remaining": self.food_days_remaining,
                "needs_shopping": self.needs_shopping,
                "reward_unlocked": self.reward_unlocked
            },
            "campaign": {
                "employment_status": self.employment_status,
                "job_hunting": self.job_hunting,
                "urgent_job_hunt": self.urgent_job_hunt,
                "recent_wins": self.recent_wins,
                "financial_pressure": self.financial_pressure
            },
            "study": {
                "pressure_index": self.pressure_index,
                "exam_mode": self.exam_mode,
                "free_time_mins": self.free_time_mins,
                "study_session_active": self.study_session_active,
                "schedule_pressure": self.schedule_pressure
            },
            "radius": {
                "current_location": self.current_location,
                "near_grocery_store": self.near_grocery_store,
                "location_mode": self.location_mode
            },
            "sync": {
                "last_sync": self.last_sync,
                "sources": self.sync_sources
            }
        }


class CrossAgentEngine:
    """
    The Context Engine that orchestrates cross-agent decisions.
    
    This engine acts as a mediator between agents, ensuring they can
    share state and make coordinated decisions without direct coupling.
    """
    
    def __init__(self):
        self._contexts: Dict[str, CrossAgentContext] = {}
    
    def get_context(self, user_id: str) -> CrossAgentContext:
        """Get or create cross-agent context for user."""
        if user_id not in self._contexts:
            self._contexts[user_id] = CrossAgentContext(user_id=user_id)
        return self._contexts[user_id]
    
    def sync_from_campaign(
        self,
        user_id: str,
        campaign_state: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Sync Vitality with Campaign Agent state.
        
        This enables the Financial Defcon System:
        - If job hunting urgently, enforce Defcon 1
        - Track employment status for budget decisions
        - Detect wins for Victory Feast
        """
        ctx = self.get_context(user_id)
        campaign_ctx = extract_campaign_financial_context(campaign_state)
        
        # Update context
        ctx.employment_status = campaign_ctx.get('employment_status', 'student')
        ctx.job_hunting = campaign_ctx.get('job_hunting', False)
        ctx.urgent_job_hunt = campaign_ctx.get('urgent_job_hunt', False)
        ctx.recent_wins = campaign_ctx.get('recent_wins', [])
        ctx.financial_pressure = campaign_ctx.get('runway_pressure', 'normal')
        
        # Calculate Defcon adjustment
        defcon_adjustment = None
        if ctx.urgent_job_hunt:
            defcon_adjustment = {
                "new_defcon": DefconLevel.DEFCON_1_SURVIVAL,
                "reason": "Urgent job hunt detected - Austerity Mode enforced"
            }
            ctx.defcon_level = DefconLevel.DEFCON_1_SURVIVAL
        
        # Check for wins (Victory Feast trigger)
        victory_trigger = None
        if ctx.recent_wins:
            latest_win = ctx.recent_wins[-1] if ctx.recent_wins else None
            if latest_win and not ctx.reward_unlocked:
                victory_trigger = {
                    "event_type": "CAREER_WIN",
                    "event_details": latest_win,
                    "should_unlock_feast": True
                }
        
        ctx.sync_sources.append("campaign")
        ctx.last_sync = datetime.now().isoformat()
        
        return {
            "synced": True,
            "source": "campaign",
            "defcon_adjustment": defcon_adjustment,
            "victory_trigger": victory_trigger,
            "context_snapshot": ctx.to_dict()
        }
    
    def sync_from_study(
        self,
        user_id: str,
        study_state: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Sync Vitality with Study Agent state.
        
        This enables the Time-Energy Matrix:
        - Exam week = convenience food mode
        - Free time = cooking recommended
        - High pressure = minimal decision fatigue
        """
        ctx = self.get_context(user_id)
        study_ctx = extract_study_schedule_context(study_state)
        
        # Update context
        ctx.pressure_index = study_ctx.get('pressure_index', 50)
        ctx.exam_mode = study_ctx.get('exam_mode', False)
        ctx.free_time_mins = study_ctx.get('free_time_mins', 60)
        ctx.schedule_pressure = study_ctx.get('schedule_pressure', 'normal')
        ctx.study_session_active = study_ctx.get('study_session_active', False)
        
        # Calculate food strategy adjustment
        food_strategy = None
        if ctx.exam_mode or ctx.schedule_pressure == 'exam_week':
            food_strategy = {
                "mode": "convenience",
                "message": "Exam/Interview mode detected. Brain fuel priority.",
                "cooking_recommended": False,
                "suggested_approach": "Ready-to-eat healthy options"
            }
        elif ctx.free_time_mins > 60 and ctx.pressure_index < 50:
            food_strategy = {
                "mode": "full_ingredients",
                "message": "Free time detected. Cooking saves money!",
                "cooking_recommended": True,
                "suggested_approach": "Buy ingredients, cook from scratch"
            }
        else:
            food_strategy = {
                "mode": "balanced",
                "message": "Balanced schedule. Mix of convenience and cooking.",
                "cooking_recommended": True,
                "suggested_approach": "Quick-cook items with some ready meals"
            }
        
        ctx.sync_sources.append("study")
        ctx.last_sync = datetime.now().isoformat()
        
        return {
            "synced": True,
            "source": "study",
            "food_strategy": food_strategy,
            "context_snapshot": ctx.to_dict()
        }
    
    def sync_from_radius(
        self,
        user_id: str,
        radius_state: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Sync Vitality with Radius Agent state.
        
        This enables location-based triggers:
        - Near grocery = shopping alert if needed
        - At home = meal preparation suggestions
        - In transit = quick fuel options
        """
        ctx = self.get_context(user_id)
        radius_ctx = extract_radius_location_context(radius_state)
        
        # Update context
        ctx.current_location = radius_ctx.get('current_location', 'unknown')
        ctx.near_grocery_store = radius_ctx.get('near_grocery_store', False)
        ctx.location_mode = radius_ctx.get('mode', 'STANDARD')
        
        # Check for shopping alert trigger
        shopping_alert = None
        if ctx.near_grocery_store and ctx.needs_shopping:
            shopping_alert = {
                "should_trigger": True,
                "location": ctx.current_location,
                "urgency": "high" if ctx.food_days_remaining <= 1 else "normal",
                "message": f"You're near a store and food supply is {'critical' if ctx.food_days_remaining <= 1 else 'low'}!"
            }
        
        # Location-based meal suggestion
        meal_suggestion = None
        location_lower = ctx.current_location.lower()
        if 'home' in location_lower or 'dorm' in location_lower:
            meal_suggestion = {
                "environment": "home",
                "suggestion": "Perfect time to cook or prep meals for the week"
            }
        elif 'work' in location_lower or 'office' in location_lower:
            meal_suggestion = {
                "environment": "work",
                "suggestion": "Quick lunch options - check if you brought food"
            }
        elif ctx.location_mode == 'MOBILE':
            meal_suggestion = {
                "environment": "transit",
                "suggestion": "Quick fuel options - protein bar, sandwich on the go"
            }
        
        ctx.sync_sources.append("radius")
        ctx.last_sync = datetime.now().isoformat()
        
        return {
            "synced": True,
            "source": "radius",
            "shopping_alert": shopping_alert,
            "meal_suggestion": meal_suggestion,
            "context_snapshot": ctx.to_dict()
        }
    
    def update_vitality_state(
        self,
        user_id: str,
        vitality_state: Dict[str, Any]
    ) -> None:
        """
        Update context with Vitality Agent state.
        
        Called when Vitality Agent processes an event.
        """
        ctx = self.get_context(user_id)
        
        survival = vitality_state.get('survival', {})
        financial = survival.get('financial', {})
        inventory = survival.get('inventory', {})
        
        ctx.defcon_level = financial.get('defcon_level', 3)
        ctx.daily_budget = financial.get('daily_runway', 20)
        ctx.reward_unlocked = survival.get('reward_unlocked', False)
        ctx.food_days_remaining = inventory.get('days_remaining', 3)
        ctx.needs_shopping = inventory.get('needs_shopping', False)
        ctx.energy_level = vitality_state.get('energy_bar', 50)
        
        ctx.sync_sources.append("vitality")
        ctx.last_sync = datetime.now().isoformat()
    
    def get_unified_decision_context(
        self,
        user_id: str,
        include_all: bool = False
    ) -> Dict[str, Any]:
        """
        Get unified context for decision making.
        
        This provides all the information needed for the Meal Time Arbitrator
        and other cross-agent decisions.
        """
        ctx = self.get_context(user_id)
        
        # Build decision context
        decision_context = {
            "user_id": user_id,
            "timestamp": datetime.now().isoformat(),
            
            # Key decision factors
            "time_available_mins": ctx.free_time_mins,
            "energy_level": ctx.energy_level,
            "daily_budget_remaining": ctx.daily_budget,
            "defcon_level": ctx.defcon_level,
            
            # Modifiers
            "schedule_pressure": ctx.schedule_pressure,
            "exam_mode": ctx.exam_mode,
            "near_grocery": ctx.near_grocery_store,
            "urgent_job_hunt": ctx.urgent_job_hunt,
            "food_days_remaining": ctx.food_days_remaining,
            
            # Recommendations
            "cooking_recommended": (
                ctx.free_time_mins > 30 and 
                ctx.energy_level > 40 and 
                not ctx.exam_mode
            ),
            "shopping_needed": ctx.needs_shopping or ctx.food_days_remaining <= 1,
            "convenience_mode": ctx.exam_mode or ctx.schedule_pressure in ['high', 'exam_week'],
            "austerity_mode": ctx.defcon_level <= 2 or ctx.urgent_job_hunt
        }
        
        if include_all:
            decision_context["full_context"] = ctx.to_dict()
        
        return decision_context
    
    def trigger_victory_feast(
        self,
        user_id: str,
        event_type: str,
        event_details: str
    ) -> Dict[str, Any]:
        """
        Trigger Victory Feast when Campaign reports a win.
        
        This is called by Campaign Agent when:
        - Job offer received
        - Interview aced
        - Major milestone achieved
        """
        ctx = self.get_context(user_id)
        
        # Mark reward as pending
        ctx.reward_unlocked = True
        ctx.recent_wins.append(event_details)
        
        return {
            "feast_triggered": True,
            "event_type": event_type,
            "event_details": event_details,
            "message": "Victory Feast unlocked! Vitality Agent will process the reward.",
            "action_required": "vitality.unlock_reward"
        }


# Global instance for cross-agent coordination
_cross_agent_engine: Optional[CrossAgentEngine] = None


def get_cross_agent_engine() -> CrossAgentEngine:
    """Get the global cross-agent engine instance."""
    global _cross_agent_engine
    if _cross_agent_engine is None:
        _cross_agent_engine = CrossAgentEngine()
    return _cross_agent_engine


# =============================================================================
# CONVENIENCE FUNCTIONS FOR AGENT USE
# =============================================================================

def sync_vitality_from_campaign(user_id: str, campaign_state: Dict[str, Any]) -> Dict[str, Any]:
    """Convenience function for Campaign → Vitality sync."""
    engine = get_cross_agent_engine()
    return engine.sync_from_campaign(user_id, campaign_state)


def sync_vitality_from_study(user_id: str, study_state: Dict[str, Any]) -> Dict[str, Any]:
    """Convenience function for Study → Vitality sync."""
    engine = get_cross_agent_engine()
    return engine.sync_from_study(user_id, study_state)


def sync_vitality_from_radius(user_id: str, radius_state: Dict[str, Any]) -> Dict[str, Any]:
    """Convenience function for Radius → Vitality sync."""
    engine = get_cross_agent_engine()
    return engine.sync_from_radius(user_id, radius_state)


def get_meal_decision_context(user_id: str) -> Dict[str, Any]:
    """Get context for meal decision making."""
    engine = get_cross_agent_engine()
    return engine.get_unified_decision_context(user_id)


def trigger_victory_feast(user_id: str, event_type: str, event_details: str) -> Dict[str, Any]:
    """Trigger Victory Feast from Campaign Agent."""
    engine = get_cross_agent_engine()
    return engine.trigger_victory_feast(user_id, event_type, event_details)
