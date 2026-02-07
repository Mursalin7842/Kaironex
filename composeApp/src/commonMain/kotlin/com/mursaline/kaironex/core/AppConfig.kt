package com.mursaline.kaironex.core

/**
 * 🔧 KAIRONEX APP CONFIGURATION
 * ==============================
 * Centralized configuration for the Kaironex app.
 *
 * In production, these would come from:
 * - BuildConfig (compile-time)
 * - local.properties (development)
 * - Remote config (runtime)
 */
object AppConfig {

    // ==========================================================================
    // API ENDPOINTS
    // ==========================================================================



    /**
     * Appwrite configuration.
     */
    object Appwrite {
        val ENDPOINT = com.mursaline.kaironex.PlatformSecrets.appwriteEndpoint
        val PROJECT_ID = com.mursaline.kaironex.PlatformSecrets.appwriteProject
        val DATABASE_ID = com.mursaline.kaironex.PlatformSecrets.appwriteDatabase
        val FUNCTION_ID = com.mursaline.kaironex.PlatformSecrets.appwriteFunctionId
        val API_KEY = com.mursaline.kaironex.PlatformSecrets.appwriteApiKey
        
        // TODO: Move to Secrets
        const val STORAGE_BUCKET_ID = "academic_files" 
    }

    object Brain {
        // TODO: Replace with your actual Kaironex Brain URL
        const val baseUrl = "https://kaironex-brain.koyeb.app" 
    }

    // ==========================================================================
    // FEATURE FLAGS
    // ==========================================================================

    /**
     * Enable/disable wake word detection.
     * Disable for devices without microphone or in demo mode.
     */
    val enableWakeWord: Boolean
        get() = getEnvOrDefault("KAIRONEX_ENABLE_WAKE_WORD", "true").toBoolean()

    /**
     * Enable real-time brain connection via WebSocket.
     */
    val enableRealtimeBrain: Boolean
        get() = getEnvOrDefault("KAIRONEX_ENABLE_REALTIME", "false").toBoolean()

    /**
     * Enable voice calls (requires microphone permission).
     */
    val enableVoiceCalls: Boolean
        get() = getEnvOrDefault("KAIRONEX_ENABLE_VOICE", "true").toBoolean()

    /**
     * Use mock data instead of real backend.
     * Useful for demos and testing.
     */
    val useMockData: Boolean
        get() = getEnvOrDefault("KAIRONEX_USE_MOCK", "false").toBoolean()

    // ==========================================================================
    // AI CONFIGURATION
    // ==========================================================================

    object AI {
        /**
         * Primary model: Gemini 3 Flash for all text reasoning.
         */
        const val GEMINI_3_FLASH = "gemini-3-flash-preview"

        /**
         * Audio model: Gemini 2.5 for real-time voice.
         */
        const val GEMINI_AUDIO = "gemini-2.5-flash-native-audio-preview-12-2025"

        /**
         * Thinking levels for different response types.
         */
        const val THINKING_MINIMAL = "MINIMAL"
        const val THINKING_MEDIUM = "MEDIUM"
        const val THINKING_HIGH = "HIGH"

        /**
         * Maximum tokens for reflex responses.
         */
        const val REFLEX_MAX_TOKENS = 1024

        /**
         * Maximum tokens for deep reasoning.
         */
        const val DEEP_MAX_TOKENS = 8192
    }

    // ==========================================================================
    // WAKE WORD CONFIGURATION
    // ==========================================================================

    object WakeWord {
        /**
         * Default wake word if not configured by user.
         */
        const val DEFAULT = "kaironex"

        /**
         * Alternative patterns to match.
         */
        val ALTERNATIVES = listOf("kairo", "cairo", "hey kairo", "okay kairo")

        /**
         * Blocked wake words (trademarked assistant names).
         */
        val BLOCKED = listOf("google", "siri", "alexa", "bixby", "cortana")

        /**
         * Minimum confidence threshold for detection.
         */
        const val MIN_CONFIDENCE = 0.6f
    }

    // ==========================================================================
    // TIMING CONFIGURATION
    // ==========================================================================

    object Timing {
        /**
         * WebSocket ping interval in milliseconds.
         */
        const val WS_PING_INTERVAL_MS = 20_000L

        /**
         * Maximum WebSocket reconnect attempts.
         */
        const val WS_MAX_RECONNECT_ATTEMPTS = 5

        /**
         * Stats refresh interval in milliseconds.
         */
        const val STATS_REFRESH_INTERVAL_MS = 60_000L

        /**
         * Cache expiry time for reflex responses.
         */
        const val REFLEX_CACHE_EXPIRY_MS = 5 * 60 * 1000L
    }

    // ==========================================================================
    // COLLECTION IDs (Appwrite Schema)
    // ==========================================================================

    object Collections {
        const val USERS = "users"
        const val SCHEDULE = "schedule"
        const val STUDY_LOGS = "study_logs"
        const val DAILY_SNAPSHOTS = "daily_snapshots"
        const val INTERVENTIONS = "interventions"
        const val AGENT_MEMORY = "agent_memory"
        const val POLICY_EPISODES = "policy_episodes"
        const val VITALITY_STATE = "vitality_state"
        const val CAMPAIGN_STATE = "campaign_state"
        const val RADIUS_STATE = "radius_state"
        const val RESOURCES = "resources"
        const val MARATHON_SESSIONS = "marathon_sessions"
        const val THOUGHT_SIGNATURES = "thought_signatures"
        const val STUDENT_PROFILES = "student_profiles"
        const val LIFE_EVENTS = "life_events"
        const val SCHEDULE_CHANGES = "schedule_changes"
        const val FINANCIAL_STATE = "financial_state"
        const val INTERNATIONAL_INFO = "international_info"
        const val CONCEPT_MASTERY = "concept_mastery"
        const val MONTHLY_PLANS = "monthly_plans"
    }

    // ==========================================================================
    // HELPERS
    // ==========================================================================

    private fun getEnvOrDefault(key: String, default: String): String {
        return try {
            System.getenv(key) ?: default
        } catch (e: Exception) {
            default
        }
    }
}
