package com.mursaline.kaironex.features.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.features.dashboard.tabs.HomeScreen
import com.mursaline.kaironex.features.dashboard.tabs.AnalyticsScreen
import com.mursaline.kaironex.features.dashboard.tabs.SettingsScreen
import com.mursaline.kaironex.features.dashboard.tabs.ChatScreen

import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant
import com.mursaline.kaironex.features.dashboard.components.ActiveAgentDeck
import com.mursaline.kaironex.features.dashboard.components.PressureMap
import com.mursaline.kaironex.features.dashboard.components.Agent
import com.mursaline.kaironex.features.dashboard.components.OmniMenuDrawer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButton


object DashboardScreen : Screen {
    private fun readResolve(): Any = DashboardScreen

    @Composable
    override fun Content() {
        var selectedTab by remember { mutableStateOf("Home") }

        @Suppress("UnusedBoxWithConstraintsScope")
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = this.maxWidth < 800.dp
            val isWideDesktop = this.maxWidth > 1400.dp // Only show separate panel on very wide screens
            val maxContentWidth = this.maxWidth

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // MAIN CONTENT - Scrollable for all screen sizes
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(if (isMobile) 12.dp else 24.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = if (isMobile) 16.dp else 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isMobile) "Good Morning!" else "Good Morning, Student.",
                                        style = if(isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
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
                                
                                // User Avatar / Profile
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = KaironexColors.GeminiBlurple,
                                    modifier = Modifier.size(if(isMobile) 32.dp else 40.dp),
                                    shadowElevation = 2.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("S", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Content Area using KxCard - Responsive height
                            val contentCardHeight = if (isMobile) {
                                Modifier.heightIn(min = 280.dp, max = 400.dp)
                            } else {
                                Modifier.heightIn(min = 300.dp, max = 500.dp)
                            }

                            KxCard(
                                modifier = Modifier.fillMaxWidth().then(contentCardHeight),
                                variant = KxCardVariant.High,
                                backgroundColor = KaironexColors.CanvasWhite
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(if (isMobile) 12.dp else 20.dp)) {
                                    when(selectedTab) {
                                        "Home" -> HomeScreen()
                                        "Chat" -> ChatScreen()
                                        "Analytics" -> AnalyticsScreen()
                                        "Settings" -> SettingsScreen()
                                        else -> HomeScreen()
                                    }
                                }
                            }

                            Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

                            // Active Agents Section - Now in scroll area for ALL screen sizes
                            val dummyAgents = listOf(
                                Agent("1", "Exam Prep", "Math", "Active", if (isMobile) "2 days" else "2 days left"),
                                Agent("2", "Fitness Coach", "Health", "Idle", "Tomorrow"),
                                Agent("3", "Code Review", "Dev", "Active", "Today")
                            )

                            // Two-column layout for desktop
                            if (!isMobile && !isWideDesktop) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    // Left: Active Agents
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Active Agents",
                                            fontWeight = FontWeight.Bold,
                                            color = KaironexColors.InkBlack,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        ActiveAgentDeck(
                                            agents = dummyAgents,
                                            modifier = Modifier.fillMaxWidth(),
                                            isMobile = false
                                        )
                                    }

                                    // Right: Academic Pressure
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Academic Pressure",
                                            fontWeight = FontWeight.Bold,
                                            color = KaironexColors.InkBlack,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        KxCard(
                                            variant = KxCardVariant.Flat,
                                            modifier = Modifier.fillMaxWidth().height(180.dp),
                                            backgroundColor = KaironexColors.CanvasWhite
                                        ) {
                                            PressureMap(modifier = Modifier.fillMaxSize(), isMobile = false)
                                        }
                                    }
                                }
                            } else if (isMobile) {
                                // Mobile: Stacked layout
                                Text(
                                    "Active Agents",
                                    fontWeight = FontWeight.Bold,
                                    color = KaironexColors.InkBlack,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(8.dp))
                                ActiveAgentDeck(
                                    agents = dummyAgents,
                                    modifier = Modifier.fillMaxWidth(),
                                    isMobile = true
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "Weekly Pressure",
                                    fontWeight = FontWeight.Bold,
                                    color = KaironexColors.InkBlack,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(8.dp))
                                KxCard(
                                    variant = KxCardVariant.Flat,
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    backgroundColor = KaironexColors.CanvasWhite
                                ) {
                                    PressureMap(modifier = Modifier.fillMaxSize(), isMobile = true)
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                        
                        // RIGHT PANEL - Only for very wide desktop screens (1400dp+)
                        if (isWideDesktop) {
                            RightPanel()
                        }
                    }
                }
            }
            // Bottom navigation is now handled by MainShell for mobile
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
                Text("Active Agents", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                // Real Active Agent Deck
                val dummyAgents = listOf(
                    Agent("1", "Exam Prep", "Math", "Active", "2 days left"),
                    Agent("2", "Fitness Coach", "Health", "Idle", "Tomorrow"),
                    Agent("3", "Code Review", "Dev", "Active", "Today")
                )
                ActiveAgentDeck(agents = dummyAgents, modifier = Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(24.dp))

                Text("Academic Pressure", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                // Real Pressure Map
                KxCard(variant = KxCardVariant.Flat, modifier = Modifier.fillMaxWidth().height(200.dp), backgroundColor = KaironexColors.CanvasWhite) {
                     PressureMap(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
