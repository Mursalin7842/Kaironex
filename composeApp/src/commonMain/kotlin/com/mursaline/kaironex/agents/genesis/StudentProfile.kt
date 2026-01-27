package com.mursaline.kaironex.agents.genesis

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    // --- IDENTITY ---
    val name: String = "",
    val university: String = "",
    val universityLocation: String? = null,
    val major: String? = null,
    val semester: String? = null,

    // --- STRATEGY ---
    val mainPriority: String? = null,      // e.g. "JOB_READY"
    val secondaryPriority: String? = null, // e.g. "CGPA"
    val targetCgpa: String? = null,        // Kept for backward compat if needed, or moved to secondary logic
    val financialStakes: String? = null,   // Kept for Judge View logic
    val careerAmbition: String? = null,    // Kept for identity

    // --- PHYSICS (Energy) ---
    val energyPreference: String? = null,  // "MORNING", "NIGHT"
    val dailyFocusCapacity: Int? = null,   // Hours, e.g. 4
    val sleepTime: String? = null,
    val wakeTime: String? = null,

    // --- LOGISTICS ---
    val hasJob: Boolean = false,
    val needsJob: Boolean = false,
    val workHoursPerWeek: Double? = null, // Changed to Double to match plan if needed, but keeping Int/Double flexible. Plan said Double.
    val commuteDuration: String? = null,  // "commuteTime" mapped to this or kept as alias
    val commuteTime: String? = null,      // Keeping original name for Judge View compatibility unless refactored
    val protectedTime: String? = null,     // "Prayer, Gym"
    val scheduleSource: String? = null,    // "VERBAL" or "DRIVE_UPLOAD"

    // --- RESOURCES ---
    val learningStyle: String? = null,     // "VIDEO", "READ"
    val primaryStudyMaterial: String? = null,

    // --- 🆕 FAILURE ANALYSIS (For "Focus Drift") ---
    val failureCause: String? = null,       // "DISTRACTION", "FATIGUE", "CLARITY"
    
    // --- LEGACY/COMPATIBILITY FIELDS (To avoid breaking Judge View immediately) ---
    val stressResponse: String? = null // Keeping this as it's used in Judge View
) {
    fun getMissingFields(stage: GenesisStage): List<String> {
        val missing = mutableListOf<String>()
        
        when (stage) {
            GenesisStage.IDENTITY -> {
                 // Nothing strictly required here, usually just transitions to Academic
            }
            GenesisStage.ACADEMIC -> {
                if (university.isBlank()) missing.add("university")
                if (major.isNullOrBlank()) missing.add("major")
            }
            GenesisStage.GOALS -> {
                if (mainPriority.isNullOrBlank()) missing.add("mainPriority")
            }
            GenesisStage.RHYTHM -> {
                if (energyPreference.isNullOrBlank()) missing.add("energyPreference")
                // Focus Capacity is Int, so null check is fine
                if (dailyFocusCapacity == null) missing.add("dailyFocusCapacity")
            }
            GenesisStage.CONSTRAINTS -> {
                if (learningStyle.isNullOrBlank()) missing.add("learningStyle")
                if (protectedTime.isNullOrBlank()) missing.add("protectedTime") 
            }
            GenesisStage.CONFIRMATION -> {
               if (failureCause.isNullOrBlank()) missing.add("failureCause")
            }
            GenesisStage.COMPLETE -> {
                // Nothing missing
            }
            else -> {}
        }
        
        return missing
    }
}
