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
import com.mursaline.kaironex.features.dashboard.components.LifeTracksGrid
import com.mursaline.kaironex.features.zones.CortexState
import com.mursaline.kaironex.features.study.StudyRoomScreen
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

/**
 * Dashboard Screen - "Hero + Support" Layout
 *
 * Based on the Kaironex Motivation: The Study Room (Cortex) is the HEART of the app.
 * The entire goal is to protect this space from "Life" distractions.
 *
 * Layout:
 * - Top Section (The Cortex): Massive, immersive "Enter Flow" area with pressure visualization
 * - Bottom Section (Life Support): The 4 Life Tracks that handle life so students can study
 */
object DashboardScreen : Screen {
    private fun readResolve(): Any = DashboardScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Cortex State - In production, this would come from ViewModel
        var cortexState by remember {
            mutableStateOf(
                CortexState(
                    currentSubject = "Calculus II",
                    currentTopic = "3 Deadlines approaching. High pressure detected.",
                    pressure = 0.7f,
                    upcomingDeadlines = 3,
                    studyStreak = 5,
                    conceptMastery = 0.45f,
                    isActive = true
                )
            )
        }

        @Suppress("UnusedBoxWithConstraintsScope")
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = this.maxWidth < 800.dp
            val isWideDesktop = this.maxWidth > 1400.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // MAIN SCROLLABLE CONTENT
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(if (isMobile) 16.dp else 24.dp)
                ) {
                    // ===== HEADER =====
                    DashboardHeader(isMobile = isMobile)

                    Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                    // ===== SECTION 1: THE HERO - CORTEX (Study Room) =====
                    Text(
                        text = "Current Focus",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(12.dp))

                    CortexHeroCard(
                        cortexState = cortexState,
                        onEnterFlow = { navigator.push(StudyRoomScreen) },
                        isMobile = isMobile
                    )

                    Spacer(Modifier.height(if (isMobile) 24.dp else 32.dp))

                    // ===== SECTION 2: LIFE SUPPORT AGENTS =====
                    // These contain the active agents - no separate "Active Agents" deck needed
                    Text(
                        text = "Life Support Agents",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(if (isMobile) 8.dp else 16.dp))

                    LifeTracksGrid(
                        isMobile = isMobile,
                        onTrackClick = { track ->
                            // Route to specific Track screens when implemented
                            // For now, tracks show their status inline
                            // TODO: navigator.push(TrackDetailScreen(track))
                        }
                    )

                    Spacer(Modifier.height(if (isMobile) 24.dp else 32.dp))

                    // ===== SECTION 3: PRESSURE MAP =====
                    Text(
                        text = if (isMobile) "Weekly Pressure" else "Academic Pressure Overview",
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.height(if (isMobile) 8.dp else 12.dp))

                    KxCard(
                        variant = KxCardVariant.Flat,
                        modifier = Modifier.fillMaxWidth().height(if (isMobile) 120.dp else 160.dp),
                        backgroundColor = KaironexColors.CanvasWhite
                    ) {
                        PressureMap(modifier = Modifier.fillMaxSize(), isMobile = isMobile)
                    }

                    Spacer(Modifier.height(32.dp))
                }

                // RIGHT PANEL - Only for very wide desktop screens
                if (isWideDesktop) {
                    RightPanel()
                }
            }
        }
    }

    @Composable
    private fun DashboardHeader(isMobile: Boolean) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMobile) "Good Morning!" else "Good Morning, Student.",
                    style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Your focus score is stable.",
                        style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = KaironexColors.SlateGray
                    )
                    Spacer(Modifier.width(8.dp))
                    KxBadge("Routine", variant = KxBadgeVariant.Success)
                }
            }

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

    @Composable
    fun RightPanel() {
        Surface(
            modifier = Modifier.width(320.dp).fillMaxHeight(),
            color = KaironexColors.CloudGray,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Quick Stats", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                KxCard(variant = KxCardVariant.Elevated, modifier = Modifier.fillMaxWidth(), backgroundColor = KaironexColors.CanvasWhite) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem("Study Streak", "5 days", "🔥")
                            StatItem("Focus Score", "78%", "🎯")
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem("Tasks Done", "12/15", "✅")
                            StatItem("Deadlines", "3", "⏰")
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Life Support", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                // Compact Life Tracks grid for side panel
                LifeTracksGrid(isMobile = true, onTrackClick = {})

                Spacer(Modifier.height(24.dp))

                Text("Weekly Pressure", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                KxCard(variant = KxCardVariant.Flat, modifier = Modifier.fillMaxWidth().height(180.dp), backgroundColor = KaironexColors.CanvasWhite) {
                    PressureMap(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    @Composable
    private fun StatItem(label: String, value: String, emoji: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack)
            Text(label, style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
        }
    }
}
