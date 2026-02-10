package com.mursaline.kaironex.features.zones.radius

/**
 * 📡 RADIUS UI STATE
 * ===================
 * Local data model for the Radius Agent Dashboard.
 */
data class RadiusUiState(
    // Visa & Admin
    val visaDaysRemaining: Int = 0,
    val workHoursUsed: Int = 0,
    val workHourLimit: Int = 20, // Default for student visa
    val advisorContact: String? = "International Students Office",
    val adminAlerts: List<String> = emptyList(),

    // Housing
    val housingStabilityScore: Int = 0,
    val utilityReadiness: Float = 0f,

    // Cultural / Signal Decoder
    val languageFluencyScore: Int = 0,
    val termsLearned: Int = 0,
    val totalTerms: Int = 50,



    // Local Scan
    val safeZoneAwareness: Float = 0f,
    val localKnowledgeScore: Int = 0,

    // Social
    val socialInteractionCount: Int = 0,
    val culturalComfort: Float = 0f,

    // Agent
    val isCalibrated: Boolean = false,
    val scamAlerts: Int = 0
)
