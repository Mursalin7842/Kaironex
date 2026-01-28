package com.mursaline.kaironex.agents.genesis

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    // Identity
    val name: String = "",
    val university: String? = null,
    val major: String? = null,
    val semester: String? = null,
    val totalSemesters: String? = null,

    // Academic
    val currentCgpa: String? = null,
    val targetCgpa: String? = null, 

    // International / Visa
    val isInternationalStudent: Boolean? = null,
    val homeCountry: String? = null,
    val currentCountry: String? = null,
    val visaStatus: String? = null,
    val workRestrictions: String? = null,
    val financialStakes: String? = null,

    // Employment
    val hasJob: Boolean? = null,
    val wantsJobHelp: Boolean? = null,
    val jobDescription: String? = null,
    val workHoursPerWeek: Double? = null, 
    val jobSchedule: String? = null,

    // Rhythm
    val commuteDuration: String? = null,
<<<<<<< HEAD
    val commuteTime: String? = null,
    val scheduleSource: String? = null,
    val primaryStudyMaterial: String? = null,
    val stressResponse: String? = null
=======
    val commuteMap: Map<String, String>? = null,
    val energyPreference: String? = null,
    val dailyFocusCapacity: Int? = null, 
    val sleepTime: String? = null, 
    val wakeTime: String? = null,
    val classSchedule: Map<String, String>? = null,
    val routineFile: String? = null,

    // Constraints / Psych
    val nonNegotiables: Map<String, String>? = null,
    val customCommitments: Map<String, String>? = null,
    val learningStyle: String? = null,
    val stressResponse: String? = null, 
    val failureCause: String? = null,
    
    // Goals
    val careerAmbition: String? = null,
    val mainPriority: String? = null,
    val secondaryPriority: String? = null
>>>>>>> 3168251dc55074dd77c20a1833a1a0f0d5e38ab7
)
