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
// StudentProfile moved to StudentProfile.kt
// GenesisStage enum retained for reference/logic use

