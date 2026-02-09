"""
STUDY AGENT - Hierarchical Academic Strategist
==============================================
Architecture:
  Layer 1 (Strategy): Generates monthly_plans with high-level semester goals
  Layer 2 (Tactical): Generates daily schedule for the CURRENT month only

Design Principle:
  User Profile is the "Constitution" - job schedules, prayer times, gym routines
  are immutable laws that must NEVER be violated.
"""

from typing import Dict, Any, List, Optional
from dataclasses import dataclass
from datetime import datetime, timedelta
from dateutil.relativedelta import relativedelta
import json
import re
import uuid
import io

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext
from ..tools.proactive_content_engine import ProactiveContentEngine


@dataclass
class UserConstitution:
    """Immutable constraints extracted from user profile."""
    job_schedule: str
    prayer_blocks: str
    health_routine: str
    energy_pattern: str
    learning_pace: str
    weakness_defense: str
    academic_goal: str
    
    def to_prompt(self) -> str:
        return f"""
*** USER CONSTITUTION (IMMUTABLE - DO NOT VIOLATE) ***
1. JOB: {self.job_schedule}
2. RELIGIOUS: {self.prayer_blocks}
3. HEALTH: {self.health_routine}
4. ENERGY: {self.energy_pattern}
5. PACE: {self.learning_pace}
6. DEFENSE: {self.weakness_defense}
7. GOAL: {self.academic_goal}
"""


class StudyAgent(BaseAgent):
    """
    The Study Agent implements a two-layer hierarchical planning system:
    - Strategy Layer: Creates monthly goals for the entire semester
    - Tactical Layer: Creates detailed daily tasks for one month at a time
    """
    
    SYSTEM_PROMPT = """You are the Senior Academic Director for Kaironex.
Your mission is to engineer student success through strategic time and content management.

HIERARCHY OF TRUTH:
1. USER PROFILE - If user says "Prayer 5 times" or "Job at 5 PM", this is LAW
2. SYLLABUS - This dictates WHAT to learn
3. AI STRATEGY - This dictates HOW to learn

OUTPUT: Always return valid JSON without markdown code blocks."""

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="study",
            display_name="Semester Director",
            system_instruction=self.SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.DEEP,
            max_thinking_tokens=65536,
            enable_thought_signatures=True,
            enable_marathon=True  # MARATHON AGENT
        )

    async def process(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        self.log_heartbeat(user_id, f"STUDY|{payload.get('type', 'unknown')}")
        event_type = payload.get('type', payload.get('status', 'focus_update'))
        
        if event_type == 'schedule_request':
            return await self._handle_hierarchical_planning(user_id, payload, context)
        elif event_type == 'resource_ingestion':
            return await self._handle_resource_ingestion(user_id, payload, context)
        elif event_type == 'content_request':
            return await self._handle_content_request(user_id, payload, context)
        
        return AgentResult(success=True, response="Event tracked", actions_taken=["tracked"])

    # =========================================================================
    # LAYER 0: CONSTITUTION EXTRACTION
    # =========================================================================
    
    def _extract_constitution(self, profile: Dict[str, Any]) -> UserConstitution:
        """Converts user profile JSON into strict AI constraints."""
        
        # Job Schedule
        if profile.get('hasJob'):
            job = f"{profile.get('jobSchedule', 'Unknown')} - {profile.get('jobDescription', 'Work')}"
        else:
            job = "No job - full flexibility"
        
        # Prayer Times (for Muslim students)
        non_neg = profile.get('nonNegotiables', '')
        if 'Muslim' in non_neg or 'prayer' in non_neg.lower():
            prayer = "5 Daily Prayers - allocate ~15min breaks at Fajr, Dhuhr, Asr, Maghrib, Isha"
        else:
            prayer = "No religious time blocks"
        
        # Health Routine
        health = non_neg if non_neg else "No fixed health routine"
        
        # Energy Pattern
        energy_pref = profile.get('energyPreference', 'DAY')
        if 'NIGHT' in energy_pref.upper():
            energy = "NIGHT OWL - Deep work best after 8 PM, light tasks before noon"
        else:
            energy = "EARLY BIRD - Deep work best before noon, wind down evening"
        
        # Learning Pace
        learning = profile.get('learningStyle', 'Average')
        if 'slow' in learning.lower():
            pace = "SLOW LEARNER - Allocate 1.5x time per topic, break into micro-tasks"
        else:
            pace = "STANDARD PACE - Normal time allocation"
        
        # Weakness Defense
        weakness = profile.get('productivityKiller', '')
        if weakness:
            defense = f"ANTI-FAILURE: User struggles with '{weakness}' - structure tasks with clear boundaries"
        else:
            defense = "No specific weakness identified"
        
        # Academic Goal
        goal = f"Target GPA: {profile.get('targetCgpa', '3.5')} | Reason: {profile.get('desiredCgpaReason', 'Academic success')}"
        
        return UserConstitution(
            job_schedule=job,
            prayer_blocks=prayer,
            health_routine=health,
            energy_pattern=energy,
            learning_pace=pace,
            weakness_defense=defense,
            academic_goal=goal
        )

    # =========================================================================
    # MAIN HANDLER: HIERARCHICAL PLANNING (OPTIMIZED - SINGLE API CALL)
    # =========================================================================
    
    async def _handle_hierarchical_planning(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """
        Executes combined planning (strategy + tactical in ONE API call for efficiency):
        1. Generate ALL monthly strategies for entire semester
        2. Generate detailed schedule for current month
        Both happen in a single Gemini call to save API quota!
        """
        
        # Step 1: Gather Intelligence
        user_doc = self.db.get_user_doc(user_id)
        if not user_doc:
            return AgentResult(success=False, response="User not found", error="no_user")
        
        profile = json.loads(user_doc.get('studentprofile_json', '{}'))
        constitution = self._extract_constitution(profile)
        
        # Step 2: Gather Academic Content
        resources = self.db.get_user_resources(user_id)
        content_context = self._build_content_context(resources)
        
        if not content_context.strip():
            return AgentResult(
                success=False, 
                response="No academic content found. Please upload your syllabus or course materials.",
                actions_taken=["aborted_no_content"]
            )
        
        # Step 3: Extract Timeline
        timeline = await self._extract_timeline(user_id, content_context, profile)
        
        # Step 4: COMBINED STRATEGY + TACTICAL (Single API call!)
        # This generates ALL monthly plans + current month's daily schedule in ONE request
        print("🧠⚡ Executing COMBINED Strategy + Tactical Layer (single API call)...")
        monthly_plans = await self._generate_monthly_strategy(
            user_id, constitution, timeline, content_context, profile
        )
        
        if not monthly_plans:
            return AgentResult(success=False, response="Failed to generate semester strategy", error="strategy_failed")
        
        # Get task count from DB (tasks were created inside _generate_monthly_strategy)
        schedule = self.db.get_schedule(user_id)
        task_count = len(schedule) if schedule else 0
        
        return AgentResult(
            success=True,
            response=f"Semester strategy created with {len(monthly_plans)} months. Current month scheduled with {task_count} tasks.",
            actions_taken=["strategy_created", f"months_{len(monthly_plans)}", f"tasks_{task_count}"]
        )

    # =========================================================================
    # LAYER 1: STRATEGY ENGINE (Monthly Plans) - SEMESTER-WIDE PLANNING
    # =========================================================================
    
    async def _generate_monthly_strategy(
        self, 
        user_id: str, 
        constitution: UserConstitution,
        timeline: Dict[str, Any],
        content: str,
        profile: Dict[str, Any]
    ) -> List[Dict]:
        """
        Creates COMPREHENSIVE monthly goals for the ENTIRE SEMESTER.
        Each monthly plan has ~5000-15000 chars of rich context.
        Also generates daily schedule for CURRENT month in same call (API efficiency).
        """
        
        # SMART DATE HANDLING: Start from TODAY if mid-semester signup
        today = datetime.now()
        semester_start = datetime.strptime(timeline.get('start_date', today.strftime('%Y-%m-%d')), '%Y-%m-%d')
        semester_end = datetime.strptime(timeline.get('end_date', (today + timedelta(days=120)).strftime('%Y-%m-%d')), '%Y-%m-%d')
        
        # If today is past semester start, adjust to start from today
        effective_start = max(today, semester_start)
        
        # Calculate remaining months in semester
        num_months = max(1, min(6, (semester_end.year - effective_start.year) * 12 + semester_end.month - effective_start.month + 1))
        
        # Calculate days remaining in current month
        days_remaining_this_month = (effective_start.replace(day=28) + timedelta(days=4) - effective_start).days
        days_remaining_this_month = min(days_remaining_this_month, (semester_end - effective_start).days + 1)
        if effective_start.month != semester_end.month:
            next_month_start = (effective_start.replace(day=1) + timedelta(days=32)).replace(day=1)
            days_remaining_this_month = (next_month_start - effective_start).days
        
        print(f"📅 Smart Planning: Today={today.strftime('%Y-%m-%d')}, Effective Start={effective_start.strftime('%Y-%m-%d')}")
        print(f"📅 Semester ends {semester_end.strftime('%Y-%m-%d')}, Planning {num_months} months, {days_remaining_this_month} days this month")
        
        # Calculate study hours needed based on CGPA target
        target_cgpa = profile.get('targetCgpa', '3.5')
        study_hours_per_day = self._calculate_study_hours(target_cgpa, profile)
        
        prompt = f"""
═══════════════════════════════════════════════════════════════════════════════
                         KAIRONEX PHILOSOPHY
              "Manage your study. Live your life."
═══════════════════════════════════════════════════════════════════════════════

You are NOT scheduling a robot. You are helping a REAL HUMAN STUDENT succeed.

REAL STUDENT LIFE (understand this deeply):
- Wake up, morning routine, commute to university
- Classes throughout the day (these are BLOCKED - not your concern)
- Breaks between classes (chatting with friends, grabbing coffee)
- Commute home (tired, decompressing)
- Rest, gym, family time, meals (SACRED - don't touch)
- Evening study sessions (this is where you help)
- Phone time, relaxation before sleep (human needs this!)
- Sleep (absolutely non-negotiable)

KAIRONEX'S ROLE:
✅ Schedule STUDY TASKS in the NATURAL GAPS of student's day
✅ Respect all blocked times (class, work, prayer, meals, sleep)
✅ Build in rest, commute, social time - assume 2+ hours daily is "life"
✅ Vary intensity based on deadlines (normal days vs crunch time)
✅ Make studying SUSTAINABLE, not a torture routine
❌ DON'T fill every minute with tasks
❌ DON'T create unrealistic back-to-back study blocks
❌ DON'T ignore human needs for breaks, socializing, rest

═══════════════════════════════════════════════════════════════════════════════
                         TODAY'S CONTEXT
═══════════════════════════════════════════════════════════════════════════════

DATE: {today.strftime('%A, %B %d, %Y')}
PLANNING: {effective_start.strftime('%B %d')} to {semester_end.strftime('%B %d, %Y')} ({num_months} months)
DAYS TO PLAN THIS MONTH: {days_remaining_this_month}

═══════════════════════════════════════════════════════════════════════════════
                         STUDENT PROFILE
═══════════════════════════════════════════════════════════════════════════════

{constitution.to_prompt()}

TARGET CGPA: {target_cgpa}
ESTIMATED STUDY NEEDED: ~{study_hours_per_day} hours/day on STUDY DAYS

FULL PROFILE:
{json.dumps(profile, indent=2)}

═══════════════════════════════════════════════════════════════════════════════
                    ACADEMIC MATERIALS (Syllabus, Schedule, Assignments)
═══════════════════════════════════════════════════════════════════════════════

{content[:120000]}

═══════════════════════════════════════════════════════════════════════════════
                    REALISTIC DAILY PATTERNS
═══════════════════════════════════════════════════════════════════════════════

NORMAL WEEKDAY (no deadline pressure):
- Morning: Maybe 30 min review before leaving
- University: Classes, breaks (Kaironex doesn't touch this)
- After classes: Student might chat with friends, grab food
- Commute home: ~30-60 min (travel audio learning possible)
- Home arrival: Rest needed! 1-2 hours before productive
- Evening window: 2-3 focused study blocks with breaks
- Night: 1-2 hours if energy (night owl) or just preview + sleep

WEEKEND (more flexibility):
- Morning: Sleep in OR early morning study (depends on type)
- Midday: Longer deep work sessions possible
- Breaks: Social time, errands, family
- Evening: Can be productive if recharged

EXAM/DEADLINE WEEK (intensity increases):
- More study hours squeezed in
- Shorter breaks (but STILL breaks!)
- Travel time becomes study time
- Evening sessions extend (but SLEEP still protected)
- Weekend becomes intensive (but still human)

═══════════════════════════════════════════════════════════════════════════════
                    WHAT TO CREATE
═══════════════════════════════════════════════════════════════════════════════

PART 1: MONTHLY STRATEGIES for ALL {num_months} months
- Each month: 5000-15000 chars of DETAILED planning
- Week-by-week breakdown with specific goals
- All assignments, deadlines, exams tracked
- Course-specific study strategies
- Risk factors and mitigation
- How intensity varies (normal vs crunch weeks)

PART 2: DAILY SCHEDULE for {days_remaining_this_month} days starting {effective_start.strftime('%Y-%m-%d')}
For EACH day, create 3-6 STUDY TASKS that:

1. FIT INTO NATURAL GAPS (not during blocked times)
2. VARY IN LENGTH:
   - Quick tasks: 20-30 min (review, preview, flashcards)
   - Medium tasks: 45-60 min (reading, note-taking, problems)
   - Deep tasks: 90-120 min (project work, lab coding, essays)
   
3. INCLUDE DIFFERENT TYPES:
   - "study" - main learning sessions
   - "review" - spaced repetition, yesterday's material
   - "practice" - problems, exercises, coding
   - "project" - assignments, labs, essays
   - "travel" - commute audio learning (ONLY during commute times)
   - "blocked" - classes, work, prayer (mark but don't schedule around)

4. RESPECT ENERGY PATTERNS:
   - Morning person: Heavy tasks early
   - Night owl: Heavy tasks evening
   - After work/class: Allow decompression before deep study

5. BUILD IN VARIATION:
   - Not same schedule every day
   - Weekends different from weekdays
   - Some days lighter, some heavier
   - Extra intensity ONLY when deadline/exam approaching

═══════════════════════════════════════════════════════════════════════════════
                    OUTPUT JSON FORMAT
═══════════════════════════════════════════════════════════════════════════════

{{
  "semester_strategy": {{
    "overview": "3-4 paragraphs: Student analysis, strategic approach, key challenges, success path",
    "total_months": {num_months},
    "months": [
      {{
        "month_index": 1,
        "phase_title": "FOUNDATION PHASE",
        "date_range": "{effective_start.strftime('%b %d')} - ...",
        "goals_context": "MONTH 1: FOUNDATION PHASE\\n\\n═══ OVERVIEW ═══\\nThis month focuses on establishing strong foundations while maintaining sustainable study habits...\\n\\n═══ KEY OBJECTIVES ═══\\n1. [CS450] Ch 1-3, Lab 1 (Due Feb 15)\\n2. [MAT301] Vector Spaces, PS 1-2\\n...\\n\\n═══ WEEK 1: CALIBRATION ═══\\nMon: Ease into semester...\\nTue: ...\\n...\\n\\n═══ WEEK 2: BUILDING RHYTHM ═══\\n...\\n\\n═══ DEADLINES ═══\\n- Feb 10: CS460 Lab 1\\n- Feb 15: CS450 Lab 1\\n...\\n\\n═══ INTENSITY MAP ═══\\nNormal pace weeks 1-2, increase week 3 (midterm prep)\\n\\n═══ STUDY TACTICS ═══\\n- OS: Simulate process states while reading\\n- Math: Daily 20 min problem practice\\n...",
        "status": "active"
      }}
    ]
  }},
  "current_month_schedule": {{
    "days": [
      {{
        "date": "{effective_start.strftime('%Y-%m-%d')}",
        "day_type": "weekday",
        "intensity": "normal",
        "day_summary": "Gentle start: Review setup + evening reading",
        "tasks": [
          {{
            "startTime": "07:00",
            "endTime": "07:25",
            "title": "Morning Review: Yesterday's Concepts",
            "type": "review",
            "priority": 5,
            "location": "Home",
            "subject": "Multi-subject",
            "difficulty": "beginner",
            "content_mode": "cram",
            "learning_objectives": ["Quick recall of recent material"],
            "topics": "5-minute flashcard review per subject. Light activation.",
            "resource_hints": ["flashcards", "concept summaries"],
            "prerequisites": [],
            "deliverables": [],
            "verification": null
          }},
          {{
            "startTime": "08:30",
            "endTime": "09:00",
            "title": "Commute: OS Concepts Audio",
            "type": "travel",
            "priority": 4,
            "location": "Transit",
            "subject": "CS450 Operating Systems",
            "difficulty": "beginner", 
            "content_mode": "travel",
            "learning_objectives": ["Reinforce process concepts passively"],
            "topics": "Listen to chapter summary audio. Preview today's lecture topic.",
            "resource_hints": ["process states audio", "OS podcast"],
            "prerequisites": [],
            "deliverables": [],
            "verification": null
          }},
          {{
            "startTime": "09:00",
            "endTime": "10:30",
            "title": "CLASS: CS450 Operating Systems",
            "type": "blocked",
            "priority": 10,
            "location": "University",
            "subject": "CS450",
            "topics": "Class time - attend and take notes"
          }},
          {{
            "startTime": "20:00",
            "endTime": "21:30",
            "title": "Deep Work: CS450 Chapter 1 Reading",
            "type": "study",
            "priority": 8,
            "location": "Home/Library",
            "subject": "CS450 Operating Systems",
            "difficulty": "intermediate",
            "content_mode": "deep_dive",
            "learning_objectives": [
              "Define process vs program vs thread",
              "Diagram 5-state process model",
              "Explain context switching"
            ],
            "topics": "═══ PHASE 1: INPUT (40 min) ═══\\nRead Chapter 1 sections 1.1-1.3\\nTake Cornell notes on key concepts\\n\\n═══ PHASE 2: PROCESS (30 min) ═══\\nCreate concept map of process states\\nWrite 3 examples of each state transition\\n\\n═══ PHASE 3: OUTPUT (20 min) ═══\\nSolve end-of-chapter problems 1-3\\nWrite summary in own words",
            "resource_hints": ["process concepts", "5-state model", "context switch"],
            "prerequisites": ["Basic programming knowledge"],
            "deliverables": ["Chapter notes", "Concept map", "Problems 1-3"],
            "verification": {{"type": "self_check", "questions": ["What triggers each state transition?"]}}
          }},
          {{
            "startTime": "21:45",
            "endTime": "22:15",
            "title": "Light Review + Preview Tomorrow",
            "type": "review",
            "priority": 5,
            "location": "Home",
            "subject": "Multi-subject",
            "difficulty": "beginner",
            "content_mode": "cram",
            "learning_objectives": ["Consolidate today's learning", "Prime tomorrow's topics"],
            "topics": "Review today's notes (10 min). Skim tomorrow's readings (15 min). Write 3 questions to answer tomorrow.",
            "resource_hints": [],
            "prerequisites": [],
            "deliverables": ["3 questions for tomorrow"],
            "verification": null
          }}
        ]
      }}
    ]
  }}
}}

CRITICAL REMINDERS:
1. Create ALL {num_months} months with FULL detail (5000+ chars each)
2. Create {days_remaining_this_month} days of schedule (NOT just 1 day!)
3. Each day should have 3-6 study tasks fitting into natural gaps
4. Respect blocked times - mark them but don't disrupt
5. Build in realistic breaks and variation
6. Increase intensity ONLY when deadlines/exams approach
7. Match tasks to student's energy pattern (morning/night person)
8. Total study time per day should average ~{study_hours_per_day} hours
"""
        
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.DEEP,
                max_thinking_tokens=65536,  # Large output to avoid truncation
                system_instruction="Output valid JSON only. No markdown code blocks. Make goals_context VERY detailed (5000+ chars each)."
            ))
            
            print(f"📊 Strategy response length: {len(response.content)} chars")
            
            # DEBUG: Show first 500 chars of raw response
            print(f"🔍 Raw response preview: {response.content[:500]}")
            
            data = self._parse_json(response.content)
            
            # DEBUG: Show parsed type and structure
            print(f"🔍 Parsed type: {type(data)}")
            if isinstance(data, dict):
                print(f"🔍 Top-level keys: {list(data.keys())}")
            
            if not isinstance(data, dict):
                print(f"⚠️ Unexpected response type: {type(data)}")
                # Try one more time - maybe it's a string that needs re-parsing
                if isinstance(data, str):
                    data = self._parse_json(data)
                if not isinstance(data, dict):
                    return []
            
            # Extract semester strategy - try multiple possible structures
            strategy = data.get('semester_strategy', {})
            months_data = strategy.get('months', [])
            
            # Fallback 1: months directly at root
            if not months_data and 'months' in data:
                months_data = data.get('months', [])
                print(f"🔍 Found months at root level: {len(months_data)}")
            
            # Fallback 2: response is array at root
            if not months_data and isinstance(data, list):
                months_data = data
                print(f"🔍 Response is array with {len(months_data)} items")
            
            # Fallback 3: look for any key containing 'month'
            if not months_data:
                for key in data.keys():
                    if 'month' in key.lower():
                        val = data[key]
                        if isinstance(val, list):
                            months_data = val
                            print(f"🔍 Found months under key '{key}': {len(months_data)}")
                            break
                        elif isinstance(val, dict) and 'months' in val:
                            months_data = val['months']
                            print(f"🔍 Found months under '{key}.months': {len(months_data)}")
                            break
            
            # Fallback 4: strategy might be wrapped differently
            if not months_data:
                for key in ['strategy', 'plan', 'semester_plan', 'academic_plan']:
                    if key in data and isinstance(data[key], dict):
                        if 'months' in data[key]:
                            months_data = data[key]['months']
                            print(f"🔍 Found months under '{key}.months': {len(months_data)}")
                            break
            if not months_data and isinstance(data, list):
                months_data = data
                print(f"🔍 Response is array with {len(months_data)} items")
            
            if not months_data:
                print("⚠️ No months in strategy response")
                print(f"🔍 Full response structure: {json.dumps(data, indent=2)[:3000]}")
                # Save raw response for debugging (saves API calls!)
                debug_file = f"debug_response_{user_id}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
                with open(debug_file, 'w', encoding='utf-8') as f:
                    f.write(f"=== RAW RESPONSE ({len(response.content)} chars) ===\n")
                    f.write(response.content)
                    f.write(f"\n\n=== PARSED DATA ===\n")
                    f.write(json.dumps(data, indent=2))
                print(f"💾 Saved debug response to: {debug_file}")
                return []
            
            # Clear existing plans
            self.db.clear_monthly_plans(user_id)
            
            saved_plans = []
            current_date = effective_start
            
            for i, month in enumerate(months_data):
                month_start = current_date
                # Calculate proper month end
                if i < len(months_data) - 1:
                    month_end = (month_start.replace(day=1) + timedelta(days=32)).replace(day=1) - timedelta(days=1)
                else:
                    month_end = semester_end
                month_end = min(month_end, semester_end)
                
                goals_text = month.get('goals_context', '')
                print(f"  Month {i+1} goals length: {len(goals_text)} chars")
                
                plan_data = {
                    'month_index': i + 1,
                    'start_date': month_start.isoformat(),
                    'end_date': month_end.isoformat(),
                    'status': 'active' if i == 0 else 'pending',
                    'goals_context': goals_text[:99000],  # Use the big capacity!
                    'achieved_context': '',
                    'missed_context': ''
                }
                
                self.db.create_monthly_plan(user_id, plan_data)
                saved_plans.append(plan_data)
                current_date = month_end + timedelta(days=1)
            
            print(f"✅ Created {len(saved_plans)} monthly plans for entire semester")
            
            # Process current month schedule from same response (efficiency!)
            current_schedule = data.get('current_month_schedule', {})
            days_data = current_schedule.get('days', [])
            
            if days_data:
                task_count = await self._process_schedule_days(user_id, days_data)
                print(f"✅ Created {task_count} tasks for current month from combined response")
            
            return saved_plans
            
        except Exception as e:
            print(f"❌ Strategy Generation Error: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def _get_scheduling_constraints(self, constitution: UserConstitution) -> str:
        """Extract scheduling constraints from constitution."""
        constraints = []
        if constitution.job_schedule:
            constraints.append(f"JOB SCHEDULE (non-negotiable): {constitution.job_schedule}")
        if constitution.prayer_blocks:
            constraints.append(f"PRAYER TIMES (non-negotiable): {constitution.prayer_blocks}")
        if constitution.health_routine:
            constraints.append(f"HEALTH ROUTINE: {constitution.health_routine}")
        if constitution.learning_pace:
            constraints.append(f"LEARNING STYLE: {constitution.learning_pace}")
        if constitution.energy_pattern:
            constraints.append(f"ENERGY PATTERNS: {constitution.energy_pattern}")
        constraints.append("- Never schedule during: class times, work hours, prayer times")
        constraints.append("- 1.5-2 hour decompression gap after work/class before deep study")
        constraints.append("- No tasks 11PM-7AM unless explicit 'Night Owl' preference")
        constraints.append("- Keep 12-2 PM and 7-9 PM flexible for meals/breaks")
        return "\n".join(constraints)
    
    def _calculate_study_hours(self, target_cgpa: str, profile: Dict) -> float:
        """
        Calculate required daily study hours based on CGPA target.
        Research-backed: Higher GPA requires exponentially more study time.
        """
        # Parse CGPA target
        try:
            if '-' in str(target_cgpa):
                cgpa = float(target_cgpa.split('-')[1])  # Take higher end
            else:
                cgpa = float(target_cgpa)
        except:
            cgpa = 3.5  # Default
        
        # Base hours calculation (research-backed formula)
        # 3.0 GPA = 3 hours, 3.5 = 4.5 hours, 3.8 = 6 hours, 4.0 = 8 hours
        if cgpa >= 3.9:
            base_hours = 7.0
        elif cgpa >= 3.7:
            base_hours = 6.0
        elif cgpa >= 3.5:
            base_hours = 5.0
        elif cgpa >= 3.0:
            base_hours = 4.0
        else:
            base_hours = 3.0
        
        # Adjustments based on profile
        has_job = profile.get('hasJob', False)
        if has_job:
            # If working, study time is more precious - need efficiency
            base_hours = max(4.0, base_hours - 0.5)
        
        # Credit load adjustment (more credits = more study)
        credits = profile.get('currentCredits', 15)
        if credits > 18:
            base_hours += 1.0
        elif credits > 15:
            base_hours += 0.5
        
        return round(base_hours, 1)
    
    async def _process_schedule_days(self, user_id: str, days: List[Dict]) -> int:
        """Process days array into schedule tasks. Returns count."""
        # Clear future schedule
        self.db.clear_future_schedule(user_id)
        
        tasks = []
        for day in days:
            date_str = day.get('date', '')
            if not date_str:
                continue
            
            for task in day.get('tasks', []):
                try:
                    task_start = datetime.strptime(f"{date_str}T{task['startTime']}", "%Y-%m-%dT%H:%M")
                    task_end = datetime.strptime(f"{date_str}T{task['endTime']}", "%Y-%m-%dT%H:%M")
                    
                    is_blocked = task.get('type') == 'blocked'
                    
                    task_metadata = {
                        'subject': task.get('subject', ''),
                        'difficulty': task.get('difficulty', 'intermediate'),
                        'content_mode': task.get('content_mode', 'deep_dive'),
                        'learning_objectives': task.get('learning_objectives', []),
                        'resource_hints': task.get('resource_hints', []),
                        'prerequisites': task.get('prerequisites', []),
                        'deliverables': task.get('deliverables', []),
                        'verification': task.get('verification')
                    }
                    
                    tasks.append({
                        'taskId': str(uuid.uuid4()),
                        'userId': user_id,
                        'title': task.get('title', 'Study Session')[:255],
                        'startTime': task_start.isoformat(),
                        'endTime': task_end.isoformat(),
                        'status': 'pending',
                        'type': task.get('type', 'study')[:50],
                        'location': task.get('location', 'Study Space')[:255],
                        'is_flexible': not is_blocked,
                        'priority': max(1, min(10, int(task.get('priority', 5)))),
                        'topics': task.get('topics', '')[:99000],  # Use big capacity!
                        'subject': task.get('subject', '')[:255],
                        'difficulty': task.get('difficulty', 'intermediate')[:50],
                        'content_mode': task.get('content_mode', 'deep_dive')[:50],
                        'metadata_json': json.dumps(task_metadata)[:99000]
                    })
                except Exception as e:
                    print(f"⚠️ Task parse error: {e}")
                    continue
        
        # Batch save
        for task in tasks:
            try:
                self.db.create_schedule_task(task)
            except Exception as e:
                print(f"⚠️ Task save error: {e}")
        
        # =====================================================================
        # JIT CONTENT PREPARATION — Prepare content for today's upcoming tasks
        # =====================================================================
        if tasks:
            try:
                from ..utils.gemini_client import GeminiClient
                content_engine = ProactiveContentEngine(gemini_client=GeminiClient(), db=self.db)
                
                # Find today's study tasks (not blocked) for JIT prep
                today_str = datetime.now().strftime('%Y-%m-%d')
                today_tasks = [
                    t for t in tasks 
                    if t.get('startTime', '').startswith(today_str) 
                    and t.get('type') not in ('blocked',)
                ]
                
                jit_count = 0
                for task_data in today_tasks[:6]:  # Max 6 tasks to avoid rate limits
                    try:
                        prepared = await content_engine.prepare_content_for_rich_task(
                            user_id=user_id,
                            task=task_data
                        )
                        if prepared:
                            jit_count += 1
                    except Exception as e:
                        print(f"⚠️ JIT prep error for '{task_data.get('title', '?')}': {e}")
                        continue
                
                if jit_count > 0:
                    print(f"🎯 JIT Content: Prepared {jit_count} content packages for today's tasks")
                    
            except Exception as e:
                print(f"⚠️ JIT content preparation skipped: {e}")
        
        return len(tasks)

    # =========================================================================
    # LAYER 2: TACTICAL ENGINE (Daily Schedule)
    # =========================================================================
    
    async def _generate_month_schedule(
        self,
        user_id: str,
        constitution: UserConstitution,
        month_plan: Dict[str, Any],
        content: str
    ) -> int:
        """
        Generates detailed daily tasks for a single month.
        Returns number of tasks created.
        """
        
        start_date = datetime.fromisoformat(month_plan['start_date'])
        end_date = datetime.fromisoformat(month_plan['end_date'])
        duration_days = (end_date - start_date).days + 1
        
        prompt = f"""
ACT AS: Tactical Scheduler creating a {duration_days}-day detailed plan.

{constitution.to_prompt()}

MONTH STRATEGY:
{month_plan.get('goals_context', 'General study')}

ACADEMIC CONTENT:
{content[:25000]}

PERIOD: {start_date.strftime('%Y-%m-%d')} to {end_date.strftime('%Y-%m-%d')}

SCHEDULING RULES:
1. BLOCKED TIMES: Never schedule during job hours or prayer times
2. DECOMPRESSION: Leave 1.5-2 hour gap after classes/work before study
3. SLEEP: No tasks between 11 PM and 7 AM unless user is "Night Owl"
4. MEALS: Keep 12-2 PM and 7-9 PM windows flexible for meals
5. CONTENT DENSITY: Each task must have granular, ACTIONABLE breakdown

TASK STRUCTURE REQUIREMENTS (CRITICAL FOR RESOURCE GENERATION):
Each task MUST include rich metadata enabling automated content preparation:

1. LEARNING OBJECTIVES: Specific, measurable outcomes (not "understand X" but "define/apply/solve X")
2. STEP-BY-STEP BREAKDOWN: Micro-tasks with time allocations adding to total duration
3. RESOURCE HINTS: Keywords for content search (topics, theorems, formulas, concepts)
4. PREREQUISITES CHECK: What must student know BEFORE starting
5. VERIFICATION CHECKPOINT: How to confirm completion (quiz, problem set, deliverable)
6. DIFFICULTY LEVEL: beginner/intermediate/advanced for content matching
7. CONTENT MODE: deep_dive (video+articles) | travel (audio) | cram (flashcards) | practice (quizzes)

OUTPUT JSON (no markdown):
{{
  "days": [
    {{
      "date": "YYYY-MM-DD",
      "tasks": [
        {{
          "startTime": "09:00",
          "endTime": "11:00",
          "title": "Deep Work: CS450 Foundations",
          "type": "study",
          "priority": 8,
          "location": "Library",
          "subject": "CS450 Operating Systems",
          "difficulty": "intermediate",
          "content_mode": "deep_dive",
          "learning_objectives": [
            "Define process vs thread and explain key differences",
            "Diagram the 5-state process model with transitions",
            "Implement a simple process scheduler in pseudocode"
          ],
          "topics": "PHASE 1 - INPUT (25 min):\\n- Read Chapter 1: Process Concepts (pp. 1-15)\\n- Watch: Process States Video (search: process scheduling tutorial)\\n\\nPHASE 2 - PROCESS (45 min):\\n- Create concept map: Process vs Thread\\n- Draw 5-state diagram from memory\\n- Annotate with real-world examples\\n\\nPHASE 3 - OUTPUT (30 min):\\n- Solve Problems 1.1-1.5 (process state transitions)\\n- Write pseudocode for Round Robin scheduler\\n- Self-quiz on key definitions\\n\\nPHASE 4 - VERIFY (20 min):\\n- Complete Gatekeeper Quiz (80% threshold)\\n- Document 3 key insights in notes",
          "resource_hints": ["process scheduling", "thread vs process", "5-state process model", "round robin scheduler"],
          "prerequisites": ["Basic programming knowledge", "Understanding of CPU execution"],
          "deliverables": ["Concept map PDF", "5-state diagram", "Problems 1.1-1.5 solutions"],
          "verification": {{
            "type": "quiz",
            "pass_threshold": 0.8,
            "topics_covered": ["process states", "thread concepts", "scheduling basics"]
          }}
        }},
        {{
          "startTime": "14:00",
          "endTime": "14:30",
          "title": "Commute to Campus",
          "type": "travel",
          "priority": 5,
          "location": "Transit",
          "subject": "CS450 Operating Systems",
          "difficulty": "beginner",
          "content_mode": "travel",
          "learning_objectives": [
            "Reinforce process concepts through audio review"
          ],
          "topics": "AUDIO LEARNING SESSION:\\n- Listen to auto-generated summary of today's reading\\n- Key terms review: PCB, context switch, scheduler\\n- Preview: Memory Management concepts for next session",
          "resource_hints": ["process control block audio", "OS concepts podcast"],
          "prerequisites": [],
          "deliverables": [],
          "verification": null
        }},
        {{
          "startTime": "17:00",
          "endTime": "20:00",
          "title": "Work - Junior SE Intern",
          "type": "blocked",
          "priority": 10,
          "location": "Office",
          "topics": "Job commitment - non-negotiable"
        }}
      ]
    }}
  ]
}}
"""
        
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.DEEP,
                system_instruction="Output valid JSON only. No markdown code blocks."
            ))
            
            # DEBUG: Log raw response
            print(f"\n🔍 DEBUG - Raw Gemini response (first 2000 chars):")
            print(response.content[:2000] if response.content else "EMPTY RESPONSE")
            print(f"\n🔍 DEBUG - Response length: {len(response.content) if response.content else 0}")
            
            data = self._parse_json(response.content)
            
            # DEBUG: Log parsed data
            print(f"\n🔍 DEBUG - Parsed data type: {type(data)}")
            if isinstance(data, dict):
                print(f"🔍 DEBUG - Keys in data: {list(data.keys())}")
                print(f"🔍 DEBUG - 'days' in data: {'days' in data}")
                if 'days' in data:
                    print(f"🔍 DEBUG - Number of days: {len(data['days'])}")
            
            days = data.get('days', []) if isinstance(data, dict) else []
            
            # Clear future schedule
            self.db.clear_future_schedule(user_id)
            
            tasks = []
            for day in days:
                date_str = day.get('date', '')
                if not date_str:
                    continue
                    
                for task in day.get('tasks', []):
                    try:
                        task_start = datetime.strptime(f"{date_str}T{task['startTime']}", "%Y-%m-%dT%H:%M")
                        task_end = datetime.strptime(f"{date_str}T{task['endTime']}", "%Y-%m-%dT%H:%M")
                        
                        is_blocked = task.get('type') == 'blocked'
                        
                        # Build rich task metadata for resource generation
                        task_metadata = {
                            'subject': task.get('subject', ''),
                            'difficulty': task.get('difficulty', 'intermediate'),
                            'content_mode': task.get('content_mode', 'deep_dive'),
                            'learning_objectives': task.get('learning_objectives', []),
                            'resource_hints': task.get('resource_hints', []),
                            'prerequisites': task.get('prerequisites', []),
                            'deliverables': task.get('deliverables', []),
                            'verification': task.get('verification')
                        }
                        
                        tasks.append({
                            'taskId': str(uuid.uuid4()),
                            'userId': user_id,
                            'title': task.get('title', 'Study Session')[:255],
                            'startTime': task_start.isoformat(),
                            'endTime': task_end.isoformat(),
                            'status': 'pending',
                            'type': task.get('type', 'study')[:50],
                            'location': task.get('location', 'Study Space')[:255],
                            'is_flexible': not is_blocked,
                            'priority': max(1, min(10, int(task.get('priority', 5)))),
                            'topics': task.get('topics', '')[:10000],
                            'subject': task.get('subject', '')[:255],
                            'difficulty': task.get('difficulty', 'intermediate')[:50],
                            'content_mode': task.get('content_mode', 'deep_dive')[:50],
                            'metadata_json': json.dumps(task_metadata)
                        })
                    except Exception as e:
                        print(f"⚠️ Task parse error: {e}")
                        continue
            
            # Batch save
            count = self.db.batch_create_schedule_tasks(tasks)
            print(f"✅ Created {count} schedule tasks for month")
            return count
            
        except Exception as e:
            print(f"❌ Tactical Generation Error: {e}")
            return 0

    # =========================================================================
    # UTILITIES
    # =========================================================================
    
    def _build_content_context(self, resources: List[Dict]) -> str:
        """Builds context string from user's academic resources.
        
        CAPACITY: We have 250K TPM, use it! Don't truncate unnecessarily.
        """
        context_parts = []
        
        for r in resources:
            title = r.get('title', 'Untitled')
            content = r.get('summaryText', '') or r.get('mined_data', '')
            
            # JIT extraction for PDFs if no pre-extracted content
            if not content:
                file_id = r.get('fileId')
                
                # If no fileId, try to find file by title (filename)
                if not file_id and title:
                    print(f"🔎 Searching for file by name: {title}")
                    file_id = self.db.find_real_file_id(None, title)
                
                if file_id:
                    try:
                        import pypdf
                        print(f"📥 Downloading file: {file_id[:20]}...")
                        file_bytes = self.db.get_file_content(file_id)
                        if file_bytes:
                            reader = pypdf.PdfReader(io.BytesIO(file_bytes))
                            # Extract ALL pages, not just 15! We have 250K capacity
                            content = "\n".join([p.extract_text() or '' for p in reader.pages])
                            print(f"✅ Extracted {len(content)} chars from {title} ({len(reader.pages)} pages)")
                            
                            # Save extracted content to resource for future use
                            resource_id = r.get('$id')
                            if resource_id and content:
                                self.db.update_resource_summary(resource_id, content[:99000])
                    except Exception as e:
                        print(f"⚠️ PDF extraction error for {title}: {e}")
            
            if content:
                # Use up to 50K per resource (we have capacity!)
                context_parts.append(f"=== {title} ===\n{content[:50000]}")
        
        return "\n\n".join(context_parts) if context_parts else ""

    async def _extract_timeline(self, user_id: str, content: str, profile: Dict) -> Dict[str, Any]:
        """Extracts semester start/end dates from content, including FINAL EXAM period."""
        
        semester = profile.get('semester', '1')
        current_year = datetime.now().year
        today = datetime.now()
        
        prompt = f"""
Extract the COMPLETE academic semester timeline from this content.

CRITICAL: Look for ALL of these:
1. First day of classes
2. Last day of classes  
3. FINAL EXAM PERIOD (usually 1-2 weeks after last class day)
4. Last possible exam date

The semester END DATE should be the LAST DAY OF FINALS, not the last assignment!

Current semester: {semester}
Year: {current_year}
Today: {today.strftime('%Y-%m-%d')}

Content (search for dates, exam schedules, academic calendar):
{content[:30000]}

OUTPUT JSON (no markdown):
{{
  "start_date": "YYYY-MM-DD",
  "end_date": "YYYY-MM-DD",
  "finals_start": "YYYY-MM-DD or null",
  "finals_end": "YYYY-MM-DD or null",
  "notes": "Brief explanation of dates found"
}}

If dates cannot be found, estimate based on typical semester (4 months from today).
"""
        
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.REFLEX,
                system_instruction="Output JSON only."
            ))
            return self._parse_json(response.content)
        except:
            # Fallback: 4 months from today
            start = datetime.now()
            end = start + timedelta(days=120)
            return {"start_date": start.strftime("%Y-%m-%d"), "end_date": end.strftime("%Y-%m-%d")}

    def _parse_json(self, content: str) -> Any:
        """Safely parses JSON from AI response."""
        # Remove markdown code blocks if present
        if "```" in content:
            content = re.sub(r'```(?:json)?\s*', '', content)
            content = content.replace('```', '')
        
        # Remove <action> wrapper tags (Gemini sometimes adds these)
        content = re.sub(r'<action>\s*', '', content)
        content = re.sub(r'\s*</action>', '', content)
        
        # Remove <response> wrapper tags
        content = re.sub(r'<response>\s*', '', content)
        content = re.sub(r'\s*</response>', '', content)
        
        content = content.strip()
        
        try:
            return json.loads(content)
        except:
            # Try to find JSON object or array
            match = re.search(r'(\{[\s\S]*\}|\[[\s\S]*\])', content)
            if match:
                try:
                    return json.loads(match.group())
                except:
                    pass
            return {}

    # =========================================================================
    # RESOURCE INGESTION
    # =========================================================================
    
    async def _handle_resource_ingestion(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Processes newly uploaded academic resources."""
        
        resource_id = payload.get('resourceId') or payload.get('$id')
        title = payload.get('title', 'Untitled Resource')
        
        # Extract content if PDF
        file_id = payload.get('fileId')
        extracted_text = ""
        
        if file_id:
            try:
                import pypdf
                file_bytes = self.db.get_file_content(file_id)
                if file_bytes:
                    reader = pypdf.PdfReader(io.BytesIO(file_bytes))
                    extracted_text = "\n".join([p.extract_text() for p in reader.pages[:20]])
            except Exception as e:
                print(f"⚠️ PDF extraction error: {e}")
        
        if extracted_text and resource_id:
            # Save extracted text to resource
            self.db.update_resource_summary(resource_id, extracted_text[:50000])
        
        # Create intervention to acknowledge
        self.db.create_intervention(
            user_id,
            "resource_ingestion",
            f"I've received and processed '{title}'. This content will be used to create your personalized study schedule.",
            "COMPLETED"
        )
        
        return AgentResult(
            success=True,
            response=f"Resource '{title}' processed successfully",
            actions_taken=["resource_ingested", "intervention_created"]
        )

    # =========================================================================
    # CONTENT REQUEST (JIT On-Demand)
    # =========================================================================

    async def _handle_content_request(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """
        On-demand JIT content for the mobile app.
        
        Payload options:
          - taskId: Fetch content for a single task
          - date: Fetch content for all tasks on a given date (YYYY-MM-DD)
          - mode: Override content_mode (deep_dive/travel/cram/practice)
        """
        from ..tools.proactive_content_engine import ProactiveContentEngine
        
        task_id = payload.get('taskId')
        date_str = payload.get('date')
        mode_override = payload.get('mode')
        
        content_engine = ProactiveContentEngine(
            gemini_client=self.engine.client if hasattr(self.engine, 'client') else None,
            db=self.db
        )
        
        prepared_items = []
        
        if task_id:
            # Single task content request — find by taskId in schedule
            all_tasks = self.db.get_schedule(user_id)
            task_doc = next((t for t in all_tasks if t.get('$id') == task_id or t.get('taskId') == task_id), None)
            if not task_doc:
                return AgentResult(
                    success=False,
                    response=f"Task {task_id} not found",
                    actions_taken=["content_request_failed"]
                )
            
            if mode_override:
                task_doc['content_mode'] = mode_override
            
            content = await content_engine.prepare_content_for_rich_task(
                user_id=user_id,
                task=task_doc
            )
            if content:
                prepared_items.append({
                    'content_id': content.content_id,
                    'task_id': task_id,
                    'subject': task_doc.get('subject', ''),
                    'content_type': content.content_type,
                    'content_data': content.content_data,
                    'prepared_at': content.prepared_at.isoformat() if content.prepared_at else None,
                    'readiness': content.readiness_state.value if hasattr(content.readiness_state, 'value') else str(content.readiness_state)
                })
        
        elif date_str:
            # All tasks for a date — filter from user's full schedule
            all_tasks = self.db.get_schedule(user_id)
            tasks = [
                t for t in all_tasks
                if t.get('startTime', '').startswith(date_str)
                and t.get('status') != 'blocked'
            ]
            
            for task_doc in (tasks or [])[:8]:
                if mode_override:
                    task_doc['content_mode'] = mode_override
                
                content = await content_engine.prepare_content_for_rich_task(
                    user_id=user_id,
                    task=task_doc
                )
                if content:
                    prepared_items.append({
                        'content_id': content.content_id,
                        'task_id': task_doc.get('$id', ''),
                        'subject': task_doc.get('subject', ''),
                        'content_type': content.content_type,
                        'content_data': content.content_data,
                        'prepared_at': content.prepared_at.isoformat() if content.prepared_at else None,
                        'readiness': content.readiness_state.value if hasattr(content.readiness_state, 'value') else str(content.readiness_state)
                    })
        
        else:
            return AgentResult(
                success=False,
                response="Provide taskId or date for content request",
                actions_taken=["content_request_invalid"]
            )
        
        return AgentResult(
            success=True,
            response=f"Prepared {len(prepared_items)} content package(s)",
            actions_taken=["content_prepared"],
            data={'prepared_content': prepared_items, 'count': len(prepared_items)}
        )


# Entry Point
async def run_study_agent(db_helper, payload, context):
    """Async entry point for Appwrite Functions."""
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    agent = StudyAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    result = await agent.run(user_id, payload, payload.get('type', 'focus_update'))
    
    return context.res.json(result.to_dict())
