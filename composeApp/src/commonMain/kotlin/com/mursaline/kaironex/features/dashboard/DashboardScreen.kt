package com.mursaline.kaironex.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.core.stats.*
import com.mursaline.kaironex.features.dashboard.components.*
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant
import com.mursaline.kaironex.features.dashboard.components.CortexHeroCard
import com.mursaline.kaironex.features.dashboard.components.CognitivePerformanceCard
import com.mursaline.kaironex.features.dashboard.components.LearningProgressCard
import com.mursaline.kaironex.features.dashboard.components.MentalStateCard
import com.mursaline.kaironex.features.dashboard.components.PressureRiskCard
import com.mursaline.kaironex.features.dashboard.components.ScheduledTasksCard
import com.mursaline.kaironex.features.dashboard.components.getSampleScheduledTasks

import com.mursaline.kaironex.features.zones.CortexState
import com.mursaline.kaironex.features.study.StudyRoomScreen
import com.mursaline.kaironex.core.stats.StatsProvider
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.mursaline.kaironex.brain.ThoughtStreamItem 
import com.mursaline.kaironex.core.KaironexSessionManager
import org.koin.compose.koinInject
import cafe.adriel.voyager.koin.koinScreenModel
import com.mursaline.kaironex.features.study.StudyViewModel
import com.mursaline.kaironex.features.dashboard.components.ScheduledTask
import com.mursaline.kaironex.features.dashboard.components.TaskStatus
import com.mursaline.kaironex.features.dashboard.components.TaskPriority
import com.mursaline.kaironex.features.dashboard.components.SemesterPlanCard
import com.mursaline.kaironex.features.study.MonthlyPlansScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward

/**
 * Dashboard Screen - "Daily Command Center"
 */
@Suppress("unused")
object DashboardScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = DashboardScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var hasAudioPermission by remember { mutableStateOf(false) }

        // Request Audio Permission on Entry
        com.mursaline.kaironex.core.EnsureAudioPermission {
            hasAudioPermission = true
        }

        // --- ViewModels ---
        val studyViewModel = koinScreenModel<StudyViewModel>()
        val dashboardViewModel = koinScreenModel<DashboardViewModel>() // New VM
        
        val schedule by studyViewModel.schedule.collectAsState()
        val homeStats by dashboardViewModel.homeStats.collectAsState()
        val isLoading by dashboardViewModel.isLoading.collectAsState()

        // Map schedule to Dashboard Tasks
        val dashboardTasks = remember(schedule) {
             schedule.take(5).map { task ->
                 val statusEnum = when(task.status.lowercase()) {
                    "completed" -> TaskStatus.COMPLETED
                    "active" -> TaskStatus.IN_PROGRESS
                    else -> TaskStatus.UPCOMING
                }
                 ScheduledTask(
                    id = task.id,
                    title = task.title,
                    subject = task.type.replaceFirstChar { it.titlecase() },
                    startTime = task.startTime.substringAfter("T").take(5), 
                    endTime = task.endTime.substringAfter("T").take(5),
                    duration = "60m", 
                    status = statusEnum
                 )
             }
        }

        // --- AGENT POLLING LOGIC ---
        val sessionManager = koinInject<KaironexSessionManager>()
        val repo = sessionManager.getStatsRepository()
        
        var polledThought by remember { mutableStateOf<ThoughtStreamItem?>(null) }
        var isBrainConnected by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while (isActive) {
                try {
                    val thought = repo?.getLatestThought()
                    if (thought != null) {
                        polledThought = thought
                        isBrainConnected = true
                    }
                } catch (e: Exception) { }
                delay(30000)
            }
        }

        // Cortex State - derived from stats
        val cortexState = remember(homeStats) {
            val stats = homeStats
            CortexState(
                currentSubject = "Data Structures",
                currentTopic = stats.mentalState.aiInsight,
                pressure = stats.pressure.pressureIndex / 100f,
                upcomingDeadlines = stats.habits.nextDeadline?.daysRemaining ?: 0,
                studyStreak = stats.habits.studyStreak,
                conceptMastery = stats.learning.overallMastery,
                isActive = true
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = this.maxWidth < 800.dp
            val currentStats = homeStats

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = this.maxWidth < 800.dp
            val currentStats = homeStats

            // REAL CONTENT (With Loading passed locally)
            Row(modifier = Modifier.fillMaxSize()) {
                // MAIN SCROLLABLE CONTENT
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(if (isMobile) 12.dp else 24.dp)
                ) {
                    // ===== HEADER =====
                    // Safe access or default
                    val headerStats = currentStats ?: HomeStats.EMPTY
                    DashboardHeader(isMobile = isMobile, mentalState = headerStats.mentalState)


                    Spacer(Modifier.height(if (isMobile) 12.dp else 24.dp))

                    // ===== AGENT PULSE (Live Brain Monitor) =====
                    KxCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = KxCardVariant.Elevated,
                        // Navigate to AgentDashboardScreen
                        onClick = { navigator.push(com.mursaline.kaironex.features.agents.AgentDashboardScreen) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🧠 Neural Pulse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    if (isBrainConnected) {
                                        KxBadge(text = "LIVE", variant = KxBadgeVariant.Success)
                                    } else {
                                        KxBadge(text = "CONNECTING", variant = KxBadgeVariant.Neutral)
                                    }
                                }
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "View",
                                    tint = KaironexColors.GeminiBlurple
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = polledThought?.thought?.take(150)?.let { "$it..." } ?: "Initializing neural link...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KaironexColors.SlateGray,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                            )
                        }
                    }

                    Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                    // ===== SECTION 1: THE HERO - CORTEX (Study Room) =====
                    Text(
                        text = "Current Focus",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(8.dp))

                    // ===== SECTION 1: THE HERO - CORTEX (Study Room) =====


                    CortexHeroCard(
                        cortexState = cortexState,
                        onEnterFlow = { navigator.push(StudyRoomScreen) },
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                    val displayStats = currentStats ?: HomeStats.EMPTY

                    // ===== SECTION 2: COGNITIVE PERFORMANCE (Mind State) =====
                    CognitivePerformanceCard(
                        stats = displayStats.cognitive,
                        isMobile = isMobile,
                        isLoading = isLoading
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 3: LEARNING PROGRESS =====
                    LearningProgressCard(
                        stats = displayStats.learning,
                        isMobile = isMobile,
                        isLoading = isLoading
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 4: MENTAL STATE & MOTIVATION =====
                    MentalStateCard(
                        stats = displayStats.mentalState,
                        isMobile = isMobile,
                        isLoading = isLoading
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // Voice Trigger Handling
                    val sessionManager = org.koin.compose.koinInject<com.mursaline.kaironex.core.KaironexSessionManager>()
                    val voiceTrigger by sessionManager.voiceTriggerRequest.collectAsState()
                    
                    LaunchedEffect(voiceTrigger) {
                        if (voiceTrigger) {
                            // navigator.push(com.mursaline.kaironex.features.voice.VoiceCallScreen())
                        }
                    }

                    // ===== SECTION 5: PRESSURE & RISK =====
                    PressureRiskCard(
                        stats = displayStats.pressure,
                        isMobile = isMobile,
                        isLoading = isLoading
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 6: SCHEDULED TASKS & SEMESTER PLAN =====
                    ScheduledTasksCard(
                        tasks = dashboardTasks,
                        isMobile = isMobile,
                        onTaskClick = { /* Navigate to task */ },
                        onAddTask = { /* Open add task dialog */ },
                        onViewAllClick = { navigator.push(com.mursaline.kaironex.features.study.StudySessionsScreen) }
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    SemesterPlanCard(
                        modifier = Modifier.fillMaxWidth(),
                        currentMonth = "February 2026", // Dynamic later
                        status = "On Track",
                        onClick = { navigator.push(MonthlyPlansScreen) }
                    )

                    Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                    // ===== SECTION 8: PRESSURE METRICS =====
                    Text(
                        text = "Pressure Overview",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(if (isMobile) 6.dp else 12.dp))

                    // Pressure Row (Today, Week, Month)
                    PressureStatsRow(
                        dailyPressure = displayStats.pressure.pressureIndex / 100f,
                        weeklyPressure = 0.65f, // Mock for now, connect to stats later
                        monthlyPressure = 0.4f, // Mock for now
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(if (isMobile) 6.dp else 12.dp))

                    // Extra bottom spacing for navbar
                    Spacer(Modifier.height(if (isMobile) 120.dp else 48.dp))
                }
            }
        }
    }
}

    @Composable
    private fun DashboardHeader(
        isMobile: Boolean,
        mentalState: com.mursaline.kaironex.core.stats.MentalStateStats
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMobile) "Command Center" else "Daily Command Center",
                    style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${mentalState.currentMode.emoji} ${mentalState.currentMode.label}",
                        style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = KaironexColors.GeminiBlurple,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    KxBadge(
                        text = when {
                            mentalState.burnoutRisk == com.mursaline.kaironex.core.stats.RiskLevel.LOW -> "Optimal"
                            mentalState.burnoutRisk == com.mursaline.kaironex.core.stats.RiskLevel.MEDIUM -> "Caution"
                            else -> "At Risk"
                        },
                        variant = when {
                            mentalState.burnoutRisk == com.mursaline.kaironex.core.stats.RiskLevel.LOW -> KxBadgeVariant.Success
                            mentalState.burnoutRisk == com.mursaline.kaironex.core.stats.RiskLevel.MEDIUM -> KxBadgeVariant.Warning
                            else -> KxBadgeVariant.Error
                        }
                    )
                }
            }

            // Wake word indicator + Avatar/Profile indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wake word listening indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KaironexColors.SuccessGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(if (isMobile) 36.dp else 44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎙️", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Avatar/Profile indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KaironexColors.GeminiBlurple,
                    modifier = Modifier.size(if (isMobile) 36.dp else 44.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("S", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
