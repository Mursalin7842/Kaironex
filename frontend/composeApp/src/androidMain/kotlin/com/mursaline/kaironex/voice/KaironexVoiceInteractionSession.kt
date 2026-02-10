package com.mursaline.kaironex.voice

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View
import android.util.Log
import com.mursaline.kaironex.MainActivity

class KaironexVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onCreate() {
        super.onCreate()
        Log.d("KaironexAssistant", "Session Created")
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d("KaironexAssistant", "Session Shown. Launching UI.")

        // When the session is shown (Wake Word or Gesture), launch our Main Activity directly to the voice screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Add extra to tell MainActivity to open Voice Screen immediately
            putExtra("EXTRA_VOICE_TRIGGER", true)
        }
        @Suppress("WrongConstant") // Lint mistakenly flags standard activity flags here
        startVoiceActivity(intent)
        
        // Hide the session UI itself since we are launching the full activity
        hide()
    }

    override fun onHide() {
        super.onHide()
        Log.d("KaironexAssistant", "Session Hidden")
    }
}
