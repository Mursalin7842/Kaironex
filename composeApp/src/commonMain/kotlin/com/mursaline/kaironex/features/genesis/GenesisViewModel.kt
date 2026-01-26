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
import kotlinx.coroutines.withContext
import com.mursaline.kaironex.brain.GeminiReasoningEngine
import com.mursaline.kaironex.brain.GeminiReasoningEngine.FunctionCallPart
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
    private val profileStorage: com.mursaline.kaironex.core.storage.ProfileStorage by inject()
    
    // --- AGENT BRAIN ---
    // Now injected as Singleton to share state with Judge View
    private val agent: GenesisAgent by inject()

    // --- STATE ---
    var uiState by mutableStateOf(GenesisUiState())
        private set

    init {
        loadPersistedProfile()
    }
    
    private fun loadPersistedProfile() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val savedProfile = profileStorage.loadProfile()
            if (savedProfile != null) {
                println("💾 Recall: Found existing profile for ${savedProfile.name}")
                // Update the singleton agent
                // We manually hydrate it. Since agent.updateProfile merges, we might need a "setProfile" or just rely on manual update logic.
                // But updateProfile is logic-heavy. Let's assume we want to restore *exact* state.
                // We'll add a 'restore' method to Agent or just rely on 'manualUpdate' logic if compatible?
                // Actually agent.profile is read-only in public API, but we have internal methods.
                // Let's add a 'restoreProfile' to GenesisAgent or just use reflection/backdoor? 
                // Wait, GenesisAgent.profile has private set.
                // clone/copy is possible if we modify GenesisAgent to allow setting it.
                // I will modify GenesisAgent to allow restoration.
                
                // Temporary Hack: use manualUpdate if fields match, or just fix GenesisAgent.
                // I will add `restoreProfile` to GenesisAgent.
                agent.restoreProfile(savedProfile)
                
                // Logic: If profile is mostly complete, mark as complete?
                if (savedProfile.major != null && savedProfile.financialStakes != null) {
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         uiState = uiState.copy(isComplete = true)
                     }
                }
            }
        }
    }
        
    // Expose Connection State for UI Orb
    val connectionState = reasoningEngine.connectionState
    val audioRms = reasoningEngine.audioRms
    
    // Expose Profile for Handoff
    val profile: StudentProfile
        get() = agent.profile

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

        // 3. Define Tools for Data Extraction
        // 3. Define Tools for Data Extraction
        val toolsConfig = """
            "tools": [
                {
                    "function_declarations": [
                        {
                            "name": "update_profile",
                            "description": "Updates the student profile with extracted information.",
                            "parameters": {
                                "type": "OBJECT",
                                "properties": {
                                    "university": { "type": "STRING" },
                                    "major": { "type": "STRING" },
                                    "semester": { "type": "STRING" },
                                    "mainPriority": { "type": "STRING", "description": "JOB_READY or CGPA" },
                                    "secondaryPriority": { "type": "STRING" },
                                    "energyPreference": { "type": "STRING", "description": "MORNING or NIGHT" },
                                    "dailyFocusCapacity": { "type": "NUMBER" },
                                    "sleepTime": { "type": "STRING" },
                                    "wakeTime": { "type": "STRING" },
                                    "needsJob": { "type": "BOOLEAN" },
                                    "workHoursPerWeek": { "type": "NUMBER" },
                                    "commuteDuration": { "type": "STRING" },
                                    "protectedTime": { "type": "STRING", "description": "Non-negotiable blocks like Prayer or Gym" },
                                    "learningStyle": { "type": "STRING", "description": "VIDEO or READ" },
                                    "failureCause": { 
                                        "type": "STRING", 
                                        "description": "The primary reason the user fails tasks: DISTRACTION (Phone/Socials), FATIGUE (Tired), or CLARITY (Don't know where to start)." 
                                    }
                                }
                            }
                        },
                        {
                            "name": "complete_interview",
                            "description": "Call this to end the interview after the Handoff script is read.",
                            "parameters": {
                                "type": "OBJECT",
                                "properties": {
                                    "success": { "type": "BOOLEAN" }
                                }
                            }
                        }
                    ]
                }
            ]
        """.trimIndent()
        
        // 4. Connect Voice Engine & Listen for Tools
        viewModelScope.launch {
            try {
                // Launch Tool Listener
                launch {
                    reasoningEngine.toolCalls.collect { toolCall ->
                        if (toolCall.name == "update_profile") {
                            // Map generic args to StudentProfile
                            val pArgs = toolCall.args ?: emptyMap()
                            
                            val profileUpdate = StudentProfile(
                                university = pArgs["university"] ?: agent.profile.university,
                                major = pArgs["major"] ?: agent.profile.major,
                                semester = pArgs["semester"] ?: agent.profile.semester,
                                
                                mainPriority = pArgs["mainPriority"] ?: agent.profile.mainPriority,
                                secondaryPriority = pArgs["secondaryPriority"] ?: agent.profile.secondaryPriority,
                                
                                energyPreference = pArgs["energyPreference"] ?: agent.profile.energyPreference,
                                dailyFocusCapacity = pArgs["dailyFocusCapacity"]?.toIntOrNull() ?: agent.profile.dailyFocusCapacity,
                                sleepTime = pArgs["sleepTime"] ?: agent.profile.sleepTime,
                                wakeTime = pArgs["wakeTime"] ?: agent.profile.wakeTime,
                                
                                needsJob = pArgs["needsJob"]?.toBooleanStrictOrNull() ?: agent.profile.needsJob,
                                workHoursPerWeek = pArgs["workHoursPerWeek"]?.toDoubleOrNull() ?: agent.profile.workHoursPerWeek,
                                hasJob = (pArgs["workHoursPerWeek"]?.toDoubleOrNull() ?: 0.0) > 0,
                                
                                commuteDuration = pArgs["commuteDuration"] ?: agent.profile.commuteDuration,
                                protectedTime = pArgs["protectedTime"] ?: agent.profile.protectedTime,
                                
                                learningStyle = pArgs["learningStyle"] ?: agent.profile.learningStyle,
                                failureCause = pArgs["failureCause"] ?: agent.profile.failureCause,
                                
                                financialStakes = pArgs["financialStakes"] ?: agent.profile.financialStakes,
                                stressResponse = if (pArgs["failureCause"]?.contains("DISTRACTION", true) == true) "Avoid" 
                                                 else if (pArgs["failureCause"]?.contains("CLARITY", true) == true) "Freeze"
                                                 else agent.profile.stressResponse
                            )
                            
                            println("🧠 Agent Logic: Updating Profile -> $profileUpdate")
                            // 5. Save to Local Persistence
                            agent.updateProfile(profileUpdate)
                            
                            launch(kotlinx.coroutines.Dispatchers.IO) {
                                profileStorage.saveProfile(agent.profile)
                            }

                            // ⚡ SEND TOOL RESPONSE
                            reasoningEngine.sendToolResponse("update_profile", mapOf("result" to "Profile Updated Successfully"), toolCall.id)
                            
                        } else if (toolCall.name == "complete_interview") {
                            println("✅ Interview Complete Triggered via Tool")
                            
                            // Final Save
                            launch(kotlinx.coroutines.Dispatchers.IO) {
                                profileStorage.saveProfile(agent.profile)
                            }

                             uiState = uiState.copy(isComplete = true)
                             
                             // ⚡ SEND TOOL RESPONSE
                             reasoningEngine.sendToolResponse("complete_interview", mapOf("success" to true), toolCall.id)
                        }
                    }
                }

                val apiKey = PlatformSecrets.apiKey
                reasoningEngine.connect(apiKey, systemInstruction = initialPrompt, toolsConfig = toolsConfig)
            } catch (e: Exception) {
                println("⚠️ Failed to start interview: ${e.message}")
            }
        }
    }
    // ... disconnect remains same
    
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
