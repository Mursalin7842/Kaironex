package com.mursaline.kaironex.features.zones

import androidx.compose.ui.graphics.Color

/**
 * The 4 Life Support Tracks
 * These agents handle the "Life" part so the student can focus on studying.
 * Based on the "Prompt Gap" thesis: Life and study collide - these tracks
 * handle life (Jobs, Food, Finance) so the student *can* study.
 */
enum class LifeTrack(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val color: Color,
    val description: String
) {
    Marathon(
        title = "Career & Food",
        subtitle = "Marathon Agent",
        emoji = "🏃",
        color = Color(0xFF5E35B1), // Deep Purple
        description = "Long-term career planning and meal prep automation"
    ),
    RealTime(
        title = "Teacher & Chef",
        subtitle = "RealTime Agent",
        emoji = "🎙️",
        color = Color(0xFF00897B), // Teal
        description = "Live tutoring sessions and cooking guidance"
    ),
    VibeCheck(
        title = "Finance & Resume",
        subtitle = "Vibe Check Agent",
        emoji = "✅",
        color = Color(0xFF1E88E5), // Blue
        description = "Budget tracking and resume optimization"
    ),
    Creative(
        title = "Brand & Design",
        subtitle = "Creative Agent",
        emoji = "🎨",
        color = Color(0xFFD81B60), // Pink
        description = "Personal branding and portfolio design"
    )
}

/**
 * Pressure Level for the Cortex (Study Room)
 * Based on the cognitive load monitoring from the motivation document
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
