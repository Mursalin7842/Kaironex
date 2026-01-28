package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mursaline.kaironex.agents.genesis.StudentProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mursaline.kaironex.brain.GeminiReasoningEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.mursaline.kaironex.PlatformSecrets
import com.mursaline.kaironex.core.storage.ProfileStorage

data class GenesisUiState(
    val isAgentSpeaking: Boolean = false,
    val isUserListening: Boolean = false, // True = Mic is ON
    val lastAgentMessage: String = "Connecting to Backend Agent...",
    val isComplete: Boolean = false
)

class GenesisViewModel : ViewModel(), KoinComponent {

    // --- DEPENDENCIES ---
    private val reasoningEngine: GeminiReasoningEngine by inject()
    private val profileStorage: ProfileStorage by inject()
    
    // --- STATE ---
    var uiState by mutableStateOf(GenesisUiState())
        private set

    // Profile state for the UI (minimal)
    var profile by mutableStateOf(StudentProfile())
        private set

    init {
        loadPersistedProfile()
    }
    
    private fun loadPersistedProfile() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val savedProfile = profileStorage.loadProfile()
            if (savedProfile != null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    profile = savedProfile
                }
            }
        }
    }
        
    // Expose Connection State for UI Orb
    val connectionState = reasoningEngine.connectionState
    val audioRms = reasoningEngine.audioRms
    
    // --- CORE LOGIC ---
    
    fun startInterview(userName: String, wakeWord: String) {
        if (reasoningEngine.connectionState.value is GeminiReasoningEngine.ConnectionState.Connected) return

        // 1. Basic Initial Instruction (Transitioning to Backend Agent)
        val initialPrompt = """
            You are Kaironex System. 
            The user $userName is initiating a session.
            Identify yourself and wait for a response.
            Maintain a helpful, student-focused personality.
            Keep responses brief and suitable for voice.
        """.trimIndent()

        // 2. Connect Voice Engine
        viewModelScope.launch {
            try {
                val apiKey = PlatformSecrets.apiKey
                // No tools configured locally for now, waiting for backend integration
                reasoningEngine.connect(apiKey, systemInstruction = initialPrompt, toolsConfig = "{}")
            } catch (e: Exception) {
                println("⚠️ Failed to connect to agent: ${e.message}")
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            reasoningEngine.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
