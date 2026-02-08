"""
🧬 VITALITY AGENT v2.0: SURVIVAL & GROWTH PROTOCOL
====================================================
The Life Logistics Engine that balances survival (food/money) with growth (career/study).

This agent transforms from a simple health tracker into a proactive system that:
- Monitors financial runway with Defcon levels
- Manages food inventory with smart fridge vision
- Makes intelligent Cook vs Order decisions
- Provides location-triggered shopping alerts
- Rewards achievements with Victory Feast protocol

Gemini 3 Features Used:
- Thinking: Deep financial and nutrition planning
- Vision: Fridge analysis, receipt scanning, bill extraction
- Native Multimodal: Seamless image + text processing
"""

import json
import base64
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext
from ..core.survival_protocol import (
    SurvivalState, SurvivalProtocol, DefconLevel,
    FinancialState, FridgeInventory, UserPreferences,
    MealDecisionContext, MealDecisionEngine,
    MealOption, DailyMealPlan,  # NEW: Proactive meal planning
    SHOPPING_LISTS_BY_DEFCON,
    extract_campaign_financial_context,
    extract_study_schedule_context,
    extract_radius_location_context
)
from ..core.event_bus import EventType, Event


class VitalityAgent(BaseAgent):
    """
    The Vitality Agent v2.0: Life Logistics Engine.
    
    Handles event types:
    LEGACY:
    - sleep_log: User logged sleep data
    - activity_log: Step count or workout data
    - meal_log: Nutrition/meal tracking
    - energy_check: User reports energy level
    - regen_request: User wants recovery mode
    - resource_update: Financial/time resource changes
    
    NEW SURVIVAL PROTOCOL:
    - one_shot_setup: Day Zero financial setup
    - fridge_scan: Smart fridge vision analysis
    - decision_matrix: Cook vs Order decision
    - shopping_alert: Location-triggered shopping
    - unlock_reward: Victory Feast protocol
    - daily_budget_check: Morning budget calculation
    - defcon_update: Financial state update
    """
    
    VITALITY_SYSTEM_PROMPT = """You are the Vitality Core v2.0 for Kaironex, a Student Life Operating System.

Your role is to be a LIFE LOGISTICS ENGINE that balances SURVIVAL (food, money) with GROWTH (career, study).

PERSONA:
- Tone: Supportive coach with strategic financial mind
- Style: Use gaming/RPG terminology + military-style Defcon levels
- Focus: Energy optimization, financial runway, food logistics

CRITICAL LANGUAGE RULES:
❌ NEVER use: health, medical, doctor, diagnosis, symptom, illness, disease, anxiety, depression, insomnia, therapy
✅ ALWAYS use: Stamina, Energy Bar, Recharge Cycle, Power-Up, Debuff, Regen Mode, Bio-Fuel, Runway, Defcon Level

DEFCON SYSTEM:
- 🟢 Defcon 5 (Abundance): High runway, stable income. Premium food choices allowed.
- 🟡 Defcon 3 (Caution): Runway < 3 months. Value meals, alert on impulse buys.
- 🔴 Defcon 1 (Survival): Unemployed OR runway < 1 month. Austerity Mode enforced.

OUTPUT FORMAT:
<analyze>Your analysis using game/military terminology</analyze>
<decision>The strategic decision made</decision>
<action>The user-facing message</action>

Remember: You're managing life logistics - money, food, energy. Not providing medical advice."""

    # Forbidden medical terms
    FORBIDDEN_MEDICAL_TERMS = [
        "health", "healthy", "medical", "doctor", "diagnosis", "symptom",
        "illness", "disease", "anxiety", "depression", "insomnia", "therapy",
        "medication", "prescription", "treatment", "condition", "disorder",
        "clinic", "hospital", "mental health", "physical health"
    ]

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.survival_protocol = SurvivalProtocol()
        self._cached_survival_state: Dict[str, SurvivalState] = {}

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="vitality",
            display_name="Vitality Core v2.0",
            system_instruction=self.VITALITY_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=8192,
            enable_thought_signatures=True,
            enable_marathon=True,  # MARATHON AGENT
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
        
        self.log_heartbeat(user_id, f"EVENT:VITALITY:{payload.get('type', 'check')}")
        
        event_type = payload.get('type', 'energy_check')
        
        handlers = {
            # Legacy handlers
            'sleep_log': self._handle_sleep_log,
            'activity_log': self._handle_activity_log,
            'meal_log': self._handle_meal_log,
            'energy_check': self._handle_energy_check,
            'regen_request': self._handle_regen_request,
            'resource_update': self._handle_resource_update,
            
            # NEW: Survival Protocol handlers
            'one_shot_setup': self._handle_one_shot_setup,
            'fridge_scan': self._handle_fridge_scan,
            'decision_matrix': self._handle_decision_matrix,
            'shopping_alert': self._handle_shopping_alert,
            'unlock_reward': self._handle_unlock_reward,
            'daily_budget_check': self._handle_daily_budget_check,
            'defcon_update': self._handle_defcon_update,
            'cross_agent_sync': self._handle_cross_agent_sync,
            
            # PROACTIVE MEAL PLANNING - The Action Era
            'proactive_meal_plan': self._handle_proactive_meal_plan,
            'select_meal_option': self._handle_select_meal_option,
            'get_todays_meals': self._handle_get_todays_meals,
        }
        
        handler = handlers.get(event_type, self._handle_energy_check)
        return await handler(user_id, payload, context)

    # =========================================================================
    # NEW: SURVIVAL PROTOCOL HANDLERS
    # =========================================================================
    
    async def _handle_one_shot_setup(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Day Zero One-Shot Setup.
        
        Parses financial info from natural language or image (bill photo, bank statement).
        Uses Gemini 3 Thinking + Vision for extraction.
        """
        financial_text = payload.get('financial_info', '')
        image_base64 = payload.get('image_base64')
        
        # Convert base64 to bytes if image provided
        image_data = None
        if image_base64:
            try:
                image_data = base64.b64decode(image_base64)
            except Exception as e:
                print(f"⚠️ Image decode error: {e}")
        
        # Parse financial data using Gemini 3
        financial_state = await self.survival_protocol.parse_financial_setup(
            input_data=financial_text,
            image_data=image_data
        )
        
        # Initialize preferences if provided
        preferences = UserPreferences(
            favorite_reward=payload.get('favorite_food', 'Pizza'),
            dietary_restrictions=payload.get('dietary_restrictions', []),
            cooking_skill_level=payload.get('cooking_skill', 'intermediate')
        )
        
        # Create survival state
        survival_state = SurvivalState(
            financial=financial_state,
            preferences=preferences
        )
        
        # Store in cache and database
        self._cached_survival_state[user_id] = survival_state
        
        # Generate AI response explaining the setup
        prompt = f"""
FINANCIAL SETUP COMPLETE - DAY ZERO CALIBRATION

EXTRACTED DATA:
- Total Balance: ${financial_state.total_balance:,.2f}
- Fixed Bills: ${financial_state.fixed_bills:,.2f}
- Days Until Payday: {financial_state.days_until_payday}
- Employment Status: {financial_state.employment_status}

CALCULATED RUNWAY:
- Daily Budget: ${financial_state.daily_runway:.2f}
- Defcon Level: {financial_state.defcon_level}

TASK:
1. Confirm the setup in an encouraging way
2. Explain what Defcon {financial_state.defcon_level} means for their daily life
3. Give ONE actionable tip for their financial situation
4. Use military/gaming terminology

Keep it brief and empowering. This is their financial command center now.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.HYBRID,
            system_instruction=self.VITALITY_SYSTEM_PROMPT
        ))
        
        # Update state cache
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "setup_complete": True,
                "setup_timestamp": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        # Emit event
        if self.events:
            await self.events.publish(Event.create(
                EventType.STATE_UPDATED,
                source="vitality",
                user_id=user_id,
                data={"action": "financial_setup", "defcon": financial_state.defcon_level}
            ))
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["financial_setup_complete", f"defcon_set:{financial_state.defcon_level}"],
            state_updates=vitality_update,
            data={
                "survival_state": survival_state.to_dict(),
                "daily_budget": financial_state.daily_runway,
                "defcon_level": financial_state.defcon_level
            }
        )
    
    async def _handle_fridge_scan(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Smart Fridge Vision.
        
        Analyzes fridge/pantry photo to identify ingredients and estimate days of food.
        Uses Gemini 3 Vision.
        """
        image_base64 = payload.get('image_base64')
        
        if not image_base64:
            return AgentResult(
                success=False,
                response="📸 No image received. Please upload a photo of your fridge or pantry.",
                error="No image provided"
            )
        
        try:
            image_data = base64.b64decode(image_base64)
        except Exception as e:
            return AgentResult(
                success=False,
                response="❌ Could not process the image. Please try again.",
                error=str(e)
            )
        
        # Analyze fridge using Gemini 3 Vision
        inventory = await self.survival_protocol.analyze_fridge(image_data)
        
        # Get or create survival state
        survival_state = self._get_survival_state(user_id)
        survival_state.inventory = inventory
        self._cached_survival_state[user_id] = survival_state
        
        # Generate AI assessment
        ingredients_list = [f"- {i['name']} ({i.get('quantity', 'unknown')})" for i in inventory.ingredients[:10]]
        
        prompt = f"""
FRIDGE SCAN COMPLETE - INVENTORY ASSESSED

DETECTED INGREDIENTS:
{chr(10).join(ingredients_list) if ingredients_list else "- Empty or unable to identify items"}

SURVIVAL ASSESSMENT:
- Estimated Days of Food: {inventory.days_remaining}
- Shopping Needed: {'Yes - Urgent!' if inventory.needs_shopping else 'Not urgently'}

Current Defcon Level: {survival_state.financial.defcon_level}

TASK:
1. Acknowledge the inventory in a game-like way ("Inventory scanned!")
2. Tell them how many days they can survive
3. If shopping needed, give urgent but not stressful advice
4. Suggest 1-2 meals they can make with current ingredients

Keep it brief and actionable. Gaming terminology.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        # Check if intervention needed
        interventions = []
        if inventory.days_remaining <= 1:
            intervention_id = self.create_intervention(
                user_id,
                "LOW_FOOD_SUPPLY",
                f"🍽️ **Food Supply Critical**\n\nEstimated: {inventory.days_remaining} day(s) remaining.\nTime to resupply, commander!",
                strategy="URGENT"
            )
            interventions.append(intervention_id)
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "last_fridge_scan": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["fridge_scanned", f"days_remaining:{inventory.days_remaining}"],
            interventions_created=interventions,
            state_updates=vitality_update,
            data={
                "inventory": inventory.to_dict(),
                "days_remaining": inventory.days_remaining,
                "needs_shopping": inventory.needs_shopping
            }
        )
    
    async def _handle_decision_matrix(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        The Meal Time Arbitrator - Cook vs Order Decision.
        
        Uses the Bicameral Engine to weigh:
        - Time (from Schedule/Study Agent)
        - Money (from Financial Runway)
        - Energy (from Vitality state)
        """
        # Get current survival state
        survival_state = self._get_survival_state(user_id)
        
        # Extract cross-agent context
        campaign_context = extract_campaign_financial_context(
            payload.get('campaign_state', {})
        )
        study_context = extract_study_schedule_context(
            payload.get('study_state', {})
        )
        
        # Build decision context
        decision_ctx = MealDecisionContext(
            time_available_mins=payload.get('time_available', study_context.get('free_time_mins', 60)),
            energy_level=payload.get('energy_level', 50),
            daily_budget_remaining=payload.get('budget_remaining', survival_state.financial.daily_runway),
            defcon_level=survival_state.financial.defcon_level,
            ingredients_available=[i.get('name', '') for i in survival_state.inventory.ingredients],
            near_restaurant=payload.get('near_restaurant', False),
            schedule_pressure=study_context.get('schedule_pressure', 'normal')
        )
        
        # Special override: Urgent Job Hunt = Austerity Mode
        if campaign_context.get('urgent_job_hunt'):
            decision_ctx.defcon_level = DefconLevel.DEFCON_1_SURVIVAL
        
        # Make decision
        decision = self.survival_protocol.make_meal_decision(decision_ctx)
        survival_state.last_decision = decision
        
        # Generate AI explanation
        prompt = f"""
MEAL TIME ARBITRATOR - DECISION MADE

CONTEXT ANALYZED:
- Time Available: {decision_ctx.time_available_mins} minutes
- Energy Level: {decision_ctx.energy_level}%
- Budget Remaining: ${decision_ctx.daily_budget_remaining:.2f}
- Defcon Level: {decision_ctx.defcon_level}
- Schedule Pressure: {decision_ctx.schedule_pressure}
- Ingredients: {len(decision_ctx.ingredients_available)} items

DECISION: {decision['action']}
REASON: {decision['reason']}
SUGGESTION: {decision['suggestion']}

TASK:
1. Announce the decision like a tactical advisor
2. Explain WHY this is the best choice right now
3. Give the specific suggestion
4. If ordering, mention budget allocation (${decision.get('budget_allocation', 0):.2f})

Be decisive and supportive. Military briefing style.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.HYBRID,
            system_instruction=self.VITALITY_SYSTEM_PROMPT
        ))
        
        vitality_update = {
            "vitality": {
                "last_meal_decision": decision,
                "decision_timestamp": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=[f"decision:{decision['action']}", "meal_arbitrated"],
            state_updates=vitality_update,
            data={
                "decision": decision,
                "context_used": {
                    "time_mins": decision_ctx.time_available_mins,
                    "energy": decision_ctx.energy_level,
                    "budget": decision_ctx.daily_budget_remaining,
                    "defcon": decision_ctx.defcon_level
                }
            }
        )
    
    async def _handle_shopping_alert(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Just-in-Time Shopping Alert.
        
        Triggered when:
        - Radius Agent detects user near grocery store
        - Estimated food days <= 1
        
        Generates shopping list based on Defcon level.
        """
        survival_state = self._get_survival_state(user_id)
        location = payload.get('location', 'Grocery Store')
        
        # Check if shopping is needed
        estimated_food_days = survival_state.inventory.days_remaining
        force_alert = payload.get('force_alert', False)
        
        if estimated_food_days > 2 and not force_alert:
            return AgentResult(
                success=True,
                response=f"📦 Inventory check: You're good for {estimated_food_days} days. No urgent shopping needed.",
                actions_taken=["shopping_check_passed"],
                data={"days_remaining": estimated_food_days, "shopping_needed": False}
            )
        
        # Generate shopping list based on Defcon
        shopping_list = self.survival_protocol.generate_shopping_list(
            defcon_level=survival_state.financial.defcon_level,
            current_inventory=survival_state.inventory.ingredients,
            dietary_restrictions=survival_state.preferences.dietary_restrictions
        )
        
        # Get time-energy adjustment
        time_energy = self.survival_protocol.get_time_energy_adjustment(
            schedule_pressure=payload.get('schedule_pressure', 'normal'),
            energy_level=payload.get('energy_level', 50)
        )
        
        # Store last shopping alert
        survival_state.last_shopping_alert = datetime.now().isoformat()
        
        # Create intervention for voice call
        intervention_id = self.create_intervention(
            user_id,
            "SHOPPING_ALERT",
            f"🛒 **Shopping Alert**\n\n{shopping_list['voice_alert']}\n\n{time_energy['message']}",
            strategy="HELPFUL",
            metadata={
                "voice_message": shopping_list['voice_alert'],
                "location": location,
                "shopping_list": shopping_list
            }
        )
        
        # Generate full response
        items_formatted = "\n".join([f"  • {item['item']}" for item in shopping_list['items'][:8]])
        
        prompt = f"""
SHOPPING ALERT TRIGGERED - YOU'RE NEAR: {location}

SITUATION:
- Food Days Remaining: {estimated_food_days}
- Defcon Level: {survival_state.financial.defcon_level}
- Max Budget: ${shopping_list['max_budget']}

SHOPPING LIST ({shopping_list['list_name']}):
{items_formatted}

STRATEGY: {shopping_list['strategy']}
TIME/ENERGY: {time_energy['message']}

TASK:
1. Alert them they're near a store (brief!)
2. State the key items they need (top 3-4)
3. Remind them of budget
4. If Defcon 1-2, strongly discourage non-list items

Make it feel like a tactical briefing. Quick and actionable. This could be a voice message.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        vitality_update = {
            "vitality": {
                "last_shopping_alert": datetime.now().isoformat(),
                "shopping_list_generated": shopping_list
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["shopping_alert_triggered", f"list_generated:defcon_{survival_state.financial.defcon_level}"],
            interventions_created=[intervention_id],
            state_updates=vitality_update,
            data={
                "shopping_list": shopping_list,
                "time_energy_adjustment": time_energy,
                "voice_message": shopping_list['voice_alert'],
                "trigger_voice_call": True
            }
        )
    
    async def _handle_unlock_reward(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Victory Feast Protocol.
        
        Triggered when Campaign Agent reports a WIN:
        - Job Offer
        - Interview Aced
        - Exam Passed
        
        Unlocks special budget for user's favorite food.
        """
        event_type = payload.get('event_type', 'CAREER_WIN')
        event_details = payload.get('event_details', 'You achieved something amazing!')
        
        survival_state = self._get_survival_state(user_id)
        
        # Unlock the victory feast
        feast = self.survival_protocol.unlock_victory_feast(
            event_type=event_type,
            event_details=event_details,
            favorite_reward=survival_state.preferences.favorite_reward,
            current_budget=survival_state.financial.daily_runway
        )
        
        # Update survival state
        survival_state.reward_unlocked = True
        survival_state.reward_amount = feast['reward_amount']
        survival_state.financial.budget_override = feast['reward_amount']
        self._cached_survival_state[user_id] = survival_state
        
        # Create celebration intervention
        intervention_id = self.create_intervention(
            user_id,
            "VICTORY_FEAST",
            feast['celebration_message'],
            strategy="CELEBRATION",
            metadata={
                "voice_message": feast['voice_message'],
                "reward_amount": feast['reward_amount'],
                "favorite_food": survival_state.preferences.favorite_reward,
                "trigger_voice": True
            }
        )
        
        # Emit event
        if self.events:
            await self.events.publish(Event.create(
                EventType.USER_ACTION,
                source="vitality",
                user_id=user_id,
                data={"action": "victory_feast_unlocked", "amount": feast['reward_amount']}
            ))
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "reward_active": True,
                "reward_expires": feast['expires_at']
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=feast['celebration_message'],
            actions_taken=["victory_feast_unlocked", f"reward:{feast['reward_amount']}"],
            interventions_created=[intervention_id],
            state_updates=vitality_update,
            data={
                "feast": feast,
                "voice_message": feast['voice_message'],
                "trigger_voice_call": True
            }
        )
    
    async def _handle_daily_budget_check(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Morning Budget Check.
        
        Calculates today's safe-to-spend with smart adjustments.
        """
        survival_state = self._get_survival_state(user_id)
        
        spent_yesterday = payload.get('spent_yesterday', survival_state.financial.spent_yesterday)
        survival_state.financial.spent_yesterday = spent_yesterday
        
        # Calculate budget
        budget_info = self.survival_protocol.calculate_daily_budget(
            financial=survival_state.financial,
            spent_yesterday=spent_yesterday
        )
        
        # Generate morning briefing
        prompt = f"""
MORNING FINANCIAL BRIEFING - DAY BUDGET CALCULATED

TODAY'S BUDGET: ${budget_info['today_budget']:.2f}
Base Daily: ${budget_info['base_daily']:.2f}
Yesterday Spent: ${budget_info['yesterday_spent']:.2f}
Adjustment: -${budget_info['overspend_adjustment']:.2f}
Defcon Level: {budget_info['defcon_level']}

MESSAGE: {budget_info['message']}

RECOMMENDATIONS:
{chr(10).join(['- ' + r for r in budget_info['recommendations']])}

TASK:
1. Deliver the morning financial briefing
2. Make it feel like a game starting a new day
3. If overspend, be supportive not judgmental
4. One quick tip for the day

Brief and energizing. Start the day right.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        vitality_update = {
            "vitality": {
                "today_budget": budget_info['today_budget'],
                "budget_info": budget_info,
                "budget_check_time": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["daily_budget_calculated"],
            state_updates=vitality_update,
            data={
                "budget_info": budget_info,
                "today_budget": budget_info['today_budget']
            }
        )
    
    async def _handle_defcon_update(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Update Defcon Level based on new financial info.
        """
        survival_state = self._get_survival_state(user_id)
        
        # Update fields if provided
        if 'total_balance' in payload:
            survival_state.financial.total_balance = float(payload['total_balance'])
        if 'spent_today' in payload:
            survival_state.financial.spent_today = float(payload['spent_today'])
        if 'employment_status' in payload:
            survival_state.financial.employment_status = payload['employment_status']
        
        # Recalculate Defcon
        daily = survival_state.financial.daily_runway
        employed = survival_state.financial.employment_status != 'unemployed'
        
        if daily > 50 and employed:
            new_defcon = DefconLevel.DEFCON_5_ABUNDANCE
        elif daily >= 30 or (daily >= 20 and employed):
            new_defcon = DefconLevel.DEFCON_4_STABLE
        elif daily >= 15:
            new_defcon = DefconLevel.DEFCON_3_CAUTION
        elif daily >= 8:
            new_defcon = DefconLevel.DEFCON_2_CRITICAL
        else:
            new_defcon = DefconLevel.DEFCON_1_SURVIVAL
        
        old_defcon = survival_state.financial.defcon_level
        survival_state.financial.defcon_level = new_defcon
        self._cached_survival_state[user_id] = survival_state
        
        # Generate response if level changed
        level_changed = old_defcon != new_defcon
        direction = "improved" if new_defcon > old_defcon else "decreased"
        
        if level_changed:
            prompt = f"""
DEFCON LEVEL CHANGE DETECTED

Previous: Defcon {old_defcon}
New: Defcon {new_defcon}
Direction: {direction.upper()}

Daily Runway: ${daily:.2f}
Employment: {survival_state.financial.employment_status}

TASK:
1. Announce the Defcon change (dramatic but appropriate)
2. Explain what this means for their daily spending
3. If improved, celebrate! If decreased, be supportive.
4. One concrete action they should take

Military/gaming style. Brief and impactful.
"""
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="vitality",
                mode=ReasoningMode.REFLEX
            ))
            response_text = self._sanitize_output(response.content)
        else:
            response_text = f"✅ Defcon {new_defcon} - Status unchanged. Runway: ${daily:.2f}/day"
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "defcon_updated": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=response_text,
            actions_taken=[f"defcon_set:{new_defcon}", f"level_changed:{level_changed}"],
            state_updates=vitality_update,
            data={
                "defcon_level": new_defcon,
                "old_defcon": old_defcon,
                "level_changed": level_changed,
                "daily_runway": daily
            }
        )
    
    async def _handle_cross_agent_sync(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        Sync state with other agents (Campaign, Study, Radius).
        
        This is the Context Engine that connects all agents.
        """
        survival_state = self._get_survival_state(user_id)
        
        # Extract context from other agents
        campaign_ctx = extract_campaign_financial_context(
            payload.get('campaign_state', {})
        )
        study_ctx = extract_study_schedule_context(
            payload.get('study_state', {})
        )
        radius_ctx = extract_radius_location_context(
            payload.get('radius_state', {})
        )
        
        actions_taken = ["cross_agent_sync"]
        data: Dict[str, Any] = {"synced_contexts": []}
        
        # Handle Career Agent connection (Financial Defcon)
        if campaign_ctx.get('urgent_job_hunt'):
            survival_state.financial.defcon_level = DefconLevel.DEFCON_1_SURVIVAL
            actions_taken.append("defcon_override:job_hunt")
            data["synced_contexts"].append("campaign")
        
        # Handle Study Agent connection (Time-Energy Matrix)
        if study_ctx.get('exam_mode'):
            # Shift to convenience mode
            time_energy = self.survival_protocol.get_time_energy_adjustment(
                schedule_pressure='exam_week',
                energy_level=payload.get('energy_level', 50)
            )
            survival_state.last_decision = {
                "override": "exam_mode",
                "recommendation": time_energy['recommendation']
            }
            actions_taken.append("exam_mode_activated")
            data["synced_contexts"].append("study")
            data["time_energy_adjustment"] = time_energy
        
        # Handle Radius Agent connection (Location Shopping)
        if radius_ctx.get('trigger_shopping_alert') and survival_state.inventory.needs_shopping:
            # Trigger shopping alert
            shopping_result = await self._handle_shopping_alert(
                user_id,
                {"location": radius_ctx.get('current_location', 'Grocery Store')},
                context
            )
            data["shopping_alert_triggered"] = True
            data["synced_contexts"].append("radius")
        
        # Check for victory events from Campaign
        recent_wins = campaign_ctx.get('recent_wins', [])
        if recent_wins:
            for win in recent_wins[-1:]:  # Only process latest
                if not survival_state.reward_unlocked:
                    await self._handle_unlock_reward(
                        user_id,
                        {"event_type": "CAREER_WIN", "event_details": win},
                        context
                    )
                    data["victory_feast_triggered"] = True
        
        self._cached_survival_state[user_id] = survival_state
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "last_sync": datetime.now().isoformat(),
                "synced_agents": data["synced_contexts"]
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=f"🔄 Cross-agent sync complete. Synced: {', '.join(data['synced_contexts']) or 'none'}",
            actions_taken=actions_taken,
            state_updates=vitality_update,
            data=data
        )

    # =========================================================================
    # 🍽️ PROACTIVE MEAL PLANNING - THE ACTION ERA
    # =========================================================================
    
    async def _handle_proactive_meal_plan(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        🍽️ PROACTIVE MEAL PLANNING ENGINE
        
        Automatically generates a complete daily meal plan with options.
        This is the Action Era - we plan BEFORE the user asks.
        """
        survival_state = self._get_survival_state(user_id)
        today = datetime.now().strftime("%Y-%m-%d")
        
        # Check if we already have today's plan
        existing_plan = survival_state.daily_meal_plan
        if existing_plan and existing_plan.date == today and not payload.get('force_regenerate'):
            return AgentResult(
                success=True,
                response="🍽️ Your meal plan for today is ready! Select what sounds good.",
                data={"plan": existing_plan.to_dict(), "status": "existing_plan"},
                actions_taken=["existing_plan_returned"]
            )
        
        # Generate new plan
        meal_plan = await self.survival_protocol.generate_proactive_meal_plan(
            financial=survival_state.financial,
            inventory=survival_state.inventory,
            preferences=survival_state.preferences,
            schedule_pressure=payload.get('schedule_pressure', 'normal'),
            energy_level=payload.get('energy_level', 50)
        )
        
        # Store the plan
        survival_state.daily_meal_plan = meal_plan
        self._cached_survival_state[user_id] = survival_state
        
        # Count options
        total_options = (
            len(meal_plan.breakfast_options) +
            len(meal_plan.lunch_options) +
            len(meal_plan.dinner_options)
        )
        
        # Generate AI summary
        prompt = f"""
DAILY MEAL PLAN GENERATED - PROACTIVE LOGISTICS

Budget: ${meal_plan.total_budget:.2f}
Defcon Level: {meal_plan.defcon_level}
Total Options: {total_options}

BREAKFAST OPTIONS: {len(meal_plan.breakfast_options)}
LUNCH OPTIONS: {len(meal_plan.lunch_options)}
DINNER OPTIONS: {len(meal_plan.dinner_options)}

TASK:
Give a 2-sentence tactical briefing announcing the meal plan.
Mention they have OPTIONS ready - no decision paralysis.
Military/gaming style. End with "Select your fuel, commander!"
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "meal_plan_date": today
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["meal_plan_generated", f"options:{total_options}"],
            state_updates=vitality_update,
            data={
                "plan": meal_plan.to_dict(),
                "total_options": total_options
            }
        )
    
    async def _handle_select_meal_option(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        🎯 SELECT A MEAL OPTION
        
        User selects their choice → budget updates → inventory updates.
        The Action Era: user selects, system handles logistics.
        """
        survival_state = self._get_survival_state(user_id)
        option_id = payload.get('option_id')
        
        if not option_id:
            return AgentResult(
                success=False,
                response="❌ No option_id provided. Which meal did you select?",
                error="Missing option_id"
            )
        
        meal_plan = survival_state.daily_meal_plan
        if not meal_plan:
            return AgentResult(
                success=False,
                response="📋 No meal plan for today! Let me generate one first.",
                error="No meal plan exists",
                data={"trigger_action": "proactive_meal_plan"}
            )
        
        # Find and select
        option = meal_plan.get_option_by_id(option_id)
        if not option:
            return AgentResult(
                success=False,
                response=f"❌ Option '{option_id}' not found in today's plan.",
                error="Option not found"
            )
        
        # Execute selection
        success, message, cost_deducted = meal_plan.select_option(option_id)
        
        if not success:
            return AgentResult(
                success=False,
                response=f"💰 {message}",
                error="Budget exceeded",
                data={"budget_remaining": meal_plan.budget_remaining, "option_cost": option.estimated_cost}
            )
        
        # Update inventory if cooking
        ingredients_consumed = []
        if option.action_type == "COOK" and option.ingredients_used_from_fridge:
            for ingredient in option.ingredients_used_from_fridge:
                for inv_item in survival_state.inventory.ingredients:
                    if ingredient.lower() in inv_item.get('name', '').lower():
                        inv_item['quantity'] = 'reduced'
                        ingredients_consumed.append(ingredient)
            
            if survival_state.inventory.days_remaining > 0 and len(ingredients_consumed) >= 2:
                survival_state.inventory.days_remaining = max(0, survival_state.inventory.days_remaining - 1)
        
        # Track spending
        survival_state.financial.spent_today += cost_deducted
        self._cached_survival_state[user_id] = survival_state
        
        # AI confirmation
        prompt = f"""
MEAL SELECTION CONFIRMED

Selected: {option.name} ({option.action_type})
Cost: ${option.estimated_cost:.2f}
Budget Remaining: ${meal_plan.budget_remaining:.2f}
Meal Type: {option.meal_type}

Give a 1-sentence enthusiastic confirmation. Military/gaming tactical approval style.
Include the remaining budget.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=[f"meal_selected:{option.meal_type}", f"spent:${cost_deducted:.2f}"],
            state_updates=vitality_update,
            data={
                "selected": {"id": option.option_id, "name": option.name, "cost": option.estimated_cost},
                "cost_deducted": cost_deducted,
                "budget_remaining": meal_plan.budget_remaining,
                "ingredients_consumed": ingredients_consumed,
                "progress": {
                    "breakfast": meal_plan.selected_breakfast is not None,
                    "lunch": meal_plan.selected_lunch is not None,
                    "dinner": meal_plan.selected_dinner is not None
                }
            }
        )
    
    async def _handle_get_todays_meals(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """
        📋 GET TODAY'S MEAL PLAN STATUS
        
        Quick view of what's planned and what's been selected.
        """
        survival_state = self._get_survival_state(user_id)
        meal_plan = survival_state.daily_meal_plan
        today = datetime.now().strftime("%Y-%m-%d")
        
        if not meal_plan or meal_plan.date != today:
            return AgentResult(
                success=True,
                response="📋 No meal plan for today yet! Let me generate one.",
                data={"status": "no_plan", "trigger_action": "proactive_meal_plan"},
                actions_taken=["no_plan"]
            )
        
        # Build status
        def meal_status(selected_id, options):
            if selected_id:
                for opt in options:
                    if opt.option_id == selected_id:
                        return {"status": "selected", "selected": opt.name, "cost": opt.estimated_cost}
            return {"status": "pending", "options_count": len(options)}
        
        breakfast_status = meal_status(meal_plan.selected_breakfast, meal_plan.breakfast_options)
        lunch_status = meal_status(meal_plan.selected_lunch, meal_plan.lunch_options)
        dinner_status = meal_status(meal_plan.selected_dinner, meal_plan.dinner_options)
        
        all_selected = all([
            breakfast_status["status"] == "selected",
            lunch_status["status"] == "selected",
            dinner_status["status"] == "selected"
        ])
        
        pending_meals = [
            m for m, s in [("breakfast", breakfast_status), ("lunch", lunch_status), ("dinner", dinner_status)]
            if s["status"] == "pending"
        ]
        
        if all_selected:
            message = f"✅ All meals planned! Budget used: ${meal_plan.spent_today:.2f} / ${meal_plan.total_budget:.2f}"
        elif pending_meals:
            message = f"🍽️ Pending: {', '.join(pending_meals)}. ${meal_plan.budget_remaining:.2f} remaining."
        else:
            message = "📋 Today's meals are ready for selection!"
        
        return AgentResult(
            success=True,
            response=message,
            actions_taken=["meal_status_retrieved"],
            data={
                "date": meal_plan.date,
                "breakfast": breakfast_status,
                "lunch": lunch_status,
                "dinner": dinner_status,
                "budget": {
                    "total": meal_plan.total_budget,
                    "spent": meal_plan.spent_today,
                    "remaining": meal_plan.budget_remaining
                },
                "all_selected": all_selected,
                "pending_meals": pending_meals
            }
        )

    # =========================================================================
    # LEGACY HANDLERS (Updated with Survival Protocol awareness)
    # =========================================================================
    
    async def _handle_sleep_log(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle sleep/recharge data (updated with survival awareness)."""
        
        sleep_hours = payload.get('sleep_hours', 0)
        sleep_quality = payload.get('quality', 'unknown')
        wake_time = payload.get('wake_time')
        
        energy_level = self._calculate_energy_level(sleep_hours, sleep_quality)
        regen_mode_needed = sleep_hours < 5 or sleep_quality == 'poor'
        
        # Get survival context for decision making
        survival_state = self._get_survival_state(user_id)
        
        prompt = f"""
RECHARGE CYCLE LOGGED:
Duration: {sleep_hours} hours
Quality Rating: {sleep_quality}
Wake Time: {wake_time or 'Unknown'}
Energy Bar: {energy_level}%
Regen Mode Recommended: {'Yes' if regen_mode_needed else 'No'}

SURVIVAL CONTEXT:
Defcon Level: {survival_state.financial.defcon_level}
Today's Budget: ${survival_state.financial.daily_runway:.2f}

TASK:
1. Analyze the recharge cycle efficiency
2. Calculate any debuffs that should apply
3. If energy low, factor in food strategy (easy meals)
4. Suggest power-up strategies

Gaming terminology only. Brief and actionable.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.HYBRID,
            system_instruction=self.VITALITY_SYSTEM_PROMPT
        ))
        
        interventions = []
        if regen_mode_needed:
            intervention_id = self.create_intervention(
                user_id,
                "LOW_RECHARGE",
                f"⚡ **Recharge Cycle Alert**\n\n{self._get_action_text(response.content)}",
                strategy="EMPATHY"
            )
            interventions.append(intervention_id)
        
        vitality_update = {
            "vitality": {
                "energy_bar": energy_level,
                "regen_mode": regen_mode_needed,
                "last_recharge": {
                    "duration": sleep_hours,
                    "quality": sleep_quality,
                    "timestamp": datetime.now().isoformat()
                },
                "debuffs": self._calculate_debuffs(sleep_hours, sleep_quality)
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
3. Suggest recovery fuel if high intensity
4. Update daily activity score

Brief and energizing. Gaming terminology only.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
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
        """Handle meal/bio-fuel intake with cost tracking."""
        
        meal_type = payload.get('meal_type', 'snack')
        meal_description = payload.get('description', '')
        meal_cost = payload.get('cost', 0.0)
        
        survival_state = self._get_survival_state(user_id)
        
        # Track spending
        if meal_cost > 0:
            survival_state.financial.spent_today += meal_cost
        
        prompt = f"""
BIO-FUEL INTAKE LOGGED:
Type: {meal_type.title()}
Description: {meal_description or 'Not specified'}
Cost: ${meal_cost:.2f}

BUDGET STATUS:
Today's Budget: ${survival_state.financial.daily_runway:.2f}
Spent Today: ${survival_state.financial.spent_today:.2f}
Remaining: ${max(0, survival_state.financial.daily_runway - survival_state.financial.spent_today):.2f}

TASK:
1. Acknowledge the refuel
2. Estimate energy boost
3. If over budget, note it gently
4. Brief encouragement

Gaming terminology. Brief and positive.
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
                    "cost": meal_cost,
                    "timestamp": datetime.now().isoformat()
                },
                "survival": survival_state.to_dict()
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["meal_logged", f"cost_tracked:{meal_cost}"],
            state_updates=vitality_update
        )
    
    async def _handle_energy_check(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Handle general energy level check with survival context."""
        
        reported_level = payload.get('energy_level')
        sleep_hours = payload.get('sleep_hours')
        steps = payload.get('steps', 0)
        
        user_doc = self.db.get_user_doc(user_id)
        current_state = {}
        if user_doc:
            try:
                current_state = json.loads(user_doc.get('studentState_json', '{}'))
            except:
                pass
        
        vitality = current_state.get('vitality', {})
        survival_state = self._get_survival_state(user_id)
        
        prompt = f"""
VITALITY STATUS CHECK:
User-Reported Energy: {reported_level or 'Not specified'}/10
Last Recharge: {sleep_hours or 'Unknown'} hours
Activity Level: {steps} steps today
Current Debuffs: {vitality.get('debuffs', 'None')}

SURVIVAL STATUS:
Defcon Level: {survival_state.financial.defcon_level}
Daily Budget: ${survival_state.financial.daily_runway:.2f}
Food Supply: {survival_state.inventory.days_remaining} days

TASK:
1. Assess overall stamina status
2. Connect energy to food/budget strategy
3. Recommend immediate actions
4. Suggest power-ups based on budget

Gaming terminology. Brief and actionable.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.HYBRID
        ))
        
        energy_level = self._calculate_composite_energy(reported_level, sleep_hours, steps)
        
        interventions = []
        if energy_level < 30:
            intervention_id = self.create_intervention(
                user_id,
                "CRITICAL_ENERGY",
                "🔋 **Energy Critical!** Activate Regen Mode. Easy meals only.",
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
        survival_state = self._get_survival_state(user_id)
        
        prompt = f"""
REGEN MODE ACTIVATION REQUESTED
Duration: {duration_hours} hours
Current Defcon: {survival_state.financial.defcon_level}

TASK:
1. Confirm Regen Mode activation
2. Provide a Regen Protocol (3-4 simple steps)
3. Include easy meal suggestion based on Defcon level
4. Schedule check-in time

Make it feel like activating a special ability. Include food strategy.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
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
        
        survival_state = self._get_survival_state(user_id)
        
        # Track spending
        if resource_type == 'currency' and amount < 0:
            survival_state.financial.spent_today += abs(amount)
        elif resource_type == 'currency' and amount > 0:
            survival_state.financial.total_balance += amount
        
        prompt = f"""
RESOURCE UPDATE:
Type: {resource_type}
Amount: {'+' if amount > 0 else ''}{amount}
Category: {category}
Current Defcon: {survival_state.financial.defcon_level}

TASK:
1. Acknowledge the resource change
2. If spending, quick tip based on Defcon level
3. Track toward budget goals

Brief and supportive. No financial advice, just tracking.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="vitality",
            mode=ReasoningMode.REFLEX
        ))
        
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict()
            },
            "resources": {
                f"last_{resource_type}": {
                    "amount": amount,
                    "category": category,
                    "timestamp": datetime.now().isoformat()
                }
            }
        }
        self.update_state_cache(user_id, vitality_update)
        
        return AgentResult(
            success=True,
            response=self._sanitize_output(response.content),
            actions_taken=["resource_logged"],
            state_updates=vitality_update
        )

    # =========================================================================
    # HELPER METHODS
    # =========================================================================
    
    def _get_survival_state(self, user_id: str) -> SurvivalState:
        """Get or create survival state for user."""
        if user_id in self._cached_survival_state:
            return self._cached_survival_state[user_id]
        
        # Try to load from database
        user_doc = self.db.get_user_doc(user_id)
        if user_doc:
            try:
                state_json = json.loads(user_doc.get('studentState_json', '{}'))
                survival_data = state_json.get('vitality', {}).get('survival', {})
                if survival_data:
                    survival_state = SurvivalState.from_dict(survival_data)
                    self._cached_survival_state[user_id] = survival_state
                    return survival_state
            except Exception as e:
                print(f"⚠️ Error loading survival state: {e}")
        
        # Create new state
        survival_state = SurvivalState()
        self._cached_survival_state[user_id] = survival_state
        return survival_state
    
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
        
        if steps > 5000:
            components.append(min(steps // 100, 20) + 50)
        
        if components:
            return int(sum(components) / len(components))
        return 50
    
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


# =============================================================================
# ASYNC COMPATIBILITY WRAPPER
# =============================================================================
async def run_vitality_agent(db_helper, payload, context):
    """Async compatibility wrapper for Appwrite function interface."""
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    agent = VitalityAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('type', 'energy_check')
    
    result = await agent.run(user_id, payload, trigger_event)
    
    return context.res.json(result.to_dict())
