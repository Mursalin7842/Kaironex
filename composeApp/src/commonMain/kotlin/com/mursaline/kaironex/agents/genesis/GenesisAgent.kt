package com.mursaline.kaironex.agents.genesis

import com.mursaline.kaironex.agents.core.ExtractedField
import com.mursaline.kaironex.core.gemini.ChatMessage
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
    
    fun restoreProfile(savedProfile: StudentProfile) {
        profile = savedProfile
        // Simple heuristic to restore stage
        if (profile.major != null && profile.financialStakes != null) {
            stage = GenesisStage.COMPLETE
        } else if (profile.university.isNotEmpty()) {
            stage = GenesisStage.ACADEMIC
        }
    }
    
    // NOTE: In the Voice-based flow, 'processTurn' is handled by the GeminiReasoningEngine via WebSocket.
    // This Agent class now strictly manages State (FSM) and Profile data.
    // Future: We can hook the 'parseSidecar' into the Voice Text Frames if needed.

    // Logic for parsing JSON from mixed text (Shared Utility)
    fun parseSidecar(raw: String): Pair<String, StudentProfile?> {
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

    fun updateProfile(newData: StudentProfile) {
        newData.major?.let { profile = profile.copy(major = it) }
        newData.semester?.let { profile = profile.copy(semester = it) }
        newData.university.takeIf { it.isNotEmpty() }?.let { profile = profile.copy(university = it) }
        
        // --- NEW ACADEMIC FIELDS ---
        newData.isInternationalStudent?.let { profile = profile.copy(isInternationalStudent = it) }
        newData.homeCountry?.let { profile = profile.copy(homeCountry = it) }
        newData.currentCountry?.let { profile = profile.copy(currentCountry = it) }
        newData.academicResults?.let { profile = profile.copy(academicResults = it) }
        newData.totalSemesters?.let { profile = profile.copy(totalSemesters = it) }
        newData.currentCgpa?.let { profile = profile.copy(currentCgpa = it) }

        // --- NEW JOB FIELDS ---
        newData.jobDescription?.let { profile = profile.copy(jobDescription = it) }
        newData.jobSchedule?.let { profile = profile.copy(jobSchedule = it) }
        newData.jobWorkDays?.let { profile = profile.copy(jobWorkDays = it) }
        newData.jobCommuteTime?.let { profile = profile.copy(jobCommuteTime = it) }
        newData.wantsJobHelp?.let { profile = profile.copy(wantsJobHelp = it) }

        // --- LEGACY ---
        newData.careerAmbition?.let { profile = profile.copy(careerAmbition = it) }
        newData.sleepTime?.let { profile = profile.copy(sleepTime = it) }
        newData.targetCgpa?.let { profile = profile.copy(targetCgpa = it) }
        newData.wakeTime?.let { profile = profile.copy(wakeTime = it) }
        newData.commuteTime?.let { profile = profile.copy(commuteTime = it) }
        
        // 🔧 FIX: Map MISSING fields to prevent data loss
        newData.mainPriority?.let { profile = profile.copy(mainPriority = it) }
        newData.secondaryPriority?.let { profile = profile.copy(secondaryPriority = it) }
        newData.financialStakes?.let { profile = profile.copy(financialStakes = it) }
        newData.energyPreference?.let { profile = profile.copy(energyPreference = it) }
        newData.dailyFocusCapacity?.let { profile = profile.copy(dailyFocusCapacity = it) }
        newData.hasJob?.let { profile = profile.copy(hasJob = it) }
        newData.needsJob?.let { profile = profile.copy(needsJob = it) }
        newData.workHoursPerWeek?.let { profile = profile.copy(workHoursPerWeek = it) }
        newData.commuteDuration?.let { profile = profile.copy(commuteDuration = it) }
        newData.commuteDuration?.let { profile = profile.copy(commuteDuration = it) }
        newData.protectedTime?.let { profile = profile.copy(protectedTime = it) }
        if (newData.classSchedule.isNotEmpty()) { profile = profile.copy(classSchedule = newData.classSchedule) }
        if (newData.customCommitments.isNotEmpty()) { profile = profile.copy(customCommitments = newData.customCommitments) }
        if (newData.nonNegotiables.isNotEmpty()) { profile = profile.copy(nonNegotiables = newData.nonNegotiables) }
        if (newData.commuteMap.isNotEmpty()) { profile = profile.copy(commuteMap = newData.commuteMap) }
        
        newData.visaStatus?.let { profile = profile.copy(visaStatus = it) }
        newData.workRestrictions?.let { profile = profile.copy(workRestrictions = it) }
        
        newData.routineFile?.let { profile = profile.copy(routineFile = it) }

        newData.scheduleSource?.let { profile = profile.copy(scheduleSource = it) }
        newData.learningStyle?.let { profile = profile.copy(learningStyle = it) }
        newData.primaryStudyMaterial?.let { profile = profile.copy(primaryStudyMaterial = it) }
        newData.failureCause?.let { profile = profile.copy(failureCause = it) }
        newData.stressResponse?.let { profile = profile.copy(stressResponse = it) }
        
        // Auto-advance stage if data is sufficient
        val missing = profile.getMissingFields()
        // Determine Stage based on sequential logic (Reverse engineering the stage from missing fields)
        // Actually, getMissingFields returns fields from the FIRST incomplete section.
        // We can map the *first missing field* to a Stage to keep UI in sync.
        
        if (missing.isEmpty()) {
            stage = GenesisStage.COMPLETE
        } else {
            // Heuristic: Map missing field to stage
            val first = missing.first()
            stage = when {
                first == "university" || first == "major" || first == "semester" || first == "currentCgpa" || first == "isInternationalStudent" -> GenesisStage.ACADEMIC
                first == "hasJob" || first == "wantsJobHelp" || first == "jobDescription" || first == "commuteDuration" -> GenesisStage.GOALS
                first == "energyPreference" || first == "dailyFocusCapacity" -> GenesisStage.RHYTHM
                first == "nonNegotiables" || first == "learningStyle" || first == "stressResponse" -> GenesisStage.CONSTRAINTS
                first == "failureCause" -> GenesisStage.CONFIRMATION
                else -> GenesisStage.ACADEMIC // Default
            }
        }
    }

    // getNextStage is no longer needed as we calculate dynamic stage above
    private fun getNextStage(current: GenesisStage): GenesisStage {
        return GenesisStage.COMPLETE // Placeholder or remove
    }
}
