package com.mursaline.kaironex.core.audio

class AndroidSpeaker : Speaker {
    override fun speak(text: String) {
        println("Android TTS: $text")
    }

    override fun stop() {
        println("Android TTS Stopped")
    }

    override fun isSpeaking(): Boolean {
        return false
    }
}

actual fun getPlatformSpeaker(): Speaker = AndroidSpeaker()
