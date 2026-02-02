package com.mursaline.kaironex.core.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * JVM Wake Word Detector - Desktop stub implementation.
 */
class JvmWakeWordDetector : WakeWordDetector {

    private val _state = MutableStateFlow(WakeWordState.IDLE)
    override val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val _detections = MutableSharedFlow<WakeWordDetection>(replay = 0, extraBufferCapacity = 10)
    override val detections: Flow<WakeWordDetection> = _detections.asSharedFlow()

    override var wakeWord: String = "kaironex"
    override var alternativePatterns: List<String> = listOf("kairo")

    private var isListeningActive = false

    override suspend fun startListening() {
        isListeningActive = true
        _state.value = WakeWordState.LISTENING
        println("🎙️ [JVM] Wake word detector started listening for: $wakeWord")
    }

    override fun stopListening() {
        isListeningActive = false
        _state.value = WakeWordState.IDLE
        println("🎙️ [JVM] Wake word detector stopped")
    }

    override fun isListening(): Boolean = isListeningActive

    override fun release() {
        stopListening()
        println("🎙️ [JVM] Wake word detector released")
    }

    /**
     * Simulate wake word detection (for testing).
     */
    fun simulateDetection(command: String? = null) {
        if (!isListeningActive) return
        _state.value = WakeWordState.DETECTED
        _detections.tryEmit(
            WakeWordDetection(wakeWord, command, 0.95f, System.currentTimeMillis())
        )
        _state.value = WakeWordState.LISTENING
        println("🎯 [JVM] Simulated wake word detection: $wakeWord, command: $command")
    }

    /**
     * Process text input as if it was spoken (for testing).
     */
    fun processTextInput(text: String) {
        val match = text.containsWakeWord(wakeWord, alternativePatterns)
        if (match != null) {
            _state.value = WakeWordState.DETECTED
            _detections.tryEmit(
                WakeWordDetection(match.pattern, match.command, 0.9f, System.currentTimeMillis())
            )
            _state.value = WakeWordState.LISTENING
            println("🎯 [JVM] Text input matched wake word: ${match.pattern}")
        }
    }
}

actual fun createWakeWordDetector(): WakeWordDetector = JvmWakeWordDetector()
