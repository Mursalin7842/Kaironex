package com.mursaline.kaironex.features.study

import androidx.compose.foundation.background
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.theme.KaironexColors

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
object StudySessionsScreen : Screen {
    private fun readResolve(): Any = StudySessionsScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedTab by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        "Study Room",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add new session */ }) {
                        Icon(Icons.Default.Add, "Add Session")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KaironexColors.CanvasWhite
                )
            )

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = KaironexColors.CanvasWhite,
                contentColor = KaironexColors.ElectricBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Current") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Upcoming") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Previous") }
                )
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> CurrentSessionContent(
                    onStartSession = { navigator.push(StudyRoomScreen) }
                )
                1 -> UpcomingSessionsContent()
                2 -> PreviousSessionsContent()
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

@Composable
private fun UpcomingSessionsContent() {
    val upcomingSessions = listOf(
        StudySession("1", "Physics", "Quantum Mechanics", "Today, 3:00 PM", 50),
        StudySession("2", "Chemistry", "Organic Reactions", "Tomorrow, 10:00 AM", 45),
        StudySession("3", "Math", "Linear Algebra", "Tomorrow, 2:00 PM", 60)
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(upcomingSessions) { session ->
            SessionCard(session, isUpcoming = true)
        }

        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun PreviousSessionsContent() {
    val previousSessions = listOf(
        StudySession("1", "Calculus II", "Derivatives", "Yesterday", 50, completed = true),
        StudySession("2", "History", "World War II", "2 days ago", 45, completed = true),
        StudySession("3", "Biology", "Cell Division", "3 days ago", 30, completed = false)
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(previousSessions) { session ->
            SessionCard(session, isUpcoming = false)
        }

        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SessionCard(session: StudySession, isUpcoming: Boolean) {
    KxCard(
        variant = KxCardVariant.Flat,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUpcoming) KaironexColors.ElectricBlue.copy(alpha = 0.1f)
                       else KaironexColors.SlateGray.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isUpcoming) "📅" else if (session.completed) "✅" else "⏸️",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                Text(
                    session.topic,
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    session.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUpcoming) KaironexColors.ElectricBlue else KaironexColors.SlateGray
                )
            }

            Text(
                "${session.duration} min",
                style = MaterialTheme.typography.labelMedium,
                color = KaironexColors.SlateGray
            )
        }
    }
}

private data class StudySession(
    val id: String,
    val subject: String,
    val topic: String,
    val time: String,
    val duration: Int,
    val completed: Boolean = false
)
