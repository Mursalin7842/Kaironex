"""
🎓 STUDY AGENT (v3.0 - GOD MODE)
================================
The Cognitive Supply Chain Manager & Executive Strategist.

Capabilities:
- Phased Campaign Generation (Foundation -> Deep Work -> Exam Crunch)
- "Gravity Well" Scheduling (Deadlines warp the routine)
- Psychometric Profiling (Visa pressure, GPA Gap analysis)
- Smart Resource Sampling (Start/End scanning + Scanned PDF detection)
- Real-time study session monitoring
- Focus tracking and intervention
- Knowledge gatekeeper (quiz validation)
"""

from typing import Optional, Dict, Any, List
import json
import logging
import io
import re
import uuid
from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta

# Internal Imports
from .base_agent import BaseAgent, AgentConfig, AgentResult
from ..core.bicameral_engine import ReasoningMode, ReasoningRequest
from ..core.state_machine import StateContext

class StudyAgent(BaseAgent):
    """
    The Study Agent: Cognitive Supply Chain Manager.
    
    Handles event types:
    - session_start: New study session beginning
    - session_end: Study session completed
    - focus_update: Focus score changed
    - quiz_request: Knowledge gatekeeper challenge
    - pressure_check: Analyze current pressure levels
    - resource_ingestion: New learning resource added
    - schedule_request: Generate Phased Semester Campaign
    """
    
    STUDY_SYSTEM_PROMPT = """You are the Study Guardian for Kaironex, a Student Life Operating System.

Your role is to protect the student's cognitive supply chain - ensuring knowledge flows smoothly and focus remains sharp.

PERSONA:
- Tone: Encouraging academic coach
- Style: Metacognitive guidance (help them think about thinking)
- Focus: Learning efficiency, not just time spent

RULES:
1. Celebrate focus milestones (30min, 1hr, 2hr streaks)
2. When focus drops, offer OPTIONS not commands
3. Generate quizzes that test understanding, not memorization
4. Track learning patterns over time
5. Never shame for breaks - they're part of the process

OUTPUT FORMAT:
<analyze>Your analysis of their learning state</analyze>
<action>The user-facing guidance</action>
"""

    def get_config(self) -> AgentConfig:
        return AgentConfig(
            agent_type="study",
            display_name="Study Guardian",
            system_instruction=self.STUDY_SYSTEM_PROMPT,
            default_reasoning_mode=ReasoningMode.HYBRID,
            max_thinking_tokens=8192,
            enable_thought_signatures=True,
            enable_marathon=False
        )
    
    async def process(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Main processing logic for study events."""
        self.log_heartbeat(user_id, f"EVENT:STUDY | {payload.get('status', 'unknown')}")
        
        event_type = payload.get('type', payload.get('status', 'focus_update'))
        
        handlers = {
            'session_start': self._handle_session_start,
            'IN_PROGRESS': self._handle_in_progress,
            'session_end': self._handle_session_end,
            'COMPLETED': self._handle_session_end,
            'focus_update': self._handle_focus_update,
            'REQUEST_UNLOCK': self._handle_quiz_request,
            'quiz_request': self._handle_quiz_request,
            'quiz_answer': self._handle_quiz_answer,
            'pressure_check': self._handle_pressure_check,
            'resource_ingestion': self._handle_resource_ingestion,
            'schedule_request': self._handle_schedule_request,
        }
        
        handler = handlers.get(event_type, self._handle_focus_update)
        return await handler(user_id, payload, context)

    # =========================================================================
    #  🧠 GOD MODE: PHASED CAMPAIGN GENERATOR (The Ultimate Scheduler)
    # =========================================================================

    async def _handle_schedule_request(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """
        Generates a 3-Phase Academic Campaign based on Student Profile & Syllabus Density.
        """
        duration_days = int(payload.get('duration_days', 120))
        
        # --- 1. PSYCHOMETRIC PROFILING (From UI Data) ---
        user_doc = self.db.get_user_doc(user_id)
        sp = json.loads(user_doc.get('studentprofile_json', '{}')) if user_doc else {}
        
        # A. Visa & Compliance Logic (The "International Student" Checkbox)
        is_international = sp.get('isInternational', False) or sp.get('international_student', False)
        visa_pressure = "CRITICAL CONSTRAINT: International Student. Attendance & Grade Compliance MUST be prioritized over self-study to maintain Visa status." if is_international else "Local Student Status."

        # B. Strategy Mode (The Gap Analysis: Target vs Current GPA)
        try:
            current_gpa = float(sp.get('currentCgpa', 3.0))
            target_gpa = float(sp.get('targetCgpa', 3.5))
            gpa_gap = target_gpa - current_gpa
            
            if gpa_gap > 0.4:
                strategy_mode = "RUTHLESS OPTIMIZATION (High Gap)"
                tone_directive = "Cut low-value social activities. Prioritize high-yield active recall."
            elif gpa_gap > 0:
                strategy_mode = "GROWTH FOCUSED"
                tone_directive = "Balanced sustainable growth."
            else:
                strategy_mode = "MAINTENANCE"
                tone_directive = "Maintain healthy habits."
        except:
            strategy_mode = "STANDARD"
            tone_directive = "Balanced approach."

        # C. Motivation Injection (The "Why")
        motivation = sp.get('goalReason', 'Academic Success')

        profile_str = f"""
        STUDENT PROFILE:
        - Major: {sp.get('major', 'General')}
        - Semester: {sp.get('currentSemester', '1')}/{sp.get('totalSemesters', '8')}
        - Employment: {sp.get('jobDescription', 'None')} ({sp.get('allowedWorkHours', 0)} hrs/wk)
        - Strategy Mode: {strategy_mode}
        - Core Motivation: "{motivation}"
        - {visa_pressure}
        """

        # --- 2. RESOURCE INGESTION (The Book Eater) ---
        all_resources = self.db.get_user_resources(user_id, limit=20)
        resource_text = ""
        MAX_CHARS = 100000 # High context for Gemini
        
        print(f"📚 Ingesting {len(all_resources)} files for Campaign Generation...")
        
        failed_files = []
        
        for r in all_resources:
            # Use summary if available to save time/tokens
            if r.get('summaryText'):
                resource_text += f"\n=== [Summary] {r.get('title')} ===\n{r.get('summaryText')}\n"
            else:
                file_id = r.get('resourceId') or r.get('fileId')
                drive_link = r.get('driveLink')
                explicit_bucket = None
                
                # Smart Link Parsing: If we have a URL, trust the URL's bucket/file data
                if drive_link and '/buckets/' in drive_link:
                    try:
                        import re
                        # Regex to extract: .../buckets/[BUCKET]/files/[FILE]
                        match = re.search(r'/buckets/([^/]+)/files/([^/?]+)', drive_link)
                        if match:
                            explicit_bucket = match.group(1)
                            # Only override file_id if it was missing or matches the link
                            # We trust the link's bucket implicitly.
                            print(f"🔗 Derived Bucket from link: {explicit_bucket}")
                    except Exception as e:
                        print(f"⚠️ Link parsing failed: {e}")

                if file_id and not file_id.startswith('link_'):
                    try:
                        content = await self._fetch_resource_content(file_id, r.get('type', 'pdf'), bucket_id=explicit_bucket, file_name=r.get('title'))
                        
                        # Strict Check: Abort if file is missing or empty
                        if content.startswith("[Error") or content.startswith("[Empty"):
                            print(f"❌ Strict Mode: File missing - {r.get('title')}")
                            failed_files.append(r.get('title', 'Unknown File'))
                        else:
                            resource_text += f"\n=== [Raw Content] {r.get('title')} ===\n{content[:8000]}\n"
                            
                    except Exception as e:
                        print(f"⚠️ Failed to read content for {file_id}: {e}")
                        failed_files.append(r.get('title', 'Unknown File'))
            
            if len(resource_text) > MAX_CHARS:
                resource_text += "\n[...Context Limit Reached...]"
                break

        # CRITITAL: Abort if files are missing
        if failed_files:
            error_msg = f"Schedule Generation Aborted. Missing Files: {', '.join(failed_files)}. Please re-upload them."
            return AgentResult(success=False, response=error_msg, error=error_msg)

        if not resource_text: resource_text = "No specific syllabi found. Use standard university curriculum for this Major."

        # --- 3. THE "KAIRO" PROMPT ---
        prompt = f"""
        ACT AS: Kairo (Executive Strategist & Campaign Manager).
        MISSION: Generate a "Phased Semester Campaign" for {duration_days} days.
        
        INPUTS:
        {profile_str}
        
        CONTEXT (Syllabus & Materials):
        {resource_text}
        
        STRATEGY DIRECTIVE:
        Break the semester into 3 Distinct Phases based on the syllabus density:
        1. "Foundation Phase" (Start): Heavy reading, lighter practice.
        2. "Deep Work Phase" (Middle): Heavy practice, complex projects.
        3. "Exam Phase" (End): Pure revision, past papers, high stress management.
        
        Apply "{tone_directive}" to the schedule intensity.
        
        TASK:
        Generate a JSON object with:
        1. `phases`: A list of 3 phases, each with a different `routine_template` (7-day pattern).
        2. `syllabus_map`: The topic list per subject.
        3. `deadlines`: A list of inferred or explicit exam dates.
        
        OUTPUT FORMAT (Strict JSON):
        {{
            "phases": [
                {{
                    "phase_name": "Foundation",
                    "start_week": 1,
                    "end_week": 6,
                    "focus_strategy": "Read & Summarize",
                    "routine_template": [ 
                        {{ "day_offset": 0, "startTime": "09:00", "endTime": "11:00", "title": "Deep Study", "type": "study", "subject": "CS101", "priority": 8 }} 
                        ... (Cover all 7 days 0-6)
                    ]
                }},
                ... (Phase 2 & 3)
            ],
            "syllabus_map": {{ "CS101": ["Topic 1", "Topic 2"] }},
            "deadlines": [ {{ "date": "YYYY-MM-DD", "title": "Midterm", "subject": "CS101" }} ],
            "narrative_report": "Brief strategy explanation..."
        }}
        """
        
        print(f"🗓️ Generating Phased Campaign...")
        
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.DEEP, # Use smartest model
                system_instruction="You are a JSON Generator. Output only the requested JSON."
            ))
            
            # --- 4. PARSE & EXECUTE ---
            data = self._clean_json(response.content)
            
            phases = data.get('phases', [])
            deadlines = data.get('deadlines', [])
            syllabus = data.get('syllabus_map', {})
            
            final_tasks = []
            self.db.clear_future_schedule(user_id)
            
            current_date = datetime.now()
            start_date = current_date.replace(hour=0, minute=0, second=0, microsecond=0)
            
            # The Generation Loop (Day-by-Day)
            for day_i in range(duration_days):
                date_cursor = start_date + timedelta(days=day_i)
                week_num = (day_i // 7) + 1
                weekday_cursor = date_cursor.weekday()
                
                # A. Determine Phase
                current_phase = next((p for p in phases if p['start_week'] <= week_num <= p['end_week']), phases[0] if phases else None)
                if not current_phase: continue

                # B. Determine "Gravity" (Crunch Mode)
                active_mode = "NORMAL"
                target_focus = None
                
                for d in deadlines:
                    try:
                        d_date = datetime.strptime(d['date'], "%Y-%m-%d")
                        days_until = (d_date - date_cursor).days
                        if 0 <= days_until <= 4: # 4 Day Gravity Well
                            active_mode = "CRUNCH"
                            target_focus = d
                            break
                    except: pass
                
                
                # C. Get Template Items
                template = current_phase.get('routine_template', [])
                daily_items = [x for x in template if x.get('day_offset') == weekday_cursor]
                
                # D. Instantiate Tasks (Schema-Perfect)
                for item in daily_items:
                    try:
                        # 1. Time Calculation
                        t_start = datetime.strptime(item['startTime'], "%H:%M")
                        t_end = datetime.strptime(item['endTime'], "%H:%M")
                        dt_start = date_cursor.replace(hour=t_start.hour, minute=t_start.minute)
                        dt_end = date_cursor.replace(hour=t_end.hour, minute=t_end.minute)
                        if dt_end < dt_start: dt_end += timedelta(days=1)
                        
                        # 2. Field Preparation
                        title = item.get('title', 'Activity')
                        desc = item.get('description', '')
                        priority = int(item.get('priority', 5))
                        status = 'pending'
                        task_type = item.get('type', 'study')[:50] # Schema limit
                        linked_deadline_val = None
                        
                        # 3. CRUNCH MODE LOGIC (The "Gravity")
                        if active_mode == "CRUNCH" and task_type == 'study' and target_focus:
                            if item.get('subject') == target_focus.get('subject'):
                                # This task is now subservient to the Exam
                                title = f"🔥 CRUNCH: {target_focus['title']} Prep"
                                desc = "High-Intensity Active Recall. Focus on weak points."
                                priority = 10 # Max priority
                                # PERFECT USE OF YOUR SCHEMA:
                                linked_deadline_val = f"{target_focus['date']} | {target_focus['title']}"
                            else:
                                # Cannibalize other subjects
                                title = f"⚠️ Skipped: {item.get('subject')}"
                                desc = f"Space cleared for {target_focus.get('subject', 'Exams')} exam preparation."
                                status = 'cancelled'

                        # 4. Progressive Syllabus Injection
                        final_topics = item.get('topics', '')
                        if status != 'cancelled' and task_type == 'study':
                            subj = item.get('subject')
                            if subj in syllabus:
                                topic_list = syllabus[subj]
                                t_idx = min(week_num - 1, len(topic_list) - 1)
                                if t_idx >= 0:
                                    topic = topic_list[t_idx]
                                    topic_clean = re.sub(r'^Week \d+[:\s-]*', '', topic)
                                    desc += f" | Topic: {topic_clean}"
                                    if not final_topics:
                                        final_topics = topic_clean

                        # 5. SCHEMA MAPPING
                        task_payload = {
                            'taskId': str(uuid.uuid4()),      # REQUIRED field (Generated)
                            'userId': user_id,                # REQUIRED
                            'title': title[:255],             # REQUIRED (Truncated)
                            'startTime': dt_start.isoformat(),# REQUIRED
                            'endTime': dt_end.isoformat(),    # REQUIRED
                            'status': status,                 # REQUIRED
                            'type': task_type,                # REQUIRED
                            'location': item.get('location', 'Study Space')[:255],
                            'is_flexible': bool(item.get('is_flexible', True)),
                            'priority': max(1, min(10, priority)), # Clamp 1-10
                            'linked_deadline': linked_deadline_val[:500] if linked_deadline_val else None,
                            'topics': final_topics[:5000] if final_topics else None # Safety Truncate
                        }
                        
                        final_tasks.append(task_payload)
                        
                    except Exception as loop_err:
                        print(f"Skipping task creation: {loop_err}")

            # --- 5. PERSIST CAMPAIGN META-DATA ---
            # Calculate Pressure Index (Real Logic)
            base_pressure = 25
            if active_mode == "CRUNCH": base_pressure += 50
            if "CRITICAL" in visa_pressure: base_pressure += 15
            if strategy_mode.startswith("RUTHLESS"): base_pressure += 10
            
            final_pressure = min(100, base_pressure)

            # Save the Strategy, Phase & Pressure info so the Dashboard can show it
            campaign_state = {
                "pressure_index": final_pressure,
                "campaign_meta": {
                    "strategy_mode": strategy_mode,
                    "visa_pressure": visa_pressure,
                    "generated_at": datetime.now().isoformat(),
                    "total_days": duration_days,
                    "active_phases": phases, # The frontend can match Current Week vs these phases
                    "motivation_anchor": motivation
                }
            }
            self.update_state_cache(user_id, campaign_state)

            # Save to DB
            count = 0
            for t in final_tasks:
                if self.db.create_schedule_task(t): count += 1

            
            return AgentResult(success=True, response=data.get('narrative_report', f"Generated {count} blocks."), actions_taken=[f"created_{count}_tasks"])

        except Exception as e:
            return AgentResult(success=False, response=f"Schedule Failed: {str(e)}", error=str(e))

    # =========================================================================
    #  📚 SMART INGESTION: THE BOOK EATER
    # =========================================================================

    async def _handle_resource_ingestion(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Handle new resource upload with Smart Sampling."""
        title = payload.get('title', 'Untitled Resource')
        file_id = payload.get('fileId') or payload.get('resourceId')
        
        extracted_text = ""
        if file_id and not file_id.startswith('link_'):
            try:
                extracted_text = await self._fetch_resource_content(file_id, 'pdf')
            except Exception as e:
                extracted_text = f"Error: {e}"
        
        # Construct Prompt for Summarization
        prompt = f"""
        ANALYZE RESOURCE: {title}
        CONTENT: {extracted_text[:15000]}
        
        TASK: Extract Syllabus, Dates, and Grading Scheme.
        OUTPUT: Plain text summary suitable for the scheduler.
        """
        
        try:
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.DEEP
            ))
            
            # Save Summary back to DB
            if file_id:
                self.db.update_resource_summary(file_id, response.content)
                
            return AgentResult(success=True, response=response.content, actions_taken=["resource_analyzed"])
        except Exception as e:
            return AgentResult(success=False, response=str(e))

    async def _fetch_resource_content(self, file_id: str, resource_type: str = 'pdf', bucket_id: Optional[str] = None, file_name: Optional[str] = None) -> str:
        """
        SMART DOWNLOADER:
        - Reads Start (Syllabus) AND End (Schedule) of PDFs.
        - Detects scanned files.
        - Self-healing: Finds real file ID if DB has wrong ID.
        """
        try: import pypdf
        except ImportError: return "[pypdf missing]"

        try:
            # 1. Try Direct ID Download
            file_bytes = self.db.get_file_content(file_id, bucket_id=bucket_id)
            
            # 2. Fallback: Search by Name (Self-Healing)
            if not file_bytes and bucket_id and file_name:
                print(f"⚠️ Direct download failed for {file_id}. Searching for '{file_name}'...")
                real_id = self.db.find_real_file_id(bucket_id, file_name)
                if real_id:
                    file_bytes = self.db.get_file_content(real_id, bucket_id=bucket_id)
            
            if not file_bytes: return "[Empty File]"
            
            text = ""
            if 'pdf' in resource_type.lower() or file_id.endswith('.pdf'):
                reader = pypdf.PdfReader(io.BytesIO(file_bytes))
                total_pages = len(reader.pages)
                
                if total_pages == 0: return "[Empty PDF]"
                
                # Smart Sampling: First 10 pages + Last 3 pages
                pages_to_read = list(range(min(10, total_pages)))
                if total_pages > 15:
                    pages_to_read += [total_pages-3, total_pages-2, total_pages-1]
                
                for p in pages_to_read:
                    if 0 <= p < total_pages:
                        extracted = reader.pages[p].extract_text()
                        if extracted: text += extracted + "\n"
                
                # Detection: Scanned PDF?
                if len(text) < 100:
                    return "[SCANNED IMAGE DETECTED] - OCR Required. Please upload a text-based PDF."
                    
                return text
            else:
                return file_bytes.decode('utf-8', errors='ignore')

        except Exception as e:
            return f"[Error: {e}]"

    # =========================================================================
    #  ⚡ STANDARD HANDLERS (Session, Focus, Quiz)
    # =========================================================================

    async def _handle_session_start(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Handle study session start."""
        topic = payload.get('topic', 'General Study')
        planned_duration = payload.get('planned_duration_minutes', 60)
        
        prompt = f"""
NEW STUDY SESSION STARTING:
Topic: {topic}
Planned Duration: {planned_duration} minutes

TASK:
1. Send a brief motivational start message
2. Set expectations for the session
3. Offer a focus tip for this topic type

Keep it brief (2-3 sentences) and energizing.
"""
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.REFLEX
        ))
        
        study_update = {
            "study_session": {
                "is_active": True,
                "topic": topic,
                "started_at": datetime.now().isoformat(),
                "planned_duration": planned_duration,
                "current_focus": 100,
                "focus_history": []
            }
        }
        self.update_state_cache(user_id, study_update)
        
        return AgentResult(success=True, response=response.content, actions_taken=["session_started"], state_updates=study_update)

    async def _handle_in_progress(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Handle in-progress session update."""
        focus_score = payload.get('focus_score', 100)
        topic = payload.get('topic', 'Unknown')
        duration = payload.get('duration_seconds', 0)
        
        interventions = []
        if focus_score < 40:
            prompt = f"""
FOCUS DROP DETECTED:
Topic: {topic}
Current Focus: {focus_score}/100
Session Duration: {duration // 60} minutes

TASK:
Write a 1-sentence empathetic nudge. Options to offer:
- Take a short break
- Switch to active recall
- Try the Pomodoro technique
- Push through for 5 more minutes

Be supportive, not judgmental.
"""
            response = await self.engine.reason(ReasoningRequest(
                prompt=prompt,
                user_id=user_id,
                agent="study",
                mode=ReasoningMode.REFLEX,
                system_instruction=self.STUDY_SYSTEM_PROMPT
            ))
            intervention_id = self.create_intervention(
                user_id,
                "FOCUS_DROP",
                response.content,
                strategy="NEGOTIATION"
            )
            interventions.append(intervention_id)
        
        study_update = {
            "study_session": {
                "is_active": True,
                "topic": topic,
                "current_focus": focus_score,
                "session_duration": duration,
                "last_updated": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, study_update)
        return AgentResult(success=True, response=response.content if interventions else "Focus tracked.", actions_taken=["focus_tracked"] + (["intervention_sent"] if interventions else []), interventions_created=interventions, state_updates=study_update)

    async def _handle_session_end(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        """Handle study session completion."""
        topic = payload.get('topic', 'Study Session')
        duration = payload.get('duration_seconds', 0)
        avg_focus = payload.get('average_focus', 70)
        duration_mins = duration // 60
        
        prompt = f"""
STUDY SESSION COMPLETED:
Topic: {topic}
Duration: {duration_mins} minutes
Average Focus: {avg_focus}/100

TASK:
1. Celebrate the completion
2. Assess session quality based on focus
3. Suggest what to do next (rest, review, or continue)

Keep it brief and positive.
"""
        response = await self.engine.reason(ReasoningRequest(
            prompt=prompt,
            user_id=user_id,
            agent="study",
            mode=ReasoningMode.REFLEX
        ))
        
        interventions = []
        if duration_mins >= 60:
            intervention_id = self.create_intervention(
                user_id,
                "SESSION_COMPLETE",
                f"🎉 **{duration_mins}-Minute Session Complete!**\n\n{response.content}",
                strategy="CELEBRATION"
            )
            interventions.append(intervention_id)
        
        study_update = {
            "study_session": {
                "is_active": False,
                "last_topic": topic,
                "last_duration": duration_mins,
                "last_focus": avg_focus,
                "completed_at": datetime.now().isoformat()
            }
        }
        self.update_state_cache(user_id, study_update)
        return AgentResult(success=True, response=response.content, actions_taken=["session_completed", "stats_recorded"], interventions_created=interventions, state_updates=study_update)

    async def _handle_focus_update(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        return await self._handle_in_progress(user_id, payload, context)

    async def _handle_quiz_request(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        topic = payload.get('topic', 'the current subject')
        difficulty = payload.get('difficulty', 'medium')
        
        prompt = f"""
KNOWLEDGE GATEKEEPER QUIZ:
Topic: {topic}
Difficulty: {difficulty}

Generate a thoughtful multiple-choice question that tests UNDERSTANDING, not just memorization.

Return as JSON:
{{
    "question": "The question text",
    "options": ["A. Option 1", "B. Option 2", "C. Option 3", "D. Option 4"],
    "correct": "A",
    "explanation": "Why this is the correct answer"
}}
"""
        response = await self.engine.reason(ReasoningRequest(prompt=prompt, user_id=user_id, agent="study", mode=ReasoningMode.DEEP))
        quiz_data = self._clean_json(response.content)
        return AgentResult(success=True, response=json.dumps(quiz_data), thought_id=response.thought_signature.thought_id if response.thought_signature else None, actions_taken=["quiz_generated"], state_updates={"pending_quiz": quiz_data})

    async def _handle_quiz_answer(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        user_answer = payload.get('answer')
        correct_answer = payload.get('correct')
        question = payload.get('question', '')
        explanation = payload.get('explanation', '')
        is_correct = user_answer == correct_answer
        
        if is_correct:
            prompt = f"User answered correctly! Generate a brief (1 sentence) celebration."
        else:
            prompt = f"""
User answered '{user_answer}' but the correct answer was '{correct_answer}'.

Question: {question}
Explanation: {explanation}

Generate a supportive response that:
1. Doesn't shame them
2. Explains why the correct answer is right
3. Encourages them to try again or continue studying
"""
        response = await self.engine.reason(ReasoningRequest(prompt=prompt, user_id=user_id, agent="study", mode=ReasoningMode.REFLEX))
        return AgentResult(success=True, response=response.content, actions_taken=["quiz_answered", f"result_{'correct' if is_correct else 'incorrect'}"], state_updates={"last_quiz_result": is_correct})

    async def _handle_pressure_check(self, user_id: str, payload: Dict[str, Any], context: StateContext) -> AgentResult:
        user_doc = self.db.get_user_doc(user_id)
        user_state = {}
        if user_doc:
            try: user_state = json.loads(user_doc.get('studentState_json', '{}'))
            except: pass
        
        prompt = f"""
PRESSURE MAP ANALYSIS:
User State: {json.dumps(user_state, indent=2)[:1000]}

TASK:
1. Identify top 3 pressure sources
2. Estimate overall pressure level (1-100)
3. Suggest one immediate action to reduce pressure

Be specific and actionable.
"""
        response = await self.engine.reason(ReasoningRequest(prompt=prompt, user_id=user_id, agent="study", mode=ReasoningMode.DEEP))
        return AgentResult(success=True, response=response.content, thought_id=response.thought_signature.thought_id if response.thought_signature else None, actions_taken=["pressure_analyzed"])

    # --- UTILITIES ---

    def _clean_json(self, content: str) -> Dict:
        """Robust JSON extraction."""
        if "```" in content:
            content = content.split("```json")[-1].split("```")[0].strip()
        try:
            return json.loads(content)
        except:
            match = re.search(r'\{[\s\S]*\}', content)
            return json.loads(match.group()) if match else {}
            
    def _parse_quiz(self, content: str) -> Dict[str, Any]:
        """Wrapper for clean_json to maintain API compatibility"""
        return self._clean_json(content)

    def _get_timestamp(self) -> str:
        """Get current timestamp string."""
        from datetime import datetime
        return datetime.now().isoformat()

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
