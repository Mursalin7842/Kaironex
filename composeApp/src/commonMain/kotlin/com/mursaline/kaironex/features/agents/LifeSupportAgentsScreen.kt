package com.mursaline.kaironex.features.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.features.zones.LifeTrack
import com.mursaline.kaironex.features.zones.ZoneDetailScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.core.stats.*
import cafe.adriel.voyager.koin.koinScreenModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * ============================================================
 * LIFE SUPPORT AGENTS SCREEN (MORE)
 * ============================================================
 *
 * This is the "Life Infrastructure Control Panel"
 * Answers: "Is my life supporting my study — or killing it?"
 *
 * Shows:
 * - Life Stability Score (composite)
 * - Campaign Agent (Career & Growth)
 * - Vitality Agent (Food + Finance + Body)
 * - Radius Agent (Habitat + Social + Survival)
 *
 * Each agent shows operational dashboards with:
 * - Mission status
 * - Progress bars
 * - Warnings
 * - Actionable next steps
 */
@Suppress("unused")
object LifeSupportAgentsScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = LifeSupportAgentsScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Inject ViewModel (Make sure to register in Koin module if needed, or just instantiate for now if strictly DI not set up for this specific VM yet)
        // assuming koinScreenModel is available.
        // If Koin fails, use remember for now: val viewModel = remember { AgentsViewModel() }
        // Inject ViewModel
        val viewModel = koinScreenModel<AgentsViewModel>()
        
        val stats by viewModel.stats.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Text(
                            "Life Command Center",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KaironexColors.CanvasWhite
                    )
                )

                // Removed Loading Animation as requested
                // Use empty stats if null to show structure immediately
                val validStats = stats ?: com.mursaline.kaironex.core.stats.MoreStats.EMPTY

                // Life Stability Score Header (Safe render)
                LifeStabilityHeader(validStats.lifeStability)

                // Agent Cards
                LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // System Meta Stats
                            item {
                                SystemMetaCard(validStats.systemMeta)
                            }

                            // Campaign Agent
                            item {
                                CampaignAgentCard(
                                    stats = validStats.campaign,
                                    onClick = { navigator.push(ZoneDetailScreen(LifeTrack.Campaign)) }
                                )
                            }

                            // Vitality Agent
                            item {
                                VitalityAgentCard(
                                    stats = validStats.vitality,
                                    onClick = { navigator.push(ZoneDetailScreen(LifeTrack.Vitality)) }
                                )
                            }

                            // Radius Agent
                            item {
                                RadiusAgentCard(
                                    stats = validStats.radius,
                                    onClick = { navigator.push(ZoneDetailScreen(LifeTrack.Radius)) }
                                )
                            }
                            
                            // Brain Dashboard
                             item {
                                BrainDashboardCard(
                                    onClick = { navigator.push(AgentDashboardScreen) }
                                )
                            }

                            item {
                                Spacer(Modifier.height(100.dp))
                            }
                            item {
                                Spacer(Modifier.height(100.dp))
                            }
                        }
            }
        }
    }
}

@Composable
private fun LifeStabilityHeader(stability: LifeStabilityScore) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Life Stability Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = KaironexColors.SlateGray
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${stability.overallScore}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stability.overallScore >= 70 -> Color(0xFF4CAF50)
                                stability.overallScore >= 40 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            }
                        )
                        Text(
                            "/100 ${stability.trend.symbol}",
                            style = MaterialTheme.typography.titleMedium,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                // Contribution breakdown
                Column(horizontalAlignment = Alignment.End) {
                    ContributionRow("🚀", "Career", stability.campaignContribution)
                    ContributionRow("⚡", "Vitality", stability.vitalityContribution)
                    ContributionRow("📡", "Radius", stability.radiusContribution)
                }
            }
        }
    }
}

@Composable
private fun ContributionRow(emoji: String, label: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.width(4.dp))
        Text(
            "$label: ${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray
        )
    }
}

@Composable
private fun SystemMetaCard(meta: SystemMetaStats) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.InkBlack,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🧬 System Intelligence",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetaStatItem(
                    value = "${meta.studentOperatingCapacity}%",
                    label = "SOC",
                    subLabel = "Operating Capacity"
                )
                MetaStatItem(
                    value = "${(meta.studyCapacityRemaining * 100).toInt()}%",
                    label = "Study Capacity",
                    subLabel = "Remaining"
                )
                MetaStatItem(
                    value = "${meta.disciplineReliabilityIndex}",
                    label = "Discipline",
                    subLabel = "Index"
                )
            }

            Spacer(Modifier.height(12.dp))

            // AI Effectiveness
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AIEffectItem("+${(meta.aiAssistEffectiveness.focusImprovement * 100).toInt()}%", "Focus")
                    AIEffectItem("+${(meta.aiAssistEffectiveness.learningSpeedBoost * 100).toInt()}%", "Speed")
                    AIEffectItem("-${(meta.aiAssistEffectiveness.stressReduction * 100).toInt()}%", "Stress")
                }
            }
        }
    }
}

@Composable
private fun MetaStatItem(value: String, label: String, subLabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.ElectricBlue
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
        Text(
            subLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun AIEffectItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.SuccessGreen
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CampaignAgentCard(stats: CampaignStats, onClick: () -> Unit) {
    AgentCard(
        emoji = "🚀",
        title = "Campaign",
        subtitle = "Career & Growth",
        status = stats.agentStatus,
        color = Color(0xFF5E35B1),
        onClick = onClick,
        content = {
            // Core metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AgentMetric("📨", "${stats.applicationsSent}", "Apps Sent")
                AgentMetric("🎤", "${stats.interviewsScheduled}", "Interviews")
                AgentMetric("📈", "${(stats.skillsProgress * 100).toInt()}%", "Skills")
            }

            Spacer(Modifier.height(12.dp))

            // AI Predictions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Hiring Probability",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
                Text(
                    "${(stats.hiringProbability * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.ElectricBlue
                )
            }

            LinearProgressIndicator(
                progress = { stats.hiringProbability },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF5E35B1),
                trackColor = KaironexColors.CloudGray
            )

            // Risk alerts
            if (stats.riskAlerts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                stats.riskAlerts.forEach { alert ->
                    Text(
                        "⚠️ $alert",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.AttentionOrange
                    )
                }
            }
        }
    )
}

@Composable
private fun VitalityAgentCard(stats: VitalityStats, onClick: () -> Unit) {
    AgentCard(
        emoji = "⚡",
        title = "Vitality",
        subtitle = "Food + Finance + Body",
        status = stats.agentStatus,
        color = Color(0xFF00897B),
        onClick = onClick,
        content = {
            // Core metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AgentMetric("💰", "${stats.budgetRunwayDays}d", "Runway")
                AgentMetric("😴", "${stats.sleepQualityScore}", "Sleep")
                AgentMetric("⚡", "${stats.energyLevelToday}", "Energy")
            }

            Spacer(Modifier.height(12.dp))

            // Burnout Risk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Burnout Risk",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(stats.burnoutRisk.color).copy(alpha = 0.15f)
                ) {
                    Text(
                        stats.burnoutRisk.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(stats.burnoutRisk.color)
                    )
                }
            }

            // Risk alerts
            if (stats.riskAlerts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                stats.riskAlerts.forEach { alert ->
                    Text(
                        "⚠️ $alert",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.AttentionOrange
                    )
                }
            }
        }
    )
}

@Composable
private fun RadiusAgentCard(stats: RadiusStats, onClick: () -> Unit) {
    AgentCard(
        emoji = "📡",
        title = "Radius",
        subtitle = "Habitat + Social + Survival",
        status = stats.agentStatus,
        color = Color(0xFF1E88E5),
        onClick = onClick,
        content = {
            // Core metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AgentMetric("📑", stats.visaDaysRemaining?.let { "${it}d" } ?: "N/A", "Visa")
                AgentMetric("🏠", "${stats.housingStabilityScore}", "Housing")
                AgentMetric("🗣️", "${stats.languageFluencyScore}", "Language")
            }

            Spacer(Modifier.height(12.dp))

            // Social & Safety
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Cultural Comfort",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
                Text(
                    "${(stats.culturalComfort * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.ElectricBlue
                )
            }

            LinearProgressIndicator(
                progress = { stats.culturalComfort },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF1E88E5),
                trackColor = KaironexColors.CloudGray
            )

            // Risk alerts
            if (stats.riskAlerts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                stats.riskAlerts.forEach { alert ->
                    Text(
                        "⚠️ $alert",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.AttentionOrange
                    )
                }
            }
        }
    )
}

@Composable
private fun AgentCard(
    emoji: String,
    title: String,
    subtitle: String,
    status: AgentStatus,
    color: Color,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(color, color.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.InkBlack
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${status.emoji} ${status.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(status.color)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun AgentMetric(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray
        )
    }
}

@Composable
private fun BrainDashboardCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = KaironexColors.Slate900
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brain icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                KaironexColors.GeminiBlurple,
                                KaironexColors.Indigo900
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Agent Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Real-time brain monitoring • Judge Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.Slate400
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = KaironexColors.Slate400
            )
        }
    }
}

