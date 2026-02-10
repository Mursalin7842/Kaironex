"""
🧬 VITALITY BRAIN v2.0: SURVIVAL & GROWTH PROTOCOL
====================================================
The Life Logistics Engine brain for Appwrite Functions.

This is the functional interface for the Vitality Agent that handles:
- Financial runway tracking (Defcon System)
- Smart fridge inventory
- Cook vs Order decisions
- Shopping alerts
- Victory Feast rewards
- PROACTIVE MEAL PLANNING (NEW!)

Compatible with Appwrite Functions execution model.
"""

import json
import datetime
import hashlib
import uuid
import base64
import asyncio
from typing import Dict, Any, Optional, List

from ..utils.gemini_client import GeminiClient
from ..config import GEMINI_API_KEY, GEMINI_3_FLASH
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


def _create_thought_signature(user_id, agent, prompt, response, db_helper, context=None):
    """Create and store thought signature - shared helper."""
    try:
        thought_id = f"thought_{uuid.uuid4().hex[:12]}"
        timestamp = datetime.datetime.now().isoformat()
        context_hash = hashlib.sha256(f"{user_id}:{prompt[:200]}:{timestamp}".encode()).hexdigest()[:16]
        
        thought_data = {
            "thought_id": thought_id, 
            "timestamp": timestamp, 
            "agent": agent,
            "context_hash": context_hash,
            "reasoning_trace": [f"Prompt: {prompt[:100]}...", f"Response: {response[:200]}..."],
            "confidence": 0.85, 
            "tool_calls": [], 
            "action_output": response[:500], 
            "parent_signature": ""
        }
        
        db_helper.create_thought_signature(user_id, thought_data)
        db_helper.update_agent_memory_full(
            user_id, 
            thought_sig_dict=thought_data, 
            active_agents=agent, 
            reasoning_mode='HYBRID'
        )
        
        if context: 
            context.log(f"🧠 Thought signature stored: {thought_id}")
        return thought_id
    except Exception as e:
        print(f"❌ Thought signature error: {e}")
        return None


def _get_survival_state(user_id: str, db_helper) -> SurvivalState:
    """Get or create survival state from database."""
    try:
        user_doc = db_helper.get_user_doc(user_id)
        if user_doc:
            state_json = json.loads(user_doc.get('studentState_json', '{}'))
            survival_data = state_json.get('vitality', {}).get('survival', {})
            if survival_data:
                return SurvivalState.from_dict(survival_data)
    except Exception as e:
        print(f"⚠️ Error loading survival state: {e}")
    
    return SurvivalState()


def _save_survival_state(user_id: str, survival_state: SurvivalState, db_helper):
    """Save survival state to database."""
    try:
        vitality_update = {
            "vitality": {
                "survival": survival_state.to_dict(),
                "last_updated": datetime.datetime.now().isoformat()
            }
        }
        db_helper.update_state_cache(user_id, vitality_update)
    except Exception as e:
        print(f"⚠️ Error saving survival state: {e}")


# =============================================================================
# MAIN BRAIN ENTRY POINT
# =============================================================================
async def run_vitality_agent(db_helper, payload, context):
    """
    Vitality Brain v2.0: Life Logistics Engine.
    
    Routes to appropriate handler based on event type.
    """
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})

    context.log(f"🧬 Vitality Brain v2.0 processing for {user_id}")
    db_helper.log_heartbeat(user_id, f"EVENT:VITALITY:{payload.get('type', 'check')}")
    
    event_type = payload.get('type', 'energy_check')
    
    # Route to appropriate handler
    handlers = {
        # New Survival Protocol handlers
        'one_shot_setup': handle_one_shot_setup,
        'fridge_scan': handle_fridge_scan,
        'decision_matrix': handle_decision_matrix,
        'shopping_alert': handle_shopping_alert,
        'unlock_reward': handle_unlock_reward,
        'daily_budget_check': handle_daily_budget_check,
        'defcon_update': handle_defcon_update,
        'cross_agent_sync': handle_cross_agent_sync,
        
        # PROACTIVE MEAL PLANNING (NEW!) - The Action Era
        'proactive_meal_plan': handle_proactive_meal_plan,
        'select_meal_option': handle_select_meal_option,
        'get_todays_meals': handle_get_todays_meals,
        
        # === MARATHON AUTONOMOUS HANDLERS ===
        'marathon_morning_routine': handle_marathon_morning_routine,
        'emergency_fund_withdraw': handle_emergency_fund_withdraw,
        'emergency_fund_deposit': handle_emergency_fund_deposit,
        'savings_status': handle_savings_status,
        'auto_end_of_day': handle_auto_end_of_day,
        
        # Legacy handlers
        'sleep_log': handle_sleep_log,
        'activity_log': handle_activity_log,
        'meal_log': handle_meal_log,
        'energy_check': handle_energy_check,
        'regen_request': handle_regen_request,
        'resource_update': handle_resource_update,
    }
    
    handler = handlers.get(event_type, handle_energy_check)
    return handler(db_helper, payload, context, user_id)


# =============================================================================
# NEW SURVIVAL PROTOCOL HANDLERS
# =============================================================================

def handle_one_shot_setup(db_helper, payload, context, user_id):
    """
    Day Zero One-Shot Setup - Financial Calibration.
    
    Parses financial info using Gemini 3 Vision + Thinking.
    """
    context.log(f"💰 Day Zero Setup for {user_id}")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    
    financial_text = payload.get('financial_info', '')
    image_base64 = payload.get('image_base64')
    
    # Build extraction prompt
    extract_prompt = f"""You are a financial data extractor. Extract the following from user input:

USER INPUT: {financial_text}

Extract and calculate:
1. total_balance: Bank balance (number)
2. fixed_bills: Monthly rent + utilities + subscriptions
3. payday_date: Next payday (YYYY-MM-DD)
4. employment_status: employed/unemployed/student
5. days_until_payday: Days until next payday
6. Calculate: daily_runway = (total_balance - fixed_bills) / days_until_payday

Assign Defcon Level:
- 5: daily_runway > $50 AND employed
- 4: daily_runway $30-50
- 3: daily_runway $15-30
- 2: daily_runway $8-15
- 1: daily_runway < $8 OR unemployed with low runway

Return ONLY JSON:
{{"total_balance": X, "fixed_bills": X, "daily_runway": X, "defcon_level": X, "payday_date": "X", "days_until_payday": X, "employment_status": "X"}}
"""
    
    # Use vision if image provided
    if image_base64:
        try:
            extracted = ai.generate_response_with_image(
                extract_prompt,
                image_base64,
                use_thinking=True,
                json_mode=True
            )
        except:
            extracted = ai.generate_response(extract_prompt, json_mode=True)
    else:
        extracted = ai.generate_response(extract_prompt, json_mode=True)
    
    # Parse response
    try:
        # Clean JSON from response
        import re
        json_match = re.search(r'\{[^{}]+\}', extracted, re.DOTALL)
        if json_match:
            financial_data = json.loads(json_match.group())
        else:
            financial_data = json.loads(extracted)
    except:
        financial_data = {
            "total_balance": 1000,
            "fixed_bills": 500,
            "daily_runway": 20,
            "defcon_level": 3,
            "days_until_payday": 25,
            "employment_status": "student"
        }
    
    # Create survival state
    financial_state = FinancialState(
        total_balance=float(financial_data.get('total_balance', 0)),
        fixed_bills=float(financial_data.get('fixed_bills', 0)),
        daily_runway=float(financial_data.get('daily_runway', 20)),
        defcon_level=int(financial_data.get('defcon_level', 3)),
        payday_date=financial_data.get('payday_date'),
        days_until_payday=int(financial_data.get('days_until_payday', 30)),
        employment_status=financial_data.get('employment_status', 'student')
    )
    
    preferences = UserPreferences(
        favorite_reward=payload.get('favorite_food', 'Pizza'),
        dietary_restrictions=payload.get('dietary_restrictions', [])
    )
    
    survival_state = SurvivalState(
        financial=financial_state,
        preferences=preferences
    )
    
    # Generate response
    defcon_messages = {
        5: "🟢 DEFCON 5 - Abundance Mode! You can enjoy premium choices.",
        4: "🟢 DEFCON 4 - Stable. Balanced spending recommended.",
        3: "🟡 DEFCON 3 - Caution. Value meals and mindful spending.",
        2: "🟠 DEFCON 2 - Critical. Strict budgeting required.",
        1: "🔴 DEFCON 1 - Survival Mode. Austerity protocols engaged."
    }
    
    response_prompt = f"""
Financial setup complete! User's situation:
- Daily Budget: ${financial_state.daily_runway:.2f}
- Defcon Level: {financial_state.defcon_level}
- Status: {defcon_messages.get(financial_state.defcon_level, 'Unknown')}

Give a 2-sentence encouraging message about their financial command center being online.
Use military/gaming terminology. Be supportive.
"""
    
    ai_response = ai.generate_response(response_prompt)
    
    # Create thought signature
    thought_id = _create_thought_signature(
        user_id, "vitality", extract_prompt, ai_response, db_helper, context
    )
    
    # Save state
    _save_survival_state(user_id, survival_state, db_helper)
    
    context.log(f"✅ Financial setup complete. Defcon {financial_state.defcon_level}")
    
    return context.res.json({
        "status": "setup_complete",
        "defcon_level": financial_state.defcon_level,
        "daily_runway": financial_state.daily_runway,
        "survival_state": survival_state.to_dict(),
        "message": ai_response,
        "thought_id": thought_id,
        "actions": ["financial_setup", f"defcon:{financial_state.defcon_level}"]
    })


def handle_fridge_scan(db_helper, payload, context, user_id):
    """Smart Fridge Vision - Analyze fridge contents."""
    context.log(f"🍽️ Fridge scan for {user_id}")
    
    image_base64 = payload.get('image_base64')
    if not image_base64:
        return context.res.json({
            "status": "error",
            "message": "📸 No image received. Please upload a photo of your fridge."
        })
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    # Analyze with vision
    scan_prompt = """Analyze this fridge/pantry image. Identify all food items and estimate:

1. List all ingredients with quantity (full/half/low/almost_empty)
2. Estimate how many days of meals are possible
3. Determine if shopping is urgently needed
4. Suggest 2 meals that can be made

Return JSON only:
{
    "ingredients": [{"name": "eggs", "quantity": "half", "servings": 6}],
    "days_remaining": X,
    "needs_shopping": true/false,
    "meal_suggestions": ["meal1", "meal2"],
    "assessment": "Brief assessment"
}
"""
    
    try:
        result = ai.generate_response_with_image(
            scan_prompt,
            image_base64,
            use_thinking=True
        )
        
        # Parse JSON
        import re
        json_match = re.search(r'\{[\s\S]*\}', result)
        if json_match:
            inventory_data = json.loads(json_match.group())
        else:
            inventory_data = {"ingredients": [], "days_remaining": 0, "needs_shopping": True}
    except Exception as e:
        context.log(f"⚠️ Fridge scan error: {e}")
        inventory_data = {"ingredients": [], "days_remaining": 0, "needs_shopping": True}
    
    # Update survival state
    survival_state.inventory = FridgeInventory(
        ingredients=inventory_data.get('ingredients', []),
        days_remaining=inventory_data.get('days_remaining', 0),
        last_scan=datetime.datetime.now().isoformat(),
        needs_shopping=inventory_data.get('needs_shopping', True)
    )
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Generate response
    days = inventory_data.get('days_remaining', 0)
    if days <= 1:
        message = f"⚠️ **Supply Critical!** ~{days} day(s) of food detected. Resupply recommended!"
    else:
        message = f"📦 Inventory scanned! You can survive ~{days} days on current supplies."
    
    # Add meal suggestions
    meals = inventory_data.get('meal_suggestions', [])
    if meals:
        message += f"\n\n🍳 You can make: {', '.join(meals)}"
    
    # Create intervention if critical
    if days <= 1:
        db_helper.create_intervention(
            user_id, "LOW_FOOD_SUPPLY",
            f"🍽️ Food supply critical! Only {days} day(s) remaining.",
            strategy="URGENT"
        )
    
    return context.res.json({
        "status": "scan_complete",
        "inventory": inventory_data,
        "days_remaining": days,
        "needs_shopping": inventory_data.get('needs_shopping'),
        "message": message,
        "actions": ["fridge_scanned", f"days:{days}"]
    })


def handle_decision_matrix(db_helper, payload, context, user_id):
    """Meal Time Arbitrator - Cook vs Order Decision."""
    context.log(f"🍳 Decision matrix for {user_id}")
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    protocol = SurvivalProtocol()
    
    # Build context
    ctx = MealDecisionContext(
        time_available_mins=payload.get('time_available', 60),
        energy_level=payload.get('energy_level', 50),
        daily_budget_remaining=payload.get('budget_remaining', survival_state.financial.daily_runway),
        defcon_level=survival_state.financial.defcon_level,
        ingredients_available=[i.get('name', '') for i in survival_state.inventory.ingredients],
        near_restaurant=payload.get('near_restaurant', False),
        schedule_pressure=payload.get('schedule_pressure', 'normal')
    )
    
    # Check for urgent job hunt (override to austerity)
    campaign_state = payload.get('campaign_state', {})
    if campaign_state.get('status') == 'Urgent_Job_Hunt':
        ctx.defcon_level = DefconLevel.DEFCON_1_SURVIVAL
    
    # Make decision
    decision = protocol.make_meal_decision(ctx)
    
    # Save decision
    survival_state.last_decision = decision
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Generate explanation
    explain_prompt = f"""
Meal decision made:
- Action: {decision['action']}
- Reason: {decision['reason']}
- Suggestion: {decision['suggestion']}
- Budget allocated: ${decision.get('budget_allocation', 0):.2f}

Give a 2-sentence tactical briefing announcing this decision. Military/gaming style. Be decisive and supportive.
"""
    
    explanation = ai.generate_response(explain_prompt)
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "meal_decision", explanation, db_helper, context
    )
    
    return context.res.json({
        "status": "decision_made",
        "decision": decision,
        "message": explanation,
        "thought_id": thought_id,
        "context": {
            "time_mins": ctx.time_available_mins,
            "energy": ctx.energy_level,
            "budget": ctx.daily_budget_remaining,
            "defcon": ctx.defcon_level
        },
        "actions": [f"decision:{decision['action']}"]
    })


def handle_shopping_alert(db_helper, payload, context, user_id):
    """Just-in-Time Shopping Alert."""
    context.log(f"🛒 Shopping alert for {user_id}")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    survival_state = _get_survival_state(user_id, db_helper)
    
    location = payload.get('location', 'Grocery Store')
    
    # Check if needed
    if survival_state.inventory.days_remaining > 2 and not payload.get('force_alert'):
        return context.res.json({
            "status": "not_needed",
            "message": f"📦 You're good for {survival_state.inventory.days_remaining} days. No urgent shopping.",
            "shopping_needed": False
        })
    
    # Generate shopping list
    shopping_list = protocol.generate_shopping_list(
        defcon_level=survival_state.financial.defcon_level,
        current_inventory=survival_state.inventory.ingredients,
        dietary_restrictions=survival_state.preferences.dietary_restrictions
    )
    
    # Get time-energy adjustment
    time_energy = protocol.get_time_energy_adjustment(
        schedule_pressure=payload.get('schedule_pressure', 'normal'),
        energy_level=payload.get('energy_level', 50)
    )
    
    # Create intervention
    voice_message = shopping_list['voice_alert']
    db_helper.create_intervention(
        user_id, "SHOPPING_ALERT",
        f"🛒 {voice_message}\n\n{time_energy['message']}",
        strategy="HELPFUL"
    )
    
    survival_state.last_shopping_alert = datetime.datetime.now().isoformat()
    _save_survival_state(user_id, survival_state, db_helper)
    
    return context.res.json({
        "status": "alert_triggered",
        "shopping_list": shopping_list,
        "voice_message": voice_message,
        "time_energy": time_energy,
        "trigger_voice_call": True,
        "message": f"🛒 Shopping alert! You're near {location}. {voice_message}",
        "actions": ["shopping_alert", f"defcon_list:{survival_state.financial.defcon_level}"]
    })


def handle_unlock_reward(db_helper, payload, context, user_id):
    """Victory Feast Protocol - Reward for achievements."""
    context.log(f"🏆 Victory Feast for {user_id}")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    survival_state = _get_survival_state(user_id, db_helper)
    
    event_type = payload.get('event_type', 'CAREER_WIN')
    event_details = payload.get('event_details', 'You achieved something amazing!')
    
    # Unlock feast
    feast = protocol.unlock_victory_feast(
        event_type=event_type,
        event_details=event_details,
        favorite_reward=survival_state.preferences.favorite_reward,
        current_budget=survival_state.financial.daily_runway
    )
    
    # Update state
    survival_state.reward_unlocked = True
    survival_state.reward_amount = feast['reward_amount']
    survival_state.financial.budget_override = feast['reward_amount']
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Create celebration intervention
    db_helper.create_intervention(
        user_id, "VICTORY_FEAST",
        feast['celebration_message'],
        strategy="CELEBRATION"
    )
    
    context.log(f"🎉 Victory Feast unlocked: ${feast['reward_amount']} for {survival_state.preferences.favorite_reward}")
    
    return context.res.json({
        "status": "feast_unlocked",
        "feast": feast,
        "voice_message": feast['voice_message'],
        "trigger_voice_call": True,
        "message": feast['celebration_message'],
        "actions": ["victory_feast", f"reward:{feast['reward_amount']}"]
    })


def handle_daily_budget_check(db_helper, payload, context, user_id):
    """
    ☀️ MORNING RITUAL: Budget Check + Proactive Meal Planning
    
    This is the daily kickoff - we calculate budget AND plan all meals.
    The Action Era: by the time user wakes up, everything is planned.
    """
    context.log(f"☀️ Morning ritual for {user_id}: Budget + Meals")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    survival_state = _get_survival_state(user_id, db_helper)
    
    spent_yesterday = payload.get('spent_yesterday', survival_state.financial.spent_yesterday)
    survival_state.financial.spent_yesterday = spent_yesterday
    survival_state.financial.spent_today = 0  # Reset daily
    
    # Calculate budget
    budget_info = protocol.calculate_daily_budget(
        financial=survival_state.financial,
        spent_yesterday=spent_yesterday
    )
    
    # Update the daily runway for meal planning
    survival_state.financial.daily_runway = budget_info['today_budget']
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    # 🍽️ PROACTIVE: Generate today's meal plan automatically!
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    generate_meals = payload.get('generate_meals', True)  # Default: yes, plan meals
    
    meal_plan = None
    meal_plan_summary = ""
    
    if generate_meals:
        try:
            # Generate meal plan proactively
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            meal_plan = loop.run_until_complete(
                protocol.generate_proactive_meal_plan(
                    financial=survival_state.financial,
                    inventory=survival_state.inventory,
                    preferences=survival_state.preferences,
                    schedule_pressure=payload.get('schedule_pressure', 'normal'),
                    energy_level=payload.get('energy_level', 50)
                )
            )
            loop.close()
            
            # Store the plan
            survival_state.daily_meal_plan = meal_plan
            _save_survival_state(user_id, survival_state, db_helper)
            
            # Summary for response
            total_options = (
                len(meal_plan.breakfast_options) +
                len(meal_plan.lunch_options) +
                len(meal_plan.dinner_options)
            )
            meal_plan_summary = f" I've prepared {total_options} meal options for today!"
            
        except Exception as e:
            context.log(f"⚠️ Meal plan error: {e}")
            meal_plan_summary = " Meal options pending - check later."
    
    # Generate briefing
    briefing_prompt = f"""
☀️ MORNING COMMAND BRIEFING

FINANCIAL SITREP:
- Today's Operational Budget: ${budget_info['today_budget']:.2f}
- Yesterday's Expenditure: ${budget_info['yesterday_spent']:.2f}
- Defcon Status: {budget_info['defcon_level']}
- Mode: {budget_info['message']}
{f"- Meal Options Ready: Yes ({total_options} choices)" if meal_plan else ""}

Give a 2-sentence morning tactical briefing. Start with "Good morning, Commander!"
Include the budget and mention that meal options are waiting.
Energizing, military/gaming style. End with a power phrase.
"""
    
    briefing = ai.generate_response(briefing_prompt)
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "morning_ritual", briefing, db_helper, context
    )
    
    response_data = {
        "status": "morning_ritual_complete",
        "budget_info": budget_info,
        "today_budget": budget_info['today_budget'],
        "defcon_level": budget_info['defcon_level'],
        "message": briefing,
        "thought_id": thought_id,
        "actions": ["daily_budget", "morning_ritual"]
    }
    
    # Include meal plan if generated
    if meal_plan:
        def format_options(options):
            return [{"id": o.option_id, "name": o.name, "cost": o.estimated_cost, "type": o.action_type} for o in options]
        
        response_data["meal_plan"] = {
            "plan_id": meal_plan.plan_id,
            "breakfast_options": format_options(meal_plan.breakfast_options),
            "lunch_options": format_options(meal_plan.lunch_options),
            "dinner_options": format_options(meal_plan.dinner_options),
            "total_options": total_options
        }
        response_data["actions"].append("meals_planned")
    
    return context.res.json(response_data)


def handle_defcon_update(db_helper, payload, context, user_id):
    """Update Defcon Level."""
    context.log(f"🚨 Defcon update for {user_id}")
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    # Update fields
    if 'total_balance' in payload:
        survival_state.financial.total_balance = float(payload['total_balance'])
    if 'spent_today' in payload:
        survival_state.financial.spent_today = float(payload['spent_today'])
    if 'employment_status' in payload:
        survival_state.financial.employment_status = payload['employment_status']
    
    # Recalculate
    daily = survival_state.financial.daily_runway
    employed = survival_state.financial.employment_status != 'unemployed'
    
    old_defcon = survival_state.financial.defcon_level
    
    if daily > 50 and employed:
        new_defcon = 5
    elif daily >= 30:
        new_defcon = 4
    elif daily >= 15:
        new_defcon = 3
    elif daily >= 8:
        new_defcon = 2
    else:
        new_defcon = 1
    
    survival_state.financial.defcon_level = new_defcon
    _save_survival_state(user_id, survival_state, db_helper)
    
    level_changed = old_defcon != new_defcon
    
    if level_changed:
        direction = "improved" if new_defcon > old_defcon else "decreased"
        message = f"🚨 DEFCON CHANGE: Level {old_defcon} → {new_defcon} ({direction})"
    else:
        message = f"✅ Defcon {new_defcon} - Status unchanged"
    
    return context.res.json({
        "status": "defcon_updated",
        "defcon_level": new_defcon,
        "old_defcon": old_defcon,
        "level_changed": level_changed,
        "daily_runway": daily,
        "message": message,
        "actions": [f"defcon:{new_defcon}"]
    })


def handle_cross_agent_sync(db_helper, payload, context, user_id):
    """Sync with Campaign, Study, and Radius agents."""
    context.log(f"🔄 Cross-agent sync for {user_id}")
    
    survival_state = _get_survival_state(user_id, db_helper)
    synced = []
    
    # Campaign sync (Financial Defcon)
    campaign_state = payload.get('campaign_state', {})
    if campaign_state.get('status') == 'Urgent_Job_Hunt':
        survival_state.financial.defcon_level = 1
        synced.append("campaign:austerity")
    
    # Study sync (Time-Energy Matrix)
    study_state = payload.get('study_state', {})
    if study_state.get('exam_mode'):
        synced.append("study:exam_mode")
    
    # Radius sync (Location)
    radius_state = payload.get('radius_state', {})
    location = radius_state.get('current_location', '').lower()
    grocery_keywords = ["grocery", "supermarket", "market", "store", "walmart", "target"]
    if any(kw in location for kw in grocery_keywords):
        if survival_state.inventory.needs_shopping:
            synced.append("radius:shopping_trigger")
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    return context.res.json({
        "status": "synced",
        "synced_agents": synced,
        "message": f"🔄 Synced: {', '.join(synced) or 'No changes'}",
        "actions": ["cross_agent_sync"] + synced
    })


# =============================================================================
# 🧠 PROACTIVE MEAL PLANNING - THE ACTION ERA
# =============================================================================
# These handlers transform Vitality from reactive to PROACTIVE.
# The agent plans meals BEFORE the user asks. User just SELECTS.

def handle_proactive_meal_plan(db_helper, payload, context, user_id):
    """
    🍽️ PROACTIVE MEAL PLANNING ENGINE
    
    Automatically generates a complete daily meal plan with options.
    Called proactively each morning OR triggered by the user.
    
    This is the Action Era - no more "what do you want to eat?"
    We plan, user selects.
    """
    context.log(f"🍽️ Proactive Meal Planning for {user_id}")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    survival_state = _get_survival_state(user_id, db_helper)
    
    # Get context from other agents
    schedule_pressure = payload.get('schedule_pressure', 'normal')
    energy_level = payload.get('energy_level', 50)
    
    # Check if we already have a plan for today
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    existing_plan = survival_state.daily_meal_plan
    
    if existing_plan and existing_plan.date == today and not payload.get('force_regenerate'):
        context.log(f"📋 Using existing meal plan for {today}")
        return context.res.json({
            "status": "existing_plan",
            "plan": existing_plan.to_dict(),
            "message": "🍽️ Your meal plan for today is ready! Select what sounds good.",
            "actions": ["existing_plan_returned"]
        })
    
    # Generate new plan using async
    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        meal_plan = loop.run_until_complete(
            protocol.generate_proactive_meal_plan(
                financial=survival_state.financial,
                inventory=survival_state.inventory,
                preferences=survival_state.preferences,
                schedule_pressure=schedule_pressure,
                energy_level=energy_level
            )
        )
        loop.close()
    except Exception as e:
        context.log(f"⚠️ Async error: {e}, using fallback")
        # Fallback to synchronous generation
        meal_plan = protocol._generate_fallback_meal_plan(
            plan_id=f"meal_plan_{uuid.uuid4().hex[:8]}",
            date=today,
            daily_budget=survival_state.financial.daily_runway,
            defcon_level=survival_state.financial.defcon_level,
            inventory=survival_state.inventory
        )
    
    # Store the plan
    survival_state.daily_meal_plan = meal_plan
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Generate AI summary
    total_options = (
        len(meal_plan.breakfast_options) +
        len(meal_plan.lunch_options) +
        len(meal_plan.dinner_options)
    )
    
    cheapest_total = (
        min([o.estimated_cost for o in meal_plan.breakfast_options], default=0) +
        min([o.estimated_cost for o in meal_plan.lunch_options], default=0) +
        min([o.estimated_cost for o in meal_plan.dinner_options], default=0)
    )
    
    summary_prompt = f"""
Today's meal plan generated:
- Budget: ${meal_plan.total_budget:.2f}
- Defcon: {meal_plan.defcon_level}
- Options: {total_options} total
- Cheapest path: ${cheapest_total:.2f}

Give a 2-sentence tactical briefing. Military/gaming style. 
Mention the budget and that they have OPTIONS ready.
End with "Select your fuel, commander!"
"""
    
    summary = ai.generate_response(summary_prompt)
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "proactive_meal_plan", summary, db_helper, context
    )
    
    # Format options for response
    def format_options(options: List[MealOption]) -> List[Dict]:
        return [{
            "id": o.option_id,
            "name": o.name,
            "description": o.description,
            "type": o.action_type,
            "cost": f"${o.estimated_cost:.2f}",
            "time": f"{o.prep_time_mins}min",
            "nutrition": o.nutrition_score,
            "energy_needed": o.energy_requirement
        } for o in options]
    
    return context.res.json({
        "status": "plan_generated",
        "plan_id": meal_plan.plan_id,
        "date": meal_plan.date,
        "total_budget": meal_plan.total_budget,
        "defcon_level": meal_plan.defcon_level,
        "breakfast": {
            "options": format_options(meal_plan.breakfast_options),
            "selected": meal_plan.selected_breakfast
        },
        "lunch": {
            "options": format_options(meal_plan.lunch_options),
            "selected": meal_plan.selected_lunch
        },
        "dinner": {
            "options": format_options(meal_plan.dinner_options),
            "selected": meal_plan.selected_dinner
        },
        "budget_remaining": meal_plan.budget_remaining,
        "spent_today": meal_plan.spent_today,
        "message": summary,
        "thought_id": thought_id,
        "actions": ["meal_plan_generated", f"options:{total_options}"]
    })


def handle_select_meal_option(db_helper, payload, context, user_id):
    """
    🎯 SELECT A MEAL OPTION
    
    User selects their choice → budget updates → fridge inventory updates.
    This is the Action Era - user selects, system handles logistics.
    """
    context.log(f"🎯 Meal Selection for {user_id}")
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    option_id = payload.get('option_id')
    if not option_id:
        return context.res.json({
            "status": "error",
            "message": "❌ No option_id provided. Which meal did you select?",
            "actions": ["selection_error"]
        })
    
    meal_plan = survival_state.daily_meal_plan
    if not meal_plan:
        return context.res.json({
            "status": "error",
            "message": "📋 No meal plan for today! Let me generate one first.",
            "trigger_action": "proactive_meal_plan",
            "actions": ["no_plan_exists"]
        })
    
    # Find and select the option
    option = meal_plan.get_option_by_id(option_id)
    if not option:
        return context.res.json({
            "status": "error",
            "message": f"❌ Option '{option_id}' not found in today's plan.",
            "actions": ["option_not_found"]
        })
    
    # Execute selection
    success, message, cost_deducted = meal_plan.select_option(option_id)
    
    if not success:
        return context.res.json({
            "status": "budget_exceeded",
            "message": f"💰 {message}",
            "budget_remaining": meal_plan.budget_remaining,
            "option_cost": option.estimated_cost,
            "actions": ["selection_rejected", "over_budget"]
        })
    
    # Update fridge inventory if cooking
    ingredients_consumed = []
    if option.action_type == "COOK" and option.ingredients_used_from_fridge:
        for ingredient in option.ingredients_used_from_fridge:
            # Remove or reduce from inventory
            for inv_item in survival_state.inventory.ingredients:
                if ingredient.lower() in inv_item.get('name', '').lower():
                    inv_item['quantity'] = 'reduced'
                    ingredients_consumed.append(ingredient)
        
        # Recalculate days remaining (reduce by 1 when cooking a full meal)
        if survival_state.inventory.days_remaining > 0 and len(ingredients_consumed) >= 2:
            survival_state.inventory.days_remaining = max(0, survival_state.inventory.days_remaining - 1)
    
    # Save updated state
    survival_state.daily_meal_plan = meal_plan
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Generate AI confirmation
    confirm_prompt = f"""
User selected: {option.name} ({option.action_type})
Cost: ${option.estimated_cost:.2f}
Budget remaining: ${meal_plan.budget_remaining:.2f}
Meal type: {option.meal_type}

Give a 1-sentence enthusiastic confirmation.
Include the remaining budget.
Gaming/military style - like a commander approving a tactical decision.
"""
    
    confirmation = ai.generate_response(confirm_prompt)
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "select_meal", confirmation, db_helper, context
    )
    
    # Track spending
    survival_state.financial.spent_today += cost_deducted
    _save_survival_state(user_id, survival_state, db_helper)
    
    return context.res.json({
        "status": "selected",
        "meal_type": option.meal_type,
        "selected": {
            "id": option.option_id,
            "name": option.name,
            "type": option.action_type,
            "cost": option.estimated_cost
        },
        "cost_deducted": cost_deducted,
        "budget_remaining": meal_plan.budget_remaining,
        "spent_today": meal_plan.spent_today,
        "ingredients_consumed": ingredients_consumed,
        "progress": {
            "breakfast": meal_plan.selected_breakfast is not None,
            "lunch": meal_plan.selected_lunch is not None,
            "dinner": meal_plan.selected_dinner is not None
        },
        "message": confirmation,
        "thought_id": thought_id,
        "actions": [f"meal_selected:{option.meal_type}", f"spent:${cost_deducted:.2f}"]
    })


def handle_get_todays_meals(db_helper, payload, context, user_id):
    """
    📋 GET TODAY'S MEAL PLAN STATUS
    
    Quick view of what's planned and what's been selected.
    Useful for reminders and check-ins.
    """
    context.log(f"📋 Getting meal status for {user_id}")
    
    survival_state = _get_survival_state(user_id, db_helper)
    meal_plan = survival_state.daily_meal_plan
    
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    if not meal_plan or meal_plan.date != today:
        return context.res.json({
            "status": "no_plan",
            "message": "📋 No meal plan for today yet! Let me generate one.",
            "trigger_action": "proactive_meal_plan",
            "actions": ["no_plan"]
        })
    
    # Build status
    def meal_status(selected_id, options, meal_type):
        if selected_id:
            for opt in options:
                if opt.option_id == selected_id:
                    return {
                        "status": "selected",
                        "selected": opt.name,
                        "cost": opt.estimated_cost,
                        "type": opt.action_type
                    }
        return {
            "status": "pending",
            "options_count": len(options),
            "options": [{"id": o.option_id, "name": o.name, "cost": o.estimated_cost} for o in options]
        }
    
    breakfast_status = meal_status(meal_plan.selected_breakfast, meal_plan.breakfast_options, "breakfast")
    lunch_status = meal_status(meal_plan.selected_lunch, meal_plan.lunch_options, "lunch")
    dinner_status = meal_status(meal_plan.selected_dinner, meal_plan.dinner_options, "dinner")
    
    all_selected = all([
        breakfast_status["status"] == "selected",
        lunch_status["status"] == "selected",
        dinner_status["status"] == "selected"
    ])
    
    pending_meals = [
        m for m, s in [("breakfast", breakfast_status), ("lunch", lunch_status), ("dinner", dinner_status)]
        if s["status"] == "pending"
    ]
    
    # Generate message
    if all_selected:
        message = f"✅ All meals planned! Budget used: ${meal_plan.spent_today:.2f} / ${meal_plan.total_budget:.2f}"
    elif pending_meals:
        message = f"🍽️ Pending: {', '.join(pending_meals)}. ${meal_plan.budget_remaining:.2f} remaining."
    else:
        message = "📋 Today's meals are ready for selection!"
    
    return context.res.json({
        "status": "ok",
        "date": meal_plan.date,
        "defcon_level": meal_plan.defcon_level,
        "breakfast": breakfast_status,
        "lunch": lunch_status,
        "dinner": dinner_status,
        "budget": {
            "total": meal_plan.total_budget,
            "spent": meal_plan.spent_today,
            "remaining": meal_plan.budget_remaining
        },
        "all_selected": all_selected,
        "pending_meals": pending_meals,
        "message": message,
        "actions": ["meal_status_retrieved"]
    })


# =============================================================================
# 🏃 MARATHON AUTONOMOUS HANDLERS - The Action Era
# =============================================================================
# These handlers run PROACTIVELY without user request.
# The system thinks ahead, plans, and only asks for confirmation.

def handle_marathon_morning_routine(db_helper, payload, context, user_id):
    """
    🌅 MARATHON MORNING ROUTINE - AUTONOMOUS
    
    Triggered automatically each morning. Does everything:
    1. Calculate today's budget
    2. Check savings/emergency fund status
    3. Generate proactive meal plan
    4. Check fridge status
    5. Create daily briefing
    
    This is THE Marathon Agent - runs without asking.
    """
    context.log(f"🌅 Marathon Morning Routine for {user_id}")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    survival_state = _get_survival_state(user_id, db_helper)
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    morning_report = {
        "date": today,
        "user_id": user_id,
        "status": "marathon_complete",
        "components": []
    }
    
    # === 1. RESET DAILY SPENDING & CALCULATE BUDGET ===
    yesterday_spent = survival_state.financial.spent_today
    survival_state.financial.spent_yesterday = yesterday_spent
    survival_state.financial.spent_today = 0.0
    
    budget_info = protocol.calculate_daily_budget(
        financial=survival_state.financial,
        spent_yesterday=yesterday_spent
    )
    morning_report["budget"] = budget_info
    morning_report["components"].append("budget_calculated")
    
    # === 2. AUTO-SAVE FROM YESTERDAY'S SURPLUS ===
    surplus = max(0, survival_state.financial.daily_runway - yesterday_spent)
    saved_amount = 0.0
    if surplus > 0 and survival_state.financial.auto_save_percentage > 0:
        saved_amount = survival_state.financial.auto_save(surplus)
        morning_report["saved_today"] = saved_amount
        morning_report["components"].append("auto_saved")
    
    # === 3. CHECK EMERGENCY FUND STATUS ===
    emergency_status = {
        "balance": survival_state.financial.emergency_fund,
        "savings_balance": survival_state.financial.savings_balance,
        "savings_goal": survival_state.financial.savings_goal,
        "progress_percent": min(100, (survival_state.financial.savings_balance / max(1, survival_state.financial.savings_goal)) * 100),
        "streak_days": survival_state.financial.savings_streak_days
    }
    morning_report["emergency_fund"] = emergency_status
    morning_report["components"].append("emergency_checked")
    
    # === 4. GENERATE PROACTIVE MEAL PLAN ===
    try:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        meal_plan = loop.run_until_complete(
            protocol.generate_proactive_meal_plan(
                financial=survival_state.financial,
                inventory=survival_state.inventory,
                preferences=survival_state.preferences,
                schedule_pressure=payload.get('schedule_pressure', 'normal'),
                energy_level=payload.get('energy_level', 70)
            )
        )
        loop.close()
        survival_state.daily_meal_plan = meal_plan
        morning_report["meal_plan_id"] = meal_plan.plan_id
        morning_report["meal_options_count"] = (
            len(meal_plan.breakfast_options) +
            len(meal_plan.lunch_options) +
            len(meal_plan.dinner_options)
        )
        morning_report["components"].append("meals_planned")
    except Exception as e:
        context.log(f"⚠️ Meal plan error: {e}")
        morning_report["meal_plan_error"] = str(e)
    
    # === 5. CHECK FRIDGE STATUS ===
    fridge_days = survival_state.inventory.days_remaining
    morning_report["fridge_days"] = fridge_days
    if fridge_days <= 1:
        morning_report["shopping_needed"] = True
        morning_report["components"].append("shopping_alert")
    
    # Save state
    _save_survival_state(user_id, survival_state, db_helper)
    
    # === 6. GENERATE AI BRIEFING ===
    briefing_prompt = f"""
MARATHON MORNING BRIEFING - Day Start

FINANCIAL STATUS:
- Today's Budget: ${budget_info['today_budget']:.2f}
- Defcon Level: {survival_state.financial.defcon_level}
- Emergency Fund: ${emergency_status['balance']:.2f}
- Savings Progress: {emergency_status['progress_percent']:.0f}%
- Saved Today: ${saved_amount:.2f}

LOGISTICS:
- Fridge Supply: {fridge_days} days
- Meals Planned: {morning_report.get('meal_options_count', 0)} options ready
- Shopping Needed: {'Yes - Low supply!' if fridge_days <= 1 else 'No'}

Generate a 3-sentence morning briefing:
1. Budget status (military style)
2. Savings achievement (if saved) or encouragement
3. Meals ready to select

Make it feel like a tactical morning briefing. Energizing but concise.
End with "Day's mission is clear. Execute!"
"""
    
    briefing = ai.generate_response(briefing_prompt)
    morning_report["briefing"] = briefing
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "marathon_morning", briefing, db_helper, context
    )
    morning_report["thought_id"] = thought_id
    
    # Create intervention for push notification
    db_helper.create_intervention(
        user_id, "MORNING_BRIEFING",
        f"🌅 MORNING BRIEFING\n\n{briefing}",
        strategy="ENERGETIC"
    )
    
    return context.res.json(morning_report)


def handle_emergency_fund_withdraw(db_helper, payload, context, user_id):
    """
    🆘 EMERGENCY FUND WITHDRAWAL
    
    For unexpected expenses: sick, accident, urgent needs.
    Requires reason tracking for financial awareness.
    """
    context.log(f"🆘 Emergency withdrawal for {user_id}")
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    amount = payload.get('amount', 0)
    reason = payload.get('reason', 'Unexpected expense')
    category = payload.get('category', 'other')  # sick, accident, urgent, other
    
    if amount <= 0:
        return context.res.json({
            "status": "error",
            "message": "❌ Invalid withdrawal amount.",
            "actions": ["invalid_amount"]
        })
    
    # Attempt withdrawal
    success, message = survival_state.financial.withdraw_emergency(amount, reason)
    
    if not success:
        return context.res.json({
            "status": "insufficient",
            "message": f"⚠️ {message}",
            "emergency_fund_balance": survival_state.financial.emergency_fund,
            "requested_amount": amount,
            "actions": ["withdrawal_rejected"]
        })
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Generate supportive message
    prompt = f"""
Emergency fund used: ${amount:.2f} for {reason} ({category}).
Remaining emergency fund: ${survival_state.financial.emergency_fund:.2f}

Give a 2-sentence supportive message:
1. Acknowledge the emergency - this is why we save
2. Encourage rebuilding the fund

Be compassionate, not judgmental. Life happens.
"""
    
    response = ai.generate_response(prompt)
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "emergency_withdraw", response, db_helper, context
    )
    
    return context.res.json({
        "status": "withdrawn",
        "amount": amount,
        "reason": reason,
        "category": category,
        "remaining_balance": survival_state.financial.emergency_fund,
        "message": response,
        "thought_id": thought_id,
        "actions": ["emergency_used", f"amount:${amount:.2f}"]
    })


def handle_emergency_fund_deposit(db_helper, payload, context, user_id):
    """
    💰 EMERGENCY FUND DEPOSIT
    
    Manual deposit to emergency/savings fund.
    """
    context.log(f"💰 Emergency deposit for {user_id}")
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    amount = payload.get('amount', 0)
    fund_type = payload.get('fund_type', 'emergency')  # emergency or savings
    
    if amount <= 0:
        return context.res.json({
            "status": "error",
            "message": "❌ Invalid deposit amount.",
            "actions": ["invalid_amount"]
        })
    
    if fund_type == 'emergency':
        survival_state.financial.emergency_fund += amount
        new_balance = survival_state.financial.emergency_fund
    else:
        survival_state.financial.savings_balance += amount
        new_balance = survival_state.financial.savings_balance
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Calculate progress
    progress = (survival_state.financial.savings_balance / max(1, survival_state.financial.savings_goal)) * 100
    
    prompt = f"""
Deposit made: ${amount:.2f} to {fund_type} fund.
New balance: ${new_balance:.2f}
Savings progress: {progress:.0f}%

Give a 1-sentence celebration. Make them feel good about saving.
"""
    
    response = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "deposited",
        "amount": amount,
        "fund_type": fund_type,
        "new_balance": new_balance,
        "savings_progress": progress,
        "message": response,
        "actions": ["deposit_made", f"amount:${amount:.2f}"]
    })


def handle_savings_status(db_helper, payload, context, user_id):
    """
    📊 SAVINGS STATUS CHECK
    
    View savings progress, emergency fund, and streak.
    """
    context.log(f"📊 Savings status for {user_id}")
    
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    emergency = survival_state.financial.emergency_fund
    savings = survival_state.financial.savings_balance
    goal = survival_state.financial.savings_goal
    streak = survival_state.financial.savings_streak_days
    progress = min(100, (savings / max(1, goal)) * 100)
    
    prompt = f"""
SAVINGS STATUS:
- Emergency Fund: ${emergency:.2f}
- Savings Balance: ${savings:.2f}
- Savings Goal: ${goal:.2f}
- Progress: {progress:.0f}%
- Savings Streak: {streak} days

Give a 2-sentence status report:
1. Current standing (encouraging)
2. Next milestone or encouragement

Gaming achievement style.
"""
    
    response = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "ok",
        "emergency_fund": emergency,
        "savings_balance": savings,
        "savings_goal": goal,
        "progress_percent": progress,
        "streak_days": streak,
        "message": response,
        "actions": ["savings_checked"]
    })


def handle_auto_end_of_day(db_helper, payload, context, user_id):
    """
    🌙 AUTO END OF DAY - MARATHON
    
    Triggered automatically at end of day:
    1. Calculate surplus/deficit
    2. Auto-save if surplus
    3. Update meal statistics
    4. Prepare for tomorrow
    """
    context.log(f"🌙 Auto End of Day for {user_id}")
    
    ai = GeminiClient()
    protocol = SurvivalProtocol()
    survival_state = _get_survival_state(user_id, db_helper)
    
    budget = survival_state.financial.daily_runway
    spent = survival_state.financial.spent_today
    surplus = max(0, budget - spent)
    deficit = max(0, spent - budget)
    
    # Auto-save if surplus
    saved = 0.0
    if surplus > 0:
        saved = survival_state.financial.auto_save(surplus)
    else:
        survival_state.financial.savings_streak_days = 0  # Reset streak
    
    # Count meals selected
    meals_selected = 0
    if survival_state.daily_meal_plan:
        if survival_state.daily_meal_plan.selected_breakfast:
            meals_selected += 1
        if survival_state.daily_meal_plan.selected_lunch:
            meals_selected += 1
        if survival_state.daily_meal_plan.selected_dinner:
            meals_selected += 1
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    prompt = f"""
END OF DAY REPORT:
- Budget: ${budget:.2f}
- Spent: ${spent:.2f}
- {'Surplus' if surplus > 0 else 'Deficit'}: ${surplus if surplus > 0 else deficit:.2f}
- Auto-Saved: ${saved:.2f}
- Meals Tracked: {meals_selected}/3
- Savings Streak: {survival_state.financial.savings_streak_days} days

Generate a 2-sentence end-of-day summary:
1. Budget performance
2. Savings achievement or encouragement for tomorrow

Calm, reflective tone. Gaming day-complete style.
"""
    
    response = ai.generate_response(prompt)
    
    thought_id = _create_thought_signature(
        user_id, "vitality", "end_of_day", response, db_helper, context
    )
    
    return context.res.json({
        "status": "day_complete",
        "daily_budget": budget,
        "spent": spent,
        "surplus": surplus,
        "deficit": deficit,
        "auto_saved": saved,
        "meals_tracked": meals_selected,
        "savings_streak": survival_state.financial.savings_streak_days,
        "message": response,
        "thought_id": thought_id,
        "actions": ["day_complete", f"saved:${saved:.2f}"]
    })


# =============================================================================
# LEGACY HANDLERS (Updated with survival awareness)
# =============================================================================

def handle_sleep_log(db_helper, payload, context, user_id):
    """Handle sleep data."""
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    sleep_hours = payload.get('sleep_hours', 0)
    quality = payload.get('quality', 'unknown')
    
    # Calculate energy
    energy = min(sleep_hours / 8.0, 1.0) * 100
    quality_mult = {'good': 1.0, 'fair': 0.8, 'poor': 0.6}.get(quality, 0.85)
    energy = int(energy * quality_mult)
    
    regen_needed = sleep_hours < 5 or quality == 'poor'
    
    prompt = f"User slept {sleep_hours}h ({quality}). Energy: {energy}%. Give 1 encouraging sentence about recharge cycle. Gaming terminology."
    ai_response = ai.generate_response(prompt)
    
    thought_id = _create_thought_signature(user_id, "vitality", prompt, ai_response, db_helper, context)
    
    if regen_needed:
        db_helper.create_intervention(user_id, "LOW_SLEEP", ai_response, strategy="EMPATHY")
    
    vitality_update = {
        "vitality": {
            "energy_bar": energy,
            "regen_mode": regen_needed,
            "last_recharge": {"duration": sleep_hours, "quality": quality},
            "survival": survival_state.to_dict()
        }
    }
    db_helper.update_state_cache(user_id, vitality_update)
    
    return context.res.json({
        "status": "sleep_logged",
        "energy": energy,
        "regen_mode": regen_needed,
        "message": ai_response,
        "thought_id": thought_id
    })


def handle_activity_log(db_helper, payload, context, user_id):
    """Handle activity data."""
    ai = GeminiClient()
    
    steps = payload.get('steps', 0)
    workout_type = payload.get('workout_type')
    duration = payload.get('duration_minutes', 0)
    
    xp = min(steps // 100, 100) + min(duration * 2, 60)
    
    prompt = f"User activity: {steps:,} steps, {duration}min {workout_type or 'training'}. +{xp} XP. 1 sentence hype message. Gaming terminology."
    ai_response = ai.generate_response(prompt)
    
    if steps >= 10000:
        db_helper.create_intervention(user_id, "ACTIVITY_MILESTONE", f"🏆 10K Steps! +{xp} XP", strategy="CELEBRATION")
    
    vitality_update = {"vitality": {"steps_today": steps, "activity_xp": xp}}
    db_helper.update_state_cache(user_id, vitality_update)
    
    return context.res.json({"status": "activity_logged", "xp": xp, "message": ai_response})


def handle_meal_log(db_helper, payload, context, user_id):
    """Handle meal logging with cost tracking."""
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    meal_type = payload.get('meal_type', 'meal')
    cost = payload.get('cost', 0)
    description = payload.get('description', '')
    
    # Track spending
    if cost > 0:
        survival_state.financial.spent_today += cost
        _save_survival_state(user_id, survival_state, db_helper)
    
    budget_remaining = survival_state.financial.daily_runway - survival_state.financial.spent_today
    
    prompt = f"User refueled: {meal_type} (${cost}). Budget remaining: ${max(0, budget_remaining):.2f}. 1 sentence acknowledgment. Gaming terminology."
    ai_response = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "meal_logged",
        "cost": cost,
        "budget_remaining": budget_remaining,
        "message": ai_response
    })


def handle_energy_check(db_helper, payload, context, user_id):
    """Handle energy check."""
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    energy = payload.get('energy_level', 50)
    
    prompt = f"""
User energy check: {energy}/100
Defcon: {survival_state.financial.defcon_level}
Food supply: {survival_state.inventory.days_remaining} days

Give 2 sentences: 1) Energy assessment 2) Food/budget tip based on energy level.
Gaming terminology. Brief and actionable.
"""
    ai_response = ai.generate_response(prompt)
    
    thought_id = _create_thought_signature(user_id, "vitality", "energy_check", ai_response, db_helper, context)
    
    if energy < 30:
        db_helper.create_intervention(user_id, "LOW_ENERGY", "🔋 Energy critical! Easy meals only.", strategy="URGENT")
    
    return context.res.json({
        "status": "energy_checked",
        "energy": energy,
        "defcon": survival_state.financial.defcon_level,
        "message": ai_response,
        "thought_id": thought_id
    })


def handle_regen_request(db_helper, payload, context, user_id):
    """Handle regen mode request."""
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    duration = payload.get('duration_hours', 2)
    
    prompt = f"""
Regen Mode activated for {duration}h.
Defcon: {survival_state.financial.defcon_level}

Give 3-step Regen Protocol including:
1. Rest step
2. Easy meal suggestion (based on Defcon {survival_state.financial.defcon_level})
3. When to check back

Gaming terminology. Make it feel like activating a power.
"""
    ai_response = ai.generate_response(prompt)
    
    db_helper.create_intervention(user_id, "REGEN_ACTIVE", f"🔄 Regen Mode Active\n\n{ai_response}", strategy="CALM")
    
    vitality_update = {"vitality": {"regen_mode": True, "regen_duration": duration}}
    db_helper.update_state_cache(user_id, vitality_update)
    
    return context.res.json({"status": "regen_activated", "duration": duration, "message": ai_response})


def handle_resource_update(db_helper, payload, context, user_id):
    """Handle resource changes."""
    ai = GeminiClient()
    survival_state = _get_survival_state(user_id, db_helper)
    
    resource_type = payload.get('resource_type', 'currency')
    amount = payload.get('amount', 0)
    
    if resource_type == 'currency':
        if amount < 0:
            survival_state.financial.spent_today += abs(amount)
        else:
            survival_state.financial.total_balance += amount
        _save_survival_state(user_id, survival_state, db_helper)
    
    prompt = f"Resource update: {resource_type} {'+' if amount > 0 else ''}{amount}. Defcon {survival_state.financial.defcon_level}. 1 sentence acknowledgment."
    ai_response = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "resource_logged",
        "type": resource_type,
        "amount": amount,
        "message": ai_response
    })
