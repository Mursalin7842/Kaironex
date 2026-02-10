"""
🔧 TOOL DECLARATIONS FOR GEMINI FUNCTION CALLING
=================================================
These declarations tell Gemini 3 what tools are available.

CRITICAL for hackathon: Gemini 3 uses these to decide when to call tools!
"""

from typing import Dict, Any, List
from google.genai import types


# =============================================================================
# STUDY TOOLS
# =============================================================================

STUDY_PLANNER_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="create_study_plan",
            description="Create an optimized study schedule based on deadlines, energy patterns, and available time slots. Use this when a student needs help planning their study sessions.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING, description="The student's user ID"),
                    "subject": types.Schema(type=types.Type.STRING, description="Subject to study (e.g., 'Calculus', 'Physics')"),
                    "deadline": types.Schema(type=types.Type.STRING, description="Deadline in ISO format (YYYY-MM-DD)"),
                    "available_hours": types.Schema(type=types.Type.NUMBER, description="Hours available per day for this subject"),
                    "difficulty": types.Schema(type=types.Type.STRING, enum=["easy", "medium", "hard"], description="Perceived difficulty of the subject"),
                },
                required=["user_id", "subject", "deadline"]
            )
        ),
        types.FunctionDeclaration(
            name="reschedule_session",
            description="Reschedule a study session to a different time. Use when student can't make a scheduled session.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "session_id": types.Schema(type=types.Type.STRING, description="ID of the session to reschedule"),
                    "new_time": types.Schema(type=types.Type.STRING, description="New time in ISO format"),
                    "reason": types.Schema(type=types.Type.STRING, description="Why rescheduling"),
                },
                required=["user_id", "session_id", "new_time"]
            )
        )
    ]
)

CONTENT_DELIVERY_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="get_study_content",
            description="Retrieve learning content for a specific topic. Adapts to student's learning style and time available.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "topic": types.Schema(type=types.Type.STRING, description="Topic to study"),
                    "mode": types.Schema(
                        type=types.Type.STRING, 
                        enum=["deep_dive", "travel", "cram", "review"],
                        description="deep_dive=45min focused, travel=15min audio, cram=5min quick, review=spaced repetition"
                    ),
                    "format_preference": types.Schema(
                        type=types.Type.STRING,
                        enum=["visual", "auditory", "text", "interactive"],
                        description="Student's preferred learning format"
                    ),
                },
                required=["user_id", "topic", "mode"]
            )
        ),
        types.FunctionDeclaration(
            name="prepare_content_ahead",
            description="Pre-load content for upcoming study sessions. Called proactively before study time.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "upcoming_sessions": types.Schema(
                        type=types.Type.ARRAY,
                        items=types.Schema(type=types.Type.STRING),
                        description="List of upcoming session IDs"
                    ),
                },
                required=["user_id", "upcoming_sessions"]
            )
        )
    ]
)

MASTERY_EVALUATOR_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="evaluate_mastery",
            description="Test student's understanding of a concept before letting them move on. The Knowledge Gatekeeper.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "concept": types.Schema(type=types.Type.STRING, description="Concept to evaluate"),
                    "subject": types.Schema(type=types.Type.STRING, description="Subject area"),
                    "depth": types.Schema(
                        type=types.Type.STRING,
                        enum=["surface", "understanding", "application", "synthesis"],
                        description="How deep to test"
                    ),
                },
                required=["user_id", "concept", "subject"]
            )
        ),
        types.FunctionDeclaration(
            name="generate_quiz",
            description="Generate a quiz for a topic with adaptive difficulty.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "topic": types.Schema(type=types.Type.STRING),
                    "num_questions": types.Schema(type=types.Type.INTEGER, description="Number of questions (1-10)"),
                    "difficulty": types.Schema(type=types.Type.STRING, enum=["easy", "medium", "hard", "adaptive"]),
                },
                required=["user_id", "topic"]
            )
        )
    ]
)

# =============================================================================
# SEARCH TOOLS
# =============================================================================

WEB_SEARCH_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="search_web",
            description="Search the web for current information. Use for research, finding resources, or answering factual questions.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "query": types.Schema(type=types.Type.STRING, description="Search query"),
                    "search_type": types.Schema(
                        type=types.Type.STRING,
                        enum=["general", "academic", "news", "video"],
                        description="Type of search to perform"
                    ),
                    "max_results": types.Schema(type=types.Type.INTEGER, description="Maximum results to return"),
                },
                required=["query"]
            )
        ),
        types.FunctionDeclaration(
            name="search_careers",
            description="Search for job opportunities, internships, or career information relevant to student's major.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "role": types.Schema(type=types.Type.STRING, description="Job role or title"),
                    "location": types.Schema(type=types.Type.STRING, description="Preferred location"),
                    "job_type": types.Schema(
                        type=types.Type.STRING,
                        enum=["internship", "part_time", "full_time", "remote"],
                    ),
                    "skills": types.Schema(
                        type=types.Type.ARRAY,
                        items=types.Schema(type=types.Type.STRING),
                        description="Required skills"
                    ),
                },
                required=["role"]
            )
        ),
        types.FunctionDeclaration(
            name="search_local",
            description="Search for local services, places, or resources near the student.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "query": types.Schema(type=types.Type.STRING, description="What to search for"),
                    "category": types.Schema(
                        type=types.Type.STRING,
                        enum=["food", "study_spots", "housing", "transportation", "healthcare", "entertainment"],
                    ),
                    "max_distance_km": types.Schema(type=types.Type.NUMBER, description="Maximum distance in kilometers"),
                },
                required=["query"]
            )
        )
    ]
)

# =============================================================================
# FINANCIAL SURVIVAL TOOLS
# =============================================================================

FINANCIAL_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="check_financial_status",
            description="Get student's current financial snapshot including balance, upcoming expenses, and warnings.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "include_forecast": types.Schema(type=types.Type.BOOLEAN, description="Include 30-day forecast"),
                },
                required=["user_id"]
            )
        ),
        types.FunctionDeclaration(
            name="suggest_meal",
            description="Suggest a meal option based on budget, time, and nutritional needs.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "budget": types.Schema(type=types.Type.NUMBER, description="Maximum budget for this meal"),
                    "time_available": types.Schema(type=types.Type.INTEGER, description="Minutes available to eat"),
                    "meal_type": types.Schema(type=types.Type.STRING, enum=["breakfast", "lunch", "dinner", "snack"]),
                },
                required=["user_id"]
            )
        ),
        types.FunctionDeclaration(
            name="assess_job_need",
            description="Evaluate whether the student needs additional income and suggest options.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "monthly_shortfall": types.Schema(type=types.Type.NUMBER, description="Current monthly budget gap"),
                },
                required=["user_id"]
            )
        )
    ]
)

# =============================================================================
# INTERNATIONAL STUDENT TOOLS
# =============================================================================

INTERNATIONAL_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="check_visa_status",
            description="Check student's visa status and any upcoming requirements or warnings.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                },
                required=["user_id"]
            )
        ),
        types.FunctionDeclaration(
            name="check_work_hours",
            description="Check how many work hours the student has used vs their visa limit.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "this_week": types.Schema(type=types.Type.BOOLEAN, description="Check this week's hours"),
                },
                required=["user_id"]
            )
        ),
        types.FunctionDeclaration(
            name="explain_slang",
            description="Explain American slang or cultural references to international students.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "phrase": types.Schema(type=types.Type.STRING, description="The slang or phrase to explain"),
                    "context": types.Schema(type=types.Type.STRING, description="Where they heard it"),
                },
                required=["phrase"]
            )
        ),
        types.FunctionDeclaration(
            name="get_cultural_tip",
            description="Provide a cultural tip for navigating a situation in the US.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "situation": types.Schema(type=types.Type.STRING, description="The situation needing guidance"),
                },
                required=["situation"]
            )
        )
    ]
)

# =============================================================================
# LOCAL RADIUS TOOLS
# =============================================================================

LOCAL_RADIUS_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="find_study_spot",
            description="Find optimal study locations near the student based on noise level, Wi-Fi, and hours.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "preference": types.Schema(
                        type=types.Type.STRING,
                        enum=["quiet", "moderate", "cafe_vibe", "outdoors"],
                    ),
                    "need_wifi": types.Schema(type=types.Type.BOOLEAN),
                    "hours_needed": types.Schema(type=types.Type.NUMBER, description="How long they plan to study"),
                },
                required=["user_id"]
            )
        ),
        types.FunctionDeclaration(
            name="get_walking_route",
            description="Get an optimized walking route between locations, considering weather and time.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "from_location": types.Schema(type=types.Type.STRING),
                    "to_location": types.Schema(type=types.Type.STRING),
                    "leave_by": types.Schema(type=types.Type.STRING, description="Time to leave by (ISO format)"),
                },
                required=["from_location", "to_location"]
            )
        ),
        types.FunctionDeclaration(
            name="check_housing_options",
            description="Search for housing options based on budget and preferences.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "max_rent": types.Schema(type=types.Type.NUMBER, description="Maximum monthly rent"),
                    "near": types.Schema(type=types.Type.STRING, description="Near what location (campus, work, etc.)"),
                    "roommates_ok": types.Schema(type=types.Type.BOOLEAN),
                },
                required=["user_id", "max_rent"]
            )
        )
    ]
)

# =============================================================================
# PRESENCE & ENGAGEMENT TOOLS
# =============================================================================

PRESENCE_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="check_engagement",
            description="Check student's current engagement level and whether they need a nudge.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "session_id": types.Schema(type=types.Type.STRING, description="Current study session ID"),
                },
                required=["user_id"]
            )
        ),
        types.FunctionDeclaration(
            name="send_gentle_nudge",
            description="Send a gentle reminder to get the student back on track. Not aggressive.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "nudge_type": types.Schema(
                        type=types.Type.STRING,
                        enum=["break_reminder", "return_to_work", "celebrate_progress", "check_in"],
                    ),
                    "message": types.Schema(type=types.Type.STRING, description="Custom message to send"),
                },
                required=["user_id", "nudge_type"]
            )
        ),
        types.FunctionDeclaration(
            name="detect_distraction",
            description="Analyze if student is distracted based on activity patterns.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "activity_log": types.Schema(
                        type=types.Type.ARRAY,
                        items=types.Schema(type=types.Type.STRING),
                        description="Recent activity events"
                    ),
                },
                required=["user_id"]
            )
        )
    ]
)

# =============================================================================
# SCHEDULE MANAGEMENT TOOLS
# =============================================================================

SCHEDULE_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="get_schedule",
            description="Get student's schedule for a specific date or date range.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "date": types.Schema(type=types.Type.STRING, description="Date in YYYY-MM-DD format"),
                    "include_suggestions": types.Schema(type=types.Type.BOOLEAN, description="Include AI optimization suggestions"),
                },
                required=["user_id", "date"]
            )
        ),
        types.FunctionDeclaration(
            name="add_schedule_block",
            description="Add a new block to the student's schedule. Will be validated by the brain.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "title": types.Schema(type=types.Type.STRING),
                    "block_type": types.Schema(
                        type=types.Type.STRING,
                        enum=["study", "work", "class", "personal", "break"],
                    ),
                    "start": types.Schema(type=types.Type.STRING, description="Start time ISO format"),
                    "end": types.Schema(type=types.Type.STRING, description="End time ISO format"),
                    "is_flexible": types.Schema(type=types.Type.BOOLEAN, description="Can this be moved if needed?"),
                },
                required=["user_id", "title", "block_type", "start", "end"]
            )
        ),
        types.FunctionDeclaration(
            name="handle_life_event",
            description="Process a life event that affects the schedule (illness, wedding, overtime, etc.).",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "event_type": types.Schema(
                        type=types.Type.STRING,
                        enum=["health", "work", "family", "social", "academic", "emergency"],
                    ),
                    "description": types.Schema(type=types.Type.STRING, description="What happened"),
                    "duration_days": types.Schema(type=types.Type.INTEGER, description="How long will this affect schedule"),
                    "severity": types.Schema(type=types.Type.STRING, enum=["minor", "moderate", "major", "critical"]),
                },
                required=["user_id", "event_type", "description"]
            )
        )
    ]
)

# =============================================================================
# INTERVENTION TOOLS
# =============================================================================

INTERVENTION_TOOL = types.Tool(
    function_declarations=[
        types.FunctionDeclaration(
            name="trigger_intervention",
            description="Trigger a proactive AI intervention when something important is detected.",
            parameters=types.Schema(
                type=types.Type.OBJECT,
                properties={
                    "user_id": types.Schema(type=types.Type.STRING),
                    "intervention_type": types.Schema(
                        type=types.Type.STRING,
                        enum=["deadline_warning", "burnout_prevention", "financial_alert", "visa_reminder", "celebration", "check_in"],
                    ),
                    "urgency": types.Schema(type=types.Type.STRING, enum=["low", "medium", "high", "critical"]),
                    "message": types.Schema(type=types.Type.STRING, description="The intervention message"),
                    "suggested_actions": types.Schema(
                        type=types.Type.ARRAY,
                        items=types.Schema(type=types.Type.STRING),
                        description="Suggested actions for the student"
                    ),
                },
                required=["user_id", "intervention_type", "message"]
            )
        )
    ]
)

# =============================================================================
# ALL TOOLS COMBINED
# =============================================================================

ALL_KAIRONEX_TOOLS = [
    STUDY_PLANNER_TOOL,
    CONTENT_DELIVERY_TOOL,
    MASTERY_EVALUATOR_TOOL,
    WEB_SEARCH_TOOL,
    FINANCIAL_TOOL,
    INTERNATIONAL_TOOL,
    LOCAL_RADIUS_TOOL,
    PRESENCE_TOOL,
    SCHEDULE_TOOL,
    INTERVENTION_TOOL,
]


def get_tools_for_agent(agent_type: str) -> List[types.Tool]:
    """Get relevant tools for a specific agent type."""
    
    tool_map = {
        "study": [STUDY_PLANNER_TOOL, CONTENT_DELIVERY_TOOL, MASTERY_EVALUATOR_TOOL, WEB_SEARCH_TOOL, PRESENCE_TOOL],
        "campaign": [WEB_SEARCH_TOOL, SCHEDULE_TOOL, INTERVENTION_TOOL],
        "vitality": [FINANCIAL_TOOL, LOCAL_RADIUS_TOOL, PRESENCE_TOOL, SCHEDULE_TOOL],
        "radius": [LOCAL_RADIUS_TOOL, INTERNATIONAL_TOOL, WEB_SEARCH_TOOL],
        "supervisor": ALL_KAIRONEX_TOOLS,  # Supervisor has access to everything
        "main_brain": ALL_KAIRONEX_TOOLS,
    }
    
    return tool_map.get(agent_type, [WEB_SEARCH_TOOL, SCHEDULE_TOOL])


def get_tool_descriptions() -> str:
    """Get human-readable descriptions of all tools for prompts."""
    descriptions = []
    
    for tool in ALL_KAIRONEX_TOOLS:
        if tool.function_declarations:
            for func in tool.function_declarations:
                descriptions.append(f"- **{func.name}**: {func.description}")
    
    return "\n".join(descriptions)
