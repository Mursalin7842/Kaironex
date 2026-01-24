package com.mursaline.kaironex.core.stats

import androidx.compose.ui.graphics.Color

/**
 * ============================================================
 * KAIRONEX STATS SYSTEM - PRODUCT-GRADE METRICS
 * ============================================================
 *
 * Philosophy:
 * - Home = Mind + Study + Pressure + Direction
 * - More = Life + Stability + Risk + Infrastructure
 *
 * Stats should:
 * 1. Motivate
 * 2. Expose risk
 * 3. Guide action
 */

// ========================================
// 🏠 HOME SCREEN STATS (Mind Layer)
// ========================================

/**
 * Focus & Cognitive Performance Metrics
 * These show BRAIN CONDITION, not just activity
 */
data class CognitiveStats(
    val focusScore: Int,                    // 0-100, color coded
    val focusTrend: TrendDirection,         // ↑ ↓ →
    val deepWorkMinutes: Int,               // Today's uninterrupted study
    val deepWorkTarget: Int,                // Target (e.g., 120 min)
    val distractionsBlocked: Int,           // Netflix, app exits, etc.
    val cognitiveLoad: CognitiveLoadLevel,  // Low / Optimal / Overloaded
    val mentalFatigueIndex: FatigueLevel,   // Low / Medium / High
    val retentionStrength: Float,           // 0-1, % remembered
    val learningEfficiency: Float           // output / time ratio
) {
    fun getFocusColor(): Color = when {
        focusScore >= 80 -> Color(0xFF4CAF50) // Green
        focusScore >= 50 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336)             // Red
    }
}

enum class TrendDirection(val symbol: String) {
    UP("↑"),
    DOWN("↓"),
    STABLE("→")
}

enum class CognitiveLoadLevel(val label: String, val color: Long) {
    LIGHT("Light", 0xFF4CAF50),
    BALANCED("Balanced", 0xFF2196F3),
    OVERLOADED("Overloaded", 0xFFF44336)
}

enum class FatigueLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

/**
 * Learning Progress Metrics
 * Makes Kaironex feel like a REAL learning OS
 */
data class LearningStats(
    val conceptMastery: Map<String, Float>,    // Subject -> % mastery
    val overallMastery: Float,                  // 0-1
    val knowledgeRetention: Float,              // From mock tests + recall
    val topicsCompletedToday: Int,
    val topicsCompletedThisWeek: Int,
    val weakAreas: List<WeakArea>,              // AI-detected gaps
    val mockTestAccuracyTrend: TrendDirection
)

data class WeakArea(
    val topic: String,
    val subject: String,
    val confidenceLevel: Float  // 0-1
)

/**
 * Schedule & Habit Metrics
 */
data class HabitStats(
    val studyStreak: Int,                // Days
    val longestStreak: Int,
    val sessionsPlanned: Int,
    val sessionsCompleted: Int,
    val sessionsSkipped: Int,
    val pomodoroSuccessRate: Float,      // 0-1
    val nextDeadline: Deadline?
)

data class Deadline(
    val title: String,
    val subject: String,
    val daysRemaining: Int,
    val hoursRemaining: Int,
    val urgencyLevel: UrgencyLevel
)

enum class UrgencyLevel(val color: Long) {
    LOW(0xFF4CAF50),
    MEDIUM(0xFFFF9800),
    HIGH(0xFFF44336),
    CRITICAL(0xFFB71C1C)
}

/**
 * Mental State & Motivation Metrics (Secret Weapon)
 * Makes the app emotionally intelligent
 */
data class MentalStateStats(
    val motivationLevel: Int,            // 0-100
    val motivationTrend: TrendDirection,
    val burnoutRisk: RiskLevel,
    val confidenceTrend: TrendDirection,
    val currentMode: StudyMode,
    val aiInsight: String                // One-liner guidance
)

enum class RiskLevel(val label: String, val color: Long) {
    LOW("Low", 0xFF4CAF50),
    MEDIUM("Medium", 0xFFFF9800),
    HIGH("High", 0xFFF44336)
}

enum class StudyMode(val label: String, val emoji: String) {
    DEEP("Deep Focus", "🎯"),
    FLOW("Flow State", "🌊"),
    LIGHT("Light Study", "📖"),
    RECOVERY("Recovery", "🧘"),
    BREAK("Break", "☕")
}

/**
 * Pressure & Risk Indicators (Life Interference Layer)
 * Your unique edge - the Prompt Gap philosophy
 */
data class PressureStats(
    val pressureIndex: Int,              // 0-100
    val pressureTrend: TrendDirection,
    val distractionRiskLevel: RiskLevel,
    val scheduleStability: Float,        // 0-1
    val deadlinePressure: Float,         // 0-1
    val burnoutProbability: Float,       // 0-1
    val crisisProximity: CrisisProximity,
    val lifeInterferenceRatio: Float     // Life stress vs study capacity
)

data class CrisisProximity(
    val examDays: Int?,
    val financialStressDays: Int?,
    val deadlineDays: Int?,
    val interviewDays: Int?
)

/**
 * Directional Stats (Purpose Layer)
 * Answers: "Where am I going?"
 */
data class DirectionStats(
    val goalAlignment: Float,            // 0-1
    val careerPathProgress: Float,       // 0-1
    val skillTreeProgress: Float,        // 0-1
    val milestoneDistance: Int,          // Days to next milestone
    val trajectoryStatus: TrajectoryStatus,
    val futureReadinessScore: Int        // 0-100
)

enum class TrajectoryStatus(val label: String, val color: Long) {
    RISING("Rising", 0xFF4CAF50),
    STABLE("Stable", 0xFF2196F3),
    DECLINING("Declining", 0xFFF44336)
}

// ========================================
// COMPOSITE HOME STATS
// ========================================

data class HomeStats(
    val cognitive: CognitiveStats,
    val learning: LearningStats,
    val habits: HabitStats,
    val mentalState: MentalStateStats,
    val pressure: PressureStats,
    val direction: DirectionStats
)

// ========================================
// 🧭 MORE SCREEN STATS (Life Support)
// ========================================

/**
 * 🚀 Campaign Agent Stats (Career & Growth)
 */
data class CampaignStats(
    // Core Metrics
    val applicationsSent: Int,
    val interviewsScheduled: Int,
    val interviewSuccessRate: Float,     // 0-1
    val skillsProgress: Float,           // 0-1
    val codingPracticeMinutes: Int,
    val resumeStrengthScore: Int,        // 0-100 AI

    // Smart AI Metrics
    val hiringProbability: Float,        // 0-1 AI predicted
    val marketFitScore: Float,           // 0-1 skills vs jobs
    val employerResponseRate: Float,     // 0-1

    // Psychological Metrics
    val interviewConfidence: Int,        // 0-100
    val faangReadinessScore: Int,        // 0-100

    // Status
    val agentStatus: AgentStatus,
    val riskAlerts: List<String>
)

/**
 * ⚡ Vitality Agent Stats (Food + Finance + Body)
 */
data class VitalityStats(
    // Financial Metrics
    val budgetRunwayDays: Int,
    val monthlyBurnRate: Float,          // Currency
    val savingsProgress: Float,          // 0-1
    val emergencyFundPercent: Float,     // 0-1

    // Nutrition Metrics
    val mealsPlanned: Int,
    val mealsSkipped: Int,
    val nutritionAdequacy: Float,        // 0-1
    val groceryEfficiency: Float,        // 0-1

    // Sleep & Health
    val sleepQualityScore: Int,          // 0-100
    val energyLevelToday: Int,           // 0-100
    val fatigueIndex: FatigueLevel,
    val focusBodyCorrelation: Float,     // -1 to 1

    // Burnout
    val burnoutRisk: RiskLevel,

    // Status
    val agentStatus: AgentStatus,
    val riskAlerts: List<String>
)

/**
 * 📡 Radius Agent Stats (Habitat + Social + Survival)
 */
data class RadiusStats(
    // Stability Metrics
    val visaDaysRemaining: Int?,
    val housingStabilityScore: Int,      // 0-100
    val utilityReadiness: Float,         // 0-1

    // Social Integration
    val socialInteractionCount: Int,     // This week
    val languageFluencyScore: Int,       // 0-100
    val culturalComfort: Float,          // 0-1

    // Safety Metrics
    val scamRiskAlerts: Int,
    val safeZoneAwareness: Float,        // 0-1
    val localKnowledgeScore: Int,        // 0-100

    // Status
    val agentStatus: AgentStatus,
    val riskAlerts: List<String>
)

enum class AgentStatus(val label: String, val emoji: String, val color: Long) {
    OPTIMAL("Optimal", "✅", 0xFF4CAF50),
    STABLE("Stable", "🟢", 0xFF8BC34A),
    ATTENTION("Needs Attention", "⚠️", 0xFFFF9800),
    CRITICAL("Critical", "🚨", 0xFFF44336)
}

/**
 * Life Stability Score (Composite)
 */
data class LifeStabilityScore(
    val overallScore: Int,               // 0-100
    val campaignContribution: Float,     // 0-1
    val vitalityContribution: Float,     // 0-1
    val radiusContribution: Float,       // 0-1
    val trend: TrendDirection
)

// ========================================
// 🧬 SYSTEM-LEVEL META METRICS
// ========================================

data class SystemMetaStats(
    val studentOperatingCapacity: Int,   // 0-100, cognitive capacity free
    val lifeToStudyInterference: Float,  // 0-1, life stress ratio
    val studyCapacityRemaining: Float,   // 0-1
    val aiAssistEffectiveness: AIEffectiveness,
    val behaviorConsistencyScore: Int,   // 0-100
    val disciplineReliabilityIndex: Int, // 0-100
    val selfControlStrength: Int,        // 0-100
    val decisionQualityScore: Int        // 0-100
)

data class AIEffectiveness(
    val focusImprovement: Float,         // e.g., +0.18 = 18%
    val learningSpeedBoost: Float,       // e.g., +0.22 = 22%
    val stressReduction: Float           // e.g., -0.41 = 41%
)

// ========================================
// COMPOSITE MORE STATS
// ========================================

data class MoreStats(
    val lifeStability: LifeStabilityScore,
    val campaign: CampaignStats,
    val vitality: VitalityStats,
    val radius: RadiusStats,
    val systemMeta: SystemMetaStats
)
