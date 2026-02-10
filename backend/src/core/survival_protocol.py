"""
🧬 SURVIVAL & GROWTH PROTOCOL v2.0
===================================
The Life Logistics Engine that balances survival (food/money) with growth (career/study).

This module transforms the Vitality Agent from a health tracker into a proactive 
"Life Logistics Engine" using Gemini 3's multimodal capabilities.

Architecture:
- Financial Defcon System: Cross-agent financial state awareness
- Time-Energy Matrix: Schedule-aware decisions
- Bio-Fuel Supply Chain: Smart fridge/shopping management
- Decision Engine: Cook vs Order arbitration
- Dopamine Reward Loop: Victory Feast Protocol

Gemini 3 Features Used:
- Vision: Fridge/pantry analysis, receipt scanning, bill extraction
- Thinking: Complex financial/nutrition decision making
- Native Function Calling: Integration with location and shopping tools
"""

import json
import base64
from enum import IntEnum
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from google import genai
from google.genai import types

from ..config import GEMINI_API_KEY, GEMINI_3_FLASH, THINKING_LEVEL_DEEP


class DefconLevel(IntEnum):
    """Financial Defcon Levels - Higher = More Secure"""
    DEFCON_1_SURVIVAL = 1      # Unemployed OR Runway < 1 month - AUSTERITY MODE
    DEFCON_2_CRITICAL = 2      # Runway 1-2 months - Strict budgeting
    DEFCON_3_CAUTION = 3       # Runway 2-3 months - Value meals, alert on spending
    DEFCON_4_STABLE = 4        # Runway 3-6 months - Balanced spending
    DEFCON_5_ABUNDANCE = 5     # High runway, stable job - Premium choices allowed


@dataclass
class FinancialState:
    """User's financial status for survival calculations."""
    total_balance: float = 0.0
    fixed_bills: float = 0.0  # Rent, utilities, subscriptions
    daily_runway: float = 0.0
    defcon_level: int = DefconLevel.DEFCON_3_CAUTION
    payday_date: Optional[str] = None
    days_until_payday: int = 30
    employment_status: str = "employed"  # employed, unemployed, student
    budget_override: float = 0.0  # Temporary bonus (e.g., Victory Feast)
    spent_today: float = 0.0
    spent_yesterday: float = 0.0
    # === SAVINGS & EMERGENCY FUND ===
    emergency_fund: float = 0.0  # Emergency savings (sick, unexpected expenses)
    savings_goal: float = 500.0  # Target savings amount
    auto_save_percentage: float = 10.0  # Auto-save % of daily surplus
    savings_balance: float = 0.0  # Current savings balance
    last_emergency_use: Optional[str] = None  # Last time emergency fund used
    savings_streak_days: int = 0  # Consecutive days of saving
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
    
    def can_use_emergency_fund(self, amount: float) -> bool:
        """Check if emergency fund has enough for withdrawal."""
        return self.emergency_fund >= amount
    
    def withdraw_emergency(self, amount: float, reason: str) -> Tuple[bool, str]:
        """Withdraw from emergency fund for unexpected expenses."""
        if amount <= 0:
            return False, "Invalid amount"
        if amount > self.emergency_fund:
            return False, f"Insufficient emergency fund. Available: ${self.emergency_fund:.2f}"
        
        self.emergency_fund -= amount
        self.last_emergency_use = datetime.now().isoformat()
        return True, f"Emergency fund withdrawal: ${amount:.2f} for {reason}. Remaining: ${self.emergency_fund:.2f}"
    
    def auto_save(self, surplus: float) -> float:
        """Auto-save percentage of daily surplus."""
        if surplus <= 0:
            return 0.0
        
        save_amount = surplus * (self.auto_save_percentage / 100)
        self.savings_balance += save_amount
        self.emergency_fund += save_amount * 0.5  # Half to emergency, half to savings
        self.savings_streak_days += 1
        return save_amount


@dataclass
class FridgeInventory:
    """Smart fridge/pantry inventory state."""
    ingredients: List[Dict[str, Any]] = field(default_factory=list)
    days_remaining: int = 0
    last_scan: Optional[str] = None
    needs_shopping: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class UserPreferences:
    """User food/reward preferences for personalization."""
    favorite_reward: str = "Pizza"  # Ultimate favorite food  
    dietary_restrictions: List[str] = field(default_factory=list)
    cuisine_preferences: List[str] = field(default_factory=list)
    cooking_skill_level: str = "intermediate"  # beginner, intermediate, advanced
    budget_style: str = "balanced"  # frugal, balanced, premium
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


# =============================================================================
# PROACTIVE MEAL PLANNING STRUCTURES
# =============================================================================
@dataclass
class MealOption:
    """A single meal option with budget and requirements."""
    option_id: str  # e.g., "breakfast_1"
    name: str
    description: str
    meal_type: str  # breakfast, lunch, dinner
    action_type: str  # COOK, ORDER, QUICK_PREP
    estimated_cost: float
    prep_time_mins: int
    ingredients_needed: List[str] = field(default_factory=list)
    ingredients_used_from_fridge: List[str] = field(default_factory=list)
    nutrition_score: int = 70  # 0-100
    energy_requirement: str = "medium"  # low, medium, high
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class DailyMealPlan:
    """Proactive daily meal plan with options for each meal."""
    plan_id: str
    date: str
    total_budget: float
    budget_remaining: float
    defcon_level: int
    breakfast_options: List[MealOption] = field(default_factory=list)
    lunch_options: List[MealOption] = field(default_factory=list)
    dinner_options: List[MealOption] = field(default_factory=list)
    selected_breakfast: Optional[str] = None  # option_id
    selected_lunch: Optional[str] = None
    selected_dinner: Optional[str] = None
    spent_today: float = 0.0
    generated_at: str = ""
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "plan_id": self.plan_id,
            "date": self.date,
            "total_budget": self.total_budget,
            "budget_remaining": self.budget_remaining,
            "defcon_level": self.defcon_level,
            "breakfast_options": [o.to_dict() for o in self.breakfast_options],
            "lunch_options": [o.to_dict() for o in self.lunch_options],
            "dinner_options": [o.to_dict() for o in self.dinner_options],
            "selected_breakfast": self.selected_breakfast,
            "selected_lunch": self.selected_lunch,
            "selected_dinner": self.selected_dinner,
            "spent_today": self.spent_today,
            "generated_at": self.generated_at
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'DailyMealPlan':
        """Reconstruct DailyMealPlan from dictionary."""
        if not data:
            return cls(plan_id="", date="", total_budget=0, budget_remaining=0, defcon_level=3)
        
        def parse_options(options_data: List[Dict]) -> List[MealOption]:
            return [MealOption(**opt) for opt in options_data] if options_data else []
        
        return cls(
            plan_id=data.get('plan_id', ''),
            date=data.get('date', ''),
            total_budget=data.get('total_budget', 0),
            budget_remaining=data.get('budget_remaining', 0),
            defcon_level=data.get('defcon_level', 3),
            breakfast_options=parse_options(data.get('breakfast_options', [])),
            lunch_options=parse_options(data.get('lunch_options', [])),
            dinner_options=parse_options(data.get('dinner_options', [])),
            selected_breakfast=data.get('selected_breakfast'),
            selected_lunch=data.get('selected_lunch'),
            selected_dinner=data.get('selected_dinner'),
            spent_today=data.get('spent_today', 0),
            generated_at=data.get('generated_at', '')
        )
    
    def get_option_by_id(self, option_id: str) -> Optional[MealOption]:
        """Find a meal option by its ID."""
        all_options = self.breakfast_options + self.lunch_options + self.dinner_options
        for opt in all_options:
            if opt.option_id == option_id:
                return opt
        return None
    
    def select_option(self, option_id: str) -> Tuple[bool, str, float]:
        """
        Select a meal option and update budget.
        
        Returns: (success, message, cost_deducted)
        """
        option = self.get_option_by_id(option_id)
        if not option:
            return False, f"Option {option_id} not found", 0.0
        
        if option.estimated_cost > self.budget_remaining:
            return False, f"Insufficient budget. Need ${option.estimated_cost:.2f}, have ${self.budget_remaining:.2f}", 0.0
        
        # Update selection
        if option.meal_type == "breakfast":
            self.selected_breakfast = option_id
        elif option.meal_type == "lunch":
            self.selected_lunch = option_id
        elif option.meal_type == "dinner":
            self.selected_dinner = option_id
        
        # Update budget
        self.budget_remaining -= option.estimated_cost
        self.spent_today += option.estimated_cost
        
        return True, f"Selected {option.name}! Budget remaining: ${self.budget_remaining:.2f}", option.estimated_cost


@dataclass
class SurvivalState:
    """Complete survival state for the Vitality Agent."""
    financial: FinancialState = field(default_factory=FinancialState)
    inventory: FridgeInventory = field(default_factory=FridgeInventory)
    preferences: UserPreferences = field(default_factory=UserPreferences)
    daily_meal_plan: Optional[DailyMealPlan] = None  # NEW: Proactive daily meal plan
    last_decision: Optional[Dict[str, Any]] = None
    last_shopping_alert: Optional[str] = None
    reward_unlocked: bool = False
    reward_amount: float = 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "financial": self.financial.to_dict(),
            "inventory": self.inventory.to_dict(),
            "preferences": self.preferences.to_dict(),
            "daily_meal_plan": self.daily_meal_plan.to_dict() if self.daily_meal_plan else None,
            "last_decision": self.last_decision,
            "last_shopping_alert": self.last_shopping_alert,
            "reward_unlocked": self.reward_unlocked,
            "reward_amount": self.reward_amount
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'SurvivalState':
        """Reconstruct SurvivalState from dictionary."""
        if not data:
            return cls()
        
        financial_data = data.get('financial', {})
        inventory_data = data.get('inventory', {})
        preferences_data = data.get('preferences', {})
        meal_plan_data = data.get('daily_meal_plan', {})
        
        return cls(
            financial=FinancialState(**financial_data) if financial_data else FinancialState(),
            inventory=FridgeInventory(**inventory_data) if inventory_data else FridgeInventory(),
            preferences=UserPreferences(**preferences_data) if preferences_data else UserPreferences(),
            daily_meal_plan=DailyMealPlan.from_dict(meal_plan_data) if meal_plan_data else None,
            last_decision=data.get('last_decision'),
            last_shopping_alert=data.get('last_shopping_alert'),
            reward_unlocked=data.get('reward_unlocked', False),
            reward_amount=data.get('reward_amount', 0.0)
        )


# =============================================================================
# DEFCON-BASED SHOPPING LISTS
# =============================================================================
SHOPPING_LISTS_BY_DEFCON = {
    DefconLevel.DEFCON_1_SURVIVAL: {
        "name": "Survival Mode List",
        "strategy": "Maximum nutrition per dollar. Bulk staples only.",
        "core_items": [
            {"item": "Rice (5lb bag)", "category": "carbs", "priority": 1},
            {"item": "Dried Beans (2lb)", "category": "protein", "priority": 1},
            {"item": "Frozen Vegetables (mixed)", "category": "vitamins", "priority": 1},
            {"item": "Eggs (18 pack)", "category": "protein", "priority": 1},
            {"item": "Oatmeal (container)", "category": "carbs", "priority": 2},
            {"item": "Peanut Butter", "category": "protein", "priority": 2},
            {"item": "Canned Tuna (4 pack)", "category": "protein", "priority": 2},
            {"item": "Ramen (bulk)", "category": "emergency", "priority": 3},
        ],
        "forbidden": ["eating out", "fancy coffee", "snacks", "alcohol"],
        "max_budget": 40
    },
    DefconLevel.DEFCON_2_CRITICAL: {
        "name": "Critical Budget List",
        "strategy": "Essential nutrition with minimal variety.",
        "core_items": [
            {"item": "Rice/Pasta", "category": "carbs", "priority": 1},
            {"item": "Chicken Thighs", "category": "protein", "priority": 1},
            {"item": "Frozen Vegetables", "category": "vitamins", "priority": 1},
            {"item": "Eggs", "category": "protein", "priority": 1},
            {"item": "Bread", "category": "carbs", "priority": 2},
            {"item": "Bananas", "category": "fruit", "priority": 2},
            {"item": "Milk", "category": "dairy", "priority": 2},
        ],
        "forbidden": ["premium brands", "organic", "eating out >1x/week"],
        "max_budget": 60
    },
    DefconLevel.DEFCON_3_CAUTION: {
        "name": "Value Conscious List",
        "strategy": "Balanced nutrition with smart choices.",
        "core_items": [
            {"item": "Protein (chicken/fish)", "category": "protein", "priority": 1},
            {"item": "Fresh Vegetables", "category": "vitamins", "priority": 1},
            {"item": "Whole Grains", "category": "carbs", "priority": 1},
            {"item": "Fruit (seasonal)", "category": "fruit", "priority": 2},
            {"item": "Dairy", "category": "dairy", "priority": 2},
            {"item": "Snacks (limited)", "category": "treats", "priority": 3},
        ],
        "forbidden": ["expensive brands without necessity"],
        "max_budget": 100
    },
    DefconLevel.DEFCON_4_STABLE: {
        "name": "Balanced Living List",
        "strategy": "Quality nutrition with occasional treats.",
        "core_items": [
            {"item": "Quality Proteins", "category": "protein", "priority": 1},
            {"item": "Fresh Produce", "category": "vitamins", "priority": 1},
            {"item": "Whole Grains", "category": "carbs", "priority": 1},
            {"item": "Healthy Snacks", "category": "snacks", "priority": 2},
            {"item": "Coffee/Tea", "category": "beverages", "priority": 2},
            {"item": "Occasional Treats", "category": "treats", "priority": 3},
        ],
        "forbidden": [],
        "max_budget": 150
    },
    DefconLevel.DEFCON_5_ABUNDANCE: {
        "name": "Premium Living List",
        "strategy": "Quality and enjoyment. You've earned it.",
        "core_items": [
            {"item": "Premium Proteins (Steak, Salmon)", "category": "protein", "priority": 1},
            {"item": "Organic Produce", "category": "vitamins", "priority": 1},
            {"item": "Fresh Berries", "category": "fruit", "priority": 2},
            {"item": "Artisan Bread", "category": "carbs", "priority": 2},
            {"item": "Quality Cheese", "category": "dairy", "priority": 2},
            {"item": "Specialty Items", "category": "specialty", "priority": 3},
        ],
        "forbidden": [],
        "max_budget": 300
    }
}


# =============================================================================
# MEAL DECISION MATRIX
# =============================================================================
@dataclass
class MealDecisionContext:
    """Context for cook vs order decision."""
    time_available_mins: int = 60
    energy_level: int = 50  # 0-100
    daily_budget_remaining: float = 20.0
    defcon_level: int = DefconLevel.DEFCON_3_CAUTION
    ingredients_available: List[str] = field(default_factory=list)
    near_restaurant: bool = False
    schedule_pressure: str = "normal"  # low, normal, high, exam_week


class MealDecisionEngine:
    """
    The Meal Time Arbitrator - Solves "What's for dinner?" paralysis.
    
    Weighs three variables:
    - Time (from Schedule)
    - Money (from Runway)
    - Energy (from Vitality)
    """
    
    @staticmethod
    def decide(ctx: MealDecisionContext) -> Dict[str, Any]:
        """
        Make a smart meal decision based on context.
        
        Returns decision with action, reason, and suggestion.
        """
        time_score = min(ctx.time_available_mins / 60, 1.0)  # 0-1
        money_score = ctx.defcon_level / 5.0  # 0.2-1.0
        energy_score = ctx.energy_level / 100.0  # 0-1
        
        # Decision matrix
        has_time = time_score >= 0.5
        has_money = money_score >= 0.6
        has_energy = energy_score >= 0.4
        has_ingredients = len(ctx.ingredients_available) >= 3
        
        # Scenario A: Low Time + High Money = Order Healthy
        if not has_time and has_money:
            return {
                "action": "ORDER",
                "sub_action": "HEALTHY_DELIVERY",
                "reason": f"Time is limited ({ctx.time_available_mins}min) but budget allows healthy delivery.",
                "suggestion": "Order a balanced meal. Consider: Grilled protein + vegetables, Poke bowl, Mediterranean wrap.",
                "budget_allocation": min(ctx.daily_budget_remaining * 0.6, 25.0),
                "priority": "nutrition > speed > cost"
            }
        
        # Scenario B: High Time + Low Money = Cook
        if has_time and not has_money:
            if has_ingredients:
                return {
                    "action": "COOK",
                    "sub_action": "USE_FRIDGE",
                    "reason": f"Time available and we should conserve funds (Defcon {ctx.defcon_level}).",
                    "suggestion": f"Cook with available: {', '.join(ctx.ingredients_available[:5])}",
                    "budget_allocation": 0,
                    "priority": "cost > nutrition > time"
                }
            else:
                return {
                    "action": "SHOP_THEN_COOK",
                    "sub_action": "STRATEGIC_SHOP",
                    "reason": "Time available but low ingredients. Quick strategic shop recommended.",
                    "suggestion": "Buy essentials for 2-3 meals. Focus on protein + vegetables.",
                    "budget_allocation": min(ctx.daily_budget_remaining * 0.8, 15.0),
                    "priority": "cost > nutrition"
                }
        
        # Scenario C: Low Time + Low Money = Emergency Fuel
        if not has_time and not has_money:
            return {
                "action": "EMERGENCY_FUEL",
                "sub_action": "QUICK_CHEAP",
                "reason": "Time and budget both constrained. Emergency fuel protocol.",
                "suggestion": "Quick fuel: Sandwich, leftovers, instant oatmeal, or eggs on toast.",
                "budget_allocation": 5.0,
                "priority": "speed > cost > nutrition"
            }
        
        # Scenario D: Low Energy (Override) - Minimal effort needed
        if not has_energy:
            if has_money:
                return {
                    "action": "ORDER",
                    "sub_action": "COMFORT_DELIVERY",
                    "reason": "Energy levels low. Rest and refuel is priority.",
                    "suggestion": "Order something comforting and nutritious. No cooking stress.",
                    "budget_allocation": min(ctx.daily_budget_remaining * 0.5, 20.0),
                    "priority": "energy_conservation > all"
                }
            else:
                return {
                    "action": "MINIMAL_PREP",
                    "sub_action": "ZERO_EFFORT",
                    "reason": "Energy low and budget tight. Minimal effort meal.",
                    "suggestion": "Cereal, sandwich, or heat up leftovers. Rest is priority.",
                    "budget_allocation": 0,
                    "priority": "energy_conservation > cost"
                }
        
        # Scenario E: Exam/Interview Week (Schedule Pressure Override)
        if ctx.schedule_pressure in ["high", "exam_week"]:
            return {
                "action": "CONVENIENCE",
                "sub_action": "READY_TO_EAT",
                "reason": f"Schedule pressure is {ctx.schedule_pressure}. Brain fuel priority.",
                "suggestion": "Ready-to-eat healthy options: Pre-made salads, rotisserie chicken, healthy frozen meals.",
                "budget_allocation": min(ctx.daily_budget_remaining * 0.5, 20.0),
                "priority": "convenience > nutrition > cost"
            }
        
        # Default: Balanced decision - Has time, money, and energy
        if has_ingredients:
            return {
                "action": "COOK",
                "sub_action": "OPTIMAL_MEAL",
                "reason": "You have time, energy, and ingredients. Perfect cooking conditions!",
                "suggestion": f"Make something great with: {', '.join(ctx.ingredients_available[:5])}",
                "budget_allocation": 0,
                "priority": "nutrition > enjoyment > cost"
            }
        else:
            return {
                "action": "FLEXIBLE",
                "sub_action": "CHOOSE_ADVENTURE",
                "reason": "All factors balanced. Choose your adventure!",
                "suggestion": "Either cook something simple or treat yourself to a nice meal out.",
                "budget_allocation": min(ctx.daily_budget_remaining * 0.4, 25.0),
                "priority": "enjoyment > nutrition > cost"
            }


class SurvivalProtocol:
    """
    The main Survival & Growth Protocol engine.
    
    Uses Gemini 3's capabilities:
    - Vision for fridge/receipt/bill analysis
    - Thinking for complex financial decisions
    - Native multimodality for seamless UX
    """
    
    def __init__(self, init_client: bool = True):
        """
        Initialize Survival Protocol.
        
        Args:
            init_client: Whether to initialize Gemini client immediately.
                        Set to False for testing without API key.
        """
        self._client = None
        self._init_client = init_client
        self.model = GEMINI_3_FLASH
        self.decision_engine = MealDecisionEngine()
    
    @property
    def client(self):
        """Lazy-load Gemini client."""
        if self._client is None and self._init_client:
            self._client = genai.Client(api_key=GEMINI_API_KEY)
        return self._client
    
    async def parse_financial_setup(
        self,
        input_data: str,
        image_data: Optional[bytes] = None
    ) -> FinancialState:
        """
        Day Zero One-Shot Setup.
        
        Parses financial info from natural language or image (bill photo).
        Uses Gemini 3 Vision + Thinking for extraction.
        
        Args:
            input_data: Natural language description of finances
            image_data: Optional image bytes (receipt, bill, bank statement)
        
        Returns:
            FinancialState with all extracted data
        """
        prompt = """You are a financial data extractor for a student life app.

TASK: Extract financial information from the user's input to calculate their daily spending budget.

REQUIRED EXTRACTIONS:
1. total_balance: Current bank/account balance
2. fixed_bills: Monthly fixed expenses (rent, utilities, subscriptions)
3. payday_date: Next expected payday (or financial aid date)
4. employment_status: employed, unemployed, or student

CALCULATION FORMULA:
daily_runway = (total_balance - fixed_bills) / days_until_payday

OUTPUT FORMAT (JSON only, no markdown):
{
    "total_balance": <float>,
    "fixed_bills": <float>,
    "rent": <float>,
    "utilities": <float>,
    "subscriptions": <float>,
    "payday_date": "<YYYY-MM-DD or null>",
    "days_until_payday": <int>,
    "employment_status": "<status>",
    "daily_runway": <float>,
    "defcon_level": <1-5>,
    "notes": "<any relevant notes>"
}

DEFCON LEVEL ASSIGNMENT:
- 5: daily_runway > $50 AND employed
- 4: daily_runway $30-50 OR stable situation
- 3: daily_runway $15-30 OR 2-3 months runway
- 2: daily_runway $8-15 OR 1-2 months runway
- 1: daily_runway < $8 OR unemployed with <1 month runway

USER INPUT:
"""
        
        parts = [types.Part.from_text(text=prompt + input_data)]
        
        if image_data:
            # Add image for vision analysis
            parts.append(types.Part.from_bytes(
                data=image_data,
                mime_type="image/jpeg"
            ))
        
        if self.client is None:
            raise RuntimeError("Gemini client not initialized. Set init_client=True.")
        
        response = await self.client.aio.models.generate_content(
            model=self.model,
            contents=parts,
            config=types.GenerateContentConfig(
                thinking_config=types.ThinkingConfig(
                    thinking_budget=65536  # Use thinking for financial analysis
                ),
                response_mime_type="application/json"
            )
        )
        
        try:
            response_text = response.text or "{}"
            result = json.loads(response_text)
            
            return FinancialState(
                total_balance=float(result.get('total_balance', 0)),
                fixed_bills=float(result.get('fixed_bills', 0)),
                daily_runway=float(result.get('daily_runway', 20)),
                defcon_level=int(result.get('defcon_level', 3)),
                payday_date=result.get('payday_date'),
                days_until_payday=int(result.get('days_until_payday', 30)),
                employment_status=result.get('employment_status', 'student')
            )
        except (json.JSONDecodeError, KeyError, TypeError) as e:
            # Fallback to safe defaults
            print(f"⚠️ Financial parsing error: {e}")
            return FinancialState(
                defcon_level=DefconLevel.DEFCON_3_CAUTION,
                daily_runway=20.0
            )
    
    async def analyze_fridge(
        self,
        image_data: bytes
    ) -> FridgeInventory:
        """
        Smart Fridge Vision.
        
        Analyzes fridge/pantry photo to identify ingredients and estimate days of food.
        
        Args:
            image_data: Image bytes of fridge/pantry contents
        
        Returns:
            FridgeInventory with identified items and survival estimate
        """
        prompt = """You are a kitchen inventory analyst for a student survival app.

TASK: Analyze this fridge/pantry image to:
1. Identify all visible food items
2. Estimate quantity for each (full, half, low, almost_empty)
3. Calculate how many days the user can survive on current inventory
4. Determine if shopping is urgently needed

OUTPUT FORMAT (JSON only):
{
    "ingredients": [
        {"name": "eggs", "quantity": "half", "estimated_servings": 6},
        {"name": "milk", "quantity": "low", "estimated_servings": 2}
    ],
    "days_remaining": <int>,
    "needs_shopping": <bool>,
    "meal_suggestions": ["<meal that can be made with ingredients>"],
    "critical_missing": ["<important items to buy>"],
    "survival_assessment": "<brief assessment>"
}

DAYS CALCULATION RULES:
- Assume 3 meals per day
- Count complete meal potential (protein + carb + vegetable = 1 complete meal)
- Partial ingredients = 0.5 meal
- If <1 day of complete meals, needs_shopping = true

Analyze the image:"""

        if self.client is None:
            raise RuntimeError("Gemini client not initialized. Set init_client=True.")
        
        response = await self.client.aio.models.generate_content(
            model=self.model,
            contents=[
                types.Part.from_text(text=prompt),
                types.Part.from_bytes(data=image_data, mime_type="image/jpeg")
            ],
            config=types.GenerateContentConfig(
                thinking_config=types.ThinkingConfig(thinking_budget=65536),
                response_mime_type="application/json"
            )
        )
        
        try:
            response_text = response.text or "{}"
            result = json.loads(response_text)
            
            return FridgeInventory(
                ingredients=result.get('ingredients', []),
                days_remaining=int(result.get('days_remaining', 0)),
                last_scan=datetime.now().isoformat(),
                needs_shopping=result.get('needs_shopping', True)
            )
        except (json.JSONDecodeError, KeyError, TypeError) as e:
            print(f"⚠️ Fridge analysis error: {e}")
            return FridgeInventory(needs_shopping=True)
    
    def calculate_daily_budget(
        self,
        financial: FinancialState,
        spent_yesterday: float = 0.0,
        target_daily: Optional[float] = None
    ) -> Dict[str, Any]:
        """
        Dynamic Safe-to-Spend Dial.
        
        Calculates today's specific budget with smart adjustments.
        
        Args:
            financial: Current financial state
            spent_yesterday: Amount spent yesterday
            target_daily: Override target daily spend
        
        Returns:
            Budget breakdown with recommendations
        """
        base_daily = target_daily or financial.daily_runway
        
        # Smart adjustment: Compensate for yesterday's spending
        overspend = max(0, spent_yesterday - base_daily)
        today_budget = max(5.0, base_daily - (overspend * 0.5))  # Min $5/day
        
        # Apply reward override if unlocked
        if financial.budget_override > 0:
            today_budget += financial.budget_override
        
        # Defcon-based recommendations
        recommendations = []
        if financial.defcon_level <= 2:
            recommendations.append("🔴 Austerity Mode: Only essential spending today.")
            recommendations.append("Cook from pantry. No eating out.")
        elif financial.defcon_level == 3:
            recommendations.append("🟡 Caution Mode: Stick to the budget.")
            if overspend > 0:
                recommendations.append(f"Compensating for ${overspend:.2f} overspend yesterday.")
        else:
            recommendations.append("🟢 Budget healthy. Enjoy responsibly!")
        
        return {
            "today_budget": round(today_budget, 2),
            "base_daily": round(base_daily, 2),
            "yesterday_spent": spent_yesterday,
            "overspend_adjustment": round(overspend * 0.5, 2),
            "reward_bonus": financial.budget_override,
            "defcon_level": financial.defcon_level,
            "recommendations": recommendations,
            "message": self._generate_budget_message(today_budget, financial.defcon_level, overspend)
        }
    
    def _generate_budget_message(
        self,
        budget: float,
        defcon: int,
        overspend: float
    ) -> str:
        """Generate a human-friendly budget message."""
        if defcon <= 2:
            if overspend > 0:
                return f"⚠️ Survival Mode. You spent extra yesterday. Today: ${budget:.2f}. Eat from the fridge."
            return f"🔴 Survival Mode active. Today's fuel budget: ${budget:.2f}. Cook at home."
        elif defcon == 3:
            if overspend > 0:
                return f"📊 Budget adjusted. Yesterday's overspend compensated. Today: ${budget:.2f}"
            return f"💰 Daily fuel budget: ${budget:.2f}. Value meals recommended."
        else:
            return f"✅ Looking good! Daily budget: ${budget:.2f}. Enjoy your day!"
    
    def generate_shopping_list(
        self,
        defcon_level: int,
        current_inventory: List[Dict[str, Any]],
        dietary_restrictions: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """
        Generate a Just-in-Time shopping list based on Defcon level.
        
        Args:
            defcon_level: Current financial Defcon level
            current_inventory: Items already in fridge
            dietary_restrictions: User's dietary needs
        
        Returns:
            Tailored shopping list with budget
        """
        # Convert int to DefconLevel enum for dict lookup
        try:
            defcon_key = DefconLevel(defcon_level)
        except ValueError:
            defcon_key = DefconLevel.DEFCON_3_CAUTION
        
        base_list = SHOPPING_LISTS_BY_DEFCON.get(
            defcon_key,
            SHOPPING_LISTS_BY_DEFCON[DefconLevel.DEFCON_3_CAUTION]
        )
        
        # Filter out items already in inventory
        inventory_names = [item.get('name', '').lower() for item in current_inventory]
        needed_items = [
            item for item in base_list['core_items']
            if not any(inv in item['item'].lower() for inv in inventory_names)
        ]
        
        # Apply dietary restrictions
        if dietary_restrictions:
            restrictions_lower = [r.lower() for r in dietary_restrictions]
            if 'vegetarian' in restrictions_lower or 'vegan' in restrictions_lower:
                needed_items = [i for i in needed_items if i['category'] != 'protein' or 'meat' not in i['item'].lower()]
        
        return {
            "list_name": base_list['name'],
            "strategy": base_list['strategy'],
            "items": needed_items,
            "forbidden_purchases": base_list['forbidden'],
            "max_budget": base_list['max_budget'],
            "defcon_level": defcon_level,
            "voice_alert": self._generate_shopping_voice_alert(needed_items, base_list['max_budget'])
        }
    
    def _generate_shopping_voice_alert(
        self,
        items: List[Dict[str, Any]],
        budget: float
    ) -> str:
        """Generate voice alert message for shopping."""
        priority_items = [i['item'] for i in items if i.get('priority', 3) == 1][:3]
        
        if not priority_items:
            return f"You're near a store. Quick check: Do you need anything? Budget: ${budget}."
        
        items_str = ", ".join(priority_items)
        return f"Hey! You're near the store. You need {items_str}. Your budget is ${budget}. Stick to the list!"
    
    def get_time_energy_adjustment(
        self,
        schedule_pressure: str,
        energy_level: int
    ) -> Dict[str, Any]:
        """
        Time-Energy Matrix adjustment.
        
        Adjusts shopping/cooking strategy based on schedule and energy.
        
        Args:
            schedule_pressure: low, normal, high, exam_week
            energy_level: 0-100 energy bar
        
        Returns:
            Strategy adjustment recommendations
        """
        if schedule_pressure in ['high', 'exam_week']:
            return {
                "shopping_mode": "convenience",
                "recommendation": "Ready-to-eat and healthy convenience foods",
                "cooking_recommended": False,
                "suggested_items": [
                    "Pre-made salads",
                    "Rotisserie chicken",
                    "Healthy frozen meals",
                    "Protein bars",
                    "Fresh fruit"
                ],
                "message": "📚 Exam/Interview mode detected. Brain fuel priority. No cooking stress."
            }
        
        if energy_level < 30:
            return {
                "shopping_mode": "minimal",
                "recommendation": "Minimal effort meals only",
                "cooking_recommended": False,
                "suggested_items": [
                    "Ready meals",
                    "Sandwiches",
                    "Easy prep items"
                ],
                "message": "🔋 Energy low. Rest priority. Easy meals only."
            }
        
        if schedule_pressure == 'low' and energy_level > 60:
            return {
                "shopping_mode": "full_ingredients",
                "recommendation": "Raw ingredients for cooking (cheaper, healthier)",
                "cooking_recommended": True,
                "suggested_items": [
                    "Fresh vegetables",
                    "Raw proteins",
                    "Whole grains",
                    "Cooking essentials"
                ],
                "message": "🍳 Free time + good energy = Perfect for cooking! Buy ingredients!"
            }
        
        # Default balanced
        return {
            "shopping_mode": "balanced",
            "recommendation": "Mix of ready items and quick-cook ingredients",
            "cooking_recommended": True,
            "suggested_items": [
                "Quick-cook items",
                "Some convenience foods",
                "Fresh staples"
            ],
            "message": "⚖️ Balanced mode. Mix of convenience and cooking."
        }
    
    def unlock_victory_feast(
        self,
        event_type: str,
        event_details: str,
        favorite_reward: str,
        current_budget: float
    ) -> Dict[str, Any]:
        """
        The Victory Feast Protocol.
        
        Unlocks a special budget when career/study win is achieved.
        
        Args:
            event_type: CAREER_WIN, EXAM_PASSED, INTERVIEW_ACED, etc.
            event_details: Description of the achievement
            favorite_reward: User's ultimate favorite food
            current_budget: Current daily budget
        
        Returns:
            Victory feast unlock with celebration message
        """
        # Determine reward amount based on achievement
        reward_amounts = {
            "JOB_OFFER": 50.0,
            "INTERVIEW_ACED": 30.0,
            "EXAM_PASSED": 25.0,
            "PROJECT_COMPLETED": 20.0,
            "CAREER_WIN": 30.0,
            "MILESTONE_REACHED": 15.0,
            "DEFAULT": 20.0
        }
        
        reward_amount = reward_amounts.get(event_type.upper(), reward_amounts["DEFAULT"])
        
        # Generate celebration message
        celebrations = {
            "JOB_OFFER": "🎉 YOU GOT THE JOB!!! This is HUGE!",
            "INTERVIEW_ACED": "🔥 You CRUSHED that interview!",
            "EXAM_PASSED": "📚 Exam CONQUERED! Knowledge = Power!",
            "PROJECT_COMPLETED": "🚀 Project SHIPPED! You're a builder!",
            "CAREER_WIN": "🏆 CAREER VICTORY! Champions deserve rewards!"
        }
        
        celebration = celebrations.get(event_type.upper(), "🎉 ACHIEVEMENT UNLOCKED!")
        
        return {
            "unlocked": True,
            "reward_amount": reward_amount,
            "favorite_reward": favorite_reward,
            "new_budget": current_budget + reward_amount,
            "event_type": event_type,
            "event_details": event_details,
            "celebration_message": f"""
{celebration}

{event_details}

I've unlocked ${reward_amount:.2f} from the reserve. 
Order that {favorite_reward} tonight. You earned it. 🏆

Your temporary budget: ${current_budget + reward_amount:.2f}

Enjoy your Victory Feast! This is what you work for.
""".strip(),
            "voice_message": f"Congratulations! You {event_details.lower()}! I've unlocked {reward_amount} dollars for your victory feast. Order that {favorite_reward}. You earned it!",
            "expires_at": (datetime.now() + timedelta(hours=24)).isoformat()
        }
    
    def make_meal_decision(
        self,
        context: MealDecisionContext
    ) -> Dict[str, Any]:
        """
        The Meal Time Arbitrator.
        
        Makes smart cook vs order decision based on context.
        """
        decision = self.decision_engine.decide(context)
        
        # Add Gemini suggestion if ordering
        if decision['action'] == 'ORDER':
            decision['delivery_options'] = self._get_healthy_delivery_options(context.daily_budget_remaining)
        elif decision['action'] == 'COOK':
            decision['recipe_hint'] = self._get_quick_recipe_hint(context.ingredients_available)
        
        return decision
    
    def _get_healthy_delivery_options(self, budget: float) -> List[Dict[str, Any]]:
        """Get healthy delivery suggestions within budget."""
        if budget > 20:
            return [
                {"name": "Grilled Chicken Salad", "type": "salad", "est_price": 15},
                {"name": "Poke Bowl", "type": "bowl", "est_price": 18},
                {"name": "Mediterranean Wrap", "type": "wrap", "est_price": 12}
            ]
        else:
            return [
                {"name": "Burrito Bowl (no delivery)", "type": "pickup", "est_price": 10},
                {"name": "Subway Sandwich", "type": "fast_casual", "est_price": 9},
                {"name": "Chinese Steamed Dishes", "type": "pickup", "est_price": 12}
            ]
    
    def _get_quick_recipe_hint(self, ingredients: List[str]) -> str:
        """Get a quick recipe idea based on ingredients."""
        if not ingredients:
            return "Check your fridge! Common staples make great meals."
        
        protein_words = ['egg', 'chicken', 'beef', 'tofu', 'beans', 'fish', 'tuna']
        carb_words = ['rice', 'pasta', 'bread', 'potato', 'noodle']
        
        has_protein = any(p in ' '.join(ingredients).lower() for p in protein_words)
        has_carb = any(c in ' '.join(ingredients).lower() for c in carb_words)
        
        if has_protein and has_carb:
            return f"Great combo! Try a stir-fry or bowl with {', '.join(ingredients[:3])}."
        elif has_protein:
            return f"Protein ready! Add rice or pasta for a complete meal."
        elif has_carb:
            return f"Base ready! Add eggs or canned protein for nutrition."
        else:
            return f"Get creative with {', '.join(ingredients[:3])}!"

    async def generate_proactive_meal_plan(
        self,
        financial: FinancialState,
        inventory: FridgeInventory,
        preferences: UserPreferences,
        schedule_pressure: str = "normal",
        energy_level: int = 50
    ) -> DailyMealPlan:
        """
        🧠 PROACTIVE MEAL PLANNING ENGINE
        
        Automatically generates a complete daily meal plan with multiple options per meal.
        User just selects, agent updates stats. No asking "what do you want to eat?"
        
        This is the Action Era - we plan BEFORE the user is hungry.
        
        Args:
            financial: User's financial state (for budget)
            inventory: Current fridge inventory (for cooking options)
            preferences: User's food preferences
            schedule_pressure: Schedule pressure level
            energy_level: Current energy (affects cooking vs order ratio)
        
        Returns:
            DailyMealPlan with 2-3 options per meal
        """
        import uuid
        
        plan_id = f"meal_plan_{uuid.uuid4().hex[:8]}"
        today = datetime.now().strftime("%Y-%m-%d")
        daily_budget = financial.daily_runway
        
        # Determine strategy based on defcon and energy
        if financial.defcon_level <= 2:
            strategy = "SURVIVAL"
            budget_split = {"breakfast": 0.15, "lunch": 0.35, "dinner": 0.50}
            cooking_bias = 0.9  # 90% cooking options
        elif financial.defcon_level == 3:
            strategy = "VALUE"
            budget_split = {"breakfast": 0.20, "lunch": 0.35, "dinner": 0.45}
            cooking_bias = 0.6
        else:
            strategy = "BALANCED"
            budget_split = {"breakfast": 0.25, "lunch": 0.35, "dinner": 0.40}
            cooking_bias = 0.4
        
        # Adjust for energy
        if energy_level < 30:
            cooking_bias = max(0.1, cooking_bias - 0.5)  # Low energy = more ordering
        
        # Get ingredients for cooking options
        ingredient_names = [i.get('name', '') for i in inventory.ingredients if i.get('name')]
        
        # Build prompt for Gemini
        prompt = f"""You are a proactive meal planning AI for a student survival app.

MISSION: Generate a complete daily meal plan with OPTIONS for the user to choose from.
The user should see all options and simply SELECT one. No decision paralysis.

USER CONTEXT:
- Daily Budget: ${daily_budget:.2f}
- Defcon Level: {financial.defcon_level} ({strategy} mode)
- Available Fridge Items: {', '.join(ingredient_names[:15]) if ingredient_names else 'Unknown/Empty'}
- Dietary Restrictions: {', '.join(preferences.dietary_restrictions) if preferences.dietary_restrictions else 'None'}
- Cooking Skill: {preferences.cooking_skill_level}
- Schedule Pressure: {schedule_pressure}
- Energy Level: {energy_level}/100
- Cooking Bias: {int(cooking_bias * 100)}% (higher = more cooking options)

BUDGET SPLIT:
- Breakfast: ${daily_budget * budget_split['breakfast']:.2f}
- Lunch: ${daily_budget * budget_split['lunch']:.2f}  
- Dinner: ${daily_budget * budget_split['dinner']:.2f}

GENERATE FOR EACH MEAL (breakfast, lunch, dinner):
- 2-3 options ranging from cheapest to most convenient
- Mix of COOK (from ingredients), QUICK_PREP, and ORDER options
- Each option MUST include: name, cost, prep time, type

OUTPUT JSON FORMAT:
{{
    "breakfast_options": [
        {{
            "option_id": "breakfast_1",
            "name": "Meal Name",
            "description": "Brief appetizing description",
            "meal_type": "breakfast",
            "action_type": "COOK|ORDER|QUICK_PREP",
            "estimated_cost": 0.00,
            "prep_time_mins": 10,
            "ingredients_needed": ["item1", "item2"],
            "ingredients_used_from_fridge": ["eggs", "bread"],
            "nutrition_score": 75,
            "energy_requirement": "low|medium|high"
        }}
    ],
    "lunch_options": [...],
    "dinner_options": [...],
    "daily_summary": "Brief tactical summary",
    "budget_optimization": "How to maximize nutrition within budget"
}}

RULES:
1. COOK options should use available fridge items when possible
2. ORDER options should suggest specific restaurants/types with realistic prices
3. Include at least one ultra-cheap option per meal for survival mode
4. All costs MUST fit within meal budget allocation
5. Be realistic about prep times and energy requirements
6. {"NO ordering options for breakfast/lunch due to budget" if financial.defcon_level <= 2 else "Include delivery/takeout options"}

Generate the meal plan NOW:"""

        if self.client is None:
            # Fallback to static options if no API
            return self._generate_fallback_meal_plan(plan_id, today, daily_budget, financial.defcon_level, inventory)
        
        try:
            response = await self.client.aio.models.generate_content(
                model=self.model,
                contents=[types.Part.from_text(text=prompt)],
                config=types.GenerateContentConfig(
                    thinking_config=types.ThinkingConfig(thinking_budget=65536),  # Deep thinking for meal planning
                    response_mime_type="application/json"
                )
            )
            
            response_text = response.text or "{}"
            result = json.loads(response_text)
            
            # Parse options
            def parse_options(options_data: List[Dict], meal_type: str) -> List[MealOption]:
                parsed = []
                for i, opt in enumerate(options_data[:3]):  # Max 3 options per meal
                    parsed.append(MealOption(
                        option_id=opt.get('option_id', f"{meal_type}_{i+1}"),
                        name=opt.get('name', f"Option {i+1}"),
                        description=opt.get('description', ''),
                        meal_type=meal_type,
                        action_type=opt.get('action_type', 'COOK'),
                        estimated_cost=float(opt.get('estimated_cost', 0)),
                        prep_time_mins=int(opt.get('prep_time_mins', 15)),
                        ingredients_needed=opt.get('ingredients_needed', []),
                        ingredients_used_from_fridge=opt.get('ingredients_used_from_fridge', []),
                        nutrition_score=int(opt.get('nutrition_score', 70)),
                        energy_requirement=opt.get('energy_requirement', 'medium')
                    ))
                return parsed
            
            return DailyMealPlan(
                plan_id=plan_id,
                date=today,
                total_budget=daily_budget,
                budget_remaining=daily_budget,
                defcon_level=financial.defcon_level,
                breakfast_options=parse_options(result.get('breakfast_options', []), 'breakfast'),
                lunch_options=parse_options(result.get('lunch_options', []), 'lunch'),
                dinner_options=parse_options(result.get('dinner_options', []), 'dinner'),
                generated_at=datetime.now().isoformat()
            )
            
        except Exception as e:
            print(f"⚠️ Proactive meal planning error: {e}")
            return self._generate_fallback_meal_plan(plan_id, today, daily_budget, financial.defcon_level, inventory)
    
    def _generate_fallback_meal_plan(
        self,
        plan_id: str,
        date: str,
        daily_budget: float,
        defcon_level: int,
        inventory: FridgeInventory
    ) -> DailyMealPlan:
        """Generate a static fallback meal plan when API is unavailable."""
        ingredient_names = [i.get('name', '') for i in inventory.ingredients[:5]]
        
        # Defcon-aware static options
        if defcon_level <= 2:
            # Survival mode - ultra cheap
            breakfast_opts = [
                MealOption("breakfast_1", "Oatmeal + Banana", "Budget fuel", "breakfast", "COOK", 0.50, 5, ["oatmeal", "banana"], [], 70, "low"),
                MealOption("breakfast_2", "Toast + Peanut Butter", "Quick protein", "breakfast", "QUICK_PREP", 0.30, 3, ["bread", "peanut butter"], [], 65, "low"),
            ]
            lunch_opts = [
                MealOption("lunch_1", "Rice & Beans Bowl", "Complete protein", "lunch", "COOK", 1.50, 25, ["rice", "beans"], ingredient_names[:2], 80, "medium"),
                MealOption("lunch_2", "Egg Fried Rice", "Pantry staples", "lunch", "COOK", 1.00, 15, ["rice", "eggs", "veggies"], ingredient_names[:3], 75, "medium"),
            ]
            dinner_opts = [
                MealOption("dinner_1", "Pasta with Veggie Sauce", "Filling & cheap", "dinner", "COOK", 2.50, 30, ["pasta", "canned tomatoes", "veggies"], [], 75, "medium"),
                MealOption("dinner_2", "Bean & Cheese Quesadilla", "Quick & satisfying", "dinner", "COOK", 2.00, 15, ["tortilla", "beans", "cheese"], [], 70, "medium"),
            ]
        elif defcon_level == 3:
            # Value mode
            breakfast_opts = [
                MealOption("breakfast_1", "Eggs & Toast", "Classic fuel", "breakfast", "COOK", 1.50, 10, ["eggs", "bread"], [], 80, "low"),
                MealOption("breakfast_2", "Smoothie Bowl", "Energy boost", "breakfast", "QUICK_PREP", 2.50, 5, ["yogurt", "banana", "berries"], [], 85, "low"),
                MealOption("breakfast_3", "Coffee + Pastry", "Cafe quick stop", "breakfast", "ORDER", 5.00, 0, [], [], 50, "low"),
            ]
            lunch_opts = [
                MealOption("lunch_1", "Chicken Stir-Fry", "Protein packed", "lunch", "COOK", 4.00, 25, ["chicken", "veggies", "rice"], ingredient_names[:3], 85, "medium"),
                MealOption("lunch_2", "Sandwich + Salad", "Balanced", "lunch", "QUICK_PREP", 3.50, 10, ["bread", "deli meat", "lettuce"], [], 75, "low"),
                MealOption("lunch_3", "Chipotle Bowl", "Fast casual", "lunch", "ORDER", 10.00, 0, [], [], 70, "low"),
            ]
            dinner_opts = [
                MealOption("dinner_1", "Baked Salmon + Veggies", "Omega boost", "dinner", "COOK", 8.00, 35, ["salmon", "broccoli", "rice"], [], 90, "medium"),
                MealOption("dinner_2", "Pasta Primavera", "Veggie loaded", "dinner", "COOK", 5.00, 30, ["pasta", "mixed veggies", "olive oil"], [], 80, "medium"),
                MealOption("dinner_3", "Thai Takeout", "Treat yourself", "dinner", "ORDER", 15.00, 0, [], [], 75, "low"),
            ]
        else:
            # Balanced/Abundance mode
            breakfast_opts = [
                MealOption("breakfast_1", "Avocado Toast + Eggs", "Instagram worthy", "breakfast", "COOK", 4.00, 15, ["avocado", "eggs", "sourdough"], [], 90, "low"),
                MealOption("breakfast_2", "Acai Bowl", "Superfood start", "breakfast", "ORDER", 12.00, 0, [], [], 85, "low"),
                MealOption("breakfast_3", "Quick Yogurt Parfait", "Ready in 2 min", "breakfast", "QUICK_PREP", 3.00, 2, ["yogurt", "granola", "berries"], [], 75, "low"),
            ]
            lunch_opts = [
                MealOption("lunch_1", "Mediterranean Salad", "Fresh & filling", "lunch", "COOK", 6.00, 15, ["greens", "feta", "olives", "chicken"], [], 90, "low"),
                MealOption("lunch_2", "Poke Bowl", "Hawaiian vibes", "lunch", "ORDER", 16.00, 0, [], [], 85, "low"),
                MealOption("lunch_3", "Leftover Magic", "Use what's there", "lunch", "QUICK_PREP", 0.00, 5, [], ingredient_names, 70, "low"),
            ]
            dinner_opts = [
                MealOption("dinner_1", "Steak & Roasted Vegetables", "Protein powerhouse", "dinner", "COOK", 15.00, 40, ["steak", "potatoes", "asparagus"], [], 90, "high"),
                MealOption("dinner_2", "Sushi Delivery", "Treat mode", "dinner", "ORDER", 25.00, 0, [], [], 80, "low"),
                MealOption("dinner_3", "Homemade Pizza", "Fun project", "dinner", "COOK", 8.00, 45, ["dough", "sauce", "cheese", "toppings"], [], 75, "high"),
            ]
        
        return DailyMealPlan(
            plan_id=plan_id,
            date=date,
            total_budget=daily_budget,
            budget_remaining=daily_budget,
            defcon_level=defcon_level,
            breakfast_options=breakfast_opts,
            lunch_options=lunch_opts,
            dinner_options=dinner_opts,
            generated_at=datetime.now().isoformat()
        )


# =============================================================================
# CROSS-AGENT INTEGRATION HELPERS
# =============================================================================
def extract_campaign_financial_context(campaign_state: Dict[str, Any]) -> Dict[str, Any]:
    """
    Extract financial context from Campaign Agent state.
    
    Used for the Financial Defcon System connection.
    """
    return {
        "employment_status": campaign_state.get("employment_status", "student"),
        "job_hunting": campaign_state.get("job_hunting", False),
        "runway_pressure": campaign_state.get("financial_pressure", "normal"),
        "recent_wins": campaign_state.get("recent_achievements", []),
        "financial_goal": campaign_state.get("financial_goal"),
        "urgent_job_hunt": campaign_state.get("status") == "Urgent_Job_Hunt"
    }


def extract_study_schedule_context(study_state: Dict[str, Any]) -> Dict[str, Any]:
    """
    Extract schedule context from Study Agent state.
    
    Used for the Time-Energy Matrix connection.
    """
    return {
        "pressure_index": study_state.get("pressure_index", 50),
        "exam_mode": study_state.get("exam_mode", False),
        "free_time_mins": study_state.get("free_time_available", 60),
        "schedule_pressure": "exam_week" if study_state.get("exam_mode") else (
            "high" if study_state.get("pressure_index", 50) > 70 else "normal"
        ),
        "next_deadline": study_state.get("next_deadline"),
        "study_session_active": study_state.get("session_active", False)
    }


def extract_radius_location_context(radius_state: Dict[str, Any]) -> Dict[str, Any]:
    """
    Extract location context from Radius Agent state.
    
    Used for location-triggered shopping alerts.
    """
    location = radius_state.get("current_location", "").lower()
    
    # Detect grocery store proximity
    grocery_keywords = ["grocery", "supermarket", "market", "store", "walmart", 
                        "target", "costco", "trader", "whole foods", "aldi", "kroger"]
    
    near_grocery = any(kw in location for kw in grocery_keywords)
    
    return {
        "current_location": radius_state.get("current_location"),
        "near_grocery_store": near_grocery,
        "location_type": radius_state.get("location_type", "unknown"),
        "mode": radius_state.get("mode", "STANDARD"),
        "trigger_shopping_alert": near_grocery and radius_state.get("mode") != "DEEP_FOCUS"
    }
