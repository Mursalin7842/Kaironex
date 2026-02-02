package com.mursaline.kaironex.core.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 🎙️ WAKE WORD DETECTOR
 * ======================
 * Listens for "Hey {AgentName}" to automatically wake the voice agent.
 *
 * Features:
 * - Continuous background listening
 * - Low power consumption mode
 * - Customizable wake word (set during onboarding)
 * - Command extraction after wake word
 *
 * Platform implementations:
 * - Android: Uses SpeechRecognizer or Vosk
 * - JVM: Uses Vosk or system microphone + STT
 */

/**
 * Wake word detection states.
 */
enum class WakeWordState {
    IDLE,           // Not listening
    LISTENING,      // Actively listening for wake word
    DETECTED,       // Wake word heard, processing command
    ERROR           // Microphone/permission error
}

/**
 * Result when wake word is detected.
 */
data class WakeWordDetection(
    val wakeWord: String,           // The wake word that was detected
    val followUpCommand: String?,   // Optional command after wake word
    val confidence: Float,          // Detection confidence 0-1
    val timestamp: Long             // When detected
)

/**
 * Common interface for wake word detection.
 */
interface WakeWordDetector {

    /**
     * Current detection state.
     */
    val state: StateFlow<WakeWordState>

    /**
     * Flow of wake word detections.
     */
    val detections: Flow<WakeWordDetection>

    /**
     * The configured wake word.
     */
    var wakeWord: String

    /**
     * Alternative wake word patterns (e.g., "Hey Kairo", "Kaironex").
     */
    var alternativePatterns: List<String>

    /**
     * Start listening for wake word.
     * Low-power mode for continuous background detection.
     */
    suspend fun startListening()

    /**
     * Stop listening.
     */
    fun stopListening()

    /**
     * Check if the detector is currently listening.
     */
    fun isListening(): Boolean

    /**
     * Release all resources.
     */
    fun release()
}

/**
 * Platform-specific implementation.
 */
expect fun createWakeWordDetector(): WakeWordDetector

/**
 * Helper: Check if text contains wake word.
 */
fun String.containsWakeWord(wakeWord: String, alternatives: List<String> = emptyList()): WakeWordMatch? {
    val normalized = this.lowercase().replace(Regex("[^a-z ]"), " ").trim() // Remove punctuation
    
    // Add misheard validations for "Kairo" if that is the wake word
    val misheard = if (wakeWord.equals("kairo", ignoreCase = true) || wakeWord.equals("kaironex", ignoreCase = true)) {
        listOf("cairo", "tyro", "caro", "hero", "gyro", "kai", "karo", "carol", "tyrone")
    } else emptyList()

    val patterns = (listOf(wakeWord.lowercase()) + alternatives.map { it.lowercase() } + misheard).distinct()
    
    val prefixes = listOf("hey", "okay", "ok", "hi", "hello", "yo")

    for (pattern in patterns) {
        // 1. Check strict phrases "hey pattern" inside the text
         for (prefix in prefixes) {
             val phrase = "$prefix $pattern"
             if (normalized.contains(phrase)) {
                 val command = normalized.substringAfter(phrase).trim()
                 return WakeWordMatch(pattern, prefix, command.ifEmpty { null })
             }
         }

        // 2. Check if just the pattern exists securely (isolated word)
        // e.g. "Kairo, what time is it" (allows start of string or spaces around it)
        val regex = Regex("(^|\\s)$pattern(\\s|$)")
        val match = regex.find(normalized)
        if (match != null) {
            val command = normalized.substring(match.range.last + 1).trim()
            return WakeWordMatch(pattern, null, command.ifEmpty { null })
        }
    }

    return null
}

data class WakeWordMatch(
    val pattern: String,
    val prefix: String?,
    val command: String?
)
