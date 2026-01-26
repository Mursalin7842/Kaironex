package com.mursaline.kaironex.agents.genesis

import com.mursaline.kaironex.agents.core.ExtractedField
import kotlinx.serialization.Serializable

// The Explicit States
enum class GenesisStage {
    IDENTITY, ACADEMIC, GOALS, RHYTHM, CONSTRAINTS, CONFIRMATION, COMPLETE
}

// The "Why" Memory (Explainability Layer)
@Suppress("unused")
object FieldRationale {
    val map = mapOf(
        "sleepTime" to "I need this to calculate your Bio-Fuel score and prevent burnout.",
        "careerAmbition" to "This helps me filter hackathons and internships relevant to you.",
        "major" to "This determines which specialist study agents (Math vs. Art) to deploy.",
        "commuteTime" to "I can schedule audio-only review sessions during this time.",
        "university" to "I can optimize your schedule based on your campus location.",
        "semester" to "I need to know if you are in foundation years or specialized years.",
        "targetCgpa" to "I need to calibrate the strictness of my study schedules."
    )
}

// The Data Schema (Typed & Confidence-Aware)
@Serializable
data class StudentProfile(
    var name: String = "",
    var university: String = "",
    
    // Wrapped Fields for Validity Checking
    var major: String? = null,
    var semester: String? = null,
    var careerAmbition: String? = null,
    var targetCgpa: String? = null,
    
    // Rhythm & Biology
    var sleepTime: String? = null,
    var wakeTime: String? = null,
    var stressResponse: String? = null, // "Freeze", "Panic", "Avoid"
    
    // Pressure Map (Work & Finance)
    var hasJob: Boolean = false,
    var workHoursPerWeek: Int? = null,
    var financialStakes: String? = null, // "Scholarship", "Visa", "Self-Funded"
    var commuteTime: String? = null
) {
    // Logic: What is missing?
    fun getMissingFields(stage: GenesisStage): List<String> {
        return when (stage) {
            GenesisStage.IDENTITY -> if (name.isEmpty()) listOf("name") else emptyList()
            GenesisStage.ACADEMIC -> listOfNotNull(
                if (university.isEmpty()) "university" else null,
                if (major == null) "major" else null,
                if (semester == null) "semester" else null
            )
            GenesisStage.GOALS -> listOfNotNull(
                if (careerAmbition == null) "careerAmbition" else null,
                if (targetCgpa == null) "targetCgpa" else null,
                if (financialStakes == null) "financialStakes" else null
            )
            GenesisStage.RHYTHM -> listOfNotNull(
                if (sleepTime == null) "sleepTime" else null,
                if (wakeTime == null) "wakeTime" else null,
                if (workHoursPerWeek == null && hasJob) "workHoursPerWeek" else null // Only ask if hasJob logic is handled or we ask "Do you work?" first
            )
            GenesisStage.CONSTRAINTS -> listOfNotNull(
                if (commuteTime == null) "commuteTime" else null,
                if (stressResponse == null) "stressResponse" else null
            )
            // ... add other stages
            else -> emptyList()
        }
    }
}
