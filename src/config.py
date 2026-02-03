"""
⚙️ KAIRONEX CONFIGURATION
==========================
Centralized configuration for the Kaironex Deep Brain.

This backend runs as Appwrite Functions - no FastAPI server needed.
"""

import os
from typing import Optional, Tuple, List

# =============================================================================
# APPWRITE CONFIGURATION
# =============================================================================
APPWRITE_ENDPOINT = os.environ.get('APPWRITE_ENDPOINT', 'https://nyc.cloud.appwrite.io/v1')
APPWRITE_PROJECT_ID = os.environ.get('APPWRITE_FUNCTION_PROJECT_ID') or os.environ.get('APPWRITE_PROJECT_ID', '696e9248002198ef6273') # Default to known ID
APPWRITE_API_KEY = os.environ.get('APPWRITE_API_KEY')
APPWRITE_DATABASE_ID = os.environ.get('APPWRITE_DATABASE_ID', '697cb20f00110f6d7530')
STORAGE_BUCKET_ID = os.environ.get('APPWRITE_STORAGE_BUCKET_ID', 'academic_files')

# =============================================================================
# AI CONFIGURATION - DEEP BRAIN ONLY
# =============================================================================
# 
# ARCHITECTURE:
# - DEEP BRAIN (this backend): gemini-3-flash-preview with HIGH thinking
# - REFLEX AGENT (mobile app): gemini-3-flash-preview with MINIMAL thinking
# - VOICE (mobile app): gemini-2.5-flash-native-audio-preview via WebSocket
#
# This backend ONLY handles deep reasoning - no reflex/quick responses.
# The mobile app handles all user-facing interactions.
#
# =============================================================================
GEMINI_API_KEY = os.environ.get('GEMINI_API_KEY')

# Primary Model: Gemini 3 Flash Preview
# Used with HIGH thinking for deep reasoning
GEMINI_3_FLASH = 'gemini-3-flash-preview'

# Thinking Levels (for reference - app uses these too)
THINKING_LEVEL_REFLEX = 'MINIMAL'  # Used by app's Reflex Agent
THINKING_LEVEL_BALANCED = 'MEDIUM' # Balanced thinking
THINKING_LEVEL_DEEP = 'HIGH'       # Used by this backend

# Legacy compatibility
GEMINI_MODEL_NAME = GEMINI_3_FLASH

# =============================================================================
# COLLECTION IDs (Appwrite Database Schema)
# =============================================================================

# Core User Data
USERS_COL = 'users'

# Activity Tracking
SCHEDULE_COL = 'schedule'
STUDY_LOGS_COL = 'study_logs'
DAILY_SNAPSHOTS_COL = 'daily_snapshots'

# Agent System
INTERVENTIONS_COL = 'interventions'
AGENT_MEMORY_COL = 'agent_memory'
POLICY_EPISODES_COL = 'policy_episodes'

# State Collections (The 4 Zones)
VITALITY_STATE_COL = 'vitality_state'
CAMPAIGN_STATE_COL = 'campaign_state'
RADIUS_STATE_COL = 'radius_state'

# Resources
RESOURCES_COL = 'resources'

# Marathon & Thought System
MARATHON_SESSIONS_COL = 'marathon_sessions'
THOUGHT_SIGNATURES_COL = 'thought_signatures'

# Student Adaptation
STUDENT_PROFILES_COL = 'student_profiles'
LIFE_EVENTS_COL = 'life_events'
SCHEDULE_CHANGES_COL = 'schedule_changes'

# Survival Systems
FINANCIAL_STATE_COL = 'financial_state'
INTERNATIONAL_INFO_COL = 'international_info'

# Learning
CONCEPT_MASTERY_COL = 'concept_mastery'

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
# HELPERS
# =============================================================================

def get_config_summary() -> dict:
    """Get a summary of current configuration (safe for logging)."""
    return {
        "appwrite_endpoint": APPWRITE_ENDPOINT,
        "appwrite_project_id": APPWRITE_PROJECT_ID[:8] + "..." if APPWRITE_PROJECT_ID else None,
        "database_id": APPWRITE_DATABASE_ID,
        "model": GEMINI_3_FLASH,
        "api_key_configured": bool(GEMINI_API_KEY),
    }


def validate_config() -> Tuple[bool, List[str]]:
    """Validate that required configuration is present."""
    errors = []
    
    if not GEMINI_API_KEY:
        errors.append("GEMINI_API_KEY is not set")
    
    if not APPWRITE_PROJECT_ID:
        errors.append("APPWRITE_PROJECT_ID is not set")
    
    if not APPWRITE_API_KEY:
        errors.append("APPWRITE_API_KEY is not set")
    
    return len(errors) == 0, errors
