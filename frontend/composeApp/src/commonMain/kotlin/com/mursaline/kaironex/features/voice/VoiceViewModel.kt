package com.mursaline.kaironex.features.voice

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mursaline.kaironex.brain.AudioRecorder
import com.mursaline.kaironex.brain.GeminiLiveAgent
import com.mursaline.kaironex.core.CurrentUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val agent: GeminiLiveAgent,
    private val recorder: AudioRecorder
) : ScreenModel {
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    fun toggleSession(userId: String = CurrentUser.userId.ifBlank { "demo_user" }) {
        if (_isListening.value) {
            // Stop
            recorder.stopRecording()
            agent.disconnect()
            _isListening.value = false
        } else {
            // Start
            _isListening.value = true
            screenModelScope.launch {
                // 1. Connect
                launch { agent.startSession(userId) }
                
                // 2. Start Mic and Pipe Data
                recorder.startRecording { audioChunk ->
                    launch { agent.sendUserAudio(audioChunk) }
                }
            }
        }
    }
    
    override fun onDispose() {
        super.onDispose()
        recorder.stopRecording()
        agent.disconnect()
    }
}
