"""
📚 CONTENT DELIVERY TOOL
========================
Just-in-time content delivery for the Study Room.

Modes:
- DEEP_DIVE: Full video tutorials, comprehensive articles
- TRAVEL: Audio-friendly content, podcasts, summaries
- CRAM: Flashcards, key points, quick revision
- PRACTICE: Quizzes, exercises, problem sets
"""

import json
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field, asdict
from enum import Enum
from datetime import datetime


class DeliveryMode(str, Enum):
    """Content delivery modes based on context."""
    DEEP_DIVE = "deep_dive"      # Full tutorials, comprehensive learning
    TRAVEL = "travel"            # Audio-friendly, commute mode
    CRAM = "cram"                # Quick revision, flashcards
    PRACTICE = "practice"        # Quizzes and exercises
    EXPLORATION = "exploration"  # Curiosity-driven discovery


class ContentType(str, Enum):
    """Types of content that can be delivered."""
    VIDEO = "video"
    ARTICLE = "article"
    AUDIO = "audio"
    FLASHCARD = "flashcard"
    QUIZ = "quiz"
    SUMMARY = "summary"
    EXERCISE = "exercise"
    VISUALIZATION = "visualization"


@dataclass
class ContentItem:
    """A single content item for delivery."""
    content_id: str
    title: str
    content_type: ContentType
    source_url: str
    duration_minutes: Optional[int] = None
    difficulty: str = "intermediate"  # beginner, intermediate, advanced
    summary: Optional[str] = None
    key_points: List[str] = field(default_factory=list)
    prerequisites: List[str] = field(default_factory=list)
    metadata: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class ContentPackage:
    """A curated package of content for a learning session."""
    package_id: str
    topic: str
    mode: DeliveryMode
    items: List[ContentItem]
    estimated_time_minutes: int
    learning_objectives: List[str]
    created_at: datetime = field(default_factory=datetime.now)
    
    def to_dict(self) -> Dict[str, Any]:
        data = asdict(self)
        data['created_at'] = self.created_at.isoformat()
        data['mode'] = self.mode.value
        data['items'] = [item.to_dict() if hasattr(item, 'to_dict') else item for item in self.items]
        return data


class ContentDeliveryTool:
    """
    Just-in-time content delivery for Cognitive Supply Chain.
    
    Uses Google Search grounding to find relevant content,
    then packages it based on the user's learning mode.
    """
    
    # Mode-specific content preferences
    MODE_PREFERENCES = {
        DeliveryMode.DEEP_DIVE: {
            "preferred_types": [ContentType.VIDEO, ContentType.ARTICLE, ContentType.VISUALIZATION],
            "min_duration": 15,
            "max_items": 5,
            "depth": "comprehensive"
        },
        DeliveryMode.TRAVEL: {
            "preferred_types": [ContentType.AUDIO, ContentType.SUMMARY],
            "min_duration": 5,
            "max_items": 3,
            "depth": "overview"
        },
        DeliveryMode.CRAM: {
            "preferred_types": [ContentType.FLASHCARD, ContentType.SUMMARY, ContentType.QUIZ],
            "min_duration": 2,
            "max_items": 10,
            "depth": "key_points"
        },
        DeliveryMode.PRACTICE: {
            "preferred_types": [ContentType.QUIZ, ContentType.EXERCISE],
            "min_duration": 10,
            "max_items": 5,
            "depth": "application"
        },
        DeliveryMode.EXPLORATION: {
            "preferred_types": [ContentType.VIDEO, ContentType.ARTICLE],
            "min_duration": 5,
            "max_items": 7,
            "depth": "varied"
        }
    }
    
    # Trusted educational sources
    TRUSTED_SOURCES = [
        "youtube.com",
        "khanacademy.org",
        "coursera.org",
        "edx.org",
        "mit.edu",
        "stanford.edu",
        "geeksforgeeks.org",
        "leetcode.com",
        "freecodecamp.org",
        "brilliant.org",
        "3blue1brown.com",
        "medium.com",
        "dev.to",
        "arxiv.org"
    ]
    
    def __init__(self, gemini_client=None):
        self.gemini = gemini_client
        self._cache: Dict[str, ContentPackage] = {}
    
    def get_tool_definition(self) -> Dict[str, Any]:
        """Return the function calling definition for Gemini."""
        return {
            "name": "deliver_content",
            "description": "Search and deliver educational content based on topic and learning mode. Use this when the user needs study materials.",
            "parameters": {
                "type": "object",
                "properties": {
                    "topic": {
                        "type": "string",
                        "description": "The topic or concept to find content for"
                    },
                    "mode": {
                        "type": "string",
                        "enum": ["deep_dive", "travel", "cram", "practice", "exploration"],
                        "description": "The learning mode (deep_dive for comprehensive, travel for audio, cram for quick revision)"
                    },
                    "difficulty": {
                        "type": "string",
                        "enum": ["beginner", "intermediate", "advanced"],
                        "description": "The difficulty level"
                    },
                    "time_available_minutes": {
                        "type": "integer",
                        "description": "How much time the user has for learning"
                    }
                },
                "required": ["topic", "mode"]
            }
        }
    
    async def execute(
        self,
        topic: str,
        mode: str,
        difficulty: str = "intermediate",
        time_available_minutes: int = 30,
        user_context: Optional[Dict[str, Any]] = None
    ) -> ContentPackage:
        """
        Execute content delivery for a topic.
        
        Args:
            topic: The learning topic
            mode: Delivery mode (deep_dive, travel, cram, practice)
            difficulty: Content difficulty level
            time_available_minutes: Available study time
            user_context: Optional user state for personalization
        
        Returns:
            ContentPackage with curated content items
        """
        import uuid
        
        delivery_mode = DeliveryMode(mode)
        preferences = self.MODE_PREFERENCES[delivery_mode]
        
        # Build search queries based on mode
        search_queries = self._build_search_queries(topic, delivery_mode, difficulty)
        
        # In production, this would use Gemini's search grounding
        # For now, we generate a structured package
        items = await self._search_and_curate(
            topic=topic,
            queries=search_queries,
            mode=delivery_mode,
            difficulty=difficulty,
            max_items=preferences["max_items"],
            time_limit=time_available_minutes
        )
        
        # Calculate learning objectives
        objectives = self._generate_objectives(topic, delivery_mode, difficulty)
        
        package = ContentPackage(
            package_id=f"pkg_{uuid.uuid4().hex[:12]}",
            topic=topic,
            mode=delivery_mode,
            items=items,
            estimated_time_minutes=min(
                sum(i.duration_minutes or 5 for i in items),
                time_available_minutes
            ),
            learning_objectives=objectives
        )
        
        # Cache for reuse
        cache_key = f"{topic}:{mode}:{difficulty}"
        self._cache[cache_key] = package
        
        return package
    
    def _build_search_queries(
        self,
        topic: str,
        mode: DeliveryMode,
        difficulty: str
    ) -> List[str]:
        """Build mode-specific search queries."""
        base_queries = [topic]
        
        if mode == DeliveryMode.DEEP_DIVE:
            return [
                f"{topic} comprehensive tutorial",
                f"{topic} full course {difficulty}",
                f"{topic} explained in depth",
                f"{topic} visualization animation"
            ]
        elif mode == DeliveryMode.TRAVEL:
            return [
                f"{topic} podcast",
                f"{topic} audio lecture",
                f"{topic} explained simply",
                f"{topic} summary overview"
            ]
        elif mode == DeliveryMode.CRAM:
            return [
                f"{topic} cheat sheet",
                f"{topic} quick revision",
                f"{topic} key concepts summary",
                f"{topic} flashcards"
            ]
        elif mode == DeliveryMode.PRACTICE:
            return [
                f"{topic} practice problems",
                f"{topic} quiz questions",
                f"{topic} exercises solutions",
                f"{topic} leetcode problems" if "algorithm" in topic.lower() else f"{topic} practice"
            ]
        else:
            return [f"{topic} learn", f"{topic} tutorial"]
    
    async def _search_and_curate(
        self,
        topic: str,
        queries: List[str],
        mode: DeliveryMode,
        difficulty: str,
        max_items: int,
        time_limit: int
    ) -> List[ContentItem]:
        """
        Search for content and curate based on mode.
        
        In production, this uses Gemini's search grounding.
        """
        import uuid
        
        items = []
        preferences = self.MODE_PREFERENCES[mode]
        
        # Generate content items based on mode
        for i, content_type in enumerate(preferences["preferred_types"][:max_items]):
            item = ContentItem(
                content_id=f"content_{uuid.uuid4().hex[:8]}",
                title=self._generate_title(topic, content_type, i),
                content_type=content_type,
                source_url=f"https://search.google.com/search?q={topic.replace(' ', '+')}",
                duration_minutes=self._estimate_duration(content_type, mode),
                difficulty=difficulty,
                summary=f"Learn {topic} through {content_type.value} format",
                key_points=[
                    f"Understand core concepts of {topic}",
                    f"Apply {topic} in practical scenarios",
                    f"Common pitfalls and best practices"
                ]
            )
            items.append(item)
        
        return items
    
    def _generate_title(self, topic: str, content_type: ContentType, index: int) -> str:
        """Generate contextual title for content."""
        templates = {
            ContentType.VIDEO: [
                f"{topic} - Complete Tutorial",
                f"Understanding {topic} Visually",
                f"{topic} Crash Course"
            ],
            ContentType.ARTICLE: [
                f"Deep Dive into {topic}",
                f"The Complete Guide to {topic}",
                f"Mastering {topic}: A Comprehensive Guide"
            ],
            ContentType.AUDIO: [
                f"{topic} Explained - Audio Guide",
                f"Learn {topic} While Commuting",
                f"{topic} Podcast Episode"
            ],
            ContentType.FLASHCARD: [
                f"{topic} Flashcard Set",
                f"Quick Review: {topic}",
                f"{topic} Key Terms"
            ],
            ContentType.QUIZ: [
                f"Test Your {topic} Knowledge",
                f"{topic} Practice Quiz",
                f"{topic} Self-Assessment"
            ],
            ContentType.SUMMARY: [
                f"{topic} in 5 Minutes",
                f"{topic} Cheat Sheet",
                f"Quick Overview: {topic}"
            ],
            ContentType.EXERCISE: [
                f"{topic} Practice Problems",
                f"Hands-on {topic} Exercises",
                f"{topic} Coding Challenges"
            ],
            ContentType.VISUALIZATION: [
                f"{topic} Animated Explanation",
                f"Visualizing {topic}",
                f"{topic} Interactive Demo"
            ]
        }
        
        titles = templates.get(content_type, [f"{topic} Resource"])
        return titles[index % len(titles)]
    
    def _estimate_duration(self, content_type: ContentType, mode: DeliveryMode) -> int:
        """Estimate content duration in minutes."""
        base_durations = {
            ContentType.VIDEO: 15,
            ContentType.ARTICLE: 10,
            ContentType.AUDIO: 20,
            ContentType.FLASHCARD: 5,
            ContentType.QUIZ: 10,
            ContentType.SUMMARY: 3,
            ContentType.EXERCISE: 15,
            ContentType.VISUALIZATION: 8
        }
        
        duration = base_durations.get(content_type, 10)
        
        # Adjust for mode
        if mode == DeliveryMode.CRAM:
            duration = min(duration, 5)
        elif mode == DeliveryMode.DEEP_DIVE:
            duration = int(duration * 1.5)
        
        return duration
    
    def _generate_objectives(
        self,
        topic: str,
        mode: DeliveryMode,
        difficulty: str
    ) -> List[str]:
        """Generate learning objectives based on mode."""
        if mode == DeliveryMode.DEEP_DIVE:
            return [
                f"Understand the fundamentals of {topic}",
                f"Apply {topic} concepts to real problems",
                f"Identify advanced patterns in {topic}",
                f"Build intuition for {topic} applications"
            ]
        elif mode == DeliveryMode.TRAVEL:
            return [
                f"Get an overview of {topic}",
                f"Recall key terminology",
                f"Understand the 'why' behind {topic}"
            ]
        elif mode == DeliveryMode.CRAM:
            return [
                f"Quick recall of {topic} essentials",
                f"Memorize key formulas/definitions",
                f"Pass exam questions on {topic}"
            ]
        elif mode == DeliveryMode.PRACTICE:
            return [
                f"Solve {difficulty} problems on {topic}",
                f"Identify and fix common mistakes",
                f"Build problem-solving speed"
            ]
        else:
            return [f"Explore {topic} at your own pace"]
    
    def get_cached_package(self, topic: str, mode: str, difficulty: str) -> Optional[ContentPackage]:
        """Retrieve cached content package."""
        cache_key = f"{topic}:{mode}:{difficulty}"
        return self._cache.get(cache_key)


# Gemini Function Call Handler
async def handle_content_delivery_call(
    tool: ContentDeliveryTool,
    function_call: Dict[str, Any]
) -> Dict[str, Any]:
    """Handle a content delivery function call from Gemini."""
    args = function_call.get("args", {})
    
    package = await tool.execute(
        topic=args.get("topic", ""),
        mode=args.get("mode", "deep_dive"),
        difficulty=args.get("difficulty", "intermediate"),
        time_available_minutes=args.get("time_available_minutes", 30)
    )
    
    return package.to_dict()
