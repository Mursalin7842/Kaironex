import os

# --- AI CONFIGURATION ---
# Default to the model you requested. Can be overridden by .env
GEMINI_MODEL_NAME = os.environ.get('GEMINI_MODEL', 'gemini-2.5-flash-native-audio-preview-12-2025')

# --- DATABASE CONFIGURATION ---
DB_ID = os.environ.get('APPWRITE_DATABASE_ID', '697cb20f00110f6d7530')

# Collection IDs (Mapped to your provided schema)
USERS_COL = 'users'
SCHEDULE_COL = 'schedule'
STUDY_LOGS_COL = 'study_logs'
INTERVENTIONS_COL = 'interventions'
AGENT_MEMORY_COL = 'agent_memory'
POLICY_EPISODES_COL = 'policy_episodes'

# State Collections
VITALITY_STATE_COL = 'vitality_state'
CAMPAIGN_STATE_COL = 'campaign_state'
RADIUS_STATE_COL = 'radius_state'

# Resource Library
RESOURCES_COL = 'resources'
