package com.mursaline.kaironex.features.agents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.brain.*
import kotlinx.coroutines.delay

/**
 * 🤖 AGENT DASHBOARD SCREEN
 * =========================
 * Real-time monitoring of the Kaironex Brain for demo/judge view.
 *
 * Shows:
 * - Brain status (ONLINE/PROCESSING/IDLE)
 * - Live activity feed (reasoning events)
 * - Current thought signature
 * - Active marathons progress
 * - Recent interventions
 *
 * This is the "Judge Mode" view for hackathon demos.
 */
@OptIn(ExperimentalMaterial3Api::class)
object AgentDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Brain state
        var brainStatus by remember { mutableStateOf(BrainStatus.ONLINE) }
        var reasoningMode by remember { mutableStateOf("REFLEX") }
        var pressureIndex by remember { mutableStateOf(45) }
        var lastActive by remember { mutableStateOf("2 min ago") }

        // Activity feed
        var activityFeed by remember { mutableStateOf(generateSampleActivity()) }

        // Current thought
        var currentThought by remember { mutableStateOf(SampleThought(
            id = "ts_20260203_0042",
            confidence = 0.92f,
            chainDepth = 5,
            agent = "campaign"
        )) }

        // Marathons
        var marathons by remember { mutableStateOf(listOf(
            SampleMarathon(
                id = "mar_001",
                title = "Week Planning",
                progress = 0.8f,
                eta = "5 min remaining",
                status = "RUNNING"
            )
        )) }

        // Interventions
        var interventions by remember { mutableStateOf(listOf(
            SampleIntervention(
                id = "int_001",
                message = "Time for a break?",
                status = "PENDING",
                trigger = "focus_timeout"
            ),
            SampleIntervention(
                id = "int_002",
                message = "Review notes from earlier?",
                status = "ACKNOWLEDGED",
                trigger = "spaced_repetition"
            )
        )) }

        // Simulate live activity
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000)
                val newActivity = ActivityItem(
                    timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()),
                    action = listOf("check_engage", "analyze_mood", "schedule_scan", "pattern_detect").random(),
                    detail = listOf("engagement=7", "mood=focused", "conflicts=0", "pattern=study").random(),
                    type = ActivityType.values().random()
                )
                activityFeed = listOf(newActivity) + activityFeed.take(19)

                // Randomly update other metrics
                if (Math.random() > 0.7) {
                    pressureIndex = (pressureIndex + (-5..5).random()).coerceIn(0, 100)
                }
                if (Math.random() > 0.8) {
                    reasoningMode = if (reasoningMode == "REFLEX") "DEEP" else "REFLEX"
                }
                lastActive = "just now"
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.Slate900)
        ) {
            // Top Bar
            TopAppBar(
                title = { Text("🤖 Agent Control Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KaironexColors.Slate900,
                    titleContentColor = Color.White
                )
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Brain Status Header
                item {
                    BrainStatusCard(
                        status = brainStatus,
                        mode = reasoningMode,
                        pressureIndex = pressureIndex,
                        lastActive = lastActive
                    )
                }

                // Live Activity Feed
                item {
                    LiveActivityFeed(activities = activityFeed)
                }

                // Current Thought
                item {
                    CurrentThoughtCard(thought = currentThought)
                }

                // Active Marathons
                item {
                    ActiveMarathonsCard(marathons = marathons)
                }

                // Recent Interventions
                item {
                    RecentInterventionsCard(interventions = interventions)
                }

                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// =========================================================================
// COMPONENTS
// =========================================================================

@Composable
private fun BrainStatusCard(
    status: BrainStatus,
    mode: String,
    pressureIndex: Int,
    lastActive: String
) {
    val statusColor = when (status) {
        BrainStatus.ONLINE -> Color(0xFF4CAF50)
        BrainStatus.PROCESSING -> Color(0xFFFF9800)
        BrainStatus.IDLE -> Color(0xFF9E9E9E)
        BrainStatus.ERROR -> Color(0xFFF44336)
    }

    // Pulse animation for status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = pulseAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "BRAIN STATUS: ${status.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Mode badge
                Surface(
                    color = if (mode == "DEEP") KaironexColors.GeminiBlurple else KaironexColors.ElectricBlue,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = mode,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Pressure Index", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    Text("$pressureIndex/100", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Active", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    Text(lastActive, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Pressure bar
            LinearProgressIndicator(
                progress = { pressureIndex / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = when {
                    pressureIndex < 40 -> Color(0xFF4CAF50)
                    pressureIndex < 70 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                trackColor = KaironexColors.Slate700
            )
        }
    }
}

@Composable
private fun LiveActivityFeed(activities: List<ActivityItem>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📡 LIVE ACTIVITY FEED",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Live indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "live")
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = dotAlpha))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Activity list (terminal style)
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(activities) { activity ->
                        Row {
                            Text(
                                text = activity.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = KaironexColors.Slate500
                            )
                            Text(" → ", color = KaironexColors.Slate600, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${activity.action}: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = when (activity.type) {
                                    ActivityType.DETECT -> Color(0xFF64B5F6)
                                    ActivityType.ACTION -> Color(0xFF81C784)
                                    ActivityType.RESULT -> Color(0xFFFFD54F)
                                    ActivityType.DECISION -> Color(0xFFBA68C8)
                                }
                            )
                            Text(
                                text = activity.detail,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentThoughtCard(thought: SampleThought) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🧠 CURRENT THOUGHT",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ID", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    Text(thought.id, style = MaterialTheme.typography.bodyMedium, color = KaironexColors.ElectricBlue, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Confidence", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    Text("${(thought.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Chain Depth", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    Text("${thought.chainDepth} thoughts", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Agent", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    Text(thought.agent.uppercase(), style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* View chain */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KaironexColors.ElectricBlue)
            ) {
                Text("View Full Chain →")
            }
        }
    }
}

@Composable
private fun ActiveMarathonsCard(marathons: List<SampleMarathon>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🏃 ACTIVE MARATHONS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            if (marathons.isEmpty()) {
                Text("No active marathons", color = KaironexColors.Slate400)
            } else {
                marathons.forEach { marathon ->
                    Surface(
                        color = KaironexColors.Slate700,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(marathon.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = if (marathon.status == "RUNNING") Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        marathon.status,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { marathon.progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = KaironexColors.GeminiBlurple,
                                trackColor = KaironexColors.Slate600
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${(marathon.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                                Text("ETA: ${marathon.eta}", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentInterventionsCard(interventions: List<SampleIntervention>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🔔 RECENT INTERVENTIONS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(Modifier.height(12.dp))

            interventions.forEach { intervention ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(intervention.message, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Text("Trigger: ${intervention.trigger}", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate400)
                    }

                    Surface(
                        color = when (intervention.status) {
                            "PENDING" -> Color(0xFFFF9800)
                            "ACKNOWLEDGED" -> Color(0xFF4CAF50)
                            else -> KaironexColors.Slate600
                        }.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            intervention.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (intervention.status) {
                                "PENDING" -> Color(0xFFFF9800)
                                "ACKNOWLEDGED" -> Color(0xFF4CAF50)
                                else -> KaironexColors.Slate400
                            }
                        )
                    }
                }

                if (intervention != interventions.last()) {
                    HorizontalDivider(color = KaironexColors.Slate700, modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { /* View all */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View All →", color = KaironexColors.ElectricBlue)
            }
        }
    }
}

// =========================================================================
// DATA MODELS
// =========================================================================

enum class BrainStatus {
    ONLINE, PROCESSING, IDLE, ERROR
}

enum class ActivityType {
    DETECT, ACTION, RESULT, DECISION
}

data class ActivityItem(
    val timestamp: String,
    val action: String,
    val detail: String,
    val type: ActivityType
)

data class SampleThought(
    val id: String,
    val confidence: Float,
    val chainDepth: Int,
    val agent: String
)

data class SampleMarathon(
    val id: String,
    val title: String,
    val progress: Float,
    val eta: String,
    val status: String
)

data class SampleIntervention(
    val id: String,
    val message: String,
    val status: String,
    val trigger: String
)

private fun generateSampleActivity(): List<ActivityItem> {
    val actions = listOf(
        "Detected: user_idle" to ActivityType.DETECT,
        "Action: check_engage" to ActivityType.ACTION,
        "Result: engagement=7" to ActivityType.RESULT,
        "Decision: no_nudge" to ActivityType.DECISION,
        "Detected: life_event" to ActivityType.DETECT,
        "Processing: wedding" to ActivityType.ACTION,
        "Updated: schedule" to ActivityType.RESULT
    )

    return actions.mapIndexed { index, (text, type) ->
        val parts = text.split(": ")
        ActivityItem(
            timestamp = "12:4${5 + index}:0${3 + index}",
            action = parts[0],
            detail = parts.getOrElse(1) { "" },
            type = type
        )
    }
}
