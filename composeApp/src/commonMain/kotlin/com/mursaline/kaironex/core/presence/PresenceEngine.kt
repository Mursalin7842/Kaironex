package com.mursaline.kaironex.core.presence

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================
 * PRESENCE ENGINE
 * ============================================================
 *
 * Replaces DesktopEye with a cross-platform presence detection system.
 *
 * Philosophy:
 * - Kaironex isn't a cop; it's a concerned friend
 * - When user drifts away, Kairo "calls" them like a friend checking in
 * - User can negotiate breaks rather than being punished
 *
 * States:
 * - FOCUSED: User is in the app, actively studying
 * - DRIFTING: User left the app, warning timer ticking
 * - INTERVENTION: Time's up! Gemini is "calling" the user
 * - NEGOTIATING: User answered, explaining why they left
 * - ON_BREAK: User negotiated a break successfully
 */

enum class PresenceState {
    FOCUSED,        // User is in the app
    DRIFTING,       // User left app, warning timer ticking (< threshold)
    INTERVENTION,   // Time up! Gemini is "calling" the user
    NEGOTIATING,    // User answered, explaining why they left
    ON_BREAK        // User is on an approved break
}

data class PresenceConfig(
    val driftToleranceMs: Long = 15_000L,  // 15 seconds grace period
    val maxDriftBeforeCallMs: Long = 30_000L, // 30 seconds before intervention
    val defaultBreakDurationMs: Long = 300_000L // 5 minutes default break
)

object PresenceEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Config - can be customized per user
    var config = PresenceConfig()

    // State
    private val _currentState = MutableStateFlow(PresenceState.FOCUSED)
    val currentState = _currentState.asStateFlow()

    private val _timeAwayMs = MutableStateFlow(0L)
    val timeAwayMs = _timeAwayMs.asStateFlow()

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive = _sessionActive.asStateFlow()

    // Tracking
    private var monitorJob: Job? = null
    private var lastFocusTime = 0L
    private var isWindowCurrentlyFocused = true

    /**
     * Start a study session - begins presence monitoring
     */
    fun startSession() {
        _sessionActive.value = true
        lastFocusTime = System.currentTimeMillis()
        _currentState.value = PresenceState.FOCUSED
        _timeAwayMs.value = 0

        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                checkPresence()
                delay(1000) // Check every second
            }
        }
    }

    /**
     * Stop the study session - stops presence monitoring
     */
    fun stopSession() {
        monitorJob?.cancel()
        _sessionActive.value = false
        _currentState.value = PresenceState.FOCUSED
        _timeAwayMs.value = 0
    }

    /**
     * Called when app gains focus (from platform-specific code)
     */
    fun onWindowFocusGained() {
        isWindowCurrentlyFocused = true
        val now = System.currentTimeMillis()
        lastFocusTime = now
        _timeAwayMs.value = 0

        // Only reset to FOCUSED if not in negotiation
        if (_currentState.value != PresenceState.NEGOTIATING &&
            _currentState.value != PresenceState.ON_BREAK) {
            _currentState.value = PresenceState.FOCUSED
        }
    }

    /**
     * Called when app loses focus (from platform-specific code)
     */
    fun onWindowFocusLost() {
        isWindowCurrentlyFocused = false
        if (_currentState.value == PresenceState.FOCUSED) {
            lastFocusTime = System.currentTimeMillis()
        }
    }

    private fun checkPresence() {
        if (!_sessionActive.value) return

        val now = System.currentTimeMillis()

        if (isWindowCurrentlyFocused) {
            // User is here
            lastFocusTime = now
            _timeAwayMs.value = 0
            if (_currentState.value != PresenceState.NEGOTIATING &&
                _currentState.value != PresenceState.ON_BREAK) {
                _currentState.value = PresenceState.FOCUSED
            }
        } else {
            // User is gone
            val driftTime = now - lastFocusTime
            _timeAwayMs.value = driftTime

            when {
                _currentState.value == PresenceState.NEGOTIATING -> {
                    // Don't change state during negotiation
                }
                _currentState.value == PresenceState.ON_BREAK -> {
                    // Don't change state during break
                }
                _currentState.value == PresenceState.INTERVENTION -> {
                    // Already intervening, wait for user response
                }
                driftTime > config.maxDriftBeforeCallMs -> {
                    triggerIntervention()
                }
                driftTime > config.driftToleranceMs -> {
                    _currentState.value = PresenceState.DRIFTING
                }
            }
        }
    }

    private fun triggerIntervention() {
        _currentState.value = PresenceState.INTERVENTION
        // TODO: In real app, play notification sound or ringtone
    }

    /**
     * User clicks "Pick up" on the Gemini Call
     */
    fun startNegotiation() {
        _currentState.value = PresenceState.NEGOTIATING
    }

    /**
     * User successfully negotiated a break
     */
    fun grantBreak(durationMs: Long = config.defaultBreakDurationMs) {
        _currentState.value = PresenceState.ON_BREAK

        scope.launch {
            delay(durationMs)
            // After break, check if user is back
            if (isWindowCurrentlyFocused) {
                _currentState.value = PresenceState.FOCUSED
            } else {
                // Break is over but user still away
                lastFocusTime = System.currentTimeMillis()
                _currentState.value = PresenceState.DRIFTING
            }
        }
    }

    /**
     * User says "I'm back" - resume session
     */
    fun resumeFromNegotiation() {
        _currentState.value = PresenceState.FOCUSED
        lastFocusTime = System.currentTimeMillis()
        _timeAwayMs.value = 0
    }

    /**
     * Get formatted time away string
     */
    fun getTimeAwayFormatted(): String {
        val seconds = (_timeAwayMs.value / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${seconds}s"
        }
    }
}
