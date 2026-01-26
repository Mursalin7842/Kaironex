package com.mursaline.kaironex.core.gemini

import kotlinx.serialization.Serializable

// ================= GEMINI MODEL MAPPING =================
object GeminiModels {
    // Live API (Audio/Video Native) - The "Eyes and Ears"
    const val LIVE_SMART_VOICE = "models/gemini-2.5-flash-native-audio-preview-12-2025"
    
    // Main Brain (Reasoning/Text) - The "Mind"
    // Using 1.5 Pro as the current "Smartest" stable model. 
    // (User requested 'Gemini 3', putting the best available logic model here)
    const val MAIN_BRAIN = "models/gemini-1.5-pro"
}

// ================= MARATHON AGENT MODELS =================

@Serializable
data class MarathonTask(
    val id: String,
    val zone: String,
    val objective: String,
    val startTime: Long,
    val estimatedDuration: Long,
    val status: MarathonStatus,
    val thoughtSignatures: List<ThoughtSignature> = emptyList(),
    val checkpoints: List<Checkpoint> = emptyList(),
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val humanInterventions: List<HumanIntervention> = emptyList()
)

enum class MarathonStatus {
    RUNNING, COMPLETED, PAUSED, FAILED
}

@Serializable
data class ThoughtSignature(
    val id: String,
    val taskId: String,
    val timestamp: Long,
    val thinkingLevel: ThinkingLevel,
    val currentState: String,
    val reasoningChain: List<ReasoningStep>,
    val confidenceScore: Float,
    val nextActions: List<PlannedAction>,
    val selfCorrectionLog: List<CorrectionEntry> = emptyList(),
    val contextTokensUsed: Int,
    val checkpointData: String = ""
)

enum class ThinkingLevel {
    FAST, MARATHON, DEEP
}

@Serializable
data class ReasoningStep(
    val stepNumber: Int,
    val thought: String,
    val evidence: List<String>,
    val confidence: Float,
    val alternatives: List<String> = emptyList()
)

@Serializable
data class PlannedAction(
    val actionType: ActionType,
    val description: String,
    val estimatedDuration: Long,
    val dependencies: List<String> = emptyList(),
    val verificationMethod: VerificationMethod
)

enum class ActionType {
    WEB_SEARCH, DOCUMENT_ANALYSIS, API_CALL, CODE_EXECUTION
}

enum class VerificationMethod {
    SELF_CHECK, TOOL_VALIDATION, NONE
}

@Serializable
data class Checkpoint(
    val id: String,
    val timestamp: Long,
    val stateSnapshot: String,
    val tokensUsed: Int,
    val progress: Float
)

@Serializable
data class ToolCallRecord(
    val toolName: String,
    val input: String,
    val output: String,
    val timestamp: Long,
    val success: Boolean,
    val verificationResult: String? = null
)

@Serializable
data class CorrectionEntry(
    val timestamp: Long,
    val originalPlan: String,
    val issue: String,
    val correction: String,
    val confidence: Float
)

@Serializable
data class HumanIntervention(
    val timestamp: Long,
    val type: InterventionType,
    val message: String,
    val agentResponse: String
)

enum class InterventionType {
    ERROR_ESCALATION, STRATEGY_SHIFT, APPROVAL_REQUEST
}

@Serializable
data class ContextWindow(
    val totalTokens: Int,
    val relevantHistory: List<String> = emptyList()
)

// ================= TEACHER AGENT MODELS =================

@Serializable
data class LiveTeachingSession(
    val sessionId: String,
    val studentId: String,
    val subject: String,
    val topic: String,
    val startTime: Long,
    val videoStreamActive: Boolean,
    val audioStreamActive: Boolean,
    val comprehensionMetrics: ComprehensionMetrics,
    val adaptations: List<TeachingAdaptation> = emptyList()
)

@Serializable
data class ComprehensionMetrics(
    val attentionScore: Float,
    val responseLatency: Long,
    val errorRate: Float,
    val questionFrequency: Float,
    val estimatedMastery: Float
)

@Serializable
data class TeachingAdaptation(
    val timestamp: Long,
    val trigger: String,
    val action: String,
    val result: String
)

@Serializable
data class ChatMessage(
    val sender: String, // "user" or "ai"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
