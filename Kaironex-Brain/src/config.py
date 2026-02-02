"""
⚙️ KAIRONEX CONFIGURATION
==========================
Centralized configuration for the Kaironex Brain backend.
"""

import os
from typing import Optional

# =============================================================================
# APPWRITE CONFIGURATION
# =============================================================================
APPWRITE_ENDPOINT = os.environ.get('APPWRITE_ENDPOINT', 'https://cloud.appwrite.io/v1')
APPWRITE_PROJECT_ID = os.environ.get('APPWRITE_FUNCTION_PROJECT_ID') or os.environ.get('APPWRITE_PROJECT_ID')
APPWRITE_API_KEY = os.environ.get('APPWRITE_API_KEY')
APPWRITE_DATABASE_ID = os.environ.get('APPWRITE_DATABASE_ID', '697cb20f00110f6d7530')

# =============================================================================
# AI CONFIGURATION - BICAMERAL ENGINE
# =============================================================================
# 
# KAIRONEX uses exactly TWO Gemini models:
#
# 1. gemini-3-flash-preview (TEXT/REASONING)
#    - All text-based reasoning, function calling, tool use
#    - Uses thinking_level: MINIMAL (fast) or HIGH (deep reasoning)
#    - Supports Google Search grounding for research
#
# 2. gemini-2.5-flash-native-audio-preview-12-2025 (AUDIO/LIVE)
#    - Real-time voice interactions via WebSocket
#    - Uses ai.live.connect() API (NOT generate_content)
#    - See kaironex-interviewer for implementation example
#
# =============================================================================
GEMINI_API_KEY = os.environ.get('GEMINI_API_KEY')

# Primary Model: Gemini 3 Flash (with thinking_level support)
# This is the main model for all reasoning, function calling, and thinking
GEMINI_3_FLASH = 'gemini-3-flash-preview'

# Audio/Live Model: For real-time voice interactions via WebSocket
# Uses ai.live.connect() - NOT regular generate_content
GEMINI_AUDIO_MODEL = 'gemini-2.5-flash-native-audio-preview-12-2025'

# Reflex Model (Fast, pattern-matching) - Uses Gemini 3 Flash with minimal thinking
GEMINI_REFLEX_MODEL = os.environ.get(
    'GEMINI_REFLEX_MODEL', 
    GEMINI_3_FLASH
)

# Deep Thinking Model (Slow, thoughtful reasoning) - Uses Gemini 3 Flash with high thinking
GEMINI_DEEP_MODEL = os.environ.get(
    'GEMINI_DEEP_MODEL',
    GEMINI_3_FLASH
)

# Legacy compatibility - reads from .env GEMINI_MODEL
GEMINI_MODEL_NAME = os.environ.get(
    'GEMINI_MODEL', 
    GEMINI_3_FLASH
)

# Thinking Levels for Gemini 3 (replaces thinking_budget)
# Options: 'MINIMAL', 'LOW', 'MEDIUM', 'HIGH'
THINKING_LEVEL_REFLEX = 'MINIMAL'  # Fast, low latency
THINKING_LEVEL_DEEP = 'HIGH'       # Full reasoning
THINKING_LEVEL_BALANCED = 'MEDIUM' # Balanced

# =============================================================================
# REASONING CONFIGURATION
# =============================================================================
DEFAULT_MAX_THINKING_TOKENS = 8192
MARATHON_MAX_THINKING_TOKENS = 16384
REFLEX_CONFIDENCE_THRESHOLD = 0.8
PRESSURE_DEEP_THRESHOLD = 70

# =============================================================================
# COLLECTION/TABLE IDs (Schema)
# =============================================================================
# Core user data
USERS_COL = 'users'

# Activity tracking
SCHEDULE_COL = 'schedule'
STUDY_LOGS_COL = 'study_logs'
DAILY_SNAPSHOTS_COL = 'daily_snapshots'

# Agent system
INTERVENTIONS_COL = 'interventions'
AGENT_MEMORY_COL = 'agent_memory'
POLICY_EPISODES_COL = 'policy_episodes'

# State Collections (The 4 Zones)
VITALITY_STATE_COL = 'vitality_state'
CAMPAIGN_STATE_COL = 'campaign_state'
RADIUS_STATE_COL = 'radius_state'

# Resource Library
RESOURCES_COL = 'resources'

# New Collections for v2.0 - Core AI
MARATHON_SESSIONS_COL = 'marathon_sessions'
THOUGHT_SIGNATURES_COL = 'thought_signatures'

# New Collections for v2.0 - Student Adaptation
STUDENT_PROFILES_COL = 'student_profiles'
LIFE_EVENTS_COL = 'life_events'
SCHEDULE_CHANGES_COL = 'schedule_changes'

# New Collections for v2.0 - Survival Systems
FINANCIAL_STATE_COL = 'financial_state'
INTERNATIONAL_INFO_COL = 'international_info'

# New Collections for v2.0 - Learning
CONCEPT_MASTERY_COL = 'concept_mastery'

# =============================================================================
# SERVER CONFIGURATION
# =============================================================================
SERVER_HOST = os.environ.get('SERVER_HOST', '0.0.0.0')
SERVER_PORT = int(os.environ.get('SERVER_PORT', '8000'))
DEBUG_MODE = os.environ.get('DEBUG_MODE', 'false').lower() == 'true'

# =============================================================================
# SAFETY CONFIGURATION
# =============================================================================
# Medical terms that must never appear in Vitality agent output
FORBIDDEN_MEDICAL_TERMS = [
    'health', 'healthy', 'medical', 'doctor', 'diagnosis', 'symptom',
    'illness', 'disease', 'anxiety', 'depression', 'insomnia', 'therapy',
    'medication', 'prescription', 'treatment', 'condition', 'disorder',
    'clinic', 'hospital', 'mental health', 'physical health'
]

# =============================================================================
# HELPER FUNCTIONS
# =============================================================================
def get_config_summary() -> dict:
    """Get a summary of current configuration (safe for logging)."""
    return {
        "appwrite_endpoint": APPWRITE_ENDPOINT,
        "appwrite_project_id": APPWRITE_PROJECT_ID[:8] + "..." if APPWRITE_PROJECT_ID else None,
        "database_id": APPWRITE_DATABASE_ID,
        "gemini_reflex_model": GEMINI_REFLEX_MODEL,
        "gemini_deep_model": GEMINI_DEEP_MODEL,
        "debug_mode": DEBUG_MODE,
        "api_key_configured": bool(GEMINI_API_KEY),
    }


def validate_config() -> tuple[bool, list[str]]:
    """Validate that required configuration is present."""
    errors = []
    
    if not GEMINI_API_KEY:
        errors.append("GEMINI_API_KEY is not set")
    
    if not APPWRITE_PROJECT_ID:
        errors.append("APPWRITE_PROJECT_ID is not set")
    
    if not APPWRITE_API_KEY:
        errors.append("APPWRITE_API_KEY is not set")
    
    return len(errors) == 0, errors
