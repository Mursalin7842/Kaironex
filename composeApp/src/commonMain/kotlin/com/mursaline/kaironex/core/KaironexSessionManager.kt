package com.mursaline.kaironex.core

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import com.mursaline.kaironex.brain.BrainApiClient
import com.mursaline.kaironex.brain.ReflexAgent
import com.mursaline.kaironex.core.audio.WakeWordDetection
import com.mursaline.kaironex.core.audio.WakeWordService
import com.mursaline.kaironex.core.audio.WakeWordState
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.features.voice.VoiceCallScreen
import com.mursaline.kaironex.features.voice.VoiceCallScreenParams
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


/**
 * 🧠 KAIRONEX SESSION MANAGER
 * ============================
 * Central manager for all active session state including:
 * - Wake word detection
 * - Brain API connection
 * - Real-time data sync
 * - User session
 *
 * This is the "glue" that connects all the components together.
 */
class KaironexSessionManager(
    private val wakeWordService: WakeWordService,
    private val brainClient: BrainApiClient
) {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisorJob)

    // Session state
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(false)
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    // Callbacks
    private var onWakeWordDetected: ((WakeWordDetection) -> Unit)? = null

    // Reflex agent (created per user)
    private var reflexAgent: ReflexAgent? = null

    // Stats repository (created per user)
    private var statsRepository: AppwriteStatsRepository? = null

    /**
     * Initialize the session for a user.
     */
    fun initialize(
        userId: String,
        wakeWord: String = "kaironex",
        agentName: String = "Kairo"
    ) {
        _userId.value = userId

        // Configure wake word
        wakeWordService.setWakeWord(
            wakeWord = wakeWord.lowercase(),
            alternatives = listOf(
                agentName.lowercase(),
                "hey ${agentName.lowercase()}",
                "okay ${agentName.lowercase()}"
            )
        )

        // Create user-specific components
        reflexAgent = ReflexAgent(brainClient, userId)
        statsRepository = AppwriteStatsRepository(brainClient, userId)

        // Connect to brain WebSocket
        scope.launch {
            try {
                brainClient.connectWebSocket(userId, scope)
                println("✅ Connected to brain WebSocket")
            } catch (e: Exception) {
                println("⚠️ Failed to connect to brain: ${e.message}")
            }
        }

        // Initial data sync
        scope.launch {
            try {
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
     * Start listening for wake word.
     */
    fun startWakeWordDetection(onDetected: (WakeWordDetection) -> Unit) {
        onWakeWordDetected = onDetected

        wakeWordService.start { detection ->
            println("🎯 Wake word detected: ${detection.wakeWord}, command: ${detection.followUpCommand}")
            onWakeWordDetected?.invoke(detection)
        }

        _wakeWordEnabled.value = true
        println("🎙️ Wake word detection started")
    }

    /**
     * Stop wake word detection.
     */
    fun stopWakeWordDetection() {
        wakeWordService.stop()
        _wakeWordEnabled.value = false
        println("🎙️ Wake word detection stopped")
    }

    /**
     * Pause wake word detection (e.g., during voice call).
     */
    fun pauseWakeWord() {
        wakeWordService.pause()
    }

    /**
     * Resume wake word detection.
     */
    fun resumeWakeWord() {
        wakeWordService.resume()
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
        stopWakeWordDetection()
        wakeWordService.release()

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
    wakeWordService: WakeWordService,
    brainClient: BrainApiClient
): KaironexSessionManager {
    return remember {
        KaironexSessionManager(
            wakeWordService = wakeWordService,
            brainClient = brainClient
        )
    }
}
