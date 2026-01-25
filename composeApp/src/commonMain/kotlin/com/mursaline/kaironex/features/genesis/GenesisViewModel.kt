package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import com.mursaline.kaironex.features.genesis.StudentProfile // Assuming this might be moved or duplicated, checking imports
import com.mursaline.kaironex.core.audio.Speaker
import com.mursaline.kaironex.core.audio.getPlatformSpeaker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Define strict data types here if not available commonly
data class StudentProfile(
    var name: String = "",
    var university: String = "",
    var degreeType: String = "",
    var major: String = "",
    var semester: String = "",
    var hasPartTimeJob: Boolean = false,
    var jobTitle: String = "",
    var workSchedule: String = "",
    var weeklyWorkHours: Int = 0,
    var studyGoals: List<String> = emptyList(),
    var challenges: List<String> = emptyList(),
    var preferredStudyTime: String = "",
    var extracurriculars: List<String> = emptyList(),
    var sleepSchedule: String = "",
    var internationalStudent: Boolean = false,
    var visaType: String = ""
)

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GenesisStage {
    INTRO,
    INTERVIEW,
    COMPLETE
}

class GenesisViewModel : ViewModel() {

    // --- DEPENDENCIES ---
    // In production, inject this via Koin. For Hackathon speed, direct call is acceptable.
    private val speaker: Speaker = getPlatformSpeaker() 

    // --- STATE ---
    var uiState by mutableStateOf(GenesisUiState())
        private set

    // We keep the logic state separate from UI state
    private var messages = mutableListOf<ChatMessage>()
    var profile by mutableStateOf(StudentProfile())
    private var currentStage = GenesisStage.INTRO
    
    // Identity Memories
    private var identityContext: String = ""
    private var userPreferredName: String = ""

    data class GenesisUiState(
        val isAgentSpeaking: Boolean = false,
        val isUserListening: Boolean = false, // True = Mic is ON
        val lastAgentMessage: String = "Initializing..."
    )

    // --- CORE LOGIC ---
    
    fun setIdentity(userName: String, wakeWord: String, addressUserAs: String) {
        // Save to Profile
        profile = profile.copy(name = userName)
        userPreferredName = addressUserAs
        
        // Create the "Persona" Context
        identityContext = """
            USER REAL NAME: $userName
            SYSTEM NAME (YOU): $wakeWord
            ADDRESS USER AS: $addressUserAs
        """.trimIndent()
        
        currentStage = GenesisStage.INTERVIEW
        
        // Auto-Trigger the first message
        val initialGreeting = "Hello $addressUserAs. I am $wakeWord. Let's begin."
        agentSpeak(initialGreeting)
    }
    
    fun processUserResponse(text: String) {
        // 1. UI Feedback: Stop Mic, Show user input accepted
        uiState = uiState.copy(isUserListening = false)
        messages.add(ChatMessage("user", text))

        viewModelScope.launch {
            // 2. The Thinking Phase (Orb Pulses Purple)
            uiState = uiState.copy(isAgentSpeaking = true)

            // 3. Gemini Call (Stubbed for now)
            val responseText = "Mock response for: $text" 

            // 4. Agent Speaks
            agentSpeak(responseText)
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
        
        // Simulation: Reset state after speech
        viewModelScope.launch {
            delay(text.length * 60L + 1000) // Estimate speech duration
            uiState = uiState.copy(isAgentSpeaking = false)
            
            // OPTIONAL: Auto-open Mic for user reply?
            // toggleListening()
        }
    }

    fun toggleListening() {
        if (uiState.isAgentSpeaking) speaker.stop() // Interrupt Agent
        
        val newListeningState = !uiState.isUserListening
        uiState = uiState.copy(isUserListening = newListeningState)
        
        if (newListeningState) {
            // TODO: Start Speech-To-Text Engine here
        }
    }
}
