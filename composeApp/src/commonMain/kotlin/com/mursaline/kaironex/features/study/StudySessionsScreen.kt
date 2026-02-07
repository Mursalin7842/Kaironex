package com.mursaline.kaironex.features.study

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.theme.KaironexColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import cafe.adriel.voyager.koin.koinScreenModel
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import com.mursaline.kaironex.features.dashboard.components.*
import com.mursaline.kaironex.features.study.ScheduleRepository.ScheduleTask as DomainTask

/**
 * Study Sessions Screen
 *
 * Shows study blocks organized by:
 * - Current Session (if active)
 * - Upcoming Sessions
 * - Previous Sessions
 *
 * Accessed via the "Study" button in the navigation bar.
 */
@Suppress("unused")
object StudySessionsScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = StudySessionsScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<StudyViewModel>()
        
        var selectedTab by remember { mutableStateOf(0) }
        val schedule by viewModel.schedule.collectAsState()
        val isGenerating by viewModel.isGenerating.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState() // Observe loading state
        val resources by viewModel.resources.collectAsState()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KaironexColors.CloudGray)
            ) {
                // Top App Bar
                TopAppBar(
                    title = { Text("Study Room", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        // Refresh Button
                        IconButton(onClick = { 
                            val topResources = resources.take(5).map { it.id }
                            if (topResources.isNotEmpty()) {
                                viewModel.generateSchedule(topResources, 15)
                            } else {
                                viewModel.loadSchedule() // Just refresh if no resources
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KaironexColors.CanvasWhite)
                )

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = KaironexColors.CanvasWhite,
                    contentColor = KaironexColors.ElectricBlue
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Current") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Upcoming") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Previous") })
                }

                // Content
                if (isLoading || isGenerating) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = KaironexColors.ElectricBlue)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (isGenerating) "Designing Strategy..." else "Syncing Profile...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KaironexColors.SlateGray
                            )
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> CurrentSessionContent(onStartSession = { navigator.push(StudyRoomScreen) })
                        1 -> UpcomingSessionsContent(schedule.filter { it.status.lowercase() != "completed" })
                        2 -> PreviousSessionsContent(schedule.filter { it.status.lowercase() == "completed" })
                    }
                }
            }
        }
    }
}




@Composable
private fun CurrentSessionContent(onStartSession: () -> Unit) {
    val hasActiveSession = false // TODO: Get from ViewModel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (hasActiveSession) {
            // Show active session
            ActiveSessionCard()
        } else {
            // No active session - show start button
            Spacer(Modifier.height(48.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = KaironexColors.CanvasWhite,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "📚",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No Active Session",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Start a study session to enter deep focus mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onStartSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KaironexColors.ElectricBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, "Start")
                        Spacer(Modifier.width(8.dp))
                        Text("Start Study Session")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick start options
            Text(
                "Quick Start",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.SlateGray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStartChip("25 min", "Pomodoro", Modifier.weight(1f))
                QuickStartChip("50 min", "Deep Work", Modifier.weight(1f))
                QuickStartChip("90 min", "Flow State", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ActiveSessionCard() {
    KxCard(
        variant = KxCardVariant.Elevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Calculus II",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KaironexColors.SuccessGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        "ACTIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SuccessGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Integration Techniques",
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.SlateGray
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { 0.65f },
                modifier = Modifier.fillMaxWidth(),
                color = KaironexColors.ElectricBlue
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "32:15 / 50:00",
                style = MaterialTheme.typography.labelMedium,
                color = KaironexColors.SlateGray
            )
        }
    }
}

@Composable
private fun QuickStartChip(duration: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        onClick = { /* TODO */ },
        shape = RoundedCornerShape(12.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                duration,
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpcomingSessionsContent(schedule: List<ScheduleRepository.ScheduleTask>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (schedule.isEmpty()) {
            item {
                Text(
                    "No upcoming sessions. Tap refresh to generate a plan.",
                    modifier = Modifier.padding(16.dp),
                    color = KaironexColors.SlateGray
                )
            }
        } else {
            // Group by Date
            val grouped = schedule.groupBy { task ->
                try {
                    task.startTime.take(10) // YYYY-MM-DD
                } catch (e: Exception) {
                    "Unknown Date"
                }
            }

            grouped.forEach { (date, tasks) ->
                stickyHeader {
                    Surface(
                        color = KaironexColors.CloudGray,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = formatDateHeader(date),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.InkBlack,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                items(tasks) { task ->
                    val uiTask = task.toUiTask()
                    ScheduledTaskItem(
                        task = uiTask,
                        isMobile = true, 
                        isDetailed = true, // Enable detailed view for Study Room
                        onClick = { /* Detail view */ }
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

// Mapper extension
private fun DomainTask.toUiTask(): ScheduledTask {
    // Parse start/end to nice 12h
    val sTime = formatTime(this.startTime)
    val eTime = formatTime(this.endTime)
    
    // Determine status enum
    val statusEnum = when(this.status.lowercase()) {
        "completed" -> TaskStatus.COMPLETED
        "active" -> TaskStatus.IN_PROGRESS
        "skipped" -> TaskStatus.OVERDUE
        else -> TaskStatus.UPCOMING
    }
    
    // Determine priority enum
    val priorityEnum = when(this.priority) {
        in 8..10 -> TaskPriority.CRITICAL
        in 5..7 -> TaskPriority.HIGH
        else -> TaskPriority.NORMAL
    }

    return ScheduledTask(
        id = this.id,
        title = this.title,
        subject = this.type.replaceFirstChar { it.titlecase() },
        startTime = sTime,
        endTime = eTime,
        duration = "60m", 
        status = statusEnum,
        priority = priorityEnum,
        topics = this.topics,
        isFlexible = this.isFlexible,
        linkedDeadline = this.linkedDeadline
    )
}

@Composable
private fun PreviousSessionsContent(completedTasks: List<ScheduleRepository.ScheduleTask>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (completedTasks.isEmpty()) {
            item {
                Text(
                    "No completed sessions found.",
                    modifier = Modifier.padding(16.dp),
                    color = KaironexColors.SlateGray,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(completedTasks) { task ->
                val uiTask = task.toUiTask()
                ScheduledTaskItem(
                    task = uiTask, 
                    isMobile=true, 
                    isDetailed = true,
                    onClick={}
                )
            }
        }

        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

// Helper: Format YYYY-MM-DD to readable (e.g., "Mon, Oct 25")
private fun formatDateHeader(dateStr: String): String {
    if (dateStr == "Unknown Date") return dateStr
    // Try simple parsing
    return try {
        // Just return ISO for now or prettify if using java.time (not avail in KMM common easily without lib)
        // For simplicity in CommonMain, we stick to the ISO string or simple split
        dateStr
    } catch(e: Exception) { dateStr }
}

// Helper: Extract HH:MM AM/PM from ISO string
private fun formatTime(isoString: String): String {
    return try {
        // Simple manual parsing for 12H since standard formatter isn't in commonMain
        // ISO: 2023-10-25T14:30:00.000+00:00
        val timePart = isoString.substringAfter("T").substringBefore(".") // 14:30:00
        val parts = timePart.split(":")
        val hour = parts[0].toInt()
        val min = parts[1]
        
        val amPm = if (hour >= 12) "PM" else "AM"
        val hour12 = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        
        "$hour12:$min $amPm"
    } catch (e: Exception) { "--:--" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(session: StudySession, isUpcoming: Boolean) {
    KxCard(
        variant = KxCardVariant.Flat,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Icon, Title, Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Priority Badge (Replacing Icon)
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = getPriorityColor(session.priority),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${session.priority}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.subject,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                    // Linked Deadline if exists
                    if (!session.linkedDeadline.isNullOrBlank()) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.Event, null, tint = KaironexColors.ErrorRed, modifier = Modifier.size(12.dp))
                             Spacer(Modifier.width(4.dp))
                             Text(
                                "Deadline: ${formatDateHeader(session.linkedDeadline.take(10))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = KaironexColors.ErrorRed
                            )
                         }
                    } else {
                         Text(
                            session.type.capitalize(),
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }

                // Time Column
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(Icons.Default.Schedule, null, tint = KaironexColors.ElectricBlue, modifier = Modifier.size(14.dp))
                         Spacer(Modifier.width(4.dp))
                         Text(
                            session.timeRange, 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KaironexColors.ElectricBlue
                        )
                    }
                }
            }
            
            // Topics & Details
            Spacer(Modifier.height(12.dp))
            
            // Topics
            if (!session.topics.isNullOrBlank()) {
                Text(
                    "Topics: ${session.topics}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.InkBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KaironexColors.CloudGray.copy(alpha=0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(session.status)
                
                if (session.isFlexible) {
                    DetailChip("Flexible", KaironexColors.SuccessGreen, Icons.Default.Autorenew)
                } else {
                    DetailChip("Fixed", KaironexColors.SlateGray, Icons.Default.Lock)
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (color, label) = when(status.lowercase()) {
        "completed" -> KaironexColors.SuccessGreen to "Done"
        "pending" -> KaironexColors.SlateGray to "Pending"
        "skipped" -> KaironexColors.ErrorRed to "Skipped"
        "active" -> KaironexColors.ElectricBlue to "Active"
        else -> KaironexColors.SlateGray to status.capitalize()
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetailChip(text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

fun getPriorityColor(priority: Int): Color {
    return when(priority) {
        in 8..10 -> KaironexColors.ErrorRed
        in 5..7 -> KaironexColors.EventsOrange
        else -> KaironexColors.SuccessGreen
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private data class StudySession(
    val id: String,
    val subject: String,
    val topic: String?,
    val timeRange: String,
    val duration: Int,
    val status: String,
    val type: String,
    val isFlexible: Boolean,
    val priority: Int,
    val topics: String,
    val linkedDeadline: String?
)
