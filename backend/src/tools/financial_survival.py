"""
💰 FINANCIAL SURVIVAL ENGINE
=============================
Tracks student finances and ensures survival.

The brain knows:
- Current balance and spending patterns
- Days until next payroll/income
- Can you afford to eat out or cook?
- Do you need a part-time job?
- Budget for entire month

"We make sure you survive the whole month."
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta, date
from enum import Enum
import math


class FinancialHealth(str, Enum):
    """Financial health status."""
    CRITICAL = "critical"       # Immediate action needed
    CONCERNING = "concerning"   # Need to be careful
    STABLE = "stable"          # OK for now
    HEALTHY = "healthy"        # Good shape
    THRIVING = "thriving"      # Excess funds


class ExpenseCategory(str, Enum):
    """Expense categories for tracking."""
    FOOD = "food"
    RENT = "rent"
    UTILITIES = "utilities"
    TRANSPORT = "transport"
    EDUCATION = "education"
    ENTERTAINMENT = "entertainment"
    HEALTH = "health"
    CLOTHING = "clothing"
    PERSONAL = "personal"
    SAVINGS = "savings"
    OTHER = "other"


@dataclass
class FinancialSnapshot:
    """Current financial state."""
    user_id: str
    current_balance: float
    next_income_date: Optional[date]
    next_income_amount: float
    monthly_rent: float
    monthly_fixed_expenses: float
    daily_budget: float
    days_until_income: int
    can_survive_until_income: bool
    health_status: FinancialHealth
    timestamp: datetime = field(default_factory=datetime.now)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "health_status": self.health_status.value,
            "next_income_date": self.next_income_date.isoformat() if self.next_income_date else None,
            "timestamp": self.timestamp.isoformat()
        }


@dataclass
class MealDecision:
    """Decision about eating options."""
    can_eat_out: bool
    recommended_option: str  # cook, cheap_eats, restaurant, skip_not_recommended
    budget_available: float
    reason: str
    suggestions: List[str]
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class JobNeedAssessment:
    """Assessment of whether user needs a job."""
    needs_job: bool
    urgency: str  # immediate, soon, optional, not_needed
    reason: str
    recommended_hours_per_week: int
    minimum_hourly_rate: float
    job_type_suggestions: List[str]
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class FinancialSurvivalEngine:
    """
    Ensures students survive financially.
    
    Features:
    1. Real-time balance tracking
    2. Budget calculation until next income
    3. Meal decision support (cook vs eat out)
    4. Job need detection
    5. Spending alerts
    6. International student work hour limits
    """
    
    def __init__(self, db=None):
        self.db = db
        self._user_finances: Dict[str, Dict[str, Any]] = {}
        
        # Work hour limits by visa type (US example)
        self._work_hour_limits = {
            "F1": 20,        # US student visa - 20 hrs/week during school
            "J1": 20,        # US exchange visitor - 20 hrs/week
            "student_uk": 20,  # UK student visa
            "student_ca": 20,  # Canada study permit
            "domestic": 40,    # No restrictions
        }
    
    def analyze_financial_health(
        self,
        user_id: str,
        current_balance: float,
        monthly_income: float,
        next_income_date: date,
        monthly_rent: float,
        monthly_utilities: float = 0,
        other_fixed: float = 0
    ) -> FinancialSnapshot:
        """
        Analyze complete financial health.
        
        Calculates:
        - Daily budget until next income
        - Whether user can survive until payday
        - Financial health status
        """
        today = date.today()
        
        # Calculate days until income
        days_until_income = (next_income_date - today).days
        if days_until_income < 0:
            # Next month's income
            days_until_income = 30 + days_until_income
        
        # Calculate fixed expenses due before income
        monthly_fixed = monthly_rent + monthly_utilities + other_fixed
        
        # Prorate fixed expenses
        days_in_month = 30
        daily_fixed = monthly_fixed / days_in_month
        fixed_due = daily_fixed * days_until_income
        
        # Available for variable expenses
        available_for_variable = current_balance - fixed_due
        
        # Daily budget for food, transport, etc.
        daily_budget = available_for_variable / max(1, days_until_income)
        
        # Determine health status
        can_survive = daily_budget >= 10  # Minimum $10/day for food
        
        if daily_budget < 5:
            health = FinancialHealth.CRITICAL
        elif daily_budget < 15:
            health = FinancialHealth.CONCERNING
        elif daily_budget < 30:
            health = FinancialHealth.STABLE
        elif daily_budget < 50:
            health = FinancialHealth.HEALTHY
        else:
            health = FinancialHealth.THRIVING
        
        snapshot = FinancialSnapshot(
            user_id=user_id,
            current_balance=current_balance,
            next_income_date=next_income_date,
            next_income_amount=monthly_income,
            monthly_rent=monthly_rent,
            monthly_fixed_expenses=monthly_fixed,
            daily_budget=round(daily_budget, 2),
            days_until_income=days_until_income,
            can_survive_until_income=can_survive,
            health_status=health
        )
        
        # Store for quick access
        self._user_finances[user_id] = snapshot.to_dict()
        
        return snapshot
    
    def can_afford_meal_out(
        self,
        user_id: str,
        current_balance: float,
        daily_budget: float,
        today_spent: float,
        meal_cost_estimate: float = 15.0,
        has_food_at_home: bool = True,
        time_available_minutes: int = 60
    ) -> MealDecision:
        """
        Decide if user can/should eat out.
        
        Considers:
        - Budget remaining today
        - Food at home
        - Time available
        - Overall financial health
        """
        budget_remaining = daily_budget - today_spent
        
        suggestions = []
        
        # Check if meal fits budget
        if budget_remaining >= meal_cost_estimate * 1.5:
            # Comfortable margin
            can_eat_out = True
            recommended = "restaurant" if time_available_minutes >= 45 else "quick_service"
            reason = "You have comfortable budget room for eating out today."
            suggestions = [
                "Consider a place that offers student discounts",
                "Check if you have any reward points or coupons"
            ]
        
        elif budget_remaining >= meal_cost_estimate:
            # Tight but possible
            can_eat_out = True
            recommended = "cheap_eats"
            reason = "You can eat out but should choose budget-friendly options."
            suggestions = [
                "Look for daily specials or lunch deals",
                "Food truck or quick service is more budget-friendly",
                "Consider splitting a larger portion for dinner"
            ]
        
        elif has_food_at_home:
            # Should cook
            can_eat_out = False
            recommended = "cook"
            reason = f"With ${budget_remaining:.2f} left today, cooking at home is the smarter choice."
            suggestions = [
                "Quick meal ideas: pasta, rice bowls, sandwiches",
                "Meal prep on weekends saves time and money",
                "Check what's about to expire and use that first"
            ]
        
        else:
            # Need to find cheap option
            can_eat_out = True
            recommended = "cheap_eats"
            reason = "No food at home - find the most affordable option."
            suggestions = [
                "Check for student meal deals",
                "University cafeteria often has cheap options",
                "Buy groceries for multiple meals instead of one expensive meal"
            ]
        
        return MealDecision(
            can_eat_out=can_eat_out,
            recommended_option=recommended,
            budget_available=round(budget_remaining, 2),
            reason=reason,
            suggestions=suggestions
        )
    
    def assess_job_need(
        self,
        user_id: str,
        current_balance: float,
        monthly_income: float,
        monthly_expenses: float,
        savings_goal: float = 0,
        visa_type: str = "domestic",
        current_work_hours: int = 0
    ) -> JobNeedAssessment:
        """
        Assess whether user needs a job or more work hours.
        
        Considers:
        - Income vs expenses gap
        - Savings goals
        - Visa work hour limits
        - Current work situation
        """
        # Calculate monthly surplus/deficit
        monthly_gap = monthly_income - monthly_expenses - savings_goal
        
        # Get work hour limit
        max_hours = self._work_hour_limits.get(visa_type, 40)
        available_hours = max_hours - current_work_hours
        
        if monthly_gap >= 200:
            # Comfortable
            return JobNeedAssessment(
                needs_job=False,
                urgency="not_needed",
                reason="Your income covers expenses with a healthy buffer.",
                recommended_hours_per_week=0,
                minimum_hourly_rate=0,
                job_type_suggestions=[]
            )
        
        elif monthly_gap >= 0:
            # Breaking even
            if savings_goal > 0:
                needed_monthly = savings_goal
                needed_hours = math.ceil(needed_monthly / (15 * 4))  # Assuming $15/hr
                
                return JobNeedAssessment(
                    needs_job=True,
                    urgency="optional",
                    reason=f"You're breaking even but not meeting savings goal of ${savings_goal}/month.",
                    recommended_hours_per_week=min(needed_hours, available_hours),
                    minimum_hourly_rate=12,
                    job_type_suggestions=self._suggest_student_jobs(available_hours, visa_type)
                )
            else:
                return JobNeedAssessment(
                    needs_job=False,
                    urgency="optional",
                    reason="You're breaking even. Consider work for extra buffer.",
                    recommended_hours_per_week=5,
                    minimum_hourly_rate=12,
                    job_type_suggestions=["Campus library", "Research assistant"]
                )
        
        else:
            # Deficit
            monthly_deficit = abs(monthly_gap)
            needed_hours = math.ceil(monthly_deficit / (13 * 4))  # Assuming $13/hr
            
            if needed_hours > available_hours:
                urgency = "immediate"
                reason = f"ALERT: ${monthly_deficit}/month deficit. Need to reduce expenses too - work hours alone won't cover it."
            elif monthly_deficit > 300:
                urgency = "immediate"
                reason = f"You have a ${monthly_deficit}/month shortfall. Job search should start now."
            else:
                urgency = "soon"
                reason = f"You're running at ${monthly_deficit}/month deficit. Finding work would help stabilize."
            
            return JobNeedAssessment(
                needs_job=True,
                urgency=urgency,
                reason=reason,
                recommended_hours_per_week=min(needed_hours, available_hours),
                minimum_hourly_rate=round(monthly_deficit / (needed_hours * 4), 2),
                job_type_suggestions=self._suggest_student_jobs(available_hours, visa_type)
            )
    
    def check_work_hour_compliance(
        self,
        visa_type: str,
        current_week_hours: float,
        planned_hours: float
    ) -> Dict[str, Any]:
        """
        Check if work hours comply with visa restrictions.
        
        CRITICAL for international students to avoid visa violations.
        """
        max_hours = self._work_hour_limits.get(visa_type, 40)
        total_planned = current_week_hours + planned_hours
        
        if total_planned > max_hours:
            return {
                "compliant": False,
                "max_allowed": max_hours,
                "current": current_week_hours,
                "planned": planned_hours,
                "over_limit_by": total_planned - max_hours,
                "warning": f"⚠️ STOP: This would put you at {total_planned} hours. "
                          f"Your {visa_type} visa only allows {max_hours} hours/week. "
                          f"Working over limit can result in visa revocation!",
                "recommendation": f"You can only work {max_hours - current_week_hours} more hours this week."
            }
        
        return {
            "compliant": True,
            "max_allowed": max_hours,
            "current": current_week_hours,
            "planned": planned_hours,
            "hours_remaining": max_hours - total_planned,
            "status": f"✅ OK: {total_planned}/{max_hours} hours used this week."
        }
    
    def generate_monthly_budget(
        self,
        monthly_income: float,
        rent: float,
        utilities: float = 100,
        phone_internet: float = 50,
        groceries_target: float = 300,
        transport: float = 50
    ) -> Dict[str, Any]:
        """Generate recommended monthly budget breakdown."""
        
        fixed_total = rent + utilities + phone_internet + transport
        remaining = monthly_income - fixed_total
        
        # Allocate remaining
        groceries = min(groceries_target, remaining * 0.4)
        savings = remaining * 0.2
        personal = remaining * 0.2
        emergency_buffer = remaining * 0.2
        
        budget = {
            "income": monthly_income,
            "fixed_expenses": {
                "rent": rent,
                "utilities": utilities,
                "phone_internet": phone_internet,
                "transport": transport,
                "total_fixed": fixed_total
            },
            "variable_expenses": {
                "groceries": round(groceries, 2),
                "personal": round(personal, 2)
            },
            "savings": round(savings, 2),
            "emergency_buffer": round(emergency_buffer, 2),
            "daily_budget": round((groceries + personal) / 30, 2),
            "tips": self._generate_budget_tips(monthly_income, fixed_total)
        }
        
        return budget
    
    def _suggest_student_jobs(
        self,
        available_hours: int,
        visa_type: str
    ) -> List[str]:
        """Suggest suitable jobs based on hours and visa."""
        
        if available_hours <= 10:
            return [
                "Campus library assistant",
                "Tutoring peers",
                "Research assistant",
                "Freelance skills (design, writing)"
            ]
        elif available_hours <= 20:
            return [
                "On-campus dining services",
                "Teaching assistant",
                "Student IT helpdesk",
                "Recreation center staff",
                "Bookstore employee"
            ]
        else:
            return [
                "Retail part-time",
                "Restaurant/cafe",
                "Delivery services (if compliant)",
                "Freelance/contract work",
                "Internship in your field"
            ]
    
    def _generate_budget_tips(
        self,
        income: float,
        fixed: float
    ) -> List[str]:
        """Generate personalized budget tips."""
        tips = []
        
        fixed_ratio = fixed / income
        
        if fixed_ratio > 0.5:
            tips.append("Your fixed expenses are over 50% of income. Consider roommates or cheaper housing.")
        
        tips.extend([
            "Cook in bulk on weekends to save time and money",
            "Use student discounts everywhere - always ask!",
            "Check if your university has emergency funds for students",
            "Track every expense for 2 weeks to find leaks"
        ])
        
        return tips
