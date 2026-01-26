package com.mursaline.kaironex.agents.core

import com.mursaline.kaironex.agents.genesis.StudentProfile

/**
 * THE UNIVERSAL HANDOFF PROTOCOL
 * This ensures that when Genesis finishes, Cortex and Vitality 
 * receive valid, typed data, not just loose strings.
 */
interface AgentContext {
    val studentProfile: StudentProfile
    val confidenceScore: Float // Global data reliability score
}

// Wrapper for data with confidence (The "Epistemic Layer")
@Suppress("unused")
data class ExtractedField<T>(
    val value: T,
    val confidence: Float = 1.0f, // 0.0 to 1.0
    val source: String = "User Interview"
) {
    fun isReliable(): Boolean = confidence > 0.7f
}
