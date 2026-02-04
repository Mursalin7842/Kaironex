package com.mursaline.kaironex.features.agents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mursaline.kaironex.core.KaironexSessionManager
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 🤖 AGENT DASHBOARD SCREEN
 * =========================
 * Real-time monitoring of the Kaironex Brain.
 * NOW WITH REAL DATA. NO MOCKS.
 */
@OptIn(ExperimentalMaterial3Api::class)
object AgentDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val brainClient = koinInject<BrainApiClient>()
        val sessionManager = koinInject<KaironexSessionManager>()
        val scope = rememberCoroutineScope()

        // 1. Connection State
        val connectionState by brainClient.connectionState.collectAsState()
        
        // 2. Stats for Pressure Index
        val repo = sessionManager.getStatsRepository()
        val homeStats by repo?.homeStats?.collectAsState() ?: mutableStateOf(null)
        val pressureIndex = homeStats?.pressure?.pressureIndex ?: 0

        // 3. Current Thought
        val wsThought by brainClient.thoughtStream.collectAsState()
        var polledThought by remember { mutableStateOf<ThoughtStreamItem?>(null) }
        val currentThought = polledThought ?: wsThought

        // 4. Activity Feed (Accumulate events)
        var activityFeed by remember { mutableStateOf(listOf<ActivityItem>()) }
        
        // Listen to Brain Events
        LaunchedEffect(Unit) {
            brainClient.brainEvents.collectLatest { event ->
                val newItem = ActivityItem(
                    timestamp = event.timestamp.takeIf { it.isNotEmpty() } ?: "Now",
                    action = event.eventType,
                    detail = event.data.toString().take(50),
                    type = when(event.eventType) {
                        "detection" -> ActivityType.DETECT
                        "action" -> ActivityType.ACTION
                        "result" -> ActivityType.RESULT
                        else -> ActivityType.DECISION
                    }
                )
                activityFeed = (listOf(newItem) + activityFeed).take(50)
            }
        }

        // Polling Fallback
        LaunchedEffect(Unit) {
            while (isActive) {
                repo?.refreshHomeStats()
                val thought = repo?.getLatestThought()
                if (thought != null) polledThought = thought
                delay(4000)
            }
        }

        // 5. Marathons (Accumulate updates)
        var marathons by remember { mutableStateOf(mapOf<String, MarathonSessionState>()) }
        
        LaunchedEffect(Unit) {
            brainClient.marathonUpdates.collectLatest { update ->
                val current = marathons[update.sessionId]
                val newState = current?.copy(
                    progress = update.progress,
                    status = update.status
                ) ?: MarathonSessionState(
                    id = update.sessionId,
                    title = "Marathon ${update.sessionId.take(4)}", // Placeholder title until we have real ones
                    progress = update.progress,
                    status = update.status,
                    eta = "Calculating..."
                )
                marathons = marathons + (update.sessionId to newState)
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
                    IconButton(onClick = { 
                        scope.launch { repo?.refreshAll() }
                    }) {
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
                        status = if (polledThought != null) BrainConnectionState.CONNECTED else connectionState,
                        pressureIndex = pressureIndex
                    )
                }

                // Live Activity Feed
                item {
                    LiveActivityFeed(activities = activityFeed)
                }

                // Current Thought
                item {
                    if (currentThought != null) {
                        CurrentThoughtCard(thought = currentThought!!)
                    } else {
                        Text("Waiting for thoughts...", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                }

                // Active Marathons
                item {
                    if (marathons.isNotEmpty()) {
                        ActiveMarathonsCard(marathons = marathons.values.toList())
                    }
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
    status: BrainConnectionState,
    pressureIndex: Int
) {
    val statusColor = when (status) {
        BrainConnectionState.CONNECTED -> Color(0xFF4CAF50)
        BrainConnectionState.CONNECTING -> Color(0xFFFF9800)
        BrainConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
        BrainConnectionState.ERROR -> Color(0xFFF44336)
    }

    // Pulse animation
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
private fun CurrentThoughtCard(thought: ThoughtStreamItem) {
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

            Text(
                text = thought.thought,
                style = MaterialTheme.typography.bodyLarge,
                color = KaironexColors.ElectricBlue,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Agent: ${thought.agent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "Conf: ${(thought.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ActiveMarathonsCard(marathons: List<MarathonSessionState>) {
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

            marathons.forEach { marathon ->
                Surface(
                    color = KaironexColors.Slate700,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(marathon.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text(marathon.status, style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                        
                        LinearProgressIndicator(
                            progress = { marathon.progress },
                            modifier = Modifier.fillMaxWidth().padding(top=8.dp),
                            color = KaironexColors.GeminiBlurple
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// DATA MODELS
// =========================================================================

enum class ActivityType {
    DETECT, ACTION, RESULT, DECISION
}

data class ActivityItem(
    val timestamp: String,
    val action: String,
    val detail: String,
    val type: ActivityType
)

data class MarathonSessionState(
    val id: String,
    val title: String,
    val progress: Float,
    val status: String,
    val eta: String
)
