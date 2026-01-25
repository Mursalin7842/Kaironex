package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mursaline.kaironex.agents.genesis.GenesisAgent
import com.mursaline.kaironex.agents.genesis.StudentProfile
import com.mursaline.kaironex.core.audio.Speaker
import com.mursaline.kaironex.core.audio.getPlatformSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Re-export ChatMessage here if used by UI, or replace usage in UI.
// Since UI might use it, we keep it but typically we'd move it to a shared Core Models.
data class ChatMessage(
    val sender: String, // "user" or "ai"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GenesisUiState(
    val isAgentSpeaking: Boolean = false,
    val isUserListening: Boolean = false, // True = Mic is ON
    val lastAgentMessage: String = "Initializing...",
    val isComplete: Boolean = false
)

class GenesisViewModel : ViewModel() {

    // --- DEPENDENCIES ---
    private val speaker: Speaker = getPlatformSpeaker()
    
    // --- AGENT BRAIN ---
    private val agent = GenesisAgent()

    // --- STATE ---
    var uiState by mutableStateOf(GenesisUiState())
        private set

    // Expose Agent's Profile directly to UI
    val profile: StudentProfile
        get() = agent.profile
    
    private var messages = mutableListOf<ChatMessage>()
    
    // Identity Memories
    private var identityContext: String = ""
    private var userPreferredName: String = ""
    private var systemName: String = ""

    // --- CORE LOGIC ---
    
    fun setIdentity(userName: String, wakeWord: String, addressUserAs: String) {
        // Update Agent with manual entries
        agent.manualUpdate(userName, wakeWord)
        
        userPreferredName = addressUserAs
        systemName = wakeWord.ifEmpty { "Kaironex" }

        // Show initial greeting immediately while waiting for API
        val initialGreeting = "Hello $addressUserAs! I'm $systemName, your AI companion. Let me learn about you to personalize your experience."
        uiState = uiState.copy(
            lastAgentMessage = initialGreeting,
            isAgentSpeaking = true
        )

        // Speak the greeting
        speaker.speak(initialGreeting)

        // Auto-Trigger the first message from agent
        viewModelScope.launch {
            // Wait a bit for initial greeting to be heard
            delay(2000)
            processAgentTurn(initialPrompt = true)
        }
    }
    
    fun processUserResponse(text: String) {
        // UI Feedback: Stop Mic
        uiState = uiState.copy(isUserListening = false)
        messages.add(ChatMessage("user", text))

        viewModelScope.launch {
            processAgentTurn(userText = text)
        }
    }

    private suspend fun processAgentTurn(userText: String? = null, initialPrompt: Boolean = false) {
        // 1. Thinking UI
        uiState = uiState.copy(isAgentSpeaking = true)

        // 2. Agent Brain Processing
        // If it's the first turn, we might send null text, or a greeting prompt.
        // For GenesisAgent, the prompt logic handles null userText as "Initialize".
        
        val response = agent.processTurn(
            userText = userText,
            agentName = systemName,
            userName = userPreferredName
        )
        
        messages.add(ChatMessage("ai", response.displayText))

        // 3. Audio & UI Update
        agentSpeak(response.displayText)
        
        // 4. Check for completion or updates
        if (response.isComplete) {
            uiState = uiState.copy(isComplete = true)
            // Trigger Handoff here if needed
        }
    }

    private fun agentSpeak(text: String) {
        // Update Text for Subtitles
        uiState = uiState.copy(
            lastAgentMessage = text,
            isAgentSpeaking = true
        )
        
        // Trigger Audio
        speaker.speak(text)
        
        // Simulation: Reset listening state after speech
        viewModelScope.launch {
            // Simple heuristic for speech duration: 60ms per char + buffer
            // In a real app, the Speaker callback would trigger this.
            val duration = (text.length * 60L).coerceAtLeast(1500)
            delay(duration)
            
            uiState = uiState.copy(isAgentSpeaking = false)
            
            // Auto-open Mic for user reply unless complete
            if (!uiState.isComplete) {
                toggleListening(forceOn = true)
            }
        }
    }

    fun toggleListening(forceOn: Boolean? = null) {
        if (uiState.isAgentSpeaking) speaker.stop() // Interrupt Agent
        
        val newListeningState = forceOn ?: !uiState.isUserListening
        uiState = uiState.copy(isUserListening = newListeningState)
        
        if (newListeningState) {
            // TODO: Start Speech-To-Text Engine here
        }
    }
}
