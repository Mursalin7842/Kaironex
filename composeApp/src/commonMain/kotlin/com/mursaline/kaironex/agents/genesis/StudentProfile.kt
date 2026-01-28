package com.mursaline.kaironex.agents.genesis

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    // --- IDENTITY & METADATA ---
    val name: String = "",
    
    // --- ACADEMIC LIFE ---
    val university: String = "",
    val major: String? = null,
    val semester: String? = null,        // Current Semester
    val totalSemesters: String? = null,  // New: Total Semesters
    val currentCgpa: String? = null,     // New: CGPA until now
    val academicResults: String? = null, // "Good", "CGPA 3.5" (Verbal)
    
    val isInternationalStudent: Boolean? = null, 
    val homeCountry: String? = null,             
    val currentCountry: String? = null,  // New: Where they are currently living

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

    // --- REAL WORLD SCHEDULE ---
    // Key: "Class Name", Value: "Time/Days"
    val classSchedule: Map<String, String> = emptyMap(),
    // Key: "Activity", Value: "Description/Time"
    val customCommitments: Map<String, String> = emptyMap(),
    // Key: "Constraint", Value: "Description" (Protected/Non-Negotiable)
    val nonNegotiables: Map<String, String> = emptyMap(),
    
    // Key: "Route" (e.g. Home-Uni), Value: "Time" (e.g. 30m)
    val commuteMap: Map<String, String> = emptyMap(),

    val routineFile: String? = null, // Path or Content of uploaded routine
    
    // --- INTERNATIONAL CONTEXT ---
    val visaStatus: String? = null,      // "F1", "Student Route"
    val workRestrictions: String? = null ,// "20h limit", "No work"

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
    fun getMissingFields(): List<String> {
        val missing = mutableListOf<String>()

        // 1. ACADEMIC SECTION
        if (university.isBlank()) missing.add("university")
        if (major.isNullOrBlank()) missing.add("major")
        if (semester.isNullOrBlank()) missing.add("semester")
        if (totalSemesters.isNullOrBlank()) missing.add("totalSemesters")
        if (currentCgpa.isNullOrBlank()) missing.add("currentCgpa")
        
        // International Status Check
        if (isInternationalStudent == null) missing.add("isInternationalStudent")
        else if (isInternationalStudent) {
             if (homeCountry.isNullOrBlank()) missing.add("homeCountry")
             if (currentCountry.isNullOrBlank()) missing.add("currentCountry")
             if (visaStatus.isNullOrBlank()) missing.add("visaStatus")
             if (workRestrictions.isNullOrBlank()) missing.add("workRestrictions")
        }

        // BLOCKER: If Academic details are missing, stop here.
        if (missing.isNotEmpty()) return missing

        // 2. JOB LIFE SECTION
        if (hasJob == null) missing.add("hasJob")
        else if (hasJob) {
             if (jobDescription.isNullOrBlank()) missing.add("jobDescription")
             if (jobSchedule.isNullOrBlank()) missing.add("jobSchedule")
             if (jobWorkDays.isNullOrBlank()) missing.add("jobWorkDays")
             // Move commute logic here for employed people as it's critical for job context
             if (jobCommuteTime.isNullOrBlank()) missing.add("jobCommuteTime") 
        } 
        else {
             // If Unemployed
             if (wantsJobHelp == null) missing.add("wantsJobHelp")
        }

        // BLOCKER: If Job details are missing, stop here.
        if (missing.isNotEmpty()) return missing

        // 3. LOGISTICS & COMMUTE (General)
        // Only ask general commute if logic suggests it (e.g. from Home to Uni)
        // For simplicity, we assume we need at least one commute leg defined if they go to uni
        // But for "Missing Fields", let's keep it simple:
        if (commuteDuration.isNullOrBlank()) missing.add("commuteDuration") // General "How long to uni?"

        if (missing.isNotEmpty()) return missing

        // 4. STRATEGY & GOALS
        if (mainPriority.isNullOrBlank()) missing.add("mainPriority")
        if (energyPreference.isNullOrBlank()) missing.add("energyPreference")
        if (dailyFocusCapacity == null) missing.add("dailyFocusCapacity")
        
        if (missing.isNotEmpty()) return missing

        // 5. CONSTRAINTS
        if (learningStyle.isNullOrBlank()) missing.add("learningStyle")
        if (stressResponse.isNullOrBlank()) missing.add("stressResponse")
        if (failureCause.isNullOrBlank()) missing.add("failureCause")

        return missing
    }

    // ✂️ CONTEXT PRUNING: Returns ONLY the data needed for the current phase
    fun getRelevantContext(missing: List<String>): Map<String, String?> {
        if (missing.isEmpty()) return emptyMap()

        val activePhase = when {
            missing.any { it in listOf("university", "major", "semester", "currentCgpa", "isInternationalStudent") } -> "ACADEMIC"
            missing.any { it in listOf("hasJob", "jobDescription", "jobSchedule", "wantsJobHelp", "jobCommuteTime") } -> "JOB"
            missing.any { it.startsWith("commute") } -> "COMMUTE"
            missing.any { it in listOf("mainPriority", "energyPreference", "nonNegotiables") } -> "STRATEGY"
            else -> "GENERAL"
        }

        return when (activePhase) {
            "ACADEMIC" -> mapOf(
                "university" to university,
                // Only send relevant previous answers in this phase
                "major" to major
            )
            "JOB" -> mapOf(
                "university" to university, // Keep Uni context just in case
                "hasJob" to (hasJob?.toString())
            )
            "COMMUTE" -> mapOf(
                "university" to university,
                "job_location" to (if (hasJob == true) "Yes" else "No"),
                "homeCountry" to homeCountry // Maybe relevant for housing context?
            )
            "STRATEGY" -> mapOf(
                 // Strategy needs holistic view, so we send a summary
                 "cgpa" to currentCgpa,
                 "job" to (if (hasJob == true) "Yes" else "No")
            )
            else -> emptyMap()
        }
    }
}
