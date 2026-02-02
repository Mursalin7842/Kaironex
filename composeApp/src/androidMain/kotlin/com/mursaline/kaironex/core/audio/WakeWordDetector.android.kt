package com.mursaline.kaironex.core.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 🎙️ ANDROID WAKE WORD DETECTOR
 * ==============================
 * Uses Android's SpeechRecognizer for continuous "Hey {AgentName}" detection.
 *
 * Features:
 * - Continuous listening in background
 * - Auto-restart on silence/error
 * - Low latency detection
 * - Configurable wake word patterns
 */
class AndroidWakeWordDetector(
    private val context: Context
) : WakeWordDetector {

    private val _state = MutableStateFlow(WakeWordState.IDLE)
    override val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val _detections = MutableSharedFlow<WakeWordDetection>(replay = 0, extraBufferCapacity = 10)
    override val detections: SharedFlow<WakeWordDetection> = _detections.asSharedFlow()

    override var wakeWord: String = "kaironex"
    override var alternativePatterns: List<String> = listOf("kairo", "cairo", "caro")

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningActive = false
    private var shouldRestart = false

    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        // Continuous listening settings
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override suspend fun startListening() {
        if (isListeningActive) {
            println("⚠️ Wake word detector already listening")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            println("❌ Speech recognition not available on this device")
            _state.value = WakeWordState.ERROR
            return
        }

        shouldRestart = true
        mainHandler.post {
            startRecognizer()
        }
    }

    override fun stopListening() {
        shouldRestart = false
        isListeningActive = false

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                println("⚠️ Error stopping speech recognizer: ${e.message}")
            }
        }

        _state.value = WakeWordState.IDLE
        println("🎙️ Wake word detector stopped")
    }

    override fun isListening(): Boolean = isListeningActive

    override fun release() {
        shouldRestart = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                println("⚠️ Error destroying speech recognizer: ${e.message}")
            }
        }
        println("🎙️ Wake word detector released")
    }

    private fun startRecognizer() {
        try {
            // Create speech recognizer on main thread
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
            }

            speechRecognizer?.startListening(recognizerIntent)
            isListeningActive = true
            _state.value = WakeWordState.LISTENING
            println("🎙️ Wake word detector started listening for: $wakeWord")
        } catch (e: Exception) {
            println("❌ Error starting speech recognizer: ${e.message}")
            _state.value = WakeWordState.ERROR
            isListeningActive = false

            // Try to restart after delay
            if (shouldRestart) {
                restartAfterDelay()
            }
        }
    }

    private fun restartAfterDelay() {
        mainHandler.postDelayed({
            if (shouldRestart) {
                println("🔄 Restarting wake word detection...")
                startRecognizer()
            }
        }, 1000L)
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                println("🎙️ Ready for speech input")
                _state.value = WakeWordState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                println("🎙️ Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed - can be used for visual feedback
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                println("🎙️ Speech ended")
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }

                println("🎙️ Recognition error: $errorMessage")
                isListeningActive = false

                // Only show error state for critical errors
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    _state.value = WakeWordState.ERROR
                    shouldRestart = false
                } else if (shouldRestart) {
                    // For non-critical errors (timeout, no match), restart listening
                    restartAfterDelay()
                }
            }

            override fun onResults(results: Bundle?) {
                processResults(results, isPartial = false)

                // Restart listening for continuous detection
                if (shouldRestart) {
                    isListeningActive = false
                    restartAfterDelay()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                processResults(partialResults, isPartial = true)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun processResults(results: Bundle?, isPartial: Boolean) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

        if (matches.isNullOrEmpty()) return

        val prefix = if (isPartial) "📝 Partial" else "✅ Final"
        println("$prefix results: ${matches.joinToString(", ")}")

        // Check each result for wake word
        for ((index, match) in matches.withIndex()) {
            val wakeWordMatch = match.containsWakeWord(wakeWord, alternativePatterns)

            if (wakeWordMatch != null) {
                val confidence = confidences?.getOrNull(index) ?: 0.8f

                println("🎯 WAKE WORD DETECTED: '${wakeWordMatch.pattern}' with command: '${wakeWordMatch.command}'")

                _state.value = WakeWordState.DETECTED

                val detection = WakeWordDetection(
                    wakeWord = wakeWordMatch.pattern,
                    followUpCommand = wakeWordMatch.command,
                    confidence = confidence,
                    timestamp = System.currentTimeMillis()
                )

                _detections.tryEmit(detection)

                // If we have a command (not just wake word), process it
                if (!isPartial || wakeWordMatch.command != null) {
                    // Stop listening temporarily to process the command
                    // Will restart after processing
                }

                return // Found wake word, stop checking
            }
        }
    }
}

/**
 * Platform-specific implementation factory.
 */
actual fun createWakeWordDetector(): WakeWordDetector {
    // This requires context - will be injected via Koin
    throw UnsupportedOperationException(
        "Use Koin injection with AndroidWakeWordDetector(context) instead"
    )
}
