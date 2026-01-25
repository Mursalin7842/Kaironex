package com.mursaline.kaironex.agents.genesis

import com.mursaline.kaironex.agents.core.ExtractedField
import com.mursaline.kaironex.core.gemini.GeminiOrchestrator
import com.mursaline.kaironex.features.genesis.ChatMessage
import kotlinx.serialization.json.Json

class GenesisAgent {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    
    var profile = StudentProfile()
        private set
    var stage = GenesisStage.IDENTITY
        private set
    
    private val history = mutableListOf<ChatMessage>()

    // REVERSIBLE STATE: Human-like Correction
    fun rollbackTo(targetStage: GenesisStage) {
        // In a real app, you might clear data here. 
        // For now, we just move the pointer so the Agent re-verifies.
        stage = targetStage
        history.add(ChatMessage("system", "User requested correction. Re-entering ${targetStage.name} phase."))
    }
    
    // Allow external manual update (e.g. from Mad Libs intro)
    fun manualUpdate(name: String, wakeWord: String) {
        profile = profile.copy(name = name)
        // If identity is set, jump to Academic
        if (stage == GenesisStage.IDENTITY) {
            stage = GenesisStage.ACADEMIC
        }
    }

    suspend fun processTurn(
        userText: String?, 
        agentName: String, 
        userName: String
    ): AgentResponse {
        
        if (userText != null) {
            history.add(ChatMessage("user", userText))
            
            // INTENT DETECTION (Simple Rule-based for now)
            if (userText.contains("wrong", ignoreCase = true) || userText.contains("mistake", ignoreCase = true)) {
                // Heuristic: If user signals error, rollback one step
                // (Advanced: Use Gemini to determine WHERE to rollback)
            }
        }

        // 1. FSM PROGRESSION
        val missing = profile.getMissingFields(stage)
        if (missing.isEmpty() && stage != GenesisStage.COMPLETE) {
            stage = getNextStage(stage)
        }

        // 2. GET RATIONALE (Explainability)
        val currentField = if (stage == GenesisStage.COMPLETE) null else profile.getMissingFields(stage).firstOrNull()
        val reason = FieldRationale.map[currentField] ?: "Required for profile setup."

        // 3. BUILD PROMPT
        val prompt = GenesisPrompts.build(
            agentName, userName, stage, 
            profile.getMissingFields(stage), reason
        )

        // 4. GEMINI REASONING
        var responseText = ""
        try {
            val response = if (userText == null) {
                 GeminiOrchestrator.chat(history, "Initialize Protocol.", prompt)
            } else {
                 GeminiOrchestrator.chat(history, userText, prompt)
            }
            responseText = response.text
        } catch (e: Exception) {
            responseText = "I'm having trouble connecting to my brain. Let's try that again. Error: ${e.message}"
        }

        // 5. SIDECAR EXTRACTION with CONFIDENCE
        val (speech, extractedJson) = parseSidecar(responseText)
        
        // 6. UPDATE MEMORY (With Validation)
        if (extractedJson != null) {
            // Here we would check confidence if Gemini returned it.
            // For hackathon simplicity, we assume extracted JSON is > 0.7 confidence
            updateProfile(extractedJson)
        }
        
        history.add(ChatMessage("ai", speech))

        return AgentResponse(
            displayText = speech,
            isComplete = stage == GenesisStage.COMPLETE,
            updatedProfile = profile
        )
    }

    private fun parseSidecar(raw: String): Pair<String, StudentProfile?> {
        val parts = raw.split("|||")
        val speech = parts[0].trim()
        var data: StudentProfile? = null
        
        if (parts.size > 1) {
            try {
                // This mimics extracting extracting { "major": "CSE" }
                data = jsonParser.decodeFromString<StudentProfile>(parts[1].trim())
            } catch (e: Exception) {
                println("Genesis Extraction Fail: ${e.message}")
            }
        }
        return Pair(speech, data)
    }

    private fun updateProfile(newData: StudentProfile) {
        newData.major?.let { profile = profile.copy(major = it) }
        newData.semester?.let { profile = profile.copy(semester = it) }
        newData.careerAmbition?.let { profile = profile.copy(careerAmbition = it) }
        newData.sleepTime?.let { profile = profile.copy(sleepTime = it) }
        newData.university.takeIf { it.isNotEmpty() }?.let { profile = profile.copy(university = it) }
        newData.targetCgpa?.let { profile = profile.copy(targetCgpa = it) }
        newData.wakeTime?.let { profile = profile.copy(wakeTime = it) }
        newData.commuteTime?.let { profile = profile.copy(commuteTime = it) }
        // ... map all fields
    }

    private fun getNextStage(current: GenesisStage): GenesisStage {
        return when (current) {
            GenesisStage.IDENTITY -> GenesisStage.ACADEMIC
            GenesisStage.ACADEMIC -> GenesisStage.GOALS
            GenesisStage.GOALS -> GenesisStage.RHYTHM
            GenesisStage.RHYTHM -> GenesisStage.CONSTRAINTS
            GenesisStage.CONSTRAINTS -> GenesisStage.CONFIRMATION
            GenesisStage.CONFIRMATION -> GenesisStage.COMPLETE
            GenesisStage.COMPLETE -> GenesisStage.COMPLETE
        }
    }
    
    data class AgentResponse(
        val displayText: String,
        val isComplete: Boolean,
        val updatedProfile: StudentProfile
    )
}
