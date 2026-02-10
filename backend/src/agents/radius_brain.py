"""
🌍 RADIUS BRAIN v2.0: CULTURAL SURVIVAL ENGINE
===============================================
The International Student Cultural Adaptation & Safety Agent.

This is a MARATHON AGENT that runs PROACTIVELY to help international students:
- Daily language micro-lessons (contextual, NOT slang dictionary)
- Cultural practice generation (based on current location culture)
- Live agent prompts for teaching specific cultural scenarios
- Safe house tracking & local scan features
- Emergency cultural assistance

PROACTIVE - Thinks ahead, plans daily cultural lessons
MARATHON - Runs continuously, maintains cultural learning continuity
NO RAG - Pure Gemini 3 reasoning, no retrieval augmented generation

Compatible with Appwrite Functions execution model.
"""

import json
import datetime
import hashlib
import uuid
import asyncio
from typing import Dict, Any, Optional, List
from dataclasses import dataclass, field, asdict

from ..utils.gemini_client import GeminiClient
from ..config import GEMINI_API_KEY, GEMINI_3_FLASH


# =============================================================================
# CULTURAL CONTEXT DATA STRUCTURES
# =============================================================================

@dataclass
class InternationalProfile:
    """International student cultural profile."""
    home_country: str = ""
    home_language: str = ""  # Native language
    current_country: str = ""
    current_city: str = ""
    target_language: str = "English"  # Language they're learning
    language_level: str = "intermediate"  # beginner, intermediate, advanced
    cultural_challenges: List[str] = field(default_factory=list)
    learned_phrases: List[str] = field(default_factory=list)
    cultural_wins: int = 0  # Achievements in cultural adaptation
    days_in_country: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'InternationalProfile':
        if not data:
            return cls()
        return cls(**{k: v for k, v in data.items() if k in cls.__dataclass_fields__})


@dataclass
class DailyLesson:
    """Daily cultural/language micro-lesson."""
    lesson_id: str
    date: str
    lesson_type: str  # language, cultural_etiquette, local_knowledge, scenario_practice
    title: str
    content: str
    practice_scenario: str
    key_phrases: List[str] = field(default_factory=list)
    cultural_tip: str = ""
    difficulty: str = "intermediate"
    completed: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass 
class SafeHouse:
    """Safe location for the student."""
    location_id: str
    name: str
    address: str
    location_type: str  # home, library, campus, community_center, embassy
    safety_rating: int = 100  # 0-100
    emergency_contacts: List[str] = field(default_factory=list)
    notes: str = ""
    verified: bool = False
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class CulturalState:
    """Complete cultural survival state."""
    profile: InternationalProfile = field(default_factory=InternationalProfile)
    today_lesson: Optional[DailyLesson] = None
    safe_houses: List[SafeHouse] = field(default_factory=list)
    current_location: str = ""
    active_mode: str = "STANDARD"
    local_scan_result: Optional[Dict[str, Any]] = None
    lesson_streak: int = 0  # Consecutive days of learning
    last_lesson_date: Optional[str] = None
    live_agent_prompt: Optional[str] = None  # Current prompt for live teaching
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "profile": self.profile.to_dict(),
            "today_lesson": self.today_lesson.to_dict() if self.today_lesson else None,
            "safe_houses": [sh.to_dict() for sh in self.safe_houses],
            "current_location": self.current_location,
            "active_mode": self.active_mode,
            "local_scan_result": self.local_scan_result,
            "lesson_streak": self.lesson_streak,
            "last_lesson_date": self.last_lesson_date,
            "live_agent_prompt": self.live_agent_prompt
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'CulturalState':
        if not data:
            return cls()
        
        profile = InternationalProfile.from_dict(data.get('profile', {}))
        
        today_lesson = None
        if data.get('today_lesson'):
            today_lesson = DailyLesson(**data['today_lesson'])
        
        safe_houses = []
        for sh in data.get('safe_houses', []):
            safe_houses.append(SafeHouse(**sh))
        
        return cls(
            profile=profile,
            today_lesson=today_lesson,
            safe_houses=safe_houses,
            current_location=data.get('current_location', ''),
            active_mode=data.get('active_mode', 'STANDARD'),
            local_scan_result=data.get('local_scan_result'),
            lesson_streak=data.get('lesson_streak', 0),
            last_lesson_date=data.get('last_lesson_date'),
            live_agent_prompt=data.get('live_agent_prompt')
        )


# =============================================================================
# HELPER FUNCTIONS
# =============================================================================

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
            reasoning_mode='DEEP'
        )
        
        if context: 
            context.log(f"🧠 Thought signature stored: {thought_id}")
        return thought_id
    except Exception as e:
        print(f"❌ Thought signature error: {e}")
        return None


def _get_cultural_state(user_id: str, db_helper) -> CulturalState:
    """Get or create cultural state from database."""
    try:
        user_doc = db_helper.get_user_doc(user_id)
        if user_doc:
            state_json = json.loads(user_doc.get('studentState_json', '{}'))
            cultural_data = state_json.get('radius', {}).get('cultural', {})
            if cultural_data:
                return CulturalState.from_dict(cultural_data)
    except Exception as e:
        print(f"⚠️ Error loading cultural state: {e}")
    
    return CulturalState()


def _save_cultural_state(user_id: str, cultural_state: CulturalState, db_helper):
    """Save cultural state to database."""
    try:
        radius_update = {
            "radius": {
                "cultural": cultural_state.to_dict(),
                "last_updated": datetime.datetime.now().isoformat()
            }
        }
        db_helper.update_state_cache(user_id, radius_update)
    except Exception as e:
        print(f"⚠️ Error saving cultural state: {e}")


# =============================================================================
# MAIN BRAIN ENTRY POINT
# =============================================================================

def run_radius_agent(db_helper, payload, context):
    """
    🌍 Radius Brain v2.0: Cultural Survival Engine
    
    MARATHON AGENT - Runs proactively for international students.
    Routes to appropriate handler based on event type.
    """
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})

    context.log(f"🌍 Radius Cultural Engine for {user_id}")
    db_helper.log_heartbeat(user_id, f"EVENT:RADIUS:{payload.get('type', 'check')}")
    
    event_type = payload.get('type', 'location_change')
    
    # Route to appropriate handler
    handlers = {
        # === CULTURAL SURVIVAL HANDLERS ===
        'cultural_setup': handle_cultural_setup,
        'daily_lesson': handle_daily_lesson,
        'complete_lesson': handle_complete_lesson,
        'live_agent_prompt': handle_live_agent_prompt,
        'cultural_scenario': handle_cultural_scenario,
        
        # === LOCATION & SAFETY HANDLERS ===
        'location_change': handle_location_change,
        'local_scan': handle_local_scan,
        'safehouse_add': handle_safehouse_add,
        'safehouse_check': handle_safehouse_check,
        'emergency_cultural': handle_emergency_cultural,
        
        # === MARATHON AUTONOMOUS ===
        'marathon_cultural_morning': handle_marathon_cultural_morning,
        'mode_request': handle_mode_request,
    }
    
    handler = handlers.get(event_type, handle_location_change)
    return handler(db_helper, payload, context, user_id)


# =============================================================================
# CULTURAL SURVIVAL HANDLERS
# =============================================================================

def handle_cultural_setup(db_helper, payload, context, user_id):
    """
    🌍 INTERNATIONAL STUDENT PROFILE SETUP
    
    Day Zero setup for cultural adaptation.
    """
    context.log(f"🌍 Cultural setup for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    # Update profile from payload
    cultural_state.profile.home_country = payload.get('home_country', '')
    cultural_state.profile.home_language = payload.get('home_language', '')
    cultural_state.profile.current_country = payload.get('current_country', 'United States')
    cultural_state.profile.current_city = payload.get('current_city', '')
    cultural_state.profile.target_language = payload.get('target_language', 'English')
    cultural_state.profile.language_level = payload.get('language_level', 'intermediate')
    cultural_state.profile.days_in_country = payload.get('days_in_country', 0)
    
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    # Generate welcome and first lesson preview
    prompt = f"""
INTERNATIONAL STUDENT PROFILE SETUP COMPLETE

Student Profile:
- From: {cultural_state.profile.home_country} (Native: {cultural_state.profile.home_language})
- Now in: {cultural_state.profile.current_city}, {cultural_state.profile.current_country}
- Learning: {cultural_state.profile.target_language} ({cultural_state.profile.language_level} level)
- Days in country: {cultural_state.profile.days_in_country}

TASK: Generate a warm welcome message (3 sentences):
1. Acknowledge their journey from home country
2. Brief cultural tip for their current location
3. Promise to help them adapt daily

Be encouraging and culturally sensitive. This is a marathon of adaptation, not a sprint.
"""
    
    welcome = ai.generate_response(prompt)
    
    thought_id = _create_thought_signature(
        user_id, "radius", "cultural_setup", welcome, db_helper, context
    )
    
    return context.res.json({
        "status": "setup_complete",
        "profile": cultural_state.profile.to_dict(),
        "message": welcome,
        "thought_id": thought_id,
        "actions": ["cultural_profile_created"]
    })


def handle_daily_lesson(db_helper, payload, context, user_id):
    """
    📚 DAILY CULTURAL/LANGUAGE LESSON
    
    Generates a personalized micro-lesson based on:
    - Student's home culture vs current culture
    - Language level
    - Current location context
    - Previous lessons learned
    
    NOT a slang dictionary - contextual cultural learning.
    """
    context.log(f"📚 Daily lesson for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    # Check if already have today's lesson
    if cultural_state.today_lesson and cultural_state.today_lesson.date == today:
        return context.res.json({
            "status": "existing_lesson",
            "lesson": cultural_state.today_lesson.to_dict(),
            "message": "📚 Today's lesson is ready! Let's learn together.",
            "actions": ["existing_lesson_returned"]
        })
    
    # Update streak
    if cultural_state.last_lesson_date:
        yesterday = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime("%Y-%m-%d")
        if cultural_state.last_lesson_date == yesterday:
            cultural_state.lesson_streak += 1
        elif cultural_state.last_lesson_date != today:
            cultural_state.lesson_streak = 1
    else:
        cultural_state.lesson_streak = 1
    
    # Determine lesson type based on needs and variety
    lesson_types = [
        "language",  # Useful phrases for daily situations
        "cultural_etiquette",  # Social norms and manners
        "local_knowledge",  # Understanding local customs
        "scenario_practice"  # Real-world scenario practice
    ]
    lesson_type = lesson_types[cultural_state.lesson_streak % len(lesson_types)]
    
    # Generate lesson using Gemini
    prompt = f"""
GENERATE DAILY CULTURAL MICRO-LESSON

Student Profile:
- From: {cultural_state.profile.home_country} (Language: {cultural_state.profile.home_language})
- Now in: {cultural_state.profile.current_city}, {cultural_state.profile.current_country}
- Learning: {cultural_state.profile.target_language} ({cultural_state.profile.language_level} level)
- Lesson Streak: {cultural_state.lesson_streak} days
- Previous phrases learned: {len(cultural_state.profile.learned_phrases)}

Lesson Type: {lesson_type}
Current Location Context: {cultural_state.current_location or 'General'}

IMPORTANT: Generate a contextual, practical lesson. NOT a slang dictionary.
Focus on real situations the student might encounter TODAY.

OUTPUT JSON FORMAT:
{{
    "title": "Brief catchy title",
    "content": "Main lesson content (2-3 paragraphs). Include cultural context comparing home vs current culture.",
    "practice_scenario": "A specific real-world scenario to practice. Written as a mini story/dialogue.",
    "key_phrases": ["phrase1 - meaning", "phrase2 - meaning", "phrase3 - meaning"],
    "cultural_tip": "One important cultural difference between home and current country",
    "difficulty": "beginner|intermediate|advanced"
}}

Make it:
1. Practical for TODAY's activities
2. Culturally respectful of both cultures
3. Encouraging for language learners
4. NOT about slang or informal language
"""
    
    lesson_response = ai.generate_response(prompt, json_mode=True)
    
    try:
        import re
        json_match = re.search(r'\{[\s\S]*\}', lesson_response)
        if json_match:
            lesson_data = json.loads(json_match.group())
        else:
            lesson_data = json.loads(lesson_response)
    except:
        lesson_data = {
            "title": "Cultural Awareness Day",
            "content": f"Today we focus on understanding the culture of {cultural_state.profile.current_country}.",
            "practice_scenario": "Practice greeting someone at a coffee shop.",
            "key_phrases": ["Hello, how are you? - Standard greeting", "Have a great day! - Friendly farewell"],
            "cultural_tip": "Eye contact norms may differ from your home country.",
            "difficulty": "intermediate"
        }
    
    # Create lesson object
    lesson = DailyLesson(
        lesson_id=f"lesson_{uuid.uuid4().hex[:8]}",
        date=today,
        lesson_type=lesson_type,
        title=lesson_data.get('title', ''),
        content=lesson_data.get('content', ''),
        practice_scenario=lesson_data.get('practice_scenario', ''),
        key_phrases=lesson_data.get('key_phrases', []),
        cultural_tip=lesson_data.get('cultural_tip', ''),
        difficulty=lesson_data.get('difficulty', 'intermediate'),
        completed=False
    )
    
    cultural_state.today_lesson = lesson
    cultural_state.last_lesson_date = today
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    thought_id = _create_thought_signature(
        user_id, "radius", "daily_lesson", json.dumps(lesson_data), db_helper, context
    )
    
    return context.res.json({
        "status": "lesson_generated",
        "lesson": lesson.to_dict(),
        "streak": cultural_state.lesson_streak,
        "thought_id": thought_id,
        "message": f"📚 Day {cultural_state.lesson_streak}: {lesson.title}",
        "actions": ["lesson_generated", f"streak:{cultural_state.lesson_streak}"]
    })


def handle_complete_lesson(db_helper, payload, context, user_id):
    """
    ✅ COMPLETE TODAY'S LESSON
    
    Mark lesson as completed and add phrases to learned list.
    """
    context.log(f"✅ Complete lesson for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    if not cultural_state.today_lesson:
        return context.res.json({
            "status": "no_lesson",
            "message": "📚 No lesson for today! Let me generate one.",
            "trigger_action": "daily_lesson",
            "actions": ["no_lesson"]
        })
    
    if cultural_state.today_lesson.completed:
        return context.res.json({
            "status": "already_completed",
            "message": "✅ You've already completed today's lesson! Great work!",
            "streak": cultural_state.lesson_streak,
            "actions": ["already_complete"]
        })
    
    # Mark complete
    cultural_state.today_lesson.completed = True
    cultural_state.profile.cultural_wins += 1
    
    # Add learned phrases to profile
    for phrase in cultural_state.today_lesson.key_phrases:
        if phrase not in cultural_state.profile.learned_phrases:
            cultural_state.profile.learned_phrases.append(phrase)
    
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    # Generate celebration
    prompt = f"""
Lesson completed! 
- Lesson: {cultural_state.today_lesson.title}
- Streak: {cultural_state.lesson_streak} days
- Total phrases learned: {len(cultural_state.profile.learned_phrases)}
- Cultural wins: {cultural_state.profile.cultural_wins}

Give a 2-sentence celebration message. Encourage them to use what they learned today.
"""
    
    celebration = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "completed",
        "lesson_title": cultural_state.today_lesson.title,
        "streak": cultural_state.lesson_streak,
        "total_phrases": len(cultural_state.profile.learned_phrases),
        "cultural_wins": cultural_state.profile.cultural_wins,
        "message": celebration,
        "actions": ["lesson_completed", f"streak:{cultural_state.lesson_streak}"]
    })


def handle_live_agent_prompt(db_helper, payload, context, user_id):
    """
    🎙️ LIVE AGENT PROMPT GENERATOR
    
    Generates a prompt for the live voice/video agent to teach the student
    a specific cultural topic in real-time conversation.
    
    This prompt is used by the live agent in the app to have a contextual
    teaching conversation with the student.
    """
    context.log(f"🎙️ Live agent prompt for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    topic = payload.get('topic', 'general_conversation')
    scenario = payload.get('scenario', '')
    
    # Generate comprehensive prompt for live agent
    prompt = f"""
GENERATE LIVE TEACHING AGENT PROMPT

Student Profile:
- From: {cultural_state.profile.home_country} (Native: {cultural_state.profile.home_language})
- Learning: {cultural_state.profile.target_language} ({cultural_state.profile.language_level})
- Days in country: {cultural_state.profile.days_in_country}
- Recent lessons: {cultural_state.today_lesson.title if cultural_state.today_lesson else 'None'}

Requested Topic: {topic}
Specific Scenario: {scenario or 'None specified'}

Generate a PROMPT that the live voice agent will use to teach this student.
The prompt should instruct the live agent to:

1. Act as a friendly local helping the student practice
2. Use context from their home culture to explain differences
3. Practice the scenario through conversation
4. Correct gently and encourage
5. Avoid using inappropriate or offensive language

OUTPUT JSON FORMAT:
{{
    "live_agent_system_prompt": "The complete system instruction for the live agent",
    "opening_line": "How the agent should start the conversation",
    "key_teaching_points": ["point1", "point2", "point3"],
    "practice_dialogue": "Example dialogue to practice",
    "success_criteria": "How to know the student has learned"
}}
"""
    
    agent_prompt_response = ai.generate_response(prompt, json_mode=True)
    
    try:
        import re
        json_match = re.search(r'\{[\s\S]*\}', agent_prompt_response)
        if json_match:
            prompt_data = json.loads(json_match.group())
        else:
            prompt_data = json.loads(agent_prompt_response)
    except:
        prompt_data = {
            "live_agent_system_prompt": f"You are helping an international student from {cultural_state.profile.home_country} practice {cultural_state.profile.target_language}. Be patient, encouraging, and culturally sensitive.",
            "opening_line": "Hi! I'm here to help you practice. Tell me about your day so far.",
            "key_teaching_points": ["Active listening", "Pronunciation", "Cultural context"],
            "practice_dialogue": "Let's practice ordering at a restaurant.",
            "success_criteria": "Student can complete the scenario with confidence."
        }
    
    cultural_state.live_agent_prompt = prompt_data.get('live_agent_system_prompt', '')
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    thought_id = _create_thought_signature(
        user_id, "radius", "live_agent_prompt", json.dumps(prompt_data), db_helper, context
    )
    
    return context.res.json({
        "status": "prompt_generated",
        "topic": topic,
        "live_agent_prompt": prompt_data,
        "thought_id": thought_id,
        "message": f"🎙️ Live teaching session ready for: {topic}",
        "actions": ["live_prompt_generated", f"topic:{topic}"]
    })


def handle_cultural_scenario(db_helper, payload, context, user_id):
    """
    🎭 CULTURAL SCENARIO PRACTICE
    
    Generate a specific cultural scenario for the student to practice.
    """
    context.log(f"🎭 Cultural scenario for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    scenario_type = payload.get('scenario_type', 'random')
    # Types: restaurant, shopping, campus, workplace, social, transportation
    
    prompt = f"""
GENERATE CULTURAL PRACTICE SCENARIO

Student: From {cultural_state.profile.home_country}, now in {cultural_state.profile.current_country}
Language Level: {cultural_state.profile.language_level}
Scenario Type: {scenario_type}

Create a realistic scenario that highlights cultural differences between their home country
and current country. Include:

1. Setting description
2. Characters involved
3. The cultural challenge/difference
4. Expected behavior in current country
5. Common mistakes to avoid
6. Sample dialogue (3-4 exchanges)
7. Key vocabulary (5 words/phrases with meanings)

Make it practical and something they might encounter THIS WEEK.
Be culturally sensitive - present differences as interesting, not better/worse.

Output as structured JSON:
{{
    "scenario_id": "unique_id",
    "title": "Scenario title",
    "setting": "Where this happens",
    "characters": ["character1", "character2"],
    "cultural_challenge": "The main cultural difference to navigate",
    "expected_behavior": "What's expected in current country",
    "common_mistakes": ["mistake1", "mistake2"],
    "dialogue": [
        {{"speaker": "Local", "line": "..."}},
        {{"speaker": "Student", "line": "..."}}
    ],
    "key_vocabulary": [
        {{"word": "...", "meaning": "..."}},
    ],
    "tips": "Quick tip for success"
}}
"""
    
    scenario_response = ai.generate_response(prompt, json_mode=True)
    
    try:
        import re
        json_match = re.search(r'\{[\s\S]*\}', scenario_response)
        if json_match:
            scenario_data = json.loads(json_match.group())
        else:
            scenario_data = json.loads(scenario_response)
    except:
        scenario_data = {
            "scenario_id": f"scenario_{uuid.uuid4().hex[:6]}",
            "title": "Coffee Shop Interaction",
            "setting": "Local coffee shop",
            "cultural_challenge": "Understanding tipping culture",
            "dialogue": [],
            "key_vocabulary": [],
            "tips": "Be confident and friendly!"
        }
    
    thought_id = _create_thought_signature(
        user_id, "radius", "cultural_scenario", json.dumps(scenario_data), db_helper, context
    )
    
    return context.res.json({
        "status": "scenario_generated",
        "scenario": scenario_data,
        "thought_id": thought_id,
        "message": f"🎭 Practice scenario: {scenario_data.get('title', 'Cultural Practice')}",
        "actions": ["scenario_generated", f"type:{scenario_type}"]
    })


# =============================================================================
# LOCATION & SAFETY HANDLERS
# =============================================================================

def handle_location_change(db_helper, payload, context, user_id):
    """
    📍 LOCATION CHANGE HANDLER
    
    Updated for international students with cultural context.
    """
    context.log(f"📍 Location change for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    location = payload.get('location', payload.get('user_location', 'Unknown'))
    cultural_state.current_location = location
    
    # Determine mode
    mode = "STANDARD"
    if any(x in location.lower() for x in ['library', 'study']):
        mode = "DEEP_FOCUS"
    elif any(x in location.lower() for x in ['gym', 'fitness']):
        mode = "WORKOUT"
    elif any(x in location.lower() for x in ['home', 'dorm', 'apartment']):
        mode = "REST"
    elif any(x in location.lower() for x in ['class', 'lecture', 'campus']):
        mode = "CAMPUS"
    
    cultural_state.active_mode = mode
    
    # Check if safehouse
    is_safehouse = any(
        sh.name.lower() in location.lower() or location.lower() in sh.name.lower()
        for sh in cultural_state.safe_houses
    )
    
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    # Generate cultural location tip
    prompt = f"""
Student from {cultural_state.profile.home_country} arrived at: {location}
Mode: {mode}
Is safehouse: {is_safehouse}

Give a 1-sentence location acknowledgment with a cultural tip if relevant.
For example, if at a coffee shop, mention any ordering customs.
Keep it brief and helpful.
"""
    
    response = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "location_updated",
        "location": location,
        "mode": mode,
        "is_safehouse": is_safehouse,
        "message": response,
        "actions": ["location_changed", f"mode:{mode}"]
    })


def handle_local_scan(db_helper, payload, context, user_id):
    """
    🔍 LOCAL SCAN - Analyze Current Area
    
    Provides cultural context about the current location area.
    """
    context.log(f"🔍 Local scan for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    location = payload.get('location', cultural_state.current_location)
    scan_focus = payload.get('focus', 'general')  # general, safety, services, social
    
    prompt = f"""
LOCAL AREA SCAN for international student

Student: From {cultural_state.profile.home_country}
Current Location: {location}
Scan Focus: {scan_focus}

Generate a local area briefing that would help an international student feel
comfortable and informed. Include:

1. Area overview (safe for students?)
2. Nearby useful services (in English if possible)
3. Cultural norms specific to this area
4. Emergency resources if any
5. Quick tips for navigating

Focus on {scan_focus} aspects specifically.

Output JSON:
{{
    "area_name": "{location}",
    "safety_rating": 1-10,
    "overview": "Brief area description",
    "useful_services": [
        {{"name": "...", "type": "...", "tip": "..."}}
    ],
    "cultural_norms": ["norm1", "norm2"],
    "emergency_info": "If relevant",
    "quick_tips": ["tip1", "tip2"],
    "student_friendly": true/false
}}
"""
    
    scan_response = ai.generate_response(prompt, json_mode=True)
    
    try:
        import re
        json_match = re.search(r'\{[\s\S]*\}', scan_response)
        if json_match:
            scan_data = json.loads(json_match.group())
        else:
            scan_data = json.loads(scan_response)
    except:
        scan_data = {
            "area_name": location,
            "safety_rating": 7,
            "overview": "Standard urban area.",
            "useful_services": [],
            "cultural_norms": ["Be polite", "Mind personal space"],
            "quick_tips": ["Keep valuables secure", "Ask for help if needed"],
            "student_friendly": True
        }
    
    cultural_state.local_scan_result = scan_data
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    thought_id = _create_thought_signature(
        user_id, "radius", "local_scan", json.dumps(scan_data), db_helper, context
    )
    
    return context.res.json({
        "status": "scan_complete",
        "scan_result": scan_data,
        "thought_id": thought_id,
        "message": f"🔍 Area scan complete: Safety {scan_data.get('safety_rating', 'N/A')}/10",
        "actions": ["local_scan", f"safety:{scan_data.get('safety_rating', 'N/A')}"]
    })


def handle_safehouse_add(db_helper, payload, context, user_id):
    """
    🏠 ADD SAFEHOUSE
    
    Add a trusted location as a safehouse.
    """
    context.log(f"🏠 Add safehouse for {user_id}")
    
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    safehouse = SafeHouse(
        location_id=f"safehouse_{uuid.uuid4().hex[:8]}",
        name=payload.get('name', 'My Safe Place'),
        address=payload.get('address', ''),
        location_type=payload.get('location_type', 'home'),
        safety_rating=payload.get('safety_rating', 100),
        emergency_contacts=payload.get('emergency_contacts', []),
        notes=payload.get('notes', ''),
        verified=False
    )
    
    cultural_state.safe_houses.append(safehouse)
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    return context.res.json({
        "status": "safehouse_added",
        "safehouse": safehouse.to_dict(),
        "total_safehouses": len(cultural_state.safe_houses),
        "message": f"🏠 Safehouse added: {safehouse.name}",
        "actions": ["safehouse_added"]
    })


def handle_safehouse_check(db_helper, payload, context, user_id):
    """
    🏠 CHECK SAFEHOUSE STATUS
    
    View all safehouses and current location status.
    """
    context.log(f"🏠 Safehouse check for {user_id}")
    
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    current_location = cultural_state.current_location
    in_safehouse = any(
        sh.name.lower() in current_location.lower() or current_location.lower() in sh.name.lower()
        for sh in cultural_state.safe_houses
    )
    
    return context.res.json({
        "status": "ok",
        "current_location": current_location,
        "in_safehouse": in_safehouse,
        "safehouses": [sh.to_dict() for sh in cultural_state.safe_houses],
        "total_safehouses": len(cultural_state.safe_houses),
        "message": "🏠 In safehouse" if in_safehouse else "📍 Not in safehouse",
        "actions": ["safehouse_checked"]
    })


def handle_emergency_cultural(db_helper, payload, context, user_id):
    """
    🆘 EMERGENCY CULTURAL ASSISTANCE
    
    Quick help for cultural confusion or difficult situations.
    """
    context.log(f"🆘 Emergency cultural help for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    situation = payload.get('situation', '')
    urgency = payload.get('urgency', 'normal')  # low, normal, high, emergency
    
    prompt = f"""
🆘 EMERGENCY CULTURAL ASSISTANCE

Student: From {cultural_state.profile.home_country}, speaks {cultural_state.profile.home_language}
Currently in: {cultural_state.profile.current_country}
Language Level: {cultural_state.profile.language_level}
Location: {cultural_state.current_location}

SITUATION: {situation}
URGENCY: {urgency}

Provide IMMEDIATE, PRACTICAL help:
1. Quick phrase to say right now (in {cultural_state.profile.target_language})
2. What to do next (2-3 steps)
3. Cultural context (why this might be happening)
4. If high urgency: emergency numbers/resources

Be calm, clear, and actionable. This student may be stressed.
"""
    
    response = ai.generate_response(prompt)
    
    thought_id = _create_thought_signature(
        user_id, "radius", "emergency_cultural", response, db_helper, context
    )
    
    # Create intervention for high urgency
    if urgency in ['high', 'emergency']:
        db_helper.create_intervention(
            user_id, "CULTURAL_EMERGENCY",
            f"🆘 Cultural Emergency\n\n{response}",
            strategy="URGENT"
        )
    
    return context.res.json({
        "status": "help_provided",
        "situation": situation,
        "urgency": urgency,
        "response": response,
        "thought_id": thought_id,
        "message": "🆘 Help is here. Stay calm.",
        "actions": ["emergency_help", f"urgency:{urgency}"]
    })


# =============================================================================
# MARATHON AUTONOMOUS HANDLERS
# =============================================================================

def handle_marathon_cultural_morning(db_helper, payload, context, user_id):
    """
    🌅 MARATHON CULTURAL MORNING - AUTONOMOUS
    
    Triggered automatically each morning. Generates:
    1. Today's cultural lesson
    2. Live agent prompt for the day
    3. Cultural tip based on schedule
    
    FULLY PROACTIVE - No user request needed.
    """
    context.log(f"🌅 Marathon Cultural Morning for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    morning_report = {
        "date": today,
        "user_id": user_id,
        "status": "marathon_complete",
        "components": []
    }
    
    # === 1. GENERATE TODAY'S LESSON ===
    # Reuse daily_lesson handler logic inline for marathon
    if not cultural_state.today_lesson or cultural_state.today_lesson.date != today:
        lesson_types = ["language", "cultural_etiquette", "local_knowledge", "scenario_practice"]
        lesson_type = lesson_types[cultural_state.lesson_streak % len(lesson_types)]
        
        lesson_prompt = f"""
Generate a daily micro-lesson for international student from {cultural_state.profile.home_country}
in {cultural_state.profile.current_country}. Language level: {cultural_state.profile.language_level}.
Lesson type: {lesson_type}. Streak day: {cultural_state.lesson_streak + 1}.

Output JSON with: title, content, practice_scenario, key_phrases (list), cultural_tip, difficulty
"""
        
        lesson_response = ai.generate_response(lesson_prompt, json_mode=True)
        try:
            import re
            json_match = re.search(r'\{[\s\S]*\}', lesson_response)
            lesson_data = json.loads(json_match.group()) if json_match else {}
        except:
            lesson_data = {"title": "Cultural Awareness", "content": "Practice being observant today.", "key_phrases": [], "cultural_tip": "Watch how locals interact."}
        
        lesson = DailyLesson(
            lesson_id=f"lesson_{uuid.uuid4().hex[:8]}",
            date=today,
            lesson_type=lesson_type,
            title=lesson_data.get('title', 'Daily Practice'),
            content=lesson_data.get('content', ''),
            practice_scenario=lesson_data.get('practice_scenario', ''),
            key_phrases=lesson_data.get('key_phrases', []),
            cultural_tip=lesson_data.get('cultural_tip', ''),
            difficulty=lesson_data.get('difficulty', 'intermediate'),
            completed=False
        )
        
        cultural_state.today_lesson = lesson
        cultural_state.lesson_streak += 1
        cultural_state.last_lesson_date = today
        morning_report["lesson"] = lesson.to_dict()
        morning_report["components"].append("lesson_generated")
    else:
        morning_report["lesson"] = cultural_state.today_lesson.to_dict()
        morning_report["components"].append("lesson_existing")
    
    # === 2. GENERATE LIVE AGENT PROMPT ===
    live_prompt = f"""You are a cultural coach helping a student from {cultural_state.profile.home_country} 
adapt to {cultural_state.profile.current_country}. Today's focus: {cultural_state.today_lesson.title if cultural_state.today_lesson else 'General practice'}.
Be patient, encouraging, and culturally sensitive. Practice real scenarios they'll encounter today.
Language level: {cultural_state.profile.language_level}. Help them build confidence!"""
    
    cultural_state.live_agent_prompt = live_prompt
    morning_report["live_agent_prompt"] = live_prompt
    morning_report["components"].append("live_prompt_ready")
    
    # Save state
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    # === 3. GENERATE MORNING BRIEFING ===
    briefing_prompt = f"""
CULTURAL MARATHON MORNING BRIEFING

Student: From {cultural_state.profile.home_country}
Day {cultural_state.lesson_streak} of cultural adaptation
Today's Lesson: {cultural_state.today_lesson.title if cultural_state.today_lesson else 'General'}
Phrases Learned: {len(cultural_state.profile.learned_phrases)}

Generate a 3-sentence encouraging morning message:
1. Acknowledge their cultural journey
2. Introduce today's lesson briefly
3. One quick cultural tip for the day

Warm and encouraging tone. End with "Let's make today count!"
"""
    
    briefing = ai.generate_response(briefing_prompt)
    morning_report["briefing"] = briefing
    
    thought_id = _create_thought_signature(
        user_id, "radius", "marathon_cultural_morning", briefing, db_helper, context
    )
    morning_report["thought_id"] = thought_id
    
    # Create push notification
    db_helper.create_intervention(
        user_id, "CULTURAL_MORNING",
        f"🌅 CULTURAL MORNING\n\n{briefing}",
        strategy="WARM"
    )
    
    return context.res.json(morning_report)


def handle_mode_request(db_helper, payload, context, user_id):
    """
    🎚️ MODE REQUEST
    
    Manual mode change (DEEP_FOCUS, WORKOUT, REST, etc.)
    """
    context.log(f"🎚️ Mode request for {user_id}")
    
    ai = GeminiClient()
    cultural_state = _get_cultural_state(user_id, db_helper)
    
    requested_mode = payload.get('mode', 'STANDARD')
    duration = payload.get('duration_minutes')
    
    cultural_state.active_mode = requested_mode
    _save_cultural_state(user_id, cultural_state, db_helper)
    
    prompt = f"Mode activated: {requested_mode}. Duration: {duration or 'Until changed'} minutes. Give 1-sentence confirmation."
    response = ai.generate_response(prompt)
    
    return context.res.json({
        "status": "mode_activated",
        "mode": requested_mode,
        "duration": duration,
        "message": response,
        "actions": ["mode_changed", f"mode:{requested_mode}"]
    })
