package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mursaline.kaironex.agents.genesis.GenesisAgent
import com.mursaline.kaironex.agents.genesis.StudentProfile
// Speaker imports removed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mursaline.kaironex.brain.GeminiReasoningEngine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.mursaline.kaironex.agents.genesis.GenesisPrompts
import com.mursaline.kaironex.PlatformSecrets

import com.mursaline.kaironex.core.gemini.ChatMessage

data class GenesisUiState(
    val isAgentSpeaking: Boolean = false,
    val isUserListening: Boolean = false, // True = Mic is ON
    val lastAgentMessage: String = "Initializing...",
    val isComplete: Boolean = false
)

class GenesisViewModel : ViewModel(), KoinComponent {

    // --- DEPENDENCIES ---
    private val reasoningEngine: GeminiReasoningEngine by inject()
    
    // --- AGENT BRAIN ---
    private val agent = GenesisAgent()

    // --- STATE ---
    var uiState by mutableStateOf(GenesisUiState())
        private set
        
    // Expose Connection State for UI Orb
    val connectionState = reasoningEngine.connectionState

    // --- CORE LOGIC ---
    
    fun startInterview(userName: String, wakeWord: String) {
        // Prevent duplicate starts if already connected or connecting
        if (reasoningEngine.connectionState.value is com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Connected) return

        // 1. Configure Agent
        agent.manualUpdate(userName, wakeWord)
        
        // 2. Build System Prompt (Dynamic Voice)
        val initialPrompt = GenesisPrompts.build(
            agentName = wakeWord.ifEmpty { "Kaironex" },
            user = userName,
            stage = agent.stage,
            missing = agent.profile.getMissingFields(agent.stage),
            rationale = "Let's get you set up to optimize your student life."
        )
        
        // 3. Connect Voice Engine
        viewModelScope.launch {
            try {
                // Assuming we use the hardcoded key for now or inject it
                val apiKey = PlatformSecrets.apiKey
                reasoningEngine.connect(apiKey, systemInstruction = initialPrompt)
            } catch (e: Exception) {
                println("⚠️ Failed to start interview: ${e.message}")
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
