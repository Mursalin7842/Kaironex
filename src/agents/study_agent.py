"""
🎓 STUDY AGENT (v14.0 - PRODUCTION LOCKED)
==========================================
Role: Principal Academic Architect.
Objective: Generate a high-fidelity, schema-compliant semester plan.

FINAL CHECKLIST:
1. SCHEMA: Enforces Priority (1-10), Topics (10k chars), Locations.
2. TIMING: Calculates Academic Week Offset & Duration dynamically.
3. CONTENT: Subject Autodiscovery (Fixes the NameError) & Cold Start generation.
4. PERFORMANCE: Parallel Batch Writes.
"""

from typing import Optional, Dict, Any, List
import json
import logging
import io
import re
import uuid
import asyncio
from datetime import datetime, timedelta
import pypdf

from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext

class StudyAgent(BaseAgent):
    """The Study Agent: Cognitive Supply Chain Manager."""
    
    STUDY_SYSTEM_PROMPT = """You are the Senior Academic Director for Kaironex.
    Your goal is to engineer a student's success by managing Time, Content, and Energy.
    
    MANDATES:
    1. SCHEMA COMPLIANCE: Return strict JSON. Priority must be 1-10.
    2. INSTRUCTIONAL DENSITY: 'topics' must contain detailed steps (readings, problem sets).
    3. TEMPORAL ACCURACY: Schedule based on the specific Academic Week offset.
    4. MISSING DATA: If syllabus details are missing, INFER standard curriculum topics.
    """

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="study",
            display_name="Semester Director",
            system_instruction=self.STUDY_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=8192,
            enable_thought_signatures=True,
            enable_marathon=False
        )
    
    async def process(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        self.log_heartbeat(user_id, f"EVENT:STUDY | {payload.get('status', 'unknown')}")
        event_type = payload.get('type', payload.get('status', 'focus_update'))
        
        print(f"DEBUG: StudyAgent.process called. EventType: {event_type}")

        if event_type == 'schedule_request':
            print("DEBUG: Routing to _handle_schedule_request")
            return await self._handle_schedule_request(user_id, payload, context)
        elif event_type == 'resource_ingestion':
            return await self._handle_resource_ingestion(user_id, payload, context)
            
        return await self._handle_in_progress(user_id, payload, context)

    # =========================================================================
    #  🧠 COMPONENT 1 & 2: TIMELINE & CONSTRAINTS
    # =========================================================================

    async def _extract_semester_timeline(self, user_id: str, context_text: str) -> Dict[str, Any]:
        """Extracts dates and exams."""
        current_year = datetime.now().year
        prompt = f"""
        TASK: Extract Academic Timeline.
        CONTEXT: {context_text}
        YEAR: {current_year}
        OUTPUT JSON: {{ "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", "exam_weeks": [{{ "start": "YYYY-MM-DD", "end": "YYYY-MM-DD" }}] }}
        """
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt, user_id=user_id, agent="study", mode=ReasoningMode.REFLEX,
                system_instruction="Output JSON only."
            ))
            return self._clean_json(response.content)
        except: return {}

    async def _extract_constraints(self, user_id: str, resource_text: str) -> List[Dict]:
        """Extracts blocked slots with LOCATION."""
        user_doc = self.db.get_user_doc(user_id)
        sp = json.loads(user_doc.get('studentprofile_json', '{}')) if user_doc else {}
        
        raw_constraints = {
            "Job": sp.get('jobSchedule'), "Non_Negotiables": sp.get('nonNegotiables'),
            "Commute": sp.get('commuteTime'), "Visa": "Strict" if sp.get('isInternationalStudent') else "Flex"
        }
        prompt = f"""
        TASK: Extract blocked time slots.
        INPUTS: {json.dumps(raw_constraints)}
        CONTEXT: {resource_text}
        INSTRUCTIONS: Identify Classes (find Room #), Work, Gym. Add Commute buffers.
        OUTPUT JSON LIST: [ {{ "day_offset": 0, "startTime": "09:00", "endTime": "10:30", "title": "Class: CS455", "location": "Room 304", "type": "blocked" }} ]
        """
        try:
            response = await self.engine.reason(ReasoningRequest(prompt=prompt, user_id=user_id, agent="study", mode=ReasoningMode.DEEP))
            data = self._clean_json(response.content)
            return data if isinstance(data, list) else []
        except:
            return [{"day_offset": d, "startTime": "09:00", "endTime": "17:00", "title": "⚠️ Schedule Pending", "type": "blocked", "is_flexible": True} for d in range(5)]

    # =========================================================================
    #  🧠 COMPONENT 3: THE DIRECTOR (MASTER SCHEDULER)
    # =========================================================================

    async def _handle_schedule_request(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        # 1. GATHER INTELLIGENCE
        user_doc = self.db.get_user_doc(user_id)
        sp = json.loads(user_doc.get('studentprofile_json', '{}')) if user_doc else {}
        
        all_resources = self.db.get_user_resources(user_id)
        
        full_context = ""
        full_context = ""
        for r in all_resources:
            # 1. Try DB Summary first
            data = r.get('summaryText', '') or r.get('mined_data', '')
            
            # 2. JIT Extraction for Fresh Files
            if not data:
                print(f"DEBUG: JIT Extraction for {r.get('title')}")
                try:
                    # Try explicit ID first
                    file_id = r.get('fileId') or r.get('resourceId')
                    file_bytes = self.db.get_file_content(file_id)
                    
                    # Fallback: Search by filename
                    if not file_bytes:
                        print(f"DEBUG: ID lookup failed. Searching for '{r.get('title')}'...")
                        real_id = self.db.find_real_file_id(self.db.STORAGE_BUCKET_ID, r.get('title'))
                        if real_id:
                            file_bytes = self.db.get_file_content(real_id)
                    
                    if file_bytes:
                        pdf_file = io.BytesIO(file_bytes)
                        reader = pypdf.PdfReader(pdf_file)
                        extracted = ""
                        for page in reader.pages:
                            extracted += page.extract_text() + "\n"
                        
                        if extracted.strip():
                            data = extracted
                            print(f"DEBUG: Extracted {len(data)} chars from PDF.")
                except Exception as e:
                    print(f"⚠️ JIT Extract Error: {e}")

            full_context += f"\n--- {r.get('title', 'Untitled')} ({r.get('resource_type', 'DOC')}) ---\n{data}"

        # --- CRITICAL: STOP IF NO DATA ---
        if not full_context.strip():
            print("🛑 ABORT: No content found in resources.")
            return AgentResult(success=False, response="No readable content found in your resources. Please upload PDF files.", actions_taken=["aborted_no_content"])
        
        if len(full_context) < 50: # "Standard Curriculum." is 20 chars
             print("🛑 ABORT: Content too short.")
             return AgentResult(success=False, response="Content too short to generate a schedule.", actions_taken=["aborted_low_content"])

        # 2. ESTABLISH TIMELINE & CONSTRAINTS
        timeline = await self._extract_semester_timeline(user_id, full_context)
        blocked_slots = await self._extract_constraints(user_id, full_context)
        
        # --- FIX: SUBJECT AUTODISCOVERY (Crucial for Cold Start) ---
        detected_subjects = set()
        for slot in blocked_slots:
            title = slot.get('title', '')
            match = re.search(r'([A-Z]{2,4}\s?\d{3})', title) # e.g. CS450
            if match: detected_subjects.add(match.group(1))
        subject_str = ", ".join(detected_subjects) if detected_subjects else "Major Courses"

        # 3. TIMING LOGIC
        current_date = datetime.now()
        academic_week_offset = 0
        duration_days = 120
        
        try:
            start_str = timeline.get('start_date')
            end_str = timeline.get('end_date')
            
            if start_str and isinstance(start_str, str):
                sem_start = datetime.strptime(start_str, "%Y-%m-%d")
                days_since_start = (current_date - sem_start).days
                academic_week_offset = max(0, days_since_start // 7)
                
                if end_str and isinstance(end_str, str):
                    sem_end = datetime.strptime(end_str, "%Y-%m-%d")
                    duration_days = max(7, (sem_end - current_date).days)
            
            requested_days = int(payload.get('duration_days', 0))
            if requested_days > 0: duration_days = requested_days
                
        except Exception as e:
            print(f"⚠️ Timeline Calc Error: {e}")

        print(f"📅 Week {academic_week_offset + 1} | Subjects: {subject_str} | Duration: {duration_days} days")

        # 4. DIRECTIVE PROMPT
        prompt = f"""
        ACT AS: The Senior Academic Director.
        MISSION: Create a detailed plan from {current_date.strftime("%Y-%m-%d")} to {timeline.get('end_date', 'End of Sem')}.
        
        STUDENT PROFILE (FULL):
        {json.dumps(sp, indent=2)}
        
        DETECTED SUBJECTS: {subject_str}
        ACADEMIC CONTEXT: Week {academic_week_offset + 1}.
        
        TIMELINE: {json.dumps(timeline, indent=2)}
        CONSTRAINTS: {json.dumps(blocked_slots, indent=2)}
        CONTENT SOURCE: {full_context}
        
        [COLD START PROTOCOL]
        If specific topics are missing for {subject_str}, YOU MUST INFER THEM using standard university curriculums.
        Example: If 'CS450' (ML) is detected but no PDF, schedule 'Linear Regression' for Week 1, 'Neural Nets' for Week 5.
        
        [CAPACITY DRIVEN SCHEDULING]
        1. **User Capacity**: STRICTLY respect the 'daily focus capacity' and study preferences found in the STUDENT PROFILE.
        2. **Variable Blocks**: Create session lengths (30m to 2h) that match the content density and user's attention span.
        3. **Full Coverage**: Ensure the plan covers the ENTIRE period from {current_date.strftime("%Y-%m-%d")} to {timeline.get('end_date')}.
        
        [HUMAN LOGISTICS & ENERGY]
        1. **Decompression Gap**: You MUST leave a **1.5 to 2 HOUR** gap after the last class of the day. Students need to commute, eat, hang out, or take a walk. DO NOT schedule study immediately after class.
        2. **Anti-Burnout**: If the student has >4 hours of classes, schedule only "Light Review" or "Routine" tasks that evening.
        3. **Sleep & Meals**: DO NOT schedule any tasks between 23:00 (11 PM) and 08:00 (8 AM) unless the user profile explicitly says "Night Owl". Ensure 1-hour gaps for Lunch (12-2pm window) and Dinner (7-9pm window).
        
        DIRECTIVES:
        1. **Content Format**: 'topics' MUST be a **Markdown List**, not a paragraph.
           - Bad: "Read chapter 4 and do questions."
           - Good: "- Read Chapter 4 (pp. 100-120)\n- Solve Practice Problems 1-5\n- Review Lecture Notes"
        2. **Priority**: 10=Exams, 8=Deep Work, 5=Routine.
        3. **Location**: Use Room Numbers for Classes, 'Library' for Deep Work.
        
        OUTPUT FORMAT (Strict JSON):
        {{
            "phases": [
                {{
                    "phase_name": "Mid-Semester",
                    "start_week": {academic_week_offset + 1}, 
                    "end_week": {academic_week_offset + 8},
                    "routine_template": [
                        {{ 
                            "day_offset": 0, "startTime": "19:00", "endTime": "21:00", 
                            "title": "Deep Study: CS450", "type": "study", "subject": "CS450", 
                            "priority": 8, "location": "Library",
                            "topics": "Read Ch 4. Solve Problem Set 3 (Q1-10)."
                        }}
                    ]
                }}
            ],
            "syllabus_map": {{ "CS450": ["Topic 1", "Topic 2"] }},
            "deadlines": [ {{ "date": "YYYY-MM-DD", "title": "Lab 1", "subject": "CS460" }} ]
        }}
        """

        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt, user_id=user_id, agent="study", mode=ReasoningMode.DEEP,
                system_instruction="Output JSON only."
            ))
            data = self._clean_json(response.content)
            
            # 5. INSTANTIATION LOOP
            phases = data.get('phases', [])
            raw_deadlines = data.get('deadlines', [])
            syllabus = data.get('syllabus_map', {})
            exam_weeks = timeline.get('exam_weeks', [])
            
            deadlines = []
            for d in raw_deadlines:
                try:
                    d_dt = datetime.strptime(d['date'], "%Y-%m-%d")
                    if d_dt >= datetime.now() - timedelta(days=1):
                        deadlines.append(d)
                except: continue
            
            final_tasks = []
            self.db.clear_future_schedule(user_id) 
            start_date = current_date.replace(hour=0, minute=0, second=0, microsecond=0)

            for day_i in range(duration_days):
                date_cursor = start_date + timedelta(days=day_i)
                semester_week_num = academic_week_offset + (day_i // 7) + 1
                weekday_cursor = date_cursor.weekday()
                
                is_exam_week = False
                for ew in exam_weeks:
                    try:
                        if datetime.strptime(ew['start'], "%Y-%m-%d") <= date_cursor <= datetime.strptime(ew['end'], "%Y-%m-%d"):
                            is_exam_week = True; break
                    except: pass

                current_phase = next((p for p in phases if p['start_week'] <= semester_week_num <= p['end_week']), phases[-1] if phases else None)
                if not current_phase: continue

                template = current_phase.get('routine_template', [])
                daily_items = [x for x in template if x.get('day_offset') == weekday_cursor]

                for item in daily_items:
                    try:
                        t_start = datetime.strptime(item['startTime'], "%H:%M")
                        t_end = datetime.strptime(item['endTime'], "%H:%M")
                        dt_start = date_cursor.replace(hour=t_start.hour, minute=t_start.minute)
                        dt_end = date_cursor.replace(hour=t_end.hour, minute=t_end.minute)
                        if dt_end < dt_start: dt_end += timedelta(days=1)

                        # Raw values
                        title = item.get('title', 'Study')[:255]
                        task_type = item.get('type', 'study')[:50]
                        subj = item.get('subject', 'General')
                        raw_priority = int(item.get('priority', 5))
                        instructions = item.get('topics', '')
                        location = item.get('location', 'Study Space')[:255]
                        
                        # --- SCHEMA ENFORCEMENT ---
                        is_flexible = bool(item.get('is_flexible', True))
                        priority = max(1, min(10, raw_priority))
                        linked_deadline = None

                        if item.get('type') == 'blocked':
                            is_flexible = False
                            task_type = 'blocked'
                            priority = 10
                            if not location or location == "Study Space": location = "On Campus" 

                        elif is_exam_week and task_type == 'study':
                            title = f"📝 EXAM PREP: {subj}"
                            instructions = f"*** ACTIVE RECALL ***\n\n1. Review Past Papers.\n2. No new content."
                            priority = 10
                            is_flexible = False
                            location = "Quiet Zone"

                        elif task_type == 'study':
                            topic_list = syllabus.get(subj, [])
                            if topic_list:
                                t_idx = semester_week_num - 1
                                if 0 <= t_idx < len(topic_list):
                                    topic_name = topic_list[t_idx]
                                    if not instructions: instructions = f"Topic: {topic_name}."
                                    else: instructions = f"Topic: {topic_name} | {instructions}"
                            
                            # Fallback if AI missed detailed instructions
                            if len(instructions) < 20 and subj != 'General':
                                instructions = f"Subject: {subj}. Review Week {semester_week_num} standard curriculum. Focus on key concepts."

                            # Crunch Mode
                            for d in deadlines:
                                try:
                                    d_dt = datetime.strptime(d['date'], "%Y-%m-%d")
                                    if 0 <= (d_dt - date_cursor).days <= 4 and d.get('subject') == subj:
                                        title = f"🔥 CRUNCH: {d['title']}"
                                        instructions = f"DEADLINE: {d['title']}. Focus strictly on submission."
                                        priority = 10
                                        is_flexible = False
                                        linked_deadline = f"{d['date']} | {d['title']}"[:500] 
                                        break
                                except: pass

                        final_tasks.append({
                            'taskId': str(uuid.uuid4()),
                            'userId': user_id,
                            'title': title,
                            'startTime': dt_start.isoformat(),
                            'endTime': dt_end.isoformat(),
                            'status': 'pending', 
                            'type': task_type,
                            'location': location,
                            'is_flexible': is_flexible,
                            'priority': priority,
                            'linked_deadline': linked_deadline,
                            'topics': instructions[:10000]
                        })
                    except Exception as e: print(f"Task Gen Error: {e}")

            # 6. PARALLEL DB INSERT
            print(f"💾 Saving {len(final_tasks)} tasks via Batch API...")
            count = self.db.batch_create_schedule_tasks(final_tasks)
            return AgentResult(success=True, response=f"Semester Plan Active: {count} tasks generated.", actions_taken=[f"created_{count}_tasks"])

        except Exception as e:
            return AgentResult(success=False, response=f"Director Error: {str(e)}", error=str(e))

    # --- UTILITIES ---
    def _clean_json(self, content: str) -> Dict:
        if "```" in content: content = content.split("```json")[-1].split("```")[0].strip()
        try: return json.loads(content)
        except: 
            match = re.search(r'(\{[\s\S]*\}|\[[\s\S]*\])', content)
            if match: 
                try: return json.loads(match.group()) 
                except: pass
            return {}

    # --- HANDLERS ---
    async def _handle_resource_ingestion(self, u, p, c): return AgentResult(success=True, response="Ingested", actions_taken=["resource_ingestion"])
    async def _handle_session_start(self, u, p, c): return AgentResult(success=True, response="Started", actions_taken=["session_started"])
    async def _handle_in_progress(self, u, p, c): return AgentResult(success=True, response="Tracking", actions_taken=["focus_tracked"])
    async def _handle_session_end(self, u, p, c): return AgentResult(success=True, response="Ended", actions_taken=["session_ended"])
    async def _handle_quiz_request(self, u, p, c): return AgentResult(success=True, response="Quiz Gen", actions_taken=["quiz_generated"])
    async def _handle_quiz_answer(self, u, p, c): return AgentResult(success=True, response="Quiz Ans", actions_taken=["quiz_answered"])
    async def _handle_pressure_check(self, u, p, c): return AgentResult(success=True, response="Pressure", actions_taken=["pressure_analyzed"])

# Async entry point (God Mode)
async def run_study_agent(db_helper, payload, context):
    import asyncio
    from ..core.bicameral_engine import BicameralEngine
    from ..core.thought_manager import ThoughtManager
    
    engine = BicameralEngine()
    thought_manager = ThoughtManager(db_helper)
    agent = StudyAgent(engine, thought_manager, db_helper)
    
    user_id = payload.get('userId')
    trigger_event = payload.get('status', payload.get('type', 'focus_update'))
    
    # Direct await - no loop creation
    result = await agent.run(user_id, payload, trigger_event)
    return context.res.json(result.to_dict())
