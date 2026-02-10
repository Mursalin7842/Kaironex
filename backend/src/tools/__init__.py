"""
🧰 KAIRONEX TOOLS MODULE
========================
Complete toolset for the Student Life Operating System.

"We have the WHOLE PICTURE of your life."
"""

# Content & Learning
from .content_delivery import ContentDeliveryTool, DeliveryMode, ContentPackage
from .proactive_content_engine import ProactiveContentEngine, PreparedContent, AudioContent
from .mastery_evaluator import MasteryEvaluator, QuizQuestion, MasteryLevel, ConceptMastery
from .study_planner import StudyPlanner, StudyBlock, Deadline, TimeSlot

# Search & Discovery
from .search_tools import WebSearchTool, CareerSearchTool, LocalSearchTool

# Student Survival
from .financial_survival import FinancialSurvivalEngine, FinancialSnapshot, MealDecision, JobNeedAssessment
from .international_student import InternationalStudentEngine, VisaStatus, VisaType, SlangEntry
from .local_radius import LocalRadiusEngine, LocalPlace, Housing, WalkingRoute

# Engagement & Presence
from .presence_engagement import PresenceEngagementEngine, EngagementSession, PresenceState

# Function Calling (HACKATHON CRITICAL)
from .tool_declarations import (
    ALL_KAIRONEX_TOOLS, get_tools_for_agent, get_tool_descriptions,
    STUDY_PLANNER_TOOL, CONTENT_DELIVERY_TOOL, MASTERY_EVALUATOR_TOOL,
    WEB_SEARCH_TOOL, FINANCIAL_TOOL, INTERNATIONAL_TOOL,
    LOCAL_RADIUS_TOOL, PRESENCE_TOOL, SCHEDULE_TOOL, INTERVENTION_TOOL
)
from .tool_executor import ToolExecutor, ToolResult

# Deep Research (GEMINI 3 FLAGSHIP)
from .deep_research import (
    DeepResearchEngine, ResearchQuery, ResearchReport,
    ResearchType, ResearchStatus, ResearchSource
)

__all__ = [
    # Content Delivery
    "ContentDeliveryTool",
    "DeliveryMode",
    "ContentPackage",
    # Proactive Content (Prepares BEFORE you need it)
    "ProactiveContentEngine",
    "PreparedContent",
    "AudioContent",
    # Mastery Evaluation (Knowledge Gatekeeper)
    "MasteryEvaluator",
    "QuizQuestion",
    "MasteryLevel",
    "ConceptMastery",
    # Study Planning
    "StudyPlanner",
    "StudyBlock",
    "Deadline",
    "TimeSlot",
    # Search Tools
    "WebSearchTool",
    "CareerSearchTool",
    "LocalSearchTool",
    # Financial Survival
    "FinancialSurvivalEngine",
    "FinancialSnapshot",
    "MealDecision",
    "JobNeedAssessment",
    # International Student Support
    "InternationalStudentEngine",
    "VisaStatus",
    "VisaType",
    "SlangEntry",
    # Local Radius (Physical World)
    "LocalRadiusEngine",
    "LocalPlace",
    "Housing",
    "WalkingRoute",
    # Presence & Engagement
    "PresenceEngagementEngine",
    "EngagementSession",
    "PresenceState",
    # Function Calling (HACKATHON CRITICAL)
    "ALL_KAIRONEX_TOOLS",
    "get_tools_for_agent",
    "get_tool_descriptions",
    "STUDY_PLANNER_TOOL",
    "CONTENT_DELIVERY_TOOL",
    "MASTERY_EVALUATOR_TOOL",
    "WEB_SEARCH_TOOL",
    "FINANCIAL_TOOL",
    "INTERNATIONAL_TOOL",
    "LOCAL_RADIUS_TOOL",
    "PRESENCE_TOOL",
    "SCHEDULE_TOOL",
    "INTERVENTION_TOOL",
    "ToolExecutor",
    "ToolResult",
    # Deep Research (GEMINI 3 FLAGSHIP)
    "DeepResearchEngine",
    "ResearchQuery",
    "ResearchReport",
    "ResearchType",
    "ResearchStatus",
    "ResearchSource",
]
