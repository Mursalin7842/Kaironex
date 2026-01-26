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
    
    // --- AGENT BRAIN ---
    private val agent = GenesisAgent()

    // --- STATE ---
    var uiState by mutableStateOf(GenesisUiState())
        private set
        
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
                                    "careerAmbition": { "type": "STRING" },
                                    "targetCgpa": { "type": "STRING" },
                                    "financialStakes": { "type": "STRING", "description": "VISA, SCHOLARSHIP, or NONE" },
                                    "workHoursPerWeek": { "type": "NUMBER" },
                                    "sleepTime": { "type": "STRING", "description": "e.g. 23:00" },
                                    "wakeTime": { "type": "STRING", "description": "e.g. 07:00" },
                                    "commuteTime": { "type": "STRING" },
                                    "stressResponse": { "type": "STRING", "description": "FREEZE, PANIC, or AVOID" }
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
                                university = pArgs["university"] ?: "",
                                major = pArgs["major"],
                                semester = pArgs["semester"],
                                careerAmbition = pArgs["careerAmbition"],
                                targetCgpa = pArgs["targetCgpa"],
                                financialStakes = pArgs["financialStakes"],
                                workHoursPerWeek = pArgs["workHoursPerWeek"]?.toIntOrNull(),
                                hasJob = pArgs["workHoursPerWeek"] != null,
                                sleepTime = pArgs["sleepTime"],
                                wakeTime = pArgs["wakeTime"],
                                commuteTime = pArgs["commuteTime"],
                                stressResponse = pArgs["stressResponse"]
                            )
                            
                            println("🧠 Agent Logic: Updating Profile -> $profileUpdate")
                            agent.updateProfile(profileUpdate)
                            
                            // Check completion
                            if (agent.stage == com.mursaline.kaironex.agents.genesis.GenesisStage.COMPLETE) {
                                uiState = uiState.copy(isComplete = true)
                                // disconnect() // Optional: Disconnect immediately or wait for goodbye?
                                // Let the agent say goodbye first.
                            }
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
