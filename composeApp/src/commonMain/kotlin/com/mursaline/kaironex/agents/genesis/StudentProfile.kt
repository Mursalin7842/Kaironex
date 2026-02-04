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

    // Work & Financials
    val hasJob: Boolean? = null,
    val jobDescription: String? = null, // jobPosition
    val jobSchedule: String? = null,
    val jobImportance: String? = null, // "Survival", "Career", "Pocket Money"
    val financialStatus: String? = null, // "Survival", "Stable", "Comfortable"

    // Skills
    val skills: List<String> = emptyList(), // Custom skills input

    // Logistics
    val commuteTime: String? = null,
    val nonNegotiables: String? = null, // String now, not Map

    // Study Strategy / Psych
    val learningStyle: String? = null, // Visual, Auditory, etc.
    val preferredResources: String? = null,
    val productivityKiller: String? = null,
    val dailyFocusCapacity: String? = null, // "2h Pomodoro", "4h Deep Work"
    val energyPreference: String? = null, // "Early Bird", "Night Owl" (Chronotype)
    
    // Work Preference (RIASEC / Environment)
    val workPreference: String? = null, // "Remote", "On-site", "Hybrid"
    val socialBattery: String? = null, // "Solo", "Team", "Public"
    val environmentType: String? = null, // "Realistic", "Investigative", etc.

    // Ambition & Values
    val targetRole: String? = null,
    val targetIndustry: String? = null,
    val workType: String? = null, // Internship, Full-time
    val allowedWorkHours: Int? = null, // e.g. 20 for F1, 40 for Break
    val valueDrivers: List<String> = emptyList(), // ["Money", "Impact", "Balance"]
    val stabilityPreference: String? = null, // "Startup" vs "Corporate"

    // Skills (Arsenal)
    // hardSkills is mapped to 'skills' for now, but explicit distinction is good
    val softSkills: Map<String, Int> = emptyMap(), // "Leadership" -> 8
    
    // Experience
    val projectCount: Int? = null,
    val hackathonCount: Int? = null
)
