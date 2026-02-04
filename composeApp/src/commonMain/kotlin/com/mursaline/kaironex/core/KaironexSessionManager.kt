package com.mursaline.kaironex.core

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import com.mursaline.kaironex.brain.BrainApiClient
import com.mursaline.kaironex.brain.AppwriteBridge
import com.mursaline.kaironex.brain.ReflexAgent
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


/**
 * 🧠 KAIRONEX SESSION MANAGER
 * ============================
 * Central manager for all active session state including:
 * - Brain API connection
 * - Real-time data sync
 * - User session
 *
 * This is the "glue" that connects all the components together.
 */
import io.ktor.client.HttpClient

class KaironexSessionManager(
    private val brainClient: BrainApiClient,
    private val appwriteBridge: AppwriteBridge,
    private val httpClient: HttpClient
) {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisorJob)

    // Session state
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Reflex agent (created per user)
    private var reflexAgent: ReflexAgent? = null

    // Stats repository (created per user)
    private var statsRepository: AppwriteStatsRepository? = null

    /**
     * Initialize the session for a user.
     */
    fun initialize(
        userId: String
    ) {
        _userId.value = userId

        // Create user-specific components
        reflexAgent = ReflexAgent(brainClient, userId)
        statsRepository = AppwriteStatsRepository(appwriteBridge, httpClient, userId)

        // Connect to brain WebSocket
        // Connect to brain WebSocket
        scope.launch {
            if (AppConfig.enableRealtimeBrain) {
                try {
                    brainClient.connectWebSocket(userId, scope)
                    println("✅ Connected to brain WebSocket")
                } catch (e: Exception) {
                    println("⚠️ Failed to connect to brain: ${e.message}")
                }
            } else {
                println("⚠️ Realtime Brain disabled in config - Skipping WebSocket connection")
            }
        }

        // Initial data sync
        scope.launch {
            try {
                statsRepository?.initializeUserTables() // Ensure all tables exist (Idempotent)
                statsRepository?.refreshAll()
                println("✅ Initial data sync complete")
            } catch (e: Exception) {
                println("⚠️ Initial sync failed: ${e.message}")
            }
        }

        _isInitialized.value = true
        println("🚀 KaironexSessionManager initialized for user: $userId")
    }

    /**
     * Get a quick reflex response.
     */
    suspend fun getQuickResponse(prompt: String): String {
        return reflexAgent?.respond(prompt)?.content ?: "I'm still waking up..."
    }

    /**
     * Get the stats repository.
     */
    fun getStatsRepository(): AppwriteStatsRepository? = statsRepository

    /**
     * Refresh all data from backend.
     */
    suspend fun refreshData() {
        statsRepository?.refreshAll()
    }

    /**
     * Trigger a brain event.
     */
    suspend fun triggerBrainEvent(eventType: String, data: Map<String, String> = emptyMap()) {
        val uid = _userId.value ?: return
        brainClient.triggerBrain(uid, eventType, data)
    }

    /**
     * Trigger voice session from Service intent
     */
    private val _voiceTriggerRequest = MutableStateFlow(false)
    val voiceTriggerRequest: StateFlow<Boolean> = _voiceTriggerRequest.asStateFlow()

    fun handleVoiceTrigger() {
        _voiceTriggerRequest.value = true
        scope.launch {
            delay(1000)
            _voiceTriggerRequest.value = false
        }
    }

    /**
     * Cleanup on session end.
     */
    fun cleanup() {
        scope.launch {
            brainClient.disconnectWebSocket()
        }

        _isInitialized.value = false
        println("🧹 KaironexSessionManager cleaned up")
    }
}

/**
 * Composable to provide the session manager via composition local.
 */
val LocalKaironexSession = staticCompositionLocalOf<KaironexSessionManager?> { null }

/**
 * Composable wrapper that provides session management.
 */
@Composable
fun rememberKaironexSession(
    brainClient: BrainApiClient,
    appwriteBridge: AppwriteBridge,
    httpClient: HttpClient
): KaironexSessionManager {
    return remember {
        KaironexSessionManager(
            brainClient = brainClient,
            appwriteBridge = appwriteBridge,
            httpClient = httpClient
        )
    }
}
