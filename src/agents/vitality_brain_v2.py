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

Compatible with Appwrite Functions execution model.
"""

import json
import datetime
import hashlib
import uuid
import base64
from typing import Dict, Any, Optional, List

from ..utils.gemini_client import GeminiClient
from ..config import GEMINI_API_KEY, GEMINI_3_FLASH
from ..core.survival_protocol import (
    SurvivalState, SurvivalProtocol, DefconLevel,
    FinancialState, FridgeInventory, UserPreferences,
    MealDecisionContext, MealDecisionEngine,
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
def run_vitality_agent(db_helper, payload, context):
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
    """Morning Budget Check."""
    context.log(f"💰 Daily budget check for {user_id}")
    
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
    
    _save_survival_state(user_id, survival_state, db_helper)
    
    # Generate briefing
    briefing_prompt = f"""
Morning financial briefing:
- Today's budget: ${budget_info['today_budget']:.2f}
- Yesterday spent: ${budget_info['yesterday_spent']:.2f}
- Defcon: {budget_info['defcon_level']}
- Message: {budget_info['message']}

Give a 2-sentence energizing morning financial briefing. Game-style day start.
"""
    
    briefing = ai.generate_response(briefing_prompt)
    
    return context.res.json({
        "status": "budget_calculated",
        "budget_info": budget_info,
        "today_budget": budget_info['today_budget'],
        "message": briefing,
        "actions": ["daily_budget"]
    })


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
