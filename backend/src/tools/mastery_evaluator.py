"""
🧠 MASTERY EVALUATOR
====================
The "Knowledge Gatekeeper" system for Study Room.

Features:
- Adaptive quiz generation based on content
- Mastery scoring per concept
- Spaced repetition scheduling
- Quiz-gate for context switching prevention
"""

import json
import random
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from enum import Enum
import hashlib


class QuizType(str, Enum):
    """Types of quiz questions."""
    MULTIPLE_CHOICE = "multiple_choice"
    TRUE_FALSE = "true_false"
    FILL_BLANK = "fill_blank"
    SHORT_ANSWER = "short_answer"
    CONCEPTUAL = "conceptual"


class MasteryLevel(str, Enum):
    """Mastery level for concepts."""
    UNKNOWN = "unknown"           # Never tested
    NOVICE = "novice"             # 0-30% correct
    DEVELOPING = "developing"     # 30-60% correct
    PROFICIENT = "proficient"     # 60-85% correct
    MASTERED = "mastered"         # 85-100% correct


@dataclass
class QuizQuestion:
    """A single quiz question."""
    question_id: str
    question: str
    question_type: QuizType
    options: Optional[List[str]] = None  # For multiple choice
    correct_answer: str = ""
    explanation: str = ""
    concept_tags: List[str] = field(default_factory=list)
    difficulty: float = 0.5  # 0.0 = easy, 1.0 = hard
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            **asdict(self),
            "question_type": self.question_type.value
        }


@dataclass
class QuizResult:
    """Result of a quiz attempt."""
    question_id: str
    user_answer: str
    is_correct: bool
    time_taken_seconds: float
    confidence: float = 0.0  # User's self-reported confidence
    timestamp: datetime = field(default_factory=datetime.now)
    
    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        data['timestamp'] = self.timestamp.isoformat()
        return data


@dataclass
class ConceptMastery:
    """Mastery tracking for a single concept."""
    concept_id: str
    concept_name: str
    attempts: int = 0
    correct: int = 0
    mastery_score: float = 0.0
    level: MasteryLevel = MasteryLevel.UNKNOWN
    last_tested: Optional[datetime] = None
    next_review: Optional[datetime] = None
    history: List[QuizResult] = field(default_factory=list)
    
    def update(self, result: QuizResult):
        """Update mastery based on quiz result."""
        self.attempts += 1
        if result.is_correct:
            self.correct += 1
        
        # Calculate mastery score (weighted recent attempts more)
        self.mastery_score = self._calculate_mastery()
        self.level = self._determine_level()
        self.last_tested = result.timestamp
        self.next_review = self._calculate_next_review()
        self.history.append(result)
    
    def _calculate_mastery(self) -> float:
        """Calculate mastery score with recency weighting."""
        if self.attempts == 0:
            return 0.0
        
        # Simple ratio for now, could be enhanced with spaced repetition
        return (self.correct / self.attempts) * 100
    
    def _determine_level(self) -> MasteryLevel:
        """Determine mastery level from score."""
        if self.attempts == 0:
            return MasteryLevel.UNKNOWN
        elif self.mastery_score < 30:
            return MasteryLevel.NOVICE
        elif self.mastery_score < 60:
            return MasteryLevel.DEVELOPING
        elif self.mastery_score < 85:
            return MasteryLevel.PROFICIENT
        else:
            return MasteryLevel.MASTERED
    
    def _calculate_next_review(self) -> datetime:
        """Calculate next review time using spaced repetition."""
        base_interval = timedelta(hours=1)
        
        # Increase interval based on mastery
        multipliers = {
            MasteryLevel.UNKNOWN: 0.5,
            MasteryLevel.NOVICE: 1,
            MasteryLevel.DEVELOPING: 2,
            MasteryLevel.PROFICIENT: 7,
            MasteryLevel.MASTERED: 14
        }
        
        multiplier = multipliers.get(self.level, 1)
        return datetime.now() + (base_interval * multiplier)
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "concept_id": self.concept_id,
            "concept_name": self.concept_name,
            "attempts": self.attempts,
            "correct": self.correct,
            "mastery_score": self.mastery_score,
            "level": self.level.value,
            "last_tested": self.last_tested.isoformat() if self.last_tested else None,
            "next_review": self.next_review.isoformat() if self.next_review else None
        }


class MasteryEvaluator:
    """
    The Knowledge Gatekeeper - evaluates and tracks concept mastery.
    
    Features:
    1. Quiz generation based on studied content
    2. Adaptive difficulty adjustment
    3. Mastery tracking per concept
    4. Gate-check for context switching
    """
    
    def __init__(self, gemini_client=None, db=None):
        self.gemini = gemini_client
        self.db = db
        self._user_mastery: Dict[str, Dict[str, ConceptMastery]] = {}
    
    def get_tool_definition(self) -> Dict[str, Any]:
        """Return the function calling definition for Gemini."""
        return {
            "name": "evaluate_mastery",
            "description": "Generate a quiz to evaluate the user's understanding of a topic. Use as a 'gate' before allowing topic switches.",
            "parameters": {
                "type": "object",
                "properties": {
                    "topic": {
                        "type": "string",
                        "description": "The topic to quiz on"
                    },
                    "num_questions": {
                        "type": "integer",
                        "description": "Number of questions (default 3)"
                    },
                    "difficulty": {
                        "type": "string",
                        "enum": ["adaptive", "easy", "medium", "hard"],
                        "description": "Quiz difficulty"
                    },
                    "is_gate_check": {
                        "type": "boolean",
                        "description": "If true, this is a gate check before context switch"
                    }
                },
                "required": ["topic"]
            }
        }
    
    async def generate_quiz(
        self,
        user_id: str,
        topic: str,
        num_questions: int = 3,
        difficulty: str = "adaptive",
        concepts: Optional[List[str]] = None,
        is_gate_check: bool = False
    ) -> List[QuizQuestion]:
        """
        Generate a quiz for the topic.
        
        Args:
            user_id: User identifier
            topic: Topic to quiz on
            num_questions: Number of questions
            difficulty: Difficulty level or 'adaptive'
            concepts: Specific concepts to test
            is_gate_check: If true, simpler quick check
        
        Returns:
            List of QuizQuestion objects
        """
        import uuid
        
        # Get user's mastery for this topic
        user_mastery = self._get_user_mastery(user_id)
        topic_concepts = concepts or self._extract_concepts(topic)
        
        # Determine difficulty
        if difficulty == "adaptive":
            difficulty = self._calculate_adaptive_difficulty(user_mastery, topic_concepts)
        
        # Gate checks are simpler
        if is_gate_check:
            num_questions = min(num_questions, 2)
        
        # In production, this would use Gemini to generate questions
        questions = []
        for i in range(num_questions):
            concept = topic_concepts[i % len(topic_concepts)] if topic_concepts else topic
            
            q = QuizQuestion(
                question_id=f"q_{uuid.uuid4().hex[:8]}",
                question=self._generate_question_text(concept, difficulty),
                question_type=self._select_question_type(difficulty),
                options=self._generate_options(concept) if random.random() > 0.5 else None,
                correct_answer=f"Correct answer for {concept}",
                explanation=f"This tests understanding of {concept}",
                concept_tags=[concept],
                difficulty={"easy": 0.3, "medium": 0.5, "hard": 0.8}.get(difficulty, 0.5)
            )
            questions.append(q)
        
        return questions
    
    async def evaluate_answer(
        self,
        user_id: str,
        question: QuizQuestion,
        user_answer: str,
        time_taken_seconds: float = 0,
        confidence: float = 0.5
    ) -> Tuple[bool, str, ConceptMastery]:
        """
        Evaluate a user's answer and update mastery.
        
        Returns:
            (is_correct, explanation, updated_mastery)
        """
        # In production, use Gemini to semantically evaluate
        is_correct = self._check_answer(question, user_answer)
        
        result = QuizResult(
            question_id=question.question_id,
            user_answer=user_answer,
            is_correct=is_correct,
            time_taken_seconds=time_taken_seconds,
            confidence=confidence
        )
        
        # Update mastery for each concept
        for concept in question.concept_tags:
            mastery = self._update_concept_mastery(user_id, concept, result)
        
        explanation = question.explanation if not is_correct else "Correct! Great job."
        
        return is_correct, explanation, mastery
    
    def gate_check(
        self,
        user_id: str,
        from_topic: str,
        to_topic: str
    ) -> Tuple[bool, Optional[List[QuizQuestion]]]:
        """
        Check if user can switch topics based on mastery.
        
        Returns:
            (can_switch, gate_quiz if needed)
        """
        user_mastery = self._get_user_mastery(user_id)
        
        # Check mastery of current topic
        from_concepts = self._extract_concepts(from_topic)
        
        total_mastery = 0
        tested_concepts = 0
        
        for concept in from_concepts:
            if concept in user_mastery:
                total_mastery += user_mastery[concept].mastery_score
                tested_concepts += 1
        
        # Require at least 60% mastery to switch freely
        if tested_concepts > 0:
            avg_mastery = total_mastery / tested_concepts
            if avg_mastery >= 60:
                return True, None
        
        # Generate gate quiz
        # In production, this would be async
        return False, None  # Would return quiz questions
    
    def get_mastery_summary(self, user_id: str) -> Dict[str, Any]:
        """Get mastery summary for a user."""
        user_mastery = self._get_user_mastery(user_id)
        
        if not user_mastery:
            return {
                "total_concepts": 0,
                "mastered": 0,
                "proficient": 0,
                "developing": 0,
                "novice": 0,
                "unknown": 0,
                "overall_score": 0
            }
        
        level_counts = {level: 0 for level in MasteryLevel}
        total_score = 0
        
        for concept, mastery in user_mastery.items():
            level_counts[mastery.level] += 1
            total_score += mastery.mastery_score
        
        return {
            "total_concepts": len(user_mastery),
            "mastered": level_counts[MasteryLevel.MASTERED],
            "proficient": level_counts[MasteryLevel.PROFICIENT],
            "developing": level_counts[MasteryLevel.DEVELOPING],
            "novice": level_counts[MasteryLevel.NOVICE],
            "unknown": level_counts[MasteryLevel.UNKNOWN],
            "overall_score": total_score / len(user_mastery) if user_mastery else 0,
            "concepts": {k: v.to_dict() for k, v in user_mastery.items()}
        }
    
    def get_due_reviews(self, user_id: str) -> List[ConceptMastery]:
        """Get concepts due for spaced repetition review."""
        user_mastery = self._get_user_mastery(user_id)
        now = datetime.now()
        
        due = []
        for concept, mastery in user_mastery.items():
            if mastery.next_review and mastery.next_review <= now:
                due.append(mastery)
        
        # Sort by urgency (oldest first)
        due.sort(key=lambda m: m.next_review or now)
        return due
    
    # Private helper methods
    
    def _get_user_mastery(self, user_id: str) -> Dict[str, ConceptMastery]:
        """Get or create user mastery dict."""
        if user_id not in self._user_mastery:
            self._user_mastery[user_id] = {}
        return self._user_mastery[user_id]
    
    def _update_concept_mastery(
        self,
        user_id: str,
        concept: str,
        result: QuizResult
    ) -> ConceptMastery:
        """Update mastery for a concept."""
        user_mastery = self._get_user_mastery(user_id)
        
        concept_id = hashlib.md5(concept.encode()).hexdigest()[:8]
        
        if concept not in user_mastery:
            user_mastery[concept] = ConceptMastery(
                concept_id=concept_id,
                concept_name=concept
            )
        
        user_mastery[concept].update(result)
        return user_mastery[concept]
    
    def _extract_concepts(self, topic: str) -> List[str]:
        """Extract key concepts from a topic."""
        # In production, use Gemini to extract concepts
        # For now, simple split
        return [topic]
    
    def _calculate_adaptive_difficulty(
        self,
        user_mastery: Dict[str, ConceptMastery],
        concepts: List[str]
    ) -> str:
        """Calculate difficulty based on user's current mastery."""
        if not user_mastery or not concepts:
            return "medium"
        
        scores = []
        for concept in concepts:
            if concept in user_mastery:
                scores.append(user_mastery[concept].mastery_score)
        
        if not scores:
            return "medium"
        
        avg_score = sum(scores) / len(scores)
        
        if avg_score < 40:
            return "easy"
        elif avg_score < 70:
            return "medium"
        else:
            return "hard"
    
    def _generate_question_text(self, concept: str, difficulty: str) -> str:
        """Generate question text (placeholder)."""
        templates = {
            "easy": f"What is the basic definition of {concept}?",
            "medium": f"How would you apply {concept} in a practical scenario?",
            "hard": f"Compare and contrast {concept} with related concepts and explain edge cases."
        }
        return templates.get(difficulty, templates["medium"])
    
    def _select_question_type(self, difficulty: str) -> QuizType:
        """Select appropriate question type."""
        if difficulty == "easy":
            return random.choice([QuizType.MULTIPLE_CHOICE, QuizType.TRUE_FALSE])
        elif difficulty == "medium":
            return random.choice([QuizType.MULTIPLE_CHOICE, QuizType.FILL_BLANK])
        else:
            return random.choice([QuizType.SHORT_ANSWER, QuizType.CONCEPTUAL])
    
    def _generate_options(self, concept: str) -> List[str]:
        """Generate multiple choice options (placeholder)."""
        return [
            f"Option A about {concept}",
            f"Option B about {concept}",
            f"Option C about {concept}",
            f"Option D about {concept}"
        ]
    
    def _check_answer(self, question: QuizQuestion, user_answer: str) -> bool:
        """Check if answer is correct (simplified)."""
        # In production, use semantic matching via Gemini
        return user_answer.lower().strip() == question.correct_answer.lower().strip()
