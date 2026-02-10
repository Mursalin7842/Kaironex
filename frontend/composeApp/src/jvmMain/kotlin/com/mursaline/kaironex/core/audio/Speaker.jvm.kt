package com.mursaline.kaironex.core.audio

/**
 * JVM/Desktop implementation of the Speaker interface.
 * Uses console output to visualize speech for demo purposes.
 * In production, integrate with FreeTTS or system TTS APIs.
 */
class JvmSpeaker : Speaker {
    private var isSpeakingState = false

    init {
        println("[Kaironex TTS] Voice synthesizer initialized (console mode)")
    }

    override fun speak(text: String) {
        isSpeakingState = true

        // Log to console (visible in IDE) with visual formatting
        println()
        println("╔════════════════════════════════════════════════════════════════╗")
        println("║ 🗣️ KAIRONEX SPEAKING:                                          ║")
        println("╠════════════════════════════════════════════════════════════════╣")
        text.chunked(60).forEach { line ->
            println("║ ${line.padEnd(62)} ║")
        }
        println("╚════════════════════════════════════════════════════════════════╝")
        println()

        // Simulate speech duration in background thread
        Thread {
            // Approximate reading speed: ~40ms per char for demo
            val duration = (text.length * 40L).coerceIn(1500L, 8000L)
            Thread.sleep(duration)
            isSpeakingState = false
            println("[Kaironex TTS] ✓ Speech complete.")
        }.start()
    }

    override fun stop() {
        isSpeakingState = false
        println("[Kaironex TTS] Speech interrupted.")
    }

    override fun isSpeaking(): Boolean = isSpeakingState
}

actual fun getPlatformSpeaker(): Speaker = JvmSpeaker()
