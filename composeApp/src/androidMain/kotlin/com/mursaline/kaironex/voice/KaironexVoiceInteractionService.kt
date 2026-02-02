package com.mursaline.kaironex.voice

import android.content.Intent
import android.service.voice.VoiceInteractionService
// import android.service.voice.AlwaysOnHotwordDetector // Unresolved/SystemApi
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class KaironexVoiceInteractionService : VoiceInteractionService() {

    // private var hotwordDetector: AlwaysOnHotwordDetector? = null

    override fun onReady() {
        super.onReady()
        Log.d("KaironexAssistant", "VoiceInteractionService Ready")
        
        // DSP Hardware detection requires system APIs or specific matching.
        // For now, we stub this out to allow compilation.
        // The fact that we are the active service allows us to run background audio if needed.
        /*
        try {
            hotwordDetector = createAlwaysOnHotwordDetector(
                "Hey Kairo",
                Locale.getDefault(),
                object : AlwaysOnHotwordDetector.Callback() {
                    override fun onAvailabilityChanged(status: Int) {}
                    override fun onDetected(eventPayload: AlwaysOnHotwordDetector.EventPayload) {}
                    override fun onError() {}
                    override fun onRecognitionPaused() {}
                    override fun onRecognitionResumed() {}
                }
            )
        } catch (e: Exception) {
            Log.e("KaironexAssistant", "Failed to init HotwordDetector: ${e.message}")
        }
        */
    }
}
