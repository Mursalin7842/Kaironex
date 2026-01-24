package com.mursaline.kaironex

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================================
 * DESKTOP EYE - PRIVACY-FIRST FOCUS MONITOR
 * ============================================================
 *
 * IMPORTANT PRIVACY NOTES:
 * - This feature is OFF by default
 * - Must be explicitly enabled by user (opt-in)
 * - Only monitors when a study session is ACTIVE
 * - Only checks window TITLES, not content
 * - Does NOT log or store window history
 * - User can disable at any time
 *
 * Purpose:
 * - Detect if student switches away from study materials
 * - Trigger gentle "drift alerts" after configurable timeout
 * - NOT for surveillance or reporting
 */
object DesktopEye {
    private val MAX_TITLE_LENGTH = 1024

    // Privacy-first: OFF by default
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // Current monitoring state
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Last detected window (only when enabled & monitoring)
    private val _currentWindow = MutableStateFlow<String?>(null)
    val currentWindow: StateFlow<String?> = _currentWindow.asStateFlow()

    /**
     * Enable the Desktop Eye feature (requires explicit user consent)
     */
    fun enable() {
        _isEnabled.value = true
    }

    /**
     * Disable the Desktop Eye feature
     */
    fun disable() {
        _isEnabled.value = false
        _isMonitoring.value = false
        _currentWindow.value = null
    }

    /**
     * Start monitoring (only works if enabled)
     * Call this when a study session starts
     */
    fun startMonitoring() {
        if (_isEnabled.value) {
            _isMonitoring.value = true
        }
    }

    /**
     * Stop monitoring
     * Call this when a study session ends
     */
    fun stopMonitoring() {
        _isMonitoring.value = false
        _currentWindow.value = null
    }

    /**
     * Watch active window - only emits when enabled AND monitoring
     *
     * This creates a stream that:
     * 1. Only runs when explicitly monitoring
     * 2. Checks window title every second
     * 3. Emits null if disabled or not monitoring
     */
    fun watchActiveWindow(): Flow<String?> = flow {
        val buffer = CharArray(MAX_TITLE_LENGTH)
        val user32 = User32.INSTANCE

        while (true) {
            // Privacy check: only monitor if explicitly enabled AND active session
            if (!_isEnabled.value || !_isMonitoring.value) {
                _currentWindow.value = null
                emit(null)
                delay(1000)
                continue
            }

            try {
                // 1. Get the "Handle" (ID) of the currently active window
                val hwnd = user32.GetForegroundWindow()

                // 2. Read the title text of that window
                val length = user32.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH)
                val windowTitle = if (length > 0) {
                    String(buffer, 0, length)
                } else {
                    "Unknown / Idle"
                }

                // 3. Update state and emit
                _currentWindow.value = windowTitle
                emit(windowTitle)
            } catch (e: Exception) {
                // Fail silently - don't crash the app for monitoring
                emit(null)
            }

            // 4. Wait 1 second before checking again
            delay(1000)
        }
    }

    /**
     * Check if current window appears to be study-related
     * Returns true if the window title contains study-related keywords
     * or is the Kaironex app itself
     */
    fun isStudyRelated(windowTitle: String): Boolean {
        val studyKeywords = listOf(
            "kaironex",
            "pdf",
            "document",
            "lecture",
            "coursera",
            "khan academy",
            "edx",
            "youtube", // Could be educational
            "notion",
            "obsidian",
            "anki",
            "quizlet",
            "google docs",
            "microsoft word",
            "powerpoint",
            "slides"
        )

        val lowerTitle = windowTitle.lowercase()
        return studyKeywords.any { keyword -> lowerTitle.contains(keyword) }
    }

    /**
     * Check if current window is definitely a distraction
     */
    fun isDistraction(windowTitle: String): Boolean {
        val distractionKeywords = listOf(
            "netflix",
            "hulu",
            "disney+",
            "twitch",
            "tiktok",
            "instagram",
            "facebook",
            "twitter",
            "reddit",
            "discord" // Could be study group, so soft warning
        )

        val lowerTitle = windowTitle.lowercase()
        return distractionKeywords.any { keyword -> lowerTitle.contains(keyword) }
    }
}