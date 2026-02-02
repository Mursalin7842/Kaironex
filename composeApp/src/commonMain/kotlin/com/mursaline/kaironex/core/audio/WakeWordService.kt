package com.mursaline.kaironex.core.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 🎙️ WAKE WORD SERVICE
 * =====================
 * Background service for continuous wake word detection.
 *
 * Features:
 * - Listens for "Hey {AgentName}" pattern
 * - Triggers voice call screen on detection
 * - Manages detector lifecycle
 * - Handles permission states
 *
 * Usage:
 *   val service = WakeWordService(detector, scope)
 *   service.setWakeWord("Kairo")
 *   service.start { detection ->
 *       // Navigate to voice call screen
 *   }
 */
class WakeWordService(
    private val detector: WakeWordDetector
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var listenerJob: Job? = null
    private var isRunning = false

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastDetection = MutableStateFlow<WakeWordDetection?>(null)
    val lastDetection: StateFlow<WakeWordDetection?> = _lastDetection.asStateFlow()

    /**
     * Configure the wake word to listen for.
     */
    fun setWakeWord(wakeWord: String, alternatives: List<String> = emptyList()) {
        detector.wakeWord = wakeWord
        detector.alternativePatterns = alternatives.ifEmpty {
            listOf(wakeWord.lowercase(), "hey $wakeWord", "okay $wakeWord")
        }
    }

    /**
     * Start listening for wake word.
     *
     * @param onDetected Callback when wake word is detected.
     *                   Receives the detection with optional follow-up command.
     */
    fun start(onDetected: (WakeWordDetection) -> Unit) {
        if (isRunning) {
            println("⚠️ Wake word service already running")
            return
        }

        isRunning = true
        _isActive.value = true

        listenerJob = scope.launch {
            // Start the detector
            detector.startListening()

            // Collect detections
            detector.detections.collect { detection ->
                println("🎯 Wake word detected: ${detection.wakeWord}")
                _lastDetection.value = detection

                // Invoke callback
                withContext(Dispatchers.Main) {
                    onDetected(detection)
                }
            }
        }

        println("🎙️ Wake word service started")
    }

    /**
     * Stop listening for wake word.
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false
        _isActive.value = false

        listenerJob?.cancel()
        listenerJob = null

        detector.stopListening()

        println("🎙️ Wake word service stopped")
    }

    /**
     * Pause temporarily (e.g., during active call).
     */
    fun pause() {
        detector.stopListening()
        _isActive.value = false
        println("🎙️ Wake word service paused")
    }

    /**
     * Resume after pause.
     */
    fun resume() {
        if (!isRunning) return

        scope.launch {
            detector.startListening()
            _isActive.value = true
        }
        println("🎙️ Wake word service resumed")
    }

    /**
     * Release all resources.
     */
    fun release() {
        stop()
        detector.release()
        println("🎙️ Wake word service released")
    }

    /**
     * Get current detector state.
     */
    fun getState(): WakeWordState = detector.state.value

    /**
     * Check if detector is actively listening.
     */
    fun isListening(): Boolean = detector.isListening()
}
