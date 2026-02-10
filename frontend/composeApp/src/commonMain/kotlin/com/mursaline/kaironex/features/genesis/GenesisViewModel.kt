package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mursaline.kaironex.agents.genesis.StudentProfile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        
    // Expose Connection State for UI Orb - Mocked/Disabled
    // val connectionState = reasoningEngine.connectionState
    // val audioRms = reasoningEngine.audioRms
    
    // --- CORE LOGIC ---
    
    fun startInterview(userName: String) {
        // if (reasoningEngine.connectionState.value is GeminiReasoningEngine.ConnectionState.Connected) return

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
                // HYBRID AGENT: We do NOT connect the local Kotlin engine here.
                // The InterviewWebView (React) handles the connection and mic.
                // reasoningEngine.connect(apiKey, systemInstruction = initialPrompt, toolsConfig = "{}")
                println("⚠️ Native Engine disabled in favor of Hybrid WebView Agent.")
            } catch (e: Exception) {
                println("⚠️ Failed to connect to agent: ${e.message}")
            }
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            // reasoningEngine.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    fun updateFromAgent(field: String, value: String?) {
        if (value == null) return
        val currentProfile = profile
        val updatedProfile = when (field) {
            // Academic
            "university" -> currentProfile.copy(university = value)
            "degreeMajor" -> currentProfile.copy(major = value)
            "totalSemesters" -> currentProfile.copy(totalSemesters = value)
            "currentSemester" -> currentProfile.copy(semester = value)
            "currentCGPA" -> currentProfile.copy(currentCgpa = value)
            "desiredCGPA" -> currentProfile.copy(targetCgpa = value)
            "desiredCGPAReason" -> currentProfile.copy(desiredCgpaReason = value)

            // International
            "isInternational" -> currentProfile.copy(isInternationalStudent = value.toBoolean())
            "hostCountry" -> currentProfile.copy(currentCountry = value)
            "homeCountry" -> currentProfile.copy(homeCountry = value)
            "visaStatus" -> currentProfile.copy(visaStatus = value)

            // Work
            "hasJob" -> currentProfile.copy(hasJob = value.toBoolean())
            "jobPosition" -> currentProfile.copy(jobDescription = value)
            "jobSchedule" -> currentProfile.copy(jobSchedule = value)

            // Rhythm & Logistics
            "commuteTime" -> currentProfile.copy(commuteTime = value)
            "nonNegotiables" -> currentProfile.copy(nonNegotiables = value)
            
            // Psych / Strategy
            "learningStyle" -> currentProfile.copy(learningStyle = value)
            "preferredResources" -> currentProfile.copy(preferredResources = value)
            "productivityKiller" -> currentProfile.copy(productivityKiller = value)
            "focusCapacity" -> currentProfile.copy(dailyFocusCapacity = value)
            "chronotype" -> currentProfile.copy(energyPreference = value)
            
            else -> {
                println("⚠️ Unknown field from Agent: $field = $value")
                currentProfile
            }
        }
        
        if (updatedProfile != currentProfile) {
            profile = updatedProfile
            // Auto-persist on every update to ensure data is there when navigating
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                profileStorage.saveProfile(updatedProfile)
            }
        }
    }
}
