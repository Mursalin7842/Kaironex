package com.mursaline.kaironex.core.audio

/**
 * JVM/Desktop implementation of the Speaker interface.
 * Uses a stub implementation for hackathon demo purposes.
 * In production, this would use FreeTTS or a native TTS library.
 */
class JvmSpeaker : Speaker {
    private var isSpeakingState = false

    override fun speak(text: String) {
        isSpeakingState = true
        // Stub: In production, integrate with FreeTTS or system TTS
        println("[Kaironex TTS]: $text")

        // Simulate speech duration
        Thread {
            Thread.sleep((text.length * 60L).coerceAtMost(5000L))
            isSpeakingState = false
        }.start()
    }

    override fun stop() {
        isSpeakingState = false
    }

    override fun isSpeaking(): Boolean = isSpeakingState
}

actual fun getPlatformSpeaker(): Speaker = JvmSpeaker()
