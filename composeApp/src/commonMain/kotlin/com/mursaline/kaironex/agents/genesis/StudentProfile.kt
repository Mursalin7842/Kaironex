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
        // ... (Your existing validation logic)
        // For Master Build, we blindly follow the script flow usually, 
        // but let's keep it empty as requested in plan.
        return emptyList() 
    }
}
