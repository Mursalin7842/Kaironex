package com.mursaline.kaironex.features.zones

import androidx.compose.ui.graphics.Color

/**
 * ============================================================
 * KAIRONEX: THE STUDENT LIFE OPERATING SYSTEM
 * ============================================================
 *
 * 4-Zone Architecture:
 * 1. CORTEX   - Study & Deep Work (The Hero Card on Dashboard)
 * 2. CAMPAIGN - Career & Growth (Strategic career development)
 * 3. VITALITY - Food & Finance (Resource management)
 * 4. RADIUS   - Habitat & Culture (Local integration)
 *
 * Each zone contains specialized Agents that handle specific tasks.
 */

/**
 * The 3 Life Support Tracks (excluding Cortex which is the Hero)
 * These handle "Life" so the student can focus on "Study" in the Cortex.
 */
enum class LifeTrack(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color,
    val description: String,
    val tagline: String
) {
    // ===== 1. THE CAMPAIGN (Growth & Career Zone) =====
    Campaign(
        title = "The Campaign",
        subtitle = "Strategy Agent",
        emoji = "🚀",
        color = Color(0xFF5E35B1), // Deep Purple
        description = "Career Roadmaps, Skill Trees, and Job Quests.",
        tagline = "Your career isn't a job—it's a strategic campaign you're leveling up in."
    ),

    // ===== 2. THE VITALITY (Sustenance & Resource Zone) =====
    Vitality(
        title = "The Vitality",
        subtitle = "Resource Agent",
        emoji = "⚡",
        color = Color(0xFF00897B), // Teal
        description = "Food Inventory, Budget Monitoring, and Health.",
        tagline = "Managing the two finite resources: Energy (Food) and Gold (Finance)."
    ),

    // ===== 3. THE RADIUS (Habitat & Assimilation Zone) =====
    Radius(
        title = "The Radius",
        subtitle = "Local Agent",
        emoji = "📡",
        color = Color(0xFF1E88E5), // Blue
        description = "Housing, Culture, Transport, and Legal help.",
        tagline = "Everything within your physical radius—city, culture, and living space."
    )
}

/**
 * Sub-features for each Life Track
 * Each track has multiple specialized tools/agents
 */

// ===== CAMPAIGN SUB-FEATURES =====
enum class CampaignFeature(
    val title: String,
    val description: String,
    val emoji: String
) {
    SkillTree(
        title = "The Skill Tree",
        description = "Brain-generated roadmap that tells you exactly which skills you need next to level up.",
        emoji = "🌳"
    ),
    QuestBoard(
        title = "Quest Board",
        description = "Intelligent job aggregator that searches for jobs tailored exactly to your setup and constraints.",
        emoji = "📋"
    ),
    Armory(
        title = "The Armory",
        description = "AI Resume Writer & ATS Scanner. Provide a Job Description and the Brain optimizes your armor to match.",
        emoji = "🛡️"
    ),
    Simulacrum(
        title = "Simulacrum",
        description = "Mock Interview with a Live AI Agent (Webview). Choose your interviewer's mood and face generated scenarios.",
        emoji = "🎭"
    )
}

// ===== VITALITY SUB-FEATURES =====
enum class VitalityFeature(
    val title: String,
    val description: String,
    val emoji: String
) {
    BioFuel(
        title = "Bio-Fuel System",
        description = "Smart Food management. Point camera at fridge to auto-list inventory. Discount Radar shows Happy Hour deals near you.",
        emoji = "🥗"
    ),
    ResourceMonitor(
        title = "Resource Monitor",
        description = "'Runway' Meter shows days you can survive on current balance. Group Splitter for restaurant bills. Currency Link for exchange rates.",
        emoji = "💰"
    ),
    RegenMode(
        title = "Regen Mode",
        description = "Battery saver for your body. Calculates latest coffee time to still sleep 7 hours before your exam.",
        emoji = "🔋"
    )
}

// ===== RADIUS SUB-FEATURES =====
enum class RadiusFeature(
    val title: String,
    val description: String,
    val emoji: String
) {
    SignalDecoder(
        title = "Signal Decoder",
        description = "Slang Dictionary for local words not in textbooks. Etiquette Guide with Do's and Don'ts for your specific country.",
        emoji = "🗣️"
    ),
    Safehouse(
        title = "Safehouse",
        description = "Vibe Check analyzes rental listings for scams. Utility Setup guides for electricity, WiFi in your city.",
        emoji = "🏠"
    ),
    LocalScan(
        title = "Local Scan",
        description = "One-tap filter for nearest Pharmacy, Grocery, Laundromat. Relaxation Nodes for hidden parks and quiet cafes.",
        emoji = "🗺️"
    ),
    AdminProtocol(
        title = "Admin Protocol",
        description = "Visa Renewal countdown clock. Allowed Work Hours tracker so you don't break the law.",
        emoji = "📑"
    )
}

/**
 * Pressure Level for the Cortex (Study Room)
 * Based on cognitive load monitoring
 */
enum class PressureLevel(
    val label: String,
    val color: Color,
    val intensity: Float
) {
    Low("Routine", Color(0xFF4CAF50), 0.3f),
    Medium("Building", Color(0xFFFFA726), 0.6f),
    High("Critical", Color(0xFFEF5350), 0.9f),
    Peak("Overload", Color(0xFFB71C1C), 1.0f)
}

/**
 * Study Session State for the Cortex Hero Card
 */
data class CortexState(
    val currentSubject: String = "No Active Session",
    val currentTopic: String = "",
    val pressure: Float = 0.0f,
    val upcomingDeadlines: Int = 0,
    val studyStreak: Int = 0,
    val conceptMastery: Float = 0.0f,
    val isActive: Boolean = false
)

// ===== MARATHON AGENT UI STATE =====
/**
 * Visible state for Marathon Agents running in each zone.
 * This makes the autonomous agents VISIBLE to the user.
 */
data class MarathonAgentState(
    val agentId: String,
    val zone: LifeTrack,
    val objective: String,
    val status: MarathonUIStatus,
    val progress: Float, // 0.0 to 1.0
    val currentThought: String, // What the agent is "thinking"
    val toolCallCount: Int,
    val selfCorrectionCount: Int,
    val startTime: Long,
    val estimatedCompletion: Long,
    val lastUpdate: Long
)

enum class MarathonUIStatus {
    INITIALIZING,
    RUNNING,
    THINKING,
    EXECUTING_TOOL,
    SELF_CORRECTING,
    WAITING_FOR_DATA,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Thought Signature for UI display.
 * Shows the agent's reasoning chain to the user.
 */
data class ThoughtSignatureUI(
    val id: String,
    val timestamp: Long,
    val thought: String,
    val confidence: Float,
    val nextAction: String?,
    val correction: String? // If self-correcting
)

// ===== REAL-TIME TEACHER UI STATE =====
/**
 * State for the Real-Time Teacher session visible in UI.
 */
data class TeachingSessionUI(
    val sessionId: String,
    val topic: String,
    val isActive: Boolean,
    val duration: Long,
    val comprehensionScore: Float,
    val teacherEmotion: String, // "explaining", "encouraging", etc.
    val currentMessage: String,
    val adaptationCount: Int
)

// ===== ZONE QUICK STATS =====
/**
 * Quick stats for each zone shown on dashboard cards.
 */
data class ZoneQuickStats(
    val zone: LifeTrack,
    val activeAgents: Int,
    val completedTasks: Int,
    val pendingActions: Int,
    val lastActivity: Long,
    val healthScore: Float // Overall zone health
)

