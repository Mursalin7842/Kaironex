package com.mursaline.kaironex.core.audio

/**
 * CORE COMPONENT: VOICE OUTPUT INTERFACE
 * This abstracts the platform-specific Text-To-Speech (TTS) engine.
 */
interface Speaker {
    fun speak(text: String)
    fun stop()
    fun isSpeaking(): Boolean
}

// Platform implementation hook
expect fun getPlatformSpeaker(): Speaker
