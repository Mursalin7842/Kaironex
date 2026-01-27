package com.mursaline.kaironex.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant
import com.mursaline.kaironex.features.dashboard.components.PressureMap
import com.mursaline.kaironex.features.dashboard.components.CortexHeroCard
import com.mursaline.kaironex.features.dashboard.components.CognitivePerformanceCard
import com.mursaline.kaironex.features.dashboard.components.LearningProgressCard
import com.mursaline.kaironex.features.dashboard.components.MentalStateCard
import com.mursaline.kaironex.features.dashboard.components.PressureRiskCard
import com.mursaline.kaironex.features.dashboard.components.ScheduledTasksCard
import com.mursaline.kaironex.features.dashboard.components.getSampleScheduledTasks
import com.mursaline.kaironex.features.dashboard.components.DriveIngestionCard
import com.mursaline.kaironex.features.dashboard.components.getSampleUploadedFiles
import com.mursaline.kaironex.features.zones.CortexState
import com.mursaline.kaironex.features.study.StudyRoomScreen
import com.mursaline.kaironex.core.stats.StatsProvider
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

/**
 * Dashboard Screen - "Daily Command Center"
 *
 * Philosophy:
 * - Home = Mind + Study + Pressure + Direction
 * - Stats should: Motivate, Expose Risk, Guide Action
 *
 * Answers:
 * - "Am I winning today or losing today?"
 * - "Am I improving?"
 * - "What should I do next?"
 */
@Suppress("unused")
object DashboardScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = DashboardScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Get comprehensive stats
        val homeStats = remember { StatsProvider.getHomeStats() }

        // Cortex State - derived from stats
        val cortexState by remember {
            mutableStateOf(
                CortexState(
                    currentSubject = "Data Structures",
                    currentTopic = homeStats.mentalState.aiInsight,
                    pressure = homeStats.pressure.pressureIndex / 100f,
                    upcomingDeadlines = homeStats.habits.nextDeadline?.daysRemaining ?: 0,
                    studyStreak = homeStats.habits.studyStreak,
                    conceptMastery = homeStats.learning.overallMastery,
                    isActive = true
                )
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = this.maxWidth < 800.dp

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
                    DashboardHeader(isMobile = isMobile, mentalState = homeStats.mentalState)

                    Spacer(Modifier.height(if (isMobile) 12.dp else 24.dp))

                    // ===== SECTION 1: THE HERO - CORTEX (Study Room) =====
                    Text(
                        text = "Current Focus",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(8.dp))

                    CortexHeroCard(
                        cortexState = cortexState,
                        onEnterFlow = { navigator.push(StudyRoomScreen) },
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                    // ===== SECTION 2: COGNITIVE PERFORMANCE (Mind State) =====
                    CognitivePerformanceCard(
                        stats = homeStats.cognitive,
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 3: LEARNING PROGRESS =====
                    LearningProgressCard(
                        stats = homeStats.learning,
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 4: MENTAL STATE & MOTIVATION =====
                    MentalStateCard(
                        stats = homeStats.mentalState,
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 5: PRESSURE & RISK =====
                    PressureRiskCard(
                        stats = homeStats.pressure,
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 6: SCHEDULED TASKS =====
                    ScheduledTasksCard(
                        tasks = getSampleScheduledTasks(),
                        isMobile = isMobile,
                        onTaskClick = { /* Navigate to task */ },
                        onAddTask = { /* Open add task dialog */ }
                    )

                    Spacer(Modifier.height(if (isMobile) 12.dp else 16.dp))

                    // ===== SECTION 7: DRIVE INGESTION / FILE UPLOAD =====
                    DriveIngestionCard(
                        files = getSampleUploadedFiles(),
                        isDriveConnected = false, // TODO: Get from user settings
                        isMobile = isMobile,
                        onConnectDrive = { /* Connect to Google Drive */ },
                        onUploadFiles = { /* Open file picker */ },
                        onFileClick = { /* Open file viewer */ }
                    )

                    Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                    // ===== SECTION 8: PRESSURE MAP VISUALIZATION =====
                    Text(
                        text = if (isMobile) "Pressure Timeline" else "Weekly Pressure Overview",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(if (isMobile) 6.dp else 12.dp))

                    // Inject Agent to get Live Profile for the Judge View
                    val agent: com.mursaline.kaironex.agents.genesis.GenesisAgent = org.koin.compose.koinInject()

                    KxCard(
                        variant = KxCardVariant.Flat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isMobile) 100.dp else 160.dp),
                        onClick = { navigator.push(com.mursaline.kaironex.features.dashboard.JudgeDashboardScreen(agent.profile)) },
                        backgroundColor = KaironexColors.CanvasWhite
                    ) {
                        PressureMap(modifier = Modifier.fillMaxSize(), isMobile = isMobile)
                    }

                    // Extra bottom spacing for navbar
                    Spacer(Modifier.height(if (isMobile) 120.dp else 48.dp))
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
