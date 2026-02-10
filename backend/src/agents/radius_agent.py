"""
🌍 Radius Agent v2.0: Cultural Survival Engine
===============================================
The International Student Cultural Adaptation & Safety Agent.

MARATHON AGENT - Runs proactively for international students:
- Daily language micro-lessons (contextual)
- Cultural practice generation
- Live agent prompts for teaching
- Safe house tracking & local scan
- Emergency cultural assistance

Uses BaseAgent pattern for compatibility.
"""

import json
from typing import Dict, Any, List, Optional
from datetime import datetime
import uuid

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext

# Import cultural state structures from radius_brain
from .radius_brain import (
    InternationalProfile,
    DailyLesson,
    SafeHouse,
    CulturalState,
    _get_cultural_state,
    _save_cultural_state
)


class RadiusAgent(BaseAgent):
    """
    🌍 Radius Agent v2.0: Cultural Survival Controller
    
    MARATHON AGENT for international students.
    
    Handles event types:
    - cultural_setup: International student profile setup
    - daily_lesson: Generate daily cultural/language lesson
    - complete_lesson: Mark lesson complete
    - live_agent_prompt: Generate prompt for live teaching agent
    - cultural_scenario: Generate cultural practice scenario
    - location_change: User moved to new location
    - local_scan: Analyze current area for student safety
    - safehouse_add: Add a safe location
    - safehouse_check: Check safehouse status
    - emergency_cultural: Emergency cultural assistance
    - marathon_cultural_morning: Autonomous morning routine
    - mode_request: Manual mode change
    """
    
    RADIUS_SYSTEM_PROMPT = """You are the Radius Cultural Survival Controller for Kaironex.

You help INTERNATIONAL STUDENTS adapt to new countries and cultures through:
1. Daily micro-lessons on language and culture
2. Real-world scenario practice
3. Safe house and local area awareness
4. Emergency cultural assistance

PERSONA:
- Tone: Warm, culturally sensitive, encouraging
- Style: Brief, practical, supportive
- Focus: Cultural adaptation, NOT slang

NEVER:
- Teach slang or informal language
- Assume one culture is "better" than another
- Share location information

RULES:
1. Compare home culture to current culture respectfully
2. Focus on practical, everyday situations
3. Build student confidence gradually
4. Track cultural learning streak for motivation
5. Provide emergency help immediately when needed

OUTPUT FORMAT:
<analyze>Cultural context assessment</analyze>
<action>Supportive, actionable guidance</action>"""

    # Location to mode mapping
    LOCATION_MODES = {
        'library': ('DEEP_FOCUS', 'WHISPER'),
        'study': ('DEEP_FOCUS', 'WHISPER'),
        'classroom': ('CAMPUS', 'SILENT'),
        'lecture': ('CAMPUS', 'SILENT'),
        'gym': ('WORKOUT', 'ENERGETIC'),
        'fitness': ('WORKOUT', 'ENERGETIC'),
        'home': ('REST', 'WARM'),
        'dorm': ('REST', 'WARM'),
        'apartment': ('REST', 'WARM'),
        'cafe': ('LIGHT_FOCUS', 'CASUAL'),
        'coffee': ('LIGHT_FOCUS', 'CASUAL'),
        'restaurant': ('SOCIAL', 'MINIMAL'),
        'commute': ('MOBILE', 'BRIEF'),
        'transit': ('MOBILE', 'BRIEF'),
        'bus': ('MOBILE', 'BRIEF'),
        'train': ('MOBILE', 'BRIEF'),
        'campus': ('CAMPUS', 'FOCUSED'),
        'class': ('CAMPUS', 'FOCUSED'),
    }

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="radius",
            display_name="🌍 Cultural Survival Controller",
            system_instruction=self.RADIUS_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=65536,  # Maximum thinking for cultural reasoning
            enable_thought_signatures=True,
            enable_marathon=True  # MARATHON AGENT
        )
    
    async def process(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """Main processing logic for cultural survival events."""
        
        location = payload.get('user_location', payload.get('location', 'Unknown'))
        self.log_heartbeat(user_id, f"EVENT:RADIUS:{payload.get('type', 'check')}")
        
        event_type = payload.get('type', 'location_change')
        
        handlers = {
            # === CULTURAL SURVIVAL HANDLERS ===
            'cultural_setup': self._handle_cultural_setup,
            'daily_lesson': self._handle_daily_lesson,
            'complete_lesson': self._handle_complete_lesson,
            'live_agent_prompt': self._handle_live_agent_prompt,
            'cultural_scenario': self._handle_cultural_scenario,
            
            # === LOCATION & SAFETY HANDLERS ===
            'location_change': self._handle_location_change,
            'local_scan': self._handle_local_scan,
            'safehouse_add': self._handle_safehouse_add,
            'safehouse_check': self._handle_safehouse_status,
            'emergency_cultural': self._handle_emergency_cultural,
            
            # === MARATHON AUTONOMOUS ===
            'marathon_cultural_morning': self._handle_marathon_cultural_morning,
            'mode_request': self._handle_mode_request,
        }
        
        handler = handlers.get(event_type, self._handle_location_change)
        return await handler(user_id, payload, context)
    
    def _detect_mode(self, location: str) -> tuple[str, str]:
        """Detect mode and notification strategy from location."""
        location_lower = location.lower()
        
        for keyword, (mode, strategy) in self.LOCATION_MODES.items():
            if keyword in location_lower:
                return mode, strategy
        
        return 'STANDARD', 'NEUTRAL'
    
    # =========================================================================
    # CULTURAL SURVIVAL HANDLERS
    # =========================================================================
    
    async def _handle_cultural_setup(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🌍 International student profile setup."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        
        # Update profile from payload
        cultural_state.profile.home_country = payload.get('home_country', '')
        cultural_state.profile.home_language = payload.get('home_language', '')
        cultural_state.profile.current_country = payload.get('current_country', 'United States')
        cultural_state.profile.current_city = payload.get('current_city', '')
        cultural_state.profile.target_language = payload.get('target_language', 'English')
        cultural_state.profile.language_level = payload.get('language_level', 'intermediate')
        cultural_state.profile.days_in_country = payload.get('days_in_country', 0)
        
        _save_cultural_state(user_id, cultural_state, self.db)
        
        prompt = f"""
INTERNATIONAL STUDENT PROFILE SETUP

Student: From {cultural_state.profile.home_country} (Native: {cultural_state.profile.home_language})
Now in: {cultural_state.profile.current_city}, {cultural_state.profile.current_country}
Learning: {cultural_state.profile.target_language} ({cultural_state.profile.language_level})

Generate a warm 3-sentence welcome:
1. Acknowledge their journey
2. Brief cultural tip for current location
3. Promise to help them adapt daily
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.HYBRID
        ))
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["cultural_profile_created"],
            state_updates={"profile": cultural_state.profile.to_dict()}
        )
    
    async def _handle_daily_lesson(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """📚 Generate daily cultural/language micro-lesson."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        today = datetime.now().strftime("%Y-%m-%d")
        
        # Check if already have today's lesson
        if cultural_state.today_lesson and cultural_state.today_lesson.date == today:
            return AgentResult(
                success=True,
                response=f"📚 Today's lesson: {cultural_state.today_lesson.title}",
                actions_taken=["existing_lesson_returned"],
                state_updates={"lesson": cultural_state.today_lesson.to_dict()}
            )
        
        # Update streak
        from datetime import timedelta
        if cultural_state.last_lesson_date:
            yesterday = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
            if cultural_state.last_lesson_date == yesterday:
                cultural_state.lesson_streak += 1
            elif cultural_state.last_lesson_date != today:
                cultural_state.lesson_streak = 1
        else:
            cultural_state.lesson_streak = 1
        
        lesson_types = ["language", "cultural_etiquette", "local_knowledge", "scenario_practice"]
        lesson_type = lesson_types[cultural_state.lesson_streak % len(lesson_types)]
        
        prompt = f"""
GENERATE DAILY CULTURAL MICRO-LESSON

Student: From {cultural_state.profile.home_country} (Language: {cultural_state.profile.home_language})
Now in: {cultural_state.profile.current_city}, {cultural_state.profile.current_country}
Learning: {cultural_state.profile.target_language} ({cultural_state.profile.language_level})
Lesson Type: {lesson_type}
Streak: Day {cultural_state.lesson_streak}

Generate a practical, contextual lesson (NOT slang dictionary).
Focus on real situations they might encounter TODAY.

Output JSON: title, content, practice_scenario, key_phrases (list), cultural_tip, difficulty
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.DEEP
        ))
        
        try:
            import re
            json_match = re.search(r'\{[\s\S]*\}', response.content)
            lesson_data = json.loads(json_match.group()) if json_match else {}
        except:
            lesson_data = {"title": "Cultural Awareness", "content": "Practice today.", "key_phrases": [], "cultural_tip": "Observe locals."}
        
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
        _save_cultural_state(user_id, cultural_state, self.db)
        
        return AgentResult(
            success=True,
            response=f"📚 Day {cultural_state.lesson_streak}: {lesson.title}",
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["lesson_generated", f"streak:{cultural_state.lesson_streak}"],
            state_updates={"lesson": lesson.to_dict(), "streak": cultural_state.lesson_streak}
        )
    
    async def _handle_complete_lesson(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """✅ Complete today's lesson."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        
        if not cultural_state.today_lesson:
            return AgentResult(
                success=False,
                response="📚 No lesson today! Generate one first.",
                actions_taken=["no_lesson"]
            )
        
        if cultural_state.today_lesson.completed:
            return AgentResult(
                success=True,
                response="✅ Already completed today!",
                actions_taken=["already_complete"]
            )
        
        cultural_state.today_lesson.completed = True
        cultural_state.profile.cultural_wins += 1
        
        for phrase in cultural_state.today_lesson.key_phrases:
            if phrase not in cultural_state.profile.learned_phrases:
                cultural_state.profile.learned_phrases.append(phrase)
        
        _save_cultural_state(user_id, cultural_state, self.db)
        
        return AgentResult(
            success=True,
            response=f"✅ Completed: {cultural_state.today_lesson.title}! Streak: {cultural_state.lesson_streak} days",
            actions_taken=["lesson_completed", f"streak:{cultural_state.lesson_streak}"],
            state_updates={"cultural_wins": cultural_state.profile.cultural_wins}
        )
    
    async def _handle_live_agent_prompt(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🎙️ Generate prompt for live teaching agent."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        topic = payload.get('topic', 'general_conversation')
        scenario = payload.get('scenario', '')
        
        prompt = f"""
GENERATE LIVE TEACHING AGENT PROMPT

Student: From {cultural_state.profile.home_country} (Native: {cultural_state.profile.home_language})
Learning: {cultural_state.profile.target_language} ({cultural_state.profile.language_level})
Topic: {topic}
Scenario: {scenario or 'None specified'}

Generate a prompt for the live voice agent to teach this student.
The agent should:
1. Act as a friendly local
2. Compare home vs current culture
3. Practice scenario through conversation
4. Correct gently, encourage

Output JSON: live_agent_system_prompt, opening_line, key_teaching_points (list), practice_dialogue, success_criteria
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.DEEP
        ))
        
        try:
            import re
            json_match = re.search(r'\{[\s\S]*\}', response.content)
            prompt_data = json.loads(json_match.group()) if json_match else {}
        except:
            prompt_data = {"live_agent_system_prompt": f"Help student from {cultural_state.profile.home_country} practice.", "opening_line": "Hi! Let's practice."}
        
        cultural_state.live_agent_prompt = prompt_data.get('live_agent_system_prompt', '')
        _save_cultural_state(user_id, cultural_state, self.db)
        
        return AgentResult(
            success=True,
            response=f"🎙️ Live teaching ready: {topic}",
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["live_prompt_generated", f"topic:{topic}"],
            state_updates={"live_prompt": prompt_data}
        )
    
    async def _handle_cultural_scenario(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🎭 Generate cultural practice scenario."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        scenario_type = payload.get('scenario_type', 'random')
        
        prompt = f"""
GENERATE CULTURAL PRACTICE SCENARIO

Student: From {cultural_state.profile.home_country}, now in {cultural_state.profile.current_country}
Level: {cultural_state.profile.language_level}
Scenario Type: {scenario_type}

Create realistic scenario highlighting cultural differences. Include:
1. Setting description
2. Cultural challenge/difference
3. Expected behavior
4. Common mistakes
5. Sample dialogue (3-4 exchanges)
6. Key vocabulary (5 items)

Output as JSON.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.DEEP
        ))
        
        try:
            import re
            json_match = re.search(r'\{[\s\S]*\}', response.content)
            scenario_data = json.loads(json_match.group()) if json_match else {}
        except:
            scenario_data = {"title": "Cultural Practice", "setting": "General"}
        
        return AgentResult(
            success=True,
            response=f"🎭 Scenario: {scenario_data.get('title', 'Cultural Practice')}",
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["scenario_generated", f"type:{scenario_type}"],
            state_updates={"scenario": scenario_data}
        )
    
    # =========================================================================
    # LOCATION & SAFETY HANDLERS
    # =========================================================================
    
    async def _handle_location_change(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """📍 Handle location change with cultural context."""
        
        location = payload.get('user_location', payload.get('location', 'Unknown'))
        trigger_voice = payload.get('trigger_voice', False)
        
        cultural_state = _get_cultural_state(user_id, self.db)
        cultural_state.current_location = location
        
        # Detect mode
        mode, strategy = self._detect_mode(location)
        cultural_state.active_mode = mode
        
        # Check if safehouse
        is_safehouse = any(
            sh.name.lower() in location.lower() or location.lower() in sh.name.lower()
            for sh in cultural_state.safe_houses
        )
        
        _save_cultural_state(user_id, cultural_state, self.db)
        
        interventions = []
        response_text = f"📍 {location} | Mode: {mode}"
        
        if trigger_voice:
            prompt = f"""
Student from {cultural_state.profile.home_country} arrived at: {location}
Mode: {mode}
Is safehouse: {is_safehouse}

Give 1-sentence cultural location tip if relevant.
"""
            
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="radius",
                mode=ReasoningMode.REFLEX
            ))
            
            response_text = response.content
            
            intervention_id = self.create_intervention(
                user_id, "LOCATION_CHANGE", response_text, strategy=strategy
            )
            interventions.append(intervention_id)
        
        radius_update = {
            "radius": {
                "cultural": cultural_state.to_dict(),
                "current_location": location,
                "active_mode": mode,
                "safehouse_status": "SECURE" if is_safehouse else "STANDARD",
                "last_updated": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, radius_update)
        
        return AgentResult(
            success=True,
            response=response_text,
            actions_taken=["location_changed", f"mode:{mode}"],
            interventions_created=interventions,
            state_updates=radius_update
        )
    
    async def _handle_local_scan(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🔍 Analyze current area for student safety."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        location = payload.get('location', cultural_state.current_location)
        scan_focus = payload.get('focus', 'general')
        
        prompt = f"""
LOCAL AREA SCAN for international student

Student: From {cultural_state.profile.home_country}
Location: {location}
Focus: {scan_focus}

Generate local area briefing with:
1. Area overview (safe for students?)
2. Useful services
3. Cultural norms for this area
4. Emergency resources
5. Quick tips

Output JSON: area_name, safety_rating (1-10), overview, useful_services, cultural_norms, quick_tips, student_friendly
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.DEEP
        ))
        
        try:
            import re
            json_match = re.search(r'\{[\s\S]*\}', response.content)
            scan_data = json.loads(json_match.group()) if json_match else {}
        except:
            scan_data = {"area_name": location, "safety_rating": 7, "student_friendly": True}
        
        cultural_state.local_scan_result = scan_data
        _save_cultural_state(user_id, cultural_state, self.db)
        
        return AgentResult(
            success=True,
            response=f"🔍 Area scan: Safety {scan_data.get('safety_rating', 'N/A')}/10",
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["local_scan", f"safety:{scan_data.get('safety_rating', 'N/A')}"],
            state_updates={"scan_result": scan_data}
        )
    
    async def _handle_safehouse_add(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🏠 Add a safe location."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        
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
        _save_cultural_state(user_id, cultural_state, self.db)
        
        return AgentResult(
            success=True,
            response=f"🏠 Safehouse added: {safehouse.name}",
            actions_taken=["safehouse_added"],
            state_updates={"safehouse": safehouse.to_dict()}
        )
    
    async def _handle_safehouse_status(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🏠 Check safehouse status."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        current_location = cultural_state.current_location
        
        is_safehouse = any(
            sh.name.lower() in current_location.lower() or current_location.lower() in sh.name.lower()
            for sh in cultural_state.safe_houses
        )
        
        return AgentResult(
            success=True,
            response="🏠 In safehouse" if is_safehouse else "📍 Not in safehouse",
            actions_taken=["safehouse_checked"],
            state_updates={
                "current_location": current_location,
                "in_safehouse": is_safehouse,
                "safehouses": [sh.to_dict() for sh in cultural_state.safe_houses]
            }
        )
    
    async def _handle_emergency_cultural(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🆘 Emergency cultural assistance."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        situation = payload.get('situation', '')
        urgency = payload.get('urgency', 'normal')
        
        prompt = f"""
🆘 EMERGENCY CULTURAL ASSISTANCE

Student: From {cultural_state.profile.home_country}, speaks {cultural_state.profile.home_language}
Currently in: {cultural_state.profile.current_country}
Location: {cultural_state.current_location}

SITUATION: {situation}
URGENCY: {urgency}

Provide IMMEDIATE help:
1. Quick phrase to say now (in {cultural_state.profile.target_language})
2. What to do next (2-3 steps)
3. Cultural context
4. If high urgency: emergency numbers

Be calm, clear, actionable.
"""
        
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.DEEP
        ))
        
        interventions = []
        if urgency in ['high', 'emergency']:
            intervention_id = self.create_intervention(
                user_id, "CULTURAL_EMERGENCY", 
                f"🆘 Cultural Emergency\n\n{response.content}",
                strategy="URGENT"
            )
            interventions.append(intervention_id)
        
        return AgentResult(
            success=True,
            response=response.content,
            thought_id=response.thought_signature.thought_id if response.thought_signature else None,
            actions_taken=["emergency_help", f"urgency:{urgency}"],
            interventions_created=interventions
        )
    
    # =========================================================================
    # MARATHON AUTONOMOUS HANDLERS
    # =========================================================================
    
    async def _handle_marathon_cultural_morning(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🌅 Marathon Cultural Morning - AUTONOMOUS."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        today = datetime.now().strftime("%Y-%m-%d")
        
        components = []
        
        # === 1. GENERATE TODAY'S LESSON ===
        if not cultural_state.today_lesson or cultural_state.today_lesson.date != today:
            lesson_types = ["language", "cultural_etiquette", "local_knowledge", "scenario_practice"]
            lesson_type = lesson_types[cultural_state.lesson_streak % len(lesson_types)]
            
            lesson_prompt = f"""
Generate daily micro-lesson for international student from {cultural_state.profile.home_country}
in {cultural_state.profile.current_country}. Level: {cultural_state.profile.language_level}.
Type: {lesson_type}. Day {cultural_state.lesson_streak + 1}.

Output JSON: title, content, practice_scenario, key_phrases (list), cultural_tip, difficulty
"""
            
            lesson_response = await self.engine.reason(ReasoningRequest(
                prompt=lesson_prompt,
                user_id=user_id,
                agent="radius",
                mode=ReasoningMode.DEEP
            ))
            
            try:
                import re
                json_match = re.search(r'\{[\s\S]*\}', lesson_response.content)
                lesson_data = json.loads(json_match.group()) if json_match else {}
            except:
                lesson_data = {"title": "Cultural Awareness", "content": "Practice today.", "key_phrases": []}
            
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
            components.append("lesson_generated")
        else:
            components.append("lesson_existing")
        
        # === 2. GENERATE LIVE AGENT PROMPT ===
        live_prompt = f"""You are a cultural coach for a student from {cultural_state.profile.home_country} 
adapting to {cultural_state.profile.current_country}. Today's focus: {cultural_state.today_lesson.title if cultural_state.today_lesson else 'General'}.
Be patient, encouraging, culturally sensitive. Level: {cultural_state.profile.language_level}."""
        
        cultural_state.live_agent_prompt = live_prompt
        components.append("live_prompt_ready")
        
        _save_cultural_state(user_id, cultural_state, self.db)
        
        # === 3. GENERATE MORNING BRIEFING ===
        briefing_prompt = f"""
CULTURAL MARATHON MORNING

Student: From {cultural_state.profile.home_country}
Day {cultural_state.lesson_streak} of cultural adaptation
Lesson: {cultural_state.today_lesson.title if cultural_state.today_lesson else 'General'}

Generate 3-sentence encouraging morning message. End with "Let's make today count!"
"""
        
        briefing_response = await self.engine.reason(ReasoningRequest(
            prompt=briefing_prompt,
            user_id=user_id,
            agent="radius",
            mode=ReasoningMode.REFLEX
        ))
        
        # Create notification
        intervention_id = self.create_intervention(
            user_id, "CULTURAL_MORNING",
            f"🌅 CULTURAL MORNING\n\n{briefing_response.content}",
            strategy="WARM"
        )
        
        return AgentResult(
            success=True,
            response=briefing_response.content,
            thought_id=briefing_response.thought_signature.thought_id if briefing_response.thought_signature else None,
            actions_taken=["marathon_morning"] + components,
            interventions_created=[intervention_id],
            state_updates={
                "lesson": cultural_state.today_lesson.to_dict() if cultural_state.today_lesson else None,
                "streak": cultural_state.lesson_streak,
                "live_prompt": live_prompt
            }
        )
    
    async def _handle_mode_request(
        self,
        user_id: str,
        payload: Dict[str, Any],
        context: StateContext
    ) -> AgentResult:
        """🎚️ Manual mode change."""
        
        cultural_state = _get_cultural_state(user_id, self.db)
        requested_mode = payload.get('mode', 'STANDARD')
        duration = payload.get('duration_minutes')
        
        cultural_state.active_mode = requested_mode
        _save_cultural_state(user_id, cultural_state, self.db)
        
        strategy_map = {
            'DEEP_FOCUS': 'WHISPER',
            'WORKOUT': 'ENERGETIC',
            'REST': 'WARM',
            'MOBILE': 'BRIEF',
            'CAMPUS': 'FOCUSED',
        }
        strategy = strategy_map.get(requested_mode, 'NEUTRAL')
        
        radius_update = {
            "radius": {
                "active_mode": requested_mode,
                "mode_source": "manual",
                "notification_strategy": strategy,
            }
        }
        self.update_state_cache(user_id, radius_update)
        
        return AgentResult(
            success=True,
            response=f"🎚️ Mode: {requested_mode}",
            actions_taken=["mode_changed", f"mode:{requested_mode}"],
            state_updates=radius_update
        )


# Async compatibility wrapper
async def run_radius_agent(db_helper, payload, context):
    """Async compatibility wrapper."""
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    
    agent = RadiusAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    if not user_id:
        return context.res.json({"error": "No userId"})
    
    trigger_event = payload.get('type', 'location_change')
    
    result = await agent.run(user_id, payload, trigger_event)
    
    return context.res.json(result.to_dict())
