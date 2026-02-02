package com.mursaline.kaironex.agents.genesis

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    // Identity
    val name: String = "",
    val university: String? = null,
    val major: String? = null, // degreeMajor
    
    // Agent Configuration (from Genesis/Onboarding)
    val wakeWord: String = "kaironex",
    val agentNickname: String = "Kairo",

    // Academic
    val totalSemesters: String? = null,
    val semester: String? = null, // currentSemester
    val currentCgpa: String? = null,
    val targetCgpa: String? = null, // desiredCGPA
    val desiredCgpaReason: String? = null,

    // International
    val isInternationalStudent: Boolean? = null,
    val homeCountry: String? = null,
    val currentCountry: String? = null, // hostCountry
    val visaStatus: String? = null,

    // Work
    val hasJob: Boolean? = null,
    val jobDescription: String? = null, // jobPosition
    val jobSchedule: String? = null,

    // Logistics
    val commuteTime: String? = null,
    val nonNegotiables: String? = null, // String now, not Map

    // Study Strategy / Psych
    val learningStyle: String? = null,
    val preferredResources: String? = null,
    val productivityKiller: String? = null,
    val dailyFocusCapacity: String? = null, // focusCapacity (String)
    val energyPreference: String? = null // chronotype
)
