package com.mursaline.kaironex.agents.genesis

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    // --- IDENTITY & METADATA ---
    val name: String = "",
    
    // --- ACADEMIC LIFE ---
    val university: String = "",
    val major: String? = null,
    val semester: String? = null,
    val isInternationalStudent: Boolean? = null, // New
    val homeCountry: String? = null,             // New
    val academicResults: String? = null,         // "Good", "CGPA 3.5" (Verbal)

    // --- JOB LIFE ---
    val hasJob: Boolean? = null,
    val jobDescription: String? = null,          // "Barista", "Developer"
    val jobSchedule: String? = null,             // "9am to 5pm"
    val jobCommuteTime: String? = null,          // "30 mins"
    val jobWorkDays: String? = null,             // "Mon-Fri"
    val wantsJobHelp: Boolean? = null,           // New: If no job, do they want help?

    // --- STRATEGY ---
    val mainPriority: String? = null,            // "JOB", "CGPA"
    
    // --- PHYSICS (Energy) ---
    val energyPreference: String? = null,
    val dailyFocusCapacity: Int? = null,

    // --- CONSTRAINTS ---
    val learningStyle: String? = null,
    val protectedTime: String? = null,           // "Prayer, Gym"
    val failureCause: String? = null,

    // --- LEGACY/COMPATIBILITY FIELDS ---
    val secondaryPriority: String? = null,
    val universityLocation: String? = null,
    val targetCgpa: String? = null,
    val financialStakes: String? = null,
    val careerAmbition: String? = null,
    val sleepTime: String? = null,
    val wakeTime: String? = null,
    val needsJob: Boolean = false,
    val workHoursPerWeek: Double? = null,
    val commuteDuration: String? = null,
    val commuteTime: String? = null,
    val scheduleSource: String? = null,
    val primaryStudyMaterial: String? = null,
    val stressResponse: String? = null
) {
    fun getMissingFields(stage: GenesisStage): List<String> {
        val missing = mutableListOf<String>()
        
        when (stage) {
            GenesisStage.IDENTITY -> {
                // Introduction Phase - No data required yet
            }
            GenesisStage.ACADEMIC -> {
                if (university.isBlank()) missing.add("university")
                else if (major.isNullOrBlank()) missing.add("major")
                else if (semester.isNullOrBlank()) missing.add("semester")
                else if (isInternationalStudent == null) missing.add("isInternationalStudent")
                else if (isInternationalStudent == true && homeCountry.isNullOrBlank()) missing.add("homeCountry")
                else if (academicResults.isNullOrBlank()) missing.add("academicResults")
            }
            GenesisStage.GOALS -> {
                // Note: We use the GOALS stage container for the "Job Life" section logic
                if (hasJob == null) missing.add("hasJob")
                else if (hasJob == true) {
                    if (jobDescription.isNullOrBlank()) missing.add("jobDescription")
                    else if (jobSchedule.isNullOrBlank()) missing.add("jobSchedule")
                    else if (jobWorkDays.isNullOrBlank()) missing.add("jobWorkDays")
                    else if (jobCommuteTime.isNullOrBlank()) missing.add("jobCommuteTime")
                } 
                else if (hasJob == false) {
                    if (wantsJobHelp == null) missing.add("wantsJobHelp")
                }
                
                // Only ask priority after Job section is cleared
                if (missing.isEmpty() && mainPriority.isNullOrBlank()) missing.add("mainPriority")
            }
            GenesisStage.RHYTHM -> {
                if (energyPreference == null) missing.add("energyPreference")
                else if (dailyFocusCapacity == null) missing.add("dailyFocusCapacity")
            }
            GenesisStage.CONSTRAINTS -> {
                if (learningStyle == null) missing.add("learningStyle")
                else if (protectedTime.isNullOrBlank()) missing.add("protectedTime") 
            }
            GenesisStage.CONFIRMATION -> {
               if (failureCause == null) missing.add("failureCause")
            }
            GenesisStage.COMPLETE -> {}
        }
        
        // Return only the FIRST missing field to ensure "One by One" behavior
        return if (missing.isNotEmpty()) listOf(missing.first()) else emptyList()
    }
}
