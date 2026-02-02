package com.mursaline.kaironex.features.zones.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ⚡ VITALITY ZONE COMPONENTS
 * ===========================
 * Bio-Fuel, Regen Mode, Resource Monitor
 *
 * Gamified wellness tracking (NO medical advice!)
 */

// =========================================================================
// BIO-FUEL COMPONENT
// =========================================================================

@Composable
fun BioFuelCard(
    energyLevel: Int,
    hydrationStatus: String,
    lastMealHours: Float,
    sleepHours: Float,
    caffeineIntake: Int,
    onLogMeal: () -> Unit,
    onLogWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔋", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Bio-Fuel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Energy & Nutrition Tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                // Energy percentage
                EnergyIndicator(level = energyLevel)
            }

            Spacer(Modifier.height(20.dp))

            // Energy Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Energy Level", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                    Text("$energyLevel%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { energyLevel / 100f },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = when {
                        energyLevel >= 70 -> Color(0xFF4CAF50)
                        energyLevel >= 40 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    trackColor = KaironexColors.CloudGray
                )
            }

            Spacer(Modifier.height(16.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(
                    icon = Icons.Default.LocalDrink,
                    label = "Hydration",
                    value = hydrationStatus,
                    color = Color(0xFF2196F3)
                )
                StatChip(
                    icon = Icons.Default.Restaurant,
                    label = "Last Meal",
                    value = "${lastMealHours.toInt()}h ago",
                    color = Color(0xFFFF9800)
                )
                StatChip(
                    icon = Icons.Default.Bedtime,
                    label = "Sleep",
                    value = "${sleepHours}h",
                    color = Color(0xFF9C27B0)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Caffeine tracker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("☕ Caffeine today: $caffeineIntake cups", style = MaterialTheme.typography.bodyMedium)
                if (caffeineIntake > 3) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Maybe slow down?",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onLogMeal,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restaurant, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Meal")
                }
                OutlinedButton(
                    onClick = onLogWater,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocalDrink, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Water")
                }
            }
        }
    }
}

@Composable
private fun EnergyIndicator(level: Int) {
    val color = when {
        level >= 70 -> Color(0xFF4CAF50)
        level >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Battery5Bar,
                null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$level%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
    }
}

// =========================================================================
// REGEN MODE COMPONENT
// =========================================================================

@Composable
fun RegenModeCard(
    status: String,
    nextBreakMinutes: Int,
    suggestedActivity: String,
    averageSleepHours: Float,
    recoveryScore: Int,
    onStartBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("♻️", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Regen Mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Rest & Recovery",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                Surface(
                    color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9C27B0),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Recovery score gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recovery Score", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                    Text(
                        "$recoveryScore/100",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            recoveryScore >= 70 -> Color(0xFF4CAF50)
                            recoveryScore >= 40 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Avg Sleep", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                    Text(
                        "${averageSleepHours}h/night",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Next break countdown
            Surface(
                color = KaironexColors.CloudGray,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Next Break", style = MaterialTheme.typography.labelMedium, color = KaironexColors.SlateGray)
                        Text(
                            "in $nextBreakMinutes min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Suggested: $suggestedActivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray
                        )
                    }

                    Button(
                        onClick = onStartBreak,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                    ) {
                        Text("Start Now")
                    }
                }
            }
        }
    }
}

// =========================================================================
// RESOURCE MONITOR COMPONENT
// =========================================================================

@Composable
fun ResourceMonitorCard(
    weeklyBudgetRemaining: Float,
    screenTimeToday: String,
    caffeineLimit: Int,
    currentCaffeine: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Resource Monitor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Track your daily limits",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Resource items
            ResourceItem(
                emoji = "💰",
                label = "Weekly Budget",
                value = "$${"%.2f".format(weeklyBudgetRemaining)} remaining",
                progress = weeklyBudgetRemaining / 150f, // Assuming $150 weekly budget
                color = Color(0xFF4CAF50)
            )

            Spacer(Modifier.height(12.dp))

            ResourceItem(
                emoji = "📱",
                label = "Screen Time",
                value = screenTimeToday,
                progress = 0.6f, // Would calculate from actual usage
                color = Color(0xFF2196F3)
            )

            Spacer(Modifier.height(12.dp))

            ResourceItem(
                emoji = "☕",
                label = "Caffeine Limit",
                value = "$currentCaffeine / $caffeineLimit cups",
                progress = currentCaffeine.toFloat() / caffeineLimit,
                color = if (currentCaffeine >= caffeineLimit) Color(0xFFF44336) else Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun ResourceItem(
    emoji: String,
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$emoji $label", style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = KaironexColors.CloudGray
        )
    }
}
