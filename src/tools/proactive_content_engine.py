"""
🎯 PROACTIVE CONTENT ENGINE
============================
Prepares content BEFORE the user needs it.

The brain anticipates:
- Upcoming study sessions → Content ready
- Travel/commute time → Audio prepared
- Exam day → Quick revision audio
- Class time → Lecture prep overview

"When you arrive to study, everything is already waiting."
"""

import json
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from enum import Enum
import hashlib


class ContentReadinessState(str, Enum):
    """State of prepared content."""
    NOT_STARTED = "not_started"
    PREPARING = "preparing"
    READY = "ready"
    DELIVERED = "delivered"
    EXPIRED = "expired"


class AnticipatedContext(str, Enum):
    """Anticipated user contexts."""
    STUDY_SESSION = "study_session"       # Scheduled study block
    COMMUTE_TO_CLASS = "commute_to_class" # Going to university
    COMMUTE_HOME = "commute_home"         # Returning home
    PRE_EXAM = "pre_exam"                 # Before an exam
    QUICK_BREAK = "quick_break"           # Short break between activities
    WAITING = "waiting"                   # Waiting for something
    EXERCISE = "exercise"                 # Working out
    COOKING = "cooking"                   # Preparing food
    WINDING_DOWN = "winding_down"         # Before sleep


@dataclass
class PreparedContent:
    """Content prepared in advance for a specific context."""
    content_id: str
    user_id: str
    context: AnticipatedContext
    scheduled_for: datetime
    content_type: str  # audio, text, flashcards, quiz
    subject: str
    topics: List[str]
    content_data: Dict[str, Any]  # The actual content
    duration_minutes: int
    readiness_state: ContentReadinessState = ContentReadinessState.NOT_STARTED
    prepared_at: Optional[datetime] = None
    delivered_at: Optional[datetime] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "content_id": self.content_id,
            "context": self.context.value,
            "scheduled_for": self.scheduled_for.isoformat(),
            "content_type": self.content_type,
            "subject": self.subject,
            "topics": self.topics,
            "duration_minutes": self.duration_minutes,
            "readiness_state": self.readiness_state.value,
            "prepared_at": self.prepared_at.isoformat() if self.prepared_at else None
        }


@dataclass
class AudioContent:
    """Generated audio content for hands-free learning."""
    audio_id: str
    title: str
    audio_type: str  # overview, revision, deep_dive, podcast_style
    script: str  # Text that would be spoken
    estimated_duration_seconds: int
    voice_style: str = "conversational"  # conversational, formal, energetic
    background_music: bool = False
    chapters: List[Dict[str, Any]] = field(default_factory=list)
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class ProactiveContentEngine:
    """
    Prepares content BEFORE the user's scheduled activities.
    
    The brain looks at:
    1. User's schedule (study blocks, classes, exams)
    2. User's location patterns (commute times)
    3. User's preferences (audio vs text)
    4. Current mastery levels (what needs review)
    
    Then prepares appropriate content so when the user
    arrives at their study time, EVERYTHING IS READY.
    """
    
    def __init__(self, gemini_client=None, db=None):
        self.gemini = gemini_client
        self.db = db
        self._prepared_content: Dict[str, List[PreparedContent]] = {}
        self._preparation_lead_time_minutes = 30  # Prepare 30 min ahead
    
    # =========================================================================
    # NEW: RICH TASK FORMAT INTEGRATION
    # =========================================================================
    
    async def prepare_content_for_rich_task(
        self,
        user_id: str,
        task: Dict[str, Any],
        mastery_data: Optional[Dict[str, float]] = None
    ) -> Optional[PreparedContent]:
        """
        Prepare content for a task using the new rich task format.
        
        Reads from task fields:
        - subject: Course name for context
        - topics: 4-phase breakdown
        - content_mode: deep_dive/travel/cram/practice
        - metadata_json: {learning_objectives, resource_hints, verification}
        """
        import uuid
        
        # Parse task start time
        task_start = self._parse_datetime(task.get('startTime'))
        if not task_start:
            return None
        
        # Extract rich metadata
        metadata = {}
        if task.get('metadata_json'):
            try:
                metadata = json.loads(task['metadata_json'])
            except:
                metadata = {}
        
        # Determine content mode (default to deep_dive)
        content_mode = task.get('content_mode', 'deep_dive')
        subject = task.get('subject', task.get('title', 'General Study'))
        difficulty = task.get('difficulty', 'intermediate')
        
        # Extract learning objectives and resource hints
        learning_objectives = metadata.get('learning_objectives', [])
        resource_hints = metadata.get('resource_hints', [])
        prerequisites = metadata.get('prerequisites', [])
        verification = metadata.get('verification')
        
        # Parse topics for main concepts
        topics_text = task.get('topics', '')
        
        # Calculate duration
        task_end = self._parse_datetime(task.get('endTime'))
        duration_minutes = 60  # default
        if task_start and task_end:
            duration_minutes = int((task_end - task_start).total_seconds() / 60)
        
        # Prepare content based on mode
        content_data = {}
        
        if content_mode == 'deep_dive':
            content_data = await self._prepare_deep_dive_content(
                subject=subject,
                learning_objectives=learning_objectives,
                resource_hints=resource_hints,
                topics_text=topics_text,
                duration_minutes=duration_minutes,
                mastery_data=mastery_data
            )
        elif content_mode == 'travel':
            content_data = await self._prepare_travel_content(
                subject=subject,
                learning_objectives=learning_objectives,
                resource_hints=resource_hints,
                duration_minutes=duration_minutes
            )
        elif content_mode == 'cram':
            content_data = await self._prepare_cram_content(
                subject=subject,
                learning_objectives=learning_objectives,
                resource_hints=resource_hints,
                mastery_data=mastery_data
            )
        elif content_mode == 'practice':
            content_data = await self._prepare_practice_content(
                subject=subject,
                learning_objectives=learning_objectives,
                resource_hints=resource_hints,
                difficulty=difficulty
            )
        
        # Add verification info if present
        if verification:
            content_data['gatekeeper_quiz'] = await self._generate_gatekeeper_quiz(
                topics=verification.get('topics_covered', []),
                pass_threshold=verification.get('pass_threshold', 0.8)
            )
        
        # Create prepared content object
        prepared = PreparedContent(
            content_id=f"pc_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            context=AnticipatedContext.STUDY_SESSION,
            scheduled_for=task_start,
            content_type=content_mode,
            subject=subject,
            topics=learning_objectives or resource_hints,
            content_data=content_data,
            duration_minutes=duration_minutes,
            readiness_state=ContentReadinessState.READY,
            prepared_at=datetime.now()
        )
        
        self._store_prepared_content(user_id, prepared)
        return prepared
    
    async def _prepare_deep_dive_content(
        self,
        subject: str,
        learning_objectives: List[str],
        resource_hints: List[str],
        topics_text: str,
        duration_minutes: int,
        mastery_data: Optional[Dict[str, float]] = None
    ) -> Dict[str, Any]:
        """Prepare comprehensive content for deep study sessions."""
        return {
            'summaries': await self._generate_summaries(resource_hints or learning_objectives),
            'video_recommendations': await self._search_videos(resource_hints),
            'practice_problems': await self._generate_problems(learning_objectives),
            'flashcards': await self._generate_flashcards(learning_objectives, mastery_data),
            'warmup_quiz': await self._generate_warmup_quiz(learning_objectives),
            'suggested_flow': self._create_session_flow(duration_minutes, learning_objectives),
            'topics_breakdown': topics_text
        }
    
    async def _prepare_travel_content(
        self,
        subject: str,
        learning_objectives: List[str],
        resource_hints: List[str],
        duration_minutes: int
    ) -> Dict[str, Any]:
        """Prepare audio-friendly content for travel/commute."""
        script = await self._generate_audio_script(
            topics=learning_objectives or resource_hints,
            style="conversational",
            duration_seconds=(duration_minutes - 2) * 60,
            destination_context="your destination"
        )
        return {
            'audio_script': script,
            'key_terms': resource_hints,
            'audio_chapters': self._create_audio_chapters(script, learning_objectives or resource_hints),
            'quick_review_points': await self._generate_quick_review(learning_objectives)
        }
    
    async def _prepare_cram_content(
        self,
        subject: str,
        learning_objectives: List[str],
        resource_hints: List[str],
        mastery_data: Optional[Dict[str, float]] = None
    ) -> Dict[str, Any]:
        """Prepare rapid revision content."""
        weak_topics = self._find_weak_topics(learning_objectives, mastery_data)
        return {
            'critical_flashcards': await self._generate_critical_flashcards(weak_topics),
            'formula_sheet': await self._generate_formula_sheet(subject),
            'common_mistakes': await self._generate_common_mistakes(subject),
            'key_definitions': await self._generate_key_definitions(learning_objectives),
            'mnemonics': await self._generate_mnemonics(resource_hints)
        }
    
    async def _prepare_practice_content(
        self,
        subject: str,
        learning_objectives: List[str],
        resource_hints: List[str],
        difficulty: str
    ) -> Dict[str, Any]:
        """Prepare practice exercises."""
        return {
            'problem_set': await self._generate_problem_set(learning_objectives, difficulty),
            'coding_exercises': await self._generate_coding_exercises(resource_hints, difficulty),
            'quiz': await self._generate_practice_quiz(learning_objectives),
            'worked_examples': await self._generate_worked_examples(learning_objectives)
        }
    
    async def _generate_gatekeeper_quiz(
        self,
        topics: List[str],
        pass_threshold: float
    ) -> Dict[str, Any]:
        """Generate verification quiz for end of study session."""
        questions = await self._generate_quiz_questions(topics, count=5)
        return {
            'questions': questions,
            'pass_threshold': pass_threshold,
            'max_attempts': 2,
            'feedback_mode': 'immediate'
        }
    
    async def _generate_quick_review(self, topics: List[str]) -> List[str]:
        """Generate quick review bullet points."""
        return [f"Key point for {t}" for t in topics[:5]]
    
    async def _generate_key_definitions(self, topics: List[str]) -> List[Dict[str, str]]:
        """Generate key definitions for cram mode."""
        return [{"term": t, "definition": f"Definition of {t}"} for t in topics]
    
    async def _generate_mnemonics(self, topics: List[str]) -> List[str]:
        """Generate memory aids."""
        return [f"Mnemonic for {t}" for t in topics[:3]]
    
    async def _generate_problem_set(self, topics: List[str], difficulty: str) -> List[Dict[str, Any]]:
        """Generate graded problem set."""
        return [{"topic": t, "problem": f"Problem on {t}", "difficulty": difficulty} for t in topics]
    
    async def _generate_coding_exercises(self, topics: List[str], difficulty: str) -> List[Dict[str, Any]]:
        """Generate coding exercises if applicable."""
        return [{"topic": t, "task": f"Implement {t}", "difficulty": difficulty} for t in topics]
    
    async def _generate_practice_quiz(self, topics: List[str]) -> List[Dict[str, Any]]:
        """Generate practice quiz."""
        return [{"question": f"Quiz Q on {t}", "options": ["A", "B", "C", "D"], "answer": "A"} for t in topics]
    
    async def _generate_worked_examples(self, topics: List[str]) -> List[Dict[str, Any]]:
        """Generate worked examples."""
        return [{"topic": t, "example": f"Step-by-step example for {t}"} for t in topics]
    
    async def _search_videos(self, topics: List[str]) -> List[Dict[str, Any]]:
        """Search for relevant video resources."""
        # In production, use Google Search API grounding
        return [{"topic": t, "search_query": f"{t} tutorial video", "url": None} for t in topics]
    
    async def _generate_quiz_questions(self, topics: List[str], count: int = 5) -> List[Dict[str, Any]]:
        """Generate quiz questions for verification."""
        return [
            {"question": f"Question {i+1} about {topics[i % len(topics)]}", 
             "options": ["A", "B", "C", "D"], 
             "correct": "A"} 
            for i in range(count)
        ]
    
    # =========================================================================
    # ORIGINAL METHODS (analyze_upcoming_schedule, etc.)
    # =========================================================================
    
    async def analyze_upcoming_schedule(
        self,
        user_id: str,
        schedule: List[Dict[str, Any]],
        look_ahead_hours: int = 24
    ) -> List[Dict[str, Any]]:
        """
        Analyze schedule and identify content preparation opportunities.
        
        Returns list of content preparation tasks.
        """
        preparation_tasks = []
        now = datetime.now()
        look_ahead_end = now + timedelta(hours=look_ahead_hours)
        
        for event in schedule:
            event_time = self._parse_datetime(event.get("startTime") or event.get("start_time"))
            if not event_time or event_time > look_ahead_end:
                continue
            
            event_type = event.get("type", "")
            content_mode = event.get("content_mode", "deep_dive")
            
            # NEW: Use rich task format if available
            if event.get("metadata_json"):
                preparation_tasks.append({
                    "context": self._map_type_to_context(event_type, content_mode),
                    "event": event,
                    "prepare_by": event_time - timedelta(minutes=30),
                    "content_mode": content_mode,
                    "priority": "high" if event.get("priority", 5) >= 7 else "medium"
                })
                continue
            
            # Legacy: Study Session → Prepare full study content
            if event_type in ["study_session", "study"]:
                preparation_tasks.append({
                    "context": AnticipatedContext.STUDY_SESSION,
                    "event": event,
                    "prepare_by": event_time - timedelta(minutes=30),
                    "content_types": ["text", "flashcards", "quiz"],
                    "priority": "high"
                })
            
            # Class → Prepare commute audio + lecture prep
            elif event_type == "class":
                commute_time = event.get("commute_minutes", 20)
                preparation_tasks.append({
                    "context": AnticipatedContext.COMMUTE_TO_CLASS,
                    "event": event,
                    "prepare_by": event_time - timedelta(minutes=commute_time + 15),
                    "content_types": ["audio"],
                    "duration_target": commute_time,
                    "priority": "medium"
                })
            
            # Exam → Prepare quick revision audio
            elif event_type == "exam":
                preparation_tasks.append({
                    "context": AnticipatedContext.PRE_EXAM,
                    "event": event,
                    "prepare_by": event_time - timedelta(hours=2),
                    "content_types": ["audio", "flashcards"],
                    "priority": "critical"
                })
        
        return preparation_tasks
    
    async def prepare_study_session_content(
        self,
        user_id: str,
        subject: str,
        topics: List[str],
        duration_minutes: int,
        scheduled_for: datetime,
        mastery_data: Optional[Dict[str, float]] = None
    ) -> PreparedContent:
        """
        Prepare all content for an upcoming study session.
        
        Includes:
        - Topic summaries
        - Practice problems
        - Flashcards for weak areas
        - Quiz for knowledge check
        """
        import uuid
        
        content_id = f"pc_{uuid.uuid4().hex[:8]}"
        
        # Prioritize topics based on mastery
        prioritized_topics = self._prioritize_by_mastery(topics, mastery_data)
        
        # Generate content package
        content_data = {
            "summaries": await self._generate_summaries(prioritized_topics),
            "practice_problems": await self._generate_problems(prioritized_topics),
            "flashcards": await self._generate_flashcards(prioritized_topics, mastery_data),
            "warmup_quiz": await self._generate_warmup_quiz(prioritized_topics),
            "suggested_flow": self._create_session_flow(duration_minutes, prioritized_topics)
        }
        
        prepared = PreparedContent(
            content_id=content_id,
            user_id=user_id,
            context=AnticipatedContext.STUDY_SESSION,
            scheduled_for=scheduled_for,
            content_type="study_package",
            subject=subject,
            topics=prioritized_topics,
            content_data=content_data,
            duration_minutes=duration_minutes,
            readiness_state=ContentReadinessState.READY,
            prepared_at=datetime.now()
        )
        
        self._store_prepared_content(user_id, prepared)
        return prepared
    
    async def prepare_commute_audio(
        self,
        user_id: str,
        destination: str,
        commute_minutes: int,
        subject: str,
        topics: List[str],
        audio_style: str = "conversational"
    ) -> AudioContent:
        """
        Generate audio content for commute/travel time.
        
        Perfect for:
        - Bus/train rides to campus
        - Walking to class
        - Waiting for appointments
        """
        import uuid
        
        audio_id = f"audio_{uuid.uuid4().hex[:8]}"
        
        # Calculate target audio length (slightly shorter than commute)
        target_seconds = (commute_minutes - 2) * 60
        
        # Generate podcast-style script
        script = await self._generate_audio_script(
            topics=topics,
            style=audio_style,
            duration_seconds=target_seconds,
            destination_context=destination
        )
        
        # Create chapter markers
        chapters = self._create_audio_chapters(script, topics)
        
        return AudioContent(
            audio_id=audio_id,
            title=f"{subject} - On Your Way to {destination}",
            audio_type="commute_overview",
            script=script,
            estimated_duration_seconds=target_seconds,
            voice_style=audio_style,
            background_music=True,
            chapters=chapters
        )
    
    async def prepare_pre_exam_revision(
        self,
        user_id: str,
        exam_subject: str,
        exam_topics: List[str],
        exam_time: datetime,
        mastery_data: Optional[Dict[str, float]] = None
    ) -> Tuple[AudioContent, PreparedContent]:
        """
        Prepare last-minute revision content before an exam.
        
        Returns both audio (for commute) and visual (for final review).
        """
        import uuid
        
        # Find weak topics that need reinforcement
        weak_topics = self._find_weak_topics(exam_topics, mastery_data)
        
        # Generate calming, confidence-building audio
        audio = AudioContent(
            audio_id=f"exam_audio_{uuid.uuid4().hex[:8]}",
            title=f"Pre-Exam Boost: {exam_subject}",
            audio_type="pre_exam_revision",
            script=await self._generate_pre_exam_script(weak_topics, exam_subject),
            estimated_duration_seconds=600,  # 10 minutes
            voice_style="calm_confident",
            background_music=True,
            chapters=[
                {"title": "Confidence Builder", "start_seconds": 0},
                {"title": "Quick Key Points", "start_seconds": 120},
                {"title": "Common Pitfalls", "start_seconds": 360},
                {"title": "You've Got This", "start_seconds": 540}
            ]
        )
        
        # Generate quick-glance flashcards for weak topics
        content = PreparedContent(
            content_id=f"exam_prep_{uuid.uuid4().hex[:8]}",
            user_id=user_id,
            context=AnticipatedContext.PRE_EXAM,
            scheduled_for=exam_time - timedelta(hours=1),
            content_type="exam_prep",
            subject=exam_subject,
            topics=weak_topics,
            content_data={
                "critical_flashcards": await self._generate_critical_flashcards(weak_topics),
                "formula_sheet": await self._generate_formula_sheet(exam_subject),
                "common_mistakes": await self._generate_common_mistakes(exam_subject),
                "confidence_notes": self._generate_confidence_notes()
            },
            duration_minutes=15,
            readiness_state=ContentReadinessState.READY,
            prepared_at=datetime.now()
        )
        
        return audio, content
    
    async def prepare_exercise_audio(
        self,
        user_id: str,
        exercise_type: str,
        duration_minutes: int,
        subject: str,
        topics: List[str]
    ) -> AudioContent:
        """
        Generate learning audio for workout/exercise time.
        
        Designed for:
        - Gym sessions
        - Running/jogging
        - Walking
        """
        import uuid
        
        # Adjust pacing for exercise
        style = "energetic" if exercise_type in ["gym", "running"] else "conversational"
        
        script = await self._generate_exercise_audio_script(
            topics=topics,
            exercise_type=exercise_type,
            duration_seconds=duration_minutes * 60,
            style=style
        )
        
        return AudioContent(
            audio_id=f"exercise_audio_{uuid.uuid4().hex[:8]}",
            title=f"Learn While You {exercise_type.title()}: {subject}",
            audio_type="exercise_learning",
            script=script,
            estimated_duration_seconds=duration_minutes * 60,
            voice_style=style,
            background_music=True,
            chapters=self._create_audio_chapters(script, topics)
        )
    
    def get_ready_content(
        self,
        user_id: str,
        context: Optional[AnticipatedContext] = None
    ) -> List[PreparedContent]:
        """Get all ready content for a user, optionally filtered by context."""
        if user_id not in self._prepared_content:
            return []
        
        ready = [
            c for c in self._prepared_content[user_id]
            if c.readiness_state == ContentReadinessState.READY
        ]
        
        if context:
            ready = [c for c in ready if c.context == context]
        
        return ready
    
    def mark_delivered(self, user_id: str, content_id: str):
        """Mark content as delivered to user."""
        if user_id in self._prepared_content:
            for content in self._prepared_content[user_id]:
                if content.content_id == content_id:
                    content.readiness_state = ContentReadinessState.DELIVERED
                    content.delivered_at = datetime.now()
                    break
    
    # Private helpers
    
    def _store_prepared_content(self, user_id: str, content: PreparedContent):
        """Store prepared content for later delivery."""
        if user_id not in self._prepared_content:
            self._prepared_content[user_id] = []
        self._prepared_content[user_id].append(content)
    
    def _map_type_to_context(self, event_type: str, content_mode: str) -> AnticipatedContext:
        """Map task type and content mode to anticipated context."""
        if content_mode == 'travel':
            return AnticipatedContext.COMMUTE_TO_CLASS
        if event_type == 'exam':
            return AnticipatedContext.PRE_EXAM
        if event_type == 'exercise':
            return AnticipatedContext.EXERCISE
        return AnticipatedContext.STUDY_SESSION
    
    def _parse_datetime(self, dt_str: Any) -> Optional[datetime]:
        """Parse datetime from string or return if already datetime."""
        if isinstance(dt_str, datetime):
            return dt_str
        if isinstance(dt_str, str):
            try:
                return datetime.fromisoformat(dt_str)
            except ValueError:
                return None
        return None
    
    def _prioritize_by_mastery(
        self,
        topics: List[str],
        mastery_data: Optional[Dict[str, float]]
    ) -> List[str]:
        """Sort topics by mastery (lowest first for more practice)."""
        if not mastery_data:
            return topics
        
        return sorted(topics, key=lambda t: mastery_data.get(t, 50))
    
    def _find_weak_topics(
        self,
        topics: List[str],
        mastery_data: Optional[Dict[str, float]]
    ) -> List[str]:
        """Find topics with mastery below threshold."""
        if not mastery_data:
            return topics[:3]  # Just return first 3
        
        weak = [t for t in topics if mastery_data.get(t, 50) < 70]
        return weak[:5]  # Max 5 weak topics
    
    def _create_session_flow(
        self,
        duration_minutes: int,
        topics: List[str]
    ) -> List[Dict[str, Any]]:
        """Create suggested study session flow."""
        flow = []
        time_per_topic = (duration_minutes - 10) // len(topics)  # Reserve 10 min
        
        flow.append({
            "phase": "warmup",
            "duration_minutes": 5,
            "activity": "Quick quiz on previous material"
        })
        
        for topic in topics:
            flow.append({
                "phase": "study",
                "topic": topic,
                "duration_minutes": time_per_topic,
                "activity": f"Deep focus on {topic}"
            })
        
        flow.append({
            "phase": "consolidation",
            "duration_minutes": 5,
            "activity": "Review and note key takeaways"
        })
        
        return flow
    
    def _create_audio_chapters(
        self,
        script: str,
        topics: List[str]
    ) -> List[Dict[str, Any]]:
        """Create chapter markers for audio content."""
        chapters = []
        estimated_words_per_second = 2.5
        words_per_topic = len(script.split()) // len(topics)
        
        current_second = 0
        for topic in topics:
            chapters.append({
                "title": topic,
                "start_seconds": current_second
            })
            current_second += int(words_per_topic / estimated_words_per_second)
        
        return chapters
    
    def _generate_confidence_notes(self) -> List[str]:
        """Generate confidence-building notes for pre-exam."""
        return [
            "You've prepared for this. Trust your preparation.",
            "Take a deep breath. Your brain knows more than you think.",
            "Read each question carefully. Don't rush.",
            "If stuck, move on and come back. Momentum matters.",
            "You've got this. Now go show them what you know!"
        ]
    
    # Async content generation (would use Gemini in production)
    
    async def _generate_summaries(self, topics: List[str]) -> Dict[str, str]:
        """Generate topic summaries."""
        return {topic: f"Summary of {topic}..." for topic in topics}
    
    async def _generate_problems(self, topics: List[str]) -> List[Dict[str, Any]]:
        """Generate practice problems."""
        return [{"topic": t, "problem": f"Problem for {t}", "solution": "..."} for t in topics]
    
    async def _generate_flashcards(
        self,
        topics: List[str],
        mastery_data: Optional[Dict[str, float]]
    ) -> List[Dict[str, str]]:
        """Generate flashcards focusing on weak areas."""
        return [{"front": f"Q about {t}", "back": f"A about {t}"} for t in topics]
    
    async def _generate_warmup_quiz(self, topics: List[str]) -> List[Dict[str, Any]]:
        """Generate quick warmup quiz."""
        return [{"question": f"Quick Q on {t}", "answer": "..."} for t in topics[:3]]
    
    async def _generate_critical_flashcards(self, topics: List[str]) -> List[Dict[str, str]]:
        """Generate must-know flashcards for exam."""
        return [{"front": f"CRITICAL: {t}", "back": "Key point..."} for t in topics]
    
    async def _generate_formula_sheet(self, subject: str) -> List[str]:
        """Generate formula/key facts sheet."""
        return [f"Formula 1 for {subject}", f"Formula 2 for {subject}"]
    
    async def _generate_common_mistakes(self, subject: str) -> List[str]:
        """Generate list of common mistakes to avoid."""
        return [f"Common mistake 1 in {subject}", f"Common mistake 2 in {subject}"]
    
    async def _generate_audio_script(
        self,
        topics: List[str],
        style: str,
        duration_seconds: int,
        destination_context: str
    ) -> str:
        """Generate conversational audio script."""
        # In production, use Gemini to generate natural script
        intro = f"Hey! On your way to {destination_context}? Let's make this commute count."
        body = " ".join([f"Let's talk about {t}. [Content about {t}]" for t in topics])
        outro = f"And that's the overview! You're ready for {destination_context}."
        return f"{intro} {body} {outro}"
    
    async def _generate_pre_exam_script(
        self,
        weak_topics: List[str],
        subject: str
    ) -> str:
        """Generate calming pre-exam audio script."""
        return f"""
        Take a deep breath. You've been preparing for this {subject} exam, and you're ready.
        
        Let's quickly run through a few key points you might want to refresh:
        
        {' '.join([f"Remember for {t}: [key point]." for t in weak_topics])}
        
        You know this material. Trust yourself. Walk into that exam with confidence.
        You've got this!
        """
    
    async def _generate_exercise_audio_script(
        self,
        topics: List[str],
        exercise_type: str,
        duration_seconds: int,
        style: str
    ) -> str:
        """Generate audio script for exercise time."""
        return f"""
        Let's learn while you {exercise_type}! 
        
        {' '.join([f"Here's a quick overview of {t}. [Content]" for t in topics])}
        
        Great workout, great learning. Double win!
        """
