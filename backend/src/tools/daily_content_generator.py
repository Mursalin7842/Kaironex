"""
📚 DAILY CONTENT GENERATOR - Just-in-Time Resources, Flashcards & Gatekeeper Quizzes
=====================================================================================
Generates learning content for today's scheduled tasks.

ARCHITECTURE:
- Resources: Part-by-part JSON (resource_1, resource_2...) for white UI display
- Flash Cards: JSON array with front/back structure
- Macro Quizzes: Gatekeeper quizzes that MUST be passed to unlock next day

This module is called by:
1. Study Agent during daily prep
2. Scheduled Appwrite Function for proactive content generation
3. CLI for manual testing

Usage:
    # As module
    from src.tools.daily_content_generator import DailyContentGenerator
    generator = DailyContentGenerator()
    generator.process_user_daily_content(user_id, target_date)
    
    # As CLI
    python -m src.tools.daily_content_generator --user demo_user_001 --date 2026-02-09
"""

import os
import sys
import json
import argparse
from datetime import datetime
from typing import Dict, Any, List, Optional

# Handle imports whether run as module or script
try:
    from ..utils.db_helper import KairoDB
    from ..utils.gemini_client import GeminiClient
except ImportError:
    # Running as script - add parent to path
    current_dir = os.path.dirname(os.path.abspath(__file__))
    src_path = os.path.dirname(current_dir)
    root_path = os.path.dirname(src_path)
    if src_path not in sys.path:
        sys.path.insert(0, src_path)
    if root_path not in sys.path:
        sys.path.insert(0, root_path)
    
    from dotenv import load_dotenv
    load_dotenv(os.path.join(root_path, '.env'))
    
    from utils.db_helper import KairoDB
    from utils.gemini_client import GeminiClient


class DailyContentGenerator:
    """
    Generates comprehensive learning content for daily study tasks.
    
    OUTPUT FORMATS:
    1. just_in_time_resources: JSON array of resources, each with:
       - id: "resource_1", "resource_2", etc.
       - name: Human-readable title
       - type: "concept", "example", "diagram_desc", "key_points", "application"
       - content: The actual learning content (text-based, white-bg friendly)
       - estimated_read_time: Minutes to consume
       
    2. flash_cards: JSON array of flashcards:
       - id: "card_1", "card_2", etc.
       - front: Question or term
       - back: Answer or definition
       - difficulty: "easy", "medium", "hard"
       - tags: Related topics
       
    3. macro_quizes: JSON object with gatekeeper quiz:
       - quiz_id: Unique identifier
       - title: Quiz title
       - description: What this quiz verifies
       - pass_threshold: Percentage needed to pass (e.g., 0.7 = 70%)
       - must_pass_to_proceed: true (blocks next day if failed)
       - questions: Array of quiz questions with options and correct answers
       - max_attempts: Number of retries allowed
       
    4. quiz_result: (Written by app after quiz attempt)
       - Used by Gemini to analyze performance and adjust scheduling
    """
    
    def __init__(self, db: Optional[KairoDB] = None, gemini: Optional[GeminiClient] = None):
        """
        Initialize the content generator.
        
        Args:
            db: Optional KairoDB instance (creates new if not provided)
            gemini: Optional GeminiClient instance (creates new if not provided)
        """
        self.db = db or KairoDB()
        self.gemini = gemini or GeminiClient()
    
    def _generate_with_gemini(self, prompt: str, json_mode: bool = True) -> str:
        """Wrapper for Gemini generation with proper settings."""
        return self.gemini.generate_response(
            prompt=prompt,
            max_tokens=8192,
            json_mode=json_mode,
            model_type="thinking"
        )
    
    def generate_resources_for_task(self, task: Dict[str, Any]) -> str:
        """
        Generate part-by-part text resources for a study task.
        Returns JSON string of resources array.
        """
        title = task.get('title', 'Study Session')
        subject = task.get('subject', 'General')
        topics = task.get('topics', '')
        learning_objectives: List[str] = []
        
        # Extract learning objectives from metadata if available
        metadata = task.get('metadata_json', '{}')
        if isinstance(metadata, str):
            try:
                metadata = json.loads(metadata)
            except:
                metadata = {}
        learning_objectives = metadata.get('learning_objectives', [])
        
        prompt = f"""Generate comprehensive text-based learning resources for this study task.

TASK: {title}
SUBJECT: {subject}
TOPICS/BREAKDOWN: {topics}
LEARNING OBJECTIVES: {json.dumps(learning_objectives) if learning_objectives else 'Based on topics'}

CREATE 5-8 RESOURCES in this EXACT JSON format:
{{
  "resources": [
    {{
      "id": "resource_1",
      "name": "Introduction to [Topic]",
      "type": "concept",
      "content": "Clear, readable explanation... Use **bold** for key terms. Break into paragraphs. Explain like teaching a peer student.",
      "estimated_read_time": 3
    }},
    {{
      "id": "resource_2", 
      "name": "Key Definitions",
      "type": "key_points",
      "content": "### Key Terms\\n\\n**Term 1**: Definition...\\n\\n**Term 2**: Definition...",
      "estimated_read_time": 2
    }},
    {{
      "id": "resource_3",
      "name": "Worked Example",
      "type": "example",
      "content": "### Example Problem\\n\\n**Given**: ...\\n**Find**: ...\\n**Solution**:\\n1. Step one...\\n2. Step two...",
      "estimated_read_time": 4
    }},
    {{
      "id": "resource_4",
      "name": "Visual Concept Explanation",
      "type": "diagram_desc",
      "content": "### Process Flow\\n\\n[State A] → (Trigger) → [State B]\\n\\nExplanation of the flow...",
      "estimated_read_time": 3
    }},
    {{
      "id": "resource_5",
      "name": "Real-World Application",
      "type": "application",
      "content": "How this applies in practice...",
      "estimated_read_time": 2
    }}
  ]
}}

REQUIREMENTS:
1. Each resource is STANDALONE - can be read independently
2. Content must be READABLE on WHITE background (dark text)
3. Use Markdown formatting for structure
4. Include practical examples and mnemonics where helpful
5. Write at university student level
6. Each resource should be 150-500 words
7. Cover all learning objectives across the resources

OUTPUT: Only the JSON object, no markdown code blocks."""

        try:
            response = self._generate_with_gemini(prompt, json_mode=True)
            
            # Clean and parse response
            response_text = response.strip()
            if response_text.startswith('```'):
                response_text = response_text.split('```')[1]
                if response_text.startswith('json'):
                    response_text = response_text[4:]
            
            # Validate JSON
            parsed = json.loads(response_text)
            resources = parsed.get('resources', [])
            
            print(f"   ✅ Generated {len(resources)} resources for: {title}")
            return json.dumps(resources)
            
        except Exception as e:
            print(f"   ❌ Resource generation failed: {e}")
            # Return minimal fallback
            return json.dumps([{
                "id": "resource_1",
                "name": f"Introduction to {title}",
                "type": "concept",
                "content": f"Study materials for {subject}. Topics: {topics}",
                "estimated_read_time": 5
            }])
    
    def generate_flashcards_for_task(self, task: Dict[str, Any]) -> str:
        """
        Generate flashcards for a study task.
        Returns JSON string of flashcards array.
        """
        title = task.get('title', 'Study Session')
        subject = task.get('subject', 'General')
        topics = task.get('topics', '')
        
        prompt = f"""Generate study flashcards for this task.

TASK: {title}
SUBJECT: {subject}
TOPICS: {topics}

CREATE 8-12 FLASHCARDS in this EXACT JSON format:
{{
  "flashcards": [
    {{
      "id": "card_1",
      "front": "What is [concept]?",
      "back": "Clear, concise answer that a student can memorize.",
      "difficulty": "easy",
      "tags": ["topic1", "definition"]
    }},
    {{
      "id": "card_2",
      "front": "Explain the relationship between X and Y",
      "back": "X relates to Y by...",
      "difficulty": "medium",
      "tags": ["topic2", "relationship"]
    }},
    {{
      "id": "card_3",
      "front": "In [scenario], what would happen if...?",
      "back": "The result would be... because...",
      "difficulty": "hard",
      "tags": ["application", "analysis"]
    }}
  ]
}}

REQUIREMENTS:
1. Mix of easy (definitions), medium (explanations), hard (applications)
2. Front should be a clear question
3. Back should be concise but complete (max 100 words)
4. Include formula cards if subject requires it
5. Tags help with filtering
6. Cover all key concepts from the topics

OUTPUT: Only the JSON object, no markdown code blocks."""

        try:
            response = self._generate_with_gemini(prompt, json_mode=True)
            
            # Clean and parse response
            response_text = response.strip()
            if response_text.startswith('```'):
                response_text = response_text.split('```')[1]
                if response_text.startswith('json'):
                    response_text = response_text[4:]
            
            # Validate JSON
            parsed = json.loads(response_text)
            flashcards = parsed.get('flashcards', [])
            
            print(f"   ✅ Generated {len(flashcards)} flashcards for: {title}")
            return json.dumps(flashcards)
            
        except Exception as e:
            print(f"   ❌ Flashcard generation failed: {e}")
            return json.dumps([{
                "id": "card_1",
                "front": f"What is the main concept of {title}?",
                "back": f"Study {subject} topics: {topics}",
                "difficulty": "medium",
                "tags": [subject.lower()]
            }])
    
    def generate_gatekeeper_quiz_for_task(self, task: Dict[str, Any]) -> str:
        """
        Generate gatekeeper quiz that MUST be passed to unlock next day's content.
        Returns JSON string of quiz object.
        """
        title = task.get('title', 'Study Session')
        subject = task.get('subject', 'General')
        topics = task.get('topics', '')
        task_id = task.get('taskId', task.get('$id', 'unknown'))
        
        prompt = f"""Generate a GATEKEEPER QUIZ to verify student understanding.

TASK: {title}
SUBJECT: {subject}
TOPICS: {topics}

This quiz MUST be passed to unlock tomorrow's study content!

CREATE A QUIZ in this EXACT JSON format:
{{
  "quiz": {{
    "quiz_id": "gatekeeper_{task_id}",
    "title": "Knowledge Check: {title}",
    "description": "Verify your understanding before proceeding to new material",
    "pass_threshold": 0.7,
    "must_pass_to_proceed": true,
    "max_attempts": 3,
    "time_limit_minutes": 10,
    "questions": [
      {{
        "question_id": "q1",
        "question_text": "What is...?",
        "question_type": "multiple_choice",
        "options": [
          {{"id": "a", "text": "Option A"}},
          {{"id": "b", "text": "Option B"}},
          {{"id": "c", "text": "Option C"}},
          {{"id": "d", "text": "Option D"}}
        ],
        "correct_answer": "b",
        "explanation": "The correct answer is B because...",
        "points": 1,
        "difficulty": "easy"
      }},
      {{
        "question_id": "q2",
        "question_text": "Which of the following...",
        "question_type": "multiple_choice",
        "options": [
          {{"id": "a", "text": "Option A"}},
          {{"id": "b", "text": "Option B"}},
          {{"id": "c", "text": "Option C"}},
          {{"id": "d", "text": "Option D"}}
        ],
        "correct_answer": "c",
        "explanation": "C is correct because...",
        "points": 1,
        "difficulty": "medium"
      }},
      {{
        "question_id": "q3",
        "question_text": "True or False: [statement]",
        "question_type": "true_false",
        "options": [
          {{"id": "true", "text": "True"}},
          {{"id": "false", "text": "False"}}
        ],
        "correct_answer": "true",
        "explanation": "This is true because...",
        "points": 1,
        "difficulty": "easy"
      }},
      {{
        "question_id": "q4",
        "question_text": "In [scenario], what would be the result of...?",
        "question_type": "multiple_choice",
        "options": [
          {{"id": "a", "text": "Option A"}},
          {{"id": "b", "text": "Option B"}},
          {{"id": "c", "text": "Option C"}},
          {{"id": "d", "text": "Option D"}}
        ],
        "correct_answer": "a",
        "explanation": "A is correct because applying the concept...",
        "points": 2,
        "difficulty": "hard"
      }},
      {{
        "question_id": "q5",
        "question_text": "Application question...",
        "question_type": "multiple_choice",
        "options": [
          {{"id": "a", "text": "Option A"}},
          {{"id": "b", "text": "Option B"}},
          {{"id": "c", "text": "Option C"}},
          {{"id": "d", "text": "Option D"}}
        ],
        "correct_answer": "d",
        "explanation": "D demonstrates understanding because...",
        "points": 2,
        "difficulty": "medium"
      }}
    ],
    "total_points": 7,
    "passing_points": 5,
    "feedback_on_fail": "Review the resources and try again. Focus on: [specific topics]",
    "feedback_on_pass": "Great job! You've mastered today's material. Tomorrow's content is now unlocked!"
  }}
}}

REQUIREMENTS:
1. 5-7 questions covering key concepts
2. Mix of easy (2), medium (2-3), hard (1-2) questions
3. Include explanations for EVERY answer
4. Points: easy=1, medium=1, hard=2
5. 70% pass threshold (must get 5/7 points)
6. Questions should test UNDERSTANDING, not just memorization
7. Include at least one application/scenario question

OUTPUT: Only the JSON object, no markdown code blocks."""

        try:
            response = self._generate_with_gemini(prompt, json_mode=True)
            
            # Clean and parse response
            response_text = response.strip()
            if response_text.startswith('```'):
                response_text = response_text.split('```')[1]
                if response_text.startswith('json'):
                    response_text = response_text[4:]
            
            # Validate JSON
            parsed = json.loads(response_text)
            quiz = parsed.get('quiz', {})
            
            num_questions = len(quiz.get('questions', []))
            print(f"   ✅ Generated gatekeeper quiz ({num_questions} questions) for: {title}")
            return json.dumps(quiz)
            
        except Exception as e:
            print(f"   ❌ Quiz generation failed: {e}")
            # Return minimal fallback quiz
            return json.dumps({
                "quiz_id": f"gatekeeper_{task_id}",
                "title": f"Knowledge Check: {title}",
                "description": "Verify your understanding",
                "pass_threshold": 0.7,
                "must_pass_to_proceed": True,
                "max_attempts": 3,
                "questions": [{
                    "question_id": "q1",
                    "question_text": f"Did you complete studying {title}?",
                    "question_type": "true_false",
                    "options": [
                        {"id": "true", "text": "Yes, I understood the material"},
                        {"id": "false", "text": "No, I need to review more"}
                    ],
                    "correct_answer": "true",
                    "explanation": "Self-assessment of understanding",
                    "points": 1
                }],
                "total_points": 1,
                "passing_points": 1
            })
    
    def generate_content_for_task(self, task: Dict[str, Any]) -> Dict[str, Any]:
        """Generate all content for a single task."""
        task_id = task.get('taskId') or task.get('$id') or ''
        title = task.get('title', 'Unknown Task')
        
        print(f"\n🎯 Generating content for: {title}")
        
        # Generate all content sequentially (Gemini calls are synchronous)
        resources = self.generate_resources_for_task(task)
        flashcards = self.generate_flashcards_for_task(task)
        quiz = self.generate_gatekeeper_quiz_for_task(task)
        
        return {
            'task_id': task_id,
            'just_in_time_resources': resources,
            'flash_cards': flashcards,
            'macro_quizes': quiz
        }
    
    def update_task_with_content(self, task_id: str, content: Dict[str, Any]) -> bool:
        """Update the schedule task with generated content."""
        try:
            updates = {
                'just_in_time_resources': content['just_in_time_resources'],
                'flash_cards': content['flash_cards'],
                'macro_quizes': content['macro_quizes']
            }
            
            result = self.db.update_schedule_task(task_id, updates)
            if result:
                print(f"   💾 Content saved to database for task: {task_id}")
            return result
        except Exception as e:
            print(f"   ❌ Failed to save content: {e}")
            return False
    
    def process_user_daily_content(self, user_id: str, target_date: Optional[str] = None) -> int:
        """
        Main entry point: Generate all content for a user's daily tasks.
        
        Args:
            user_id: The user ID to generate content for
            target_date: Date in YYYY-MM-DD format (defaults to today)
            
        Returns:
            Number of tasks successfully processed
        """
        if target_date is None:
            target_date = datetime.now().strftime('%Y-%m-%d')
        
        print(f"\n{'='*60}")
        print(f"📚 DAILY CONTENT GENERATOR")
        print(f"{'='*60}")
        print(f"User: {user_id}")
        print(f"Date: {target_date}")
        print(f"{'='*60}")
        
        # Get today's tasks
        schedule = self.db.get_schedule(user_id)
        
        # Filter for today's tasks (study, review, practice - not blocked)
        tasks = []
        for task in schedule:
            task_start = task.get('startTime', '')
            task_type = task.get('type', '')
            
            # Check if task is today and is a study-type task
            if target_date in task_start and task_type not in ['blocked']:
                tasks.append(task)
        
        print(f"📅 Found {len(tasks)} study tasks for {target_date}")
        
        if not tasks:
            print("\n⚠️ No study tasks found for this date!")
            print("   Make sure you have scheduled tasks in the schedule table.")
            return 0
        
        # Generate content for each task
        success_count = 0
        for i, task in enumerate(tasks, 1):
            print(f"\n[{i}/{len(tasks)}] Processing task...")
            
            try:
                content = self.generate_content_for_task(task)
                
                task_id = task.get('$id')  # Use Appwrite document ID
                if task_id:
                    if self.update_task_with_content(task_id, content):
                        success_count += 1
                else:
                    print(f"   ⚠️ Task has no $id, cannot save to database")
                    
            except Exception as e:
                print(f"   ❌ Error processing task: {e}")
                import traceback
                traceback.print_exc()
        
        print(f"\n{'='*60}")
        print(f"✅ COMPLETE: Generated content for {success_count}/{len(tasks)} tasks")
        print(f"{'='*60}")
        
        # Summary
        print("\n📊 CONTENT SUMMARY:")
        print("   - just_in_time_resources: 5-8 text resources per task (white UI ready)")
        print("   - flash_cards: 8-12 flashcards per task")
        print("   - macro_quizes: 5-7 question gatekeeper quiz per task")
        print("\n🔐 GATEKEEPER: Students must pass quiz (70%) to unlock next day's content!")
        
        return success_count
    
    def process_all_users_daily_content(self, target_date: Optional[str] = None) -> Dict[str, int]:
        """
        Generate content for ALL users with tasks on the target date.
        Called by scheduled Appwrite Function for proactive content generation.
        
        Args:
            target_date: Date in YYYY-MM-DD format (defaults to today)
            
        Returns:
            Dict mapping user_id to number of tasks processed
        """
        if target_date is None:
            target_date = datetime.now().strftime('%Y-%m-%d')
        
        print(f"\n{'='*60}")
        print(f"📚 DAILY CONTENT GENERATOR - ALL USERS")
        print(f"{'='*60}")
        print(f"Date: {target_date}")
        print(f"{'='*60}")
        
        # Get all users with agent_memory (active users)
        try:
            from ..config import APPWRITE_DATABASE_ID, AGENT_MEMORY_COL
            api_response = self.db.db.list_rows(
                database_id=APPWRITE_DATABASE_ID,
                table_id=AGENT_MEMORY_COL,
                queries=[]
            )
            
            users = [row.get('userId') for row in api_response.get('rows', []) if row.get('userId')]
            print(f"📋 Found {len(users)} active users")
            
        except Exception as e:
            print(f"❌ Failed to get user list: {e}")
            return {}
        
        # Process each user
        user_results: Dict[str, int] = {}
        for user_id in users:
            try:
                count = self.process_user_daily_content(user_id, target_date)
                user_results[user_id] = count
            except Exception as e:
                print(f"❌ Failed to process user {user_id}: {e}")
                user_results[user_id] = 0
        
        print(f"\n{'='*60}")
        print(f"✅ ALL USERS COMPLETE")
        for user_id, count in user_results.items():
            print(f"   {user_id}: {count} tasks")
        print(f"{'='*60}")
        
        return user_results


def main():
    """CLI entry point for manual testing."""
    parser = argparse.ArgumentParser(
        description="Generate daily learning content for Kaironex students"
    )
    parser.add_argument(
        '--user', '-u',
        type=str,
        required=True,
        help='User ID to generate content for'
    )
    parser.add_argument(
        '--date', '-d',
        type=str,
        default=None,
        help='Target date (YYYY-MM-DD format, defaults to today)'
    )
    parser.add_argument(
        '--all-users',
        action='store_true',
        help='Process all active users (ignores --user)'
    )
    
    args = parser.parse_args()
    
    generator = DailyContentGenerator()
    
    if args.all_users:
        generator.process_all_users_daily_content(args.date)
    else:
        generator.process_user_daily_content(args.user, args.date)


if __name__ == "__main__":
    main()
