package com.mursaline.kaironex.voice

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class KaironexVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle?): VoiceInteractionSession {
        return KaironexVoiceInteractionSession(this)
    }
}
