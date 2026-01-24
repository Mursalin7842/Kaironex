package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.core.stats.*
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * HOME SCREEN STAT CARDS
 * ============================================================
 *
 * Product-grade metrics that answer:
 * - "Am I winning today or losing today?"
 * - "Am I improving?"
 * - "What should I do next?"
 */

// ========================================
// 🧠 COGNITIVE PERFORMANCE CARD
// ========================================

@Composable
fun CognitivePerformanceCard(
    stats: CognitiveStats,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🧠 Mind State",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                // Cognitive Load Badge
                CognitiveLoadBadge(stats.cognitiveLoad)
            }

            Spacer(Modifier.height(12.dp))

            // Focus Score (Hero Metric)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Big Focus Score
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stats.focusScore}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = stats.getFocusColor()
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stats.focusTrend.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        color = when (stats.focusTrend) {
                            TrendDirection.UP -> Color(0xFF4CAF50)
                            TrendDirection.DOWN -> Color(0xFFF44336)
                            TrendDirection.STABLE -> KaironexColors.SlateGray
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Focus Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                // Deep Work Progress
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${stats.deepWorkMinutes}/${stats.deepWorkTarget} min",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.ElectricBlue
                    )
                    Text(
                        "Deep Work",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Secondary metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("🛡️", "${stats.distractionsBlocked}", "Blocked")
                MiniStat("📊", "${(stats.retentionStrength * 100).toInt()}%", "Retention")
                MiniStat("⚡", "${(stats.learningEfficiency * 100).toInt()}%", "Efficiency")
            }
        }
    }
}

@Composable
private fun CognitiveLoadBadge(level: CognitiveLoadLevel) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(level.color).copy(alpha = 0.15f)
    ) {
        Text(
            text = level.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(level.color)
        )
    }
}

// ========================================
// 📚 LEARNING PROGRESS CARD
// ========================================

@Composable
fun LearningProgressCard(
    stats: LearningStats,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp)) {
            Text(
                "📚 Learning Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )

            Spacer(Modifier.height(12.dp))

            // Overall Mastery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Concept Mastery",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.SlateGray
                )
                Text(
                    "${(stats.overallMastery * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.ElectricBlue
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { stats.overallMastery },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = KaironexColors.ElectricBlue,
                trackColor = KaironexColors.CloudGray
            )

            Spacer(Modifier.height(12.dp))

            // Topics stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("📖", "${stats.topicsCompletedToday}", "Today")
                MiniStat("📅", "${stats.topicsCompletedThisWeek}", "This Week")
                MiniStat("🧠", "${(stats.knowledgeRetention * 100).toInt()}%", "Retention")
            }

            // Weak areas (if any)
            if (stats.weakAreas.isNotEmpty() && !isMobile) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "⚠️ Focus Areas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.AttentionOrange
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.weakAreas.take(3).forEach { area ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = KaironexColors.AttentionOrange.copy(alpha = 0.1f)
                        ) {
                            Text(
                                area.topic,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = KaironexColors.AttentionOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================================
// 🎯 MENTAL STATE & MOTIVATION CARD
// ========================================

@Composable
fun MentalStateCard(
    stats: MentalStateStats,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🎯 Mental State",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                // Current Mode Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KaironexColors.GeminiBlurple.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${stats.currentMode.emoji} ${stats.currentMode.label}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.GeminiBlurple
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${stats.motivationLevel}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.ElectricBlue
                    )
                    Text(
                        "Motivation",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SlateGray
                    )
                }

                // Burnout Risk
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stats.burnoutRisk.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(stats.burnoutRisk.color)
                    )
                    Text(
                        "Burnout Risk",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SlateGray
                    )
                }

                // Confidence
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stats.confidenceTrend.symbol,
                        style = MaterialTheme.typography.headlineSmall,
                        color = when (stats.confidenceTrend) {
                            TrendDirection.UP -> Color(0xFF4CAF50)
                            TrendDirection.DOWN -> Color(0xFFF44336)
                            TrendDirection.STABLE -> KaironexColors.SlateGray
                        }
                    )
                    Text(
                        "Confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            // AI Insight
            if (!isMobile) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KaironexColors.GeminiBlurple.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stats.aiInsight,
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.InkBlack
                        )
                    }
                }
            }
        }
    }
}

// ========================================
// ⚡ PRESSURE & RISK CARD
// ========================================

@Composable
fun PressureRiskCard(
    stats: PressureStats,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⚡ Pressure Index",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )

                // Pressure Score with trend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stats.pressureIndex}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            stats.pressureIndex > 70 -> Color(0xFFF44336)
                            stats.pressureIndex > 40 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                    Text(
                        stats.pressureTrend.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        color = when (stats.pressureTrend) {
                            TrendDirection.UP -> Color(0xFFF44336) // Up is bad for pressure
                            TrendDirection.DOWN -> Color(0xFF4CAF50)
                            TrendDirection.STABLE -> KaironexColors.SlateGray
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Life Interference ratio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Life → Study Interference",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
                Text(
                    "${(stats.lifeInterferenceRatio * 100).toInt()}% → ${((1 - stats.lifeInterferenceRatio) * 100).toInt()}% capacity",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
            }

            Spacer(Modifier.height(8.dp))

            // Interference bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(KaironexColors.CloudGray)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(stats.lifeInterferenceRatio)
                            .fillMaxHeight()
                            .background(Color(0xFFFF9800))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1 - stats.lifeInterferenceRatio)
                            .fillMaxHeight()
                            .background(KaironexColors.ElectricBlue)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Crisis proximity
            if (!isMobile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stats.crisisProximity.examDays?.let {
                        CrisisChip("📝 Exam: ${it}d", it <= 7)
                    }
                    stats.crisisProximity.deadlineDays?.let {
                        CrisisChip("📅 Deadline: ${it}d", it <= 3)
                    }
                    stats.crisisProximity.interviewDays?.let {
                        CrisisChip("🎤 Interview: ${it}d", it <= 7)
                    }
                }
            }
        }
    }
}

@Composable
private fun CrisisChip(text: String, isUrgent: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isUrgent) Color(0xFFF44336).copy(alpha = 0.1f)
               else KaironexColors.CloudGray
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isUrgent) Color(0xFFF44336) else KaironexColors.SlateGray
        )
    }
}

// ========================================
// HELPER COMPONENTS
// ========================================

@Composable
fun MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray
        )
    }
}
