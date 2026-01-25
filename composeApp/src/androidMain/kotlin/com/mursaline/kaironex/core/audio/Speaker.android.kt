package com.mursaline.kaironex.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Android implementation of the Speaker interface.
 * Uses Android's native TextToSpeech engine.
 */
class AndroidSpeaker(private val context: Context) : Speaker {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeakingState = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isInitialized = true
                Log.d("KaironexTTS", "TextToSpeech initialized successfully")

                // Speak any pending text
                pendingText?.let { speak(it) }
                pendingText = null
            } else {
                Log.e("KaironexTTS", "TextToSpeech initialization failed")
            }
        }
    }

    override fun speak(text: String) {
        if (!isInitialized) {
            // Queue for when TTS is ready
            pendingText = text
            Log.d("KaironexTTS", "TTS not ready, queuing: $text")
            return
        }

        isSpeakingState = true
        Log.d("KaironexTTS", "Speaking: $text")

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kaironex_${System.currentTimeMillis()}")

        // Monitor for completion
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeakingState = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeakingState = false
                Log.d("KaironexTTS", "Speech complete")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeakingState = false
                Log.e("KaironexTTS", "Speech error")
            }
        })
    }

    override fun stop() {
        tts?.stop()
        isSpeakingState = false
        Log.d("KaironexTTS", "Speech stopped")
    }

    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true || isSpeakingState
    }

    fun shutdown() {
        tts?.shutdown()
    }
}

// Singleton holder for the speaker with context
private var speakerInstance: AndroidSpeaker? = null

fun initAndroidSpeaker(context: Context) {
    if (speakerInstance == null) {
        speakerInstance = AndroidSpeaker(context.applicationContext)
    }
}

actual fun getPlatformSpeaker(): Speaker {
    return speakerInstance ?: object : Speaker {
        override fun speak(text: String) {
            Log.w("KaironexTTS", "Speaker not initialized! Call initAndroidSpeaker() first. Text: $text")
        }

        override fun stop() {}
        override fun isSpeaking(): Boolean = false
    }
}
