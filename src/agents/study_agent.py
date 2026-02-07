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
            max_thinking_tokens=8192,
            enable_thought_signatures=True,
            enable_marathon=False
        )

    async def process(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        self.log_heartbeat(user_id, f"STUDY|{payload.get('type', 'unknown')}")
        event_type = payload.get('type', payload.get('status', 'focus_update'))
        
        if event_type == 'schedule_request':
            return await self._handle_hierarchical_planning(user_id, payload, context)
        elif event_type == 'resource_ingestion':
            return await self._handle_resource_ingestion(user_id, payload, context)
        
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
    # MAIN HANDLER: HIERARCHICAL PLANNING
    # =========================================================================
    
    async def _handle_hierarchical_planning(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """
        Executes the two-layer planning:
        1. Generate/refresh monthly strategy (if needed)
        2. Generate detailed schedule for current month
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
        
        # Step 4: STRATEGY LAYER - Generate Monthly Plans
        print("🧠 Executing Strategy Layer: Monthly Plans...")
        monthly_plans = await self._generate_monthly_strategy(
            user_id, constitution, timeline, content_context, profile
        )
        
        if not monthly_plans:
            return AgentResult(success=False, response="Failed to generate semester strategy", error="strategy_failed")
        
        # Step 5: TACTICAL LAYER - Generate Schedule for Month 1
        current_month = monthly_plans[0]
        print(f"⚡ Executing Tactical Layer: Month 1 Schedule...")
        
        task_count = await self._generate_month_schedule(
            user_id, constitution, current_month, content_context
        )
        
        return AgentResult(
            success=True,
            response=f"Semester strategy created with {len(monthly_plans)} months. Month 1 scheduled with {task_count} tasks.",
            actions_taken=["strategy_created", f"month_1_scheduled_{task_count}_tasks"]
        )

    # =========================================================================
    # LAYER 1: STRATEGY ENGINE (Monthly Plans)
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
        Creates high-level monthly goals for the entire semester.
        Outputs rows for the monthly_plans table.
        """
        
        start_date = datetime.strptime(timeline.get('start_date', datetime.now().strftime('%Y-%m-%d')), '%Y-%m-%d')
        end_date = datetime.strptime(timeline.get('end_date', (datetime.now() + timedelta(days=120)).strftime('%Y-%m-%d')), '%Y-%m-%d')
        
        # Calculate semester duration in months
        num_months = max(1, min(6, (end_date.year - start_date.year) * 12 + end_date.month - start_date.month + 1))
        
        prompt = f"""
ACT AS: Academic Strategist creating a {num_months}-month semester roadmap.

STUDENT PROFILE:
{json.dumps(profile, indent=2)}

{constitution.to_prompt()}

ACADEMIC CONTENT:
{content[:30000]}

SEMESTER: {start_date.strftime('%B %d, %Y')} to {end_date.strftime('%B %d, %Y')} ({num_months} months)

TASK: Create a strategic plan breaking the semester into {num_months} monthly phases.

For EACH month, provide:
1. goals_context: Detailed description of what must be accomplished (chapters, projects, milestones)
2. dos_donts: Specific advice based on student's learning style and constraints
3. critical_focus: The single most important thing that month
4. risk_factors: What could derail progress and how to prevent it

OUTPUT JSON (no markdown):
[
  {{
    "month_index": 1,
    "goals_context": "MONTH 1 STRATEGY\\n\\nPrimary Objectives:\\n- Complete Chapter 1-3 of CS450\\n- Setup development environment\\n- Submit Lab 1\\n\\nWeekly Breakdown:\\n- Week 1: Environment setup, introductory readings\\n- Week 2: Core concepts, practice problems\\n- Week 3: Deep dive, lab preparation\\n- Week 4: Lab completion, review\\n\\nSuccess Metrics:\\n- All readings completed\\n- Lab 1 submitted on time\\n- Foundation concepts mastered",
    "dos_donts": "DO: Start assignments early (you have job on M/W/F). DO: Use 30-min focused blocks with breaks. DON'T: Schedule study right after work. DON'T: Skip the gym - it helps focus.",
    "critical_focus": "Build strong foundations - this month sets the pace",
    "status": "pending"
  }}
]
"""
        
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.DEEP,
                system_instruction="Output valid JSON array only. No markdown."
            ))
            
            plans = self._parse_json(response.content)
            if not isinstance(plans, list):
                plans = [plans] if plans else []
            
            # Clear existing plans and save new ones
            self.db.clear_monthly_plans(user_id)
            
            saved_plans = []
            current_date = start_date
            
            for i, plan in enumerate(plans):
                month_start = current_date
                month_end = current_date + relativedelta(months=1) - timedelta(days=1)
                
                plan_data = {
                    'month_index': i + 1,
                    'start_date': month_start.isoformat(),
                    'end_date': month_end.isoformat(),
                    'status': 'active' if i == 0 else 'pending',
                    'goals_context': plan.get('goals_context', ''),
                    'achieved_context': '',
                    'missed_context': ''
                }
                
                self.db.create_monthly_plan(user_id, plan_data)
                saved_plans.append(plan_data)
                current_date = month_end + timedelta(days=1)
            
            print(f"✅ Created {len(saved_plans)} monthly plans")
            return saved_plans
            
        except Exception as e:
            print(f"❌ Strategy Generation Error: {e}")
            return []

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
5. CONTENT DENSITY: Each 'topics' field must have actionable steps (not just "Study Chapter 1")

TASK DETAIL FORMAT:
- topics field must contain: specific pages, problem numbers, or clear deliverables
- Example: "Read Chapter 4 (pp. 100-120). Solve Problems 4.1-4.5. Summarize key theorems."

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
          "topics": "- Read Chapter 1 (pp. 1-25)\\n- Complete concept map\\n- Solve practice problems 1.1-1.5"
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
            
            data = self._parse_json(response.content)
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
                            'topics': task.get('topics', '')[:10000]
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
        """Builds context string from user's academic resources."""
        context_parts = []
        
        for r in resources:
            title = r.get('title', 'Untitled')
            content = r.get('summaryText', '') or r.get('mined_data', '')
            
            if not content and r.get('fileId'):
                # JIT extraction for PDFs
                try:
                    import pypdf
                    file_bytes = self.db.get_file_content(r.get('fileId'))
                    if file_bytes:
                        reader = pypdf.PdfReader(io.BytesIO(file_bytes))
                        content = "\n".join([p.extract_text() for p in reader.pages[:15]])
                except:
                    pass
            
            if content:
                context_parts.append(f"=== {title} ===\n{content[:8000]}")
        
        return "\n\n".join(context_parts) if context_parts else ""

    async def _extract_timeline(self, user_id: str, content: str, profile: Dict) -> Dict[str, Any]:
        """Extracts semester start/end dates from content."""
        
        semester = profile.get('semester', '1')
        current_year = datetime.now().year
        
        prompt = f"""
Extract the academic semester timeline from this content.
Current semester: {semester}
Year: {current_year}

Content: {content[:10000]}

OUTPUT JSON (no markdown):
{{"start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD"}}

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
