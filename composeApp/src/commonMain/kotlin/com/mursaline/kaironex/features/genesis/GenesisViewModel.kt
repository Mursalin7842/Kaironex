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

import com.mursaline.kaironex.PlatformSecrets

import kotlinx.serialization.json.*
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
        if (reasoningEngine.connectionState.value is com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Connected) return

        agent.manualUpdate(userName, wakeWord)
        
        // 1. EXACT PROMPT FROM REACT CODE (Ported to Kotlin String)
        // This prompt is stricter and controls the flow better than the previous one.
        val systemPrompt = """
            You are Kairo, a professional, warm, and efficient AI interviewer for 'Kaironex'.
            Your goal is to calibrate a user's profile by asking specific questions one by one.
            
            **PROTOCOL:**
            1. Introduce yourself briefly ("Hello, I am Kairo. Welcome to Kaironex. I am here to calibrate your profile. Are you ready?").
            2. Wait for user confirmation.
            3. Ask the questions in this EXACT order:
               - University Name?
               - Major/Field of study?
               - Current Semester (e.g., Fall 2024, 5th Semester)?
               - Current CGPA?
               - Are you an International Student? (Yes/No)
               - Do you currently have a job? (Yes/No)
               - Do you want help finding a job? (Yes/No)
               - Describe your ideal job?
               - What is your commute duration?
               - Do you prefer High or Low Energy environments?
               - What is your daily focus capacity (in hours)?
               - What are your non-negotiables?
               - What is your learning style?
               - How do you respond to stress?
               - What is a common cause of failure for you?
            
            **RULES:**
            - ASK ONE QUESTION AT A TIME.
            - When the user answers, validate the answer. 
            - IMMEDIATELY call the 'saveField' tool with the corresponding field name (university, major, etc.) and the value.
            - WAIT for the tool execution to complete.
            - THEN acknowledge briefly and ask the next question.
            - If the user is unclear, ask for clarification before saving.
            - At the end, call 'endInterview'.
        """.trimIndent()

        // 2. CLEANER TOOL CONFIGURATION
        // Ensure this JSON structure is valid.
        val toolsConfig = """
            "tools": [
                {
                    "function_declarations": [
                        {
                            "name": "saveField",
                            "description": "Saves a validated answer for a specific user profile field. Call this immediately after the user provides an answer.",
                            "parameters": {
                                "type": "OBJECT",
                                "properties": {
                                    "field": {
                                        "type": "STRING",
                                        "enum": [
                                            "university", "major", "semester", "currentCgpa", "isInternationalStudent",
                                            "hasJob", "wantsJobHelp", "jobDescription", "commuteDuration",
                                            "energyPreference", "dailyFocusCapacity", "nonNegotiables",
                                            "learningStyle", "stressResponse", "failureCause"
                                        ],
                                        "description": "The specific field being saved."
                                    },
                                    "value": {
                                        "type": "STRING",
                                        "description": "The extracted and validated value from the user answer."
                                    }
                                },
                                "required": ["field", "value"]
                            }
                        },
                        {
                            "name": "endInterview",
                            "description": "Call this when all questions have been asked and the interview is complete.",
                            "parameters": {
                                "type": "OBJECT",
                                "properties": {}
                            }
                        }
                    ]
                }
            ]
        """.trimIndent()
        
        viewModelScope.launch {
            try {
                // Launch Tool Listener
                launch {
                    reasoningEngine.toolCalls.collect { toolCall ->
                        if (toolCall.name == "saveField") {
                            // Atomic Field Update
                            val args = toolCall.args ?: emptyMap()
                            val field = args["field"] as? String
                            val value = args["value"] as? String
                            
                            if (field != null && value != null) {
                                println("💾 Saving Field: $field = $value")
                                
                                // Directly update the profile based on field name
                                // We use a helper helper to map string -> profile property
                                // Note: Kotlin Copy is robust.
                                val current = agent.profile
                                val updated = when(field) {
                                    "university" -> current.copy(university = value)
                                    "major" -> current.copy(major = value)
                                    "semester" -> current.copy(semester = value)
                                    "currentCgpa" -> current.copy(currentCgpa = value)
                                    "isInternationalStudent" -> current.copy(isInternationalStudent = value.toBoolean())
                                    "hasJob" -> current.copy(hasJob = value.toBoolean())
                                    "wantsJobHelp" -> current.copy(wantsJobHelp = value.toBoolean())
                                    "jobDescription" -> current.copy(jobDescription = value)
                                    "commuteDuration" -> current.copy(commuteDuration = value)
                                    "energyPreference" -> current.copy(energyPreference = value)
                                    "dailyFocusCapacity" -> current.copy(dailyFocusCapacity = value.toIntOrNull() ?: 4)
                                    "nonNegotiables" -> current.copy(nonNegotiables = mapOf("Summary" to value))
                                    "learningStyle" -> current.copy(learningStyle = value)
                                    "stressResponse" -> current.copy(stressResponse = value)
                                    "failureCause" -> current.copy(failureCause = value)
                                    else -> current
                                }
                                
                                agent.updateProfile(updated)
                                
                                reasoningEngine.sendToolResponse(
                                    toolName = toolCall.name,
                                    toolId = toolCall.id,
                                    response = mapOf("result" to "Field saved successfully.")
                                )
                            } else {
                                reasoningEngine.sendToolResponse(
                                    toolName = toolCall.name,
                                    toolId = toolCall.id,
                                    response = mapOf("error" to "Missing field or value")
                                )
                            }
                            
                        } else if (toolCall.name == "endInterview") {
                            println("✅ Interview Complete Triggered via Tool")
                            
                            // Final Save
                            launch(kotlinx.coroutines.Dispatchers.IO) {
                                profileStorage.saveProfile(agent.profile)
                            }

                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                uiState = uiState.copy(isComplete = true)
                             }
                             
                             reasoningEngine.sendToolResponse("endInterview", mapOf("result" to "Interview ended."), toolCall.id)
                        }
                    }
                }
                
                // Launch Speaking State Listener
                launch {
                     reasoningEngine.isAgentSpeaking.collect { speaking ->
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             uiState = uiState.copy(
                                 isAgentSpeaking = speaking,
                                 isUserListening = !speaking // If agent not speaking, we listen
                             )
                         }
                     }
                }

                // Connect with the new STRICT prompt
                val apiKey = PlatformSecrets.apiKey
                reasoningEngine.connect(apiKey, systemInstruction = systemPrompt, toolsConfig = toolsConfig)
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
