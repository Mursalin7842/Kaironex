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
import androidx.compose.material.icons.filled.Menu

object DashboardScreen : Screen {
    private fun readResolve(): Any = DashboardScreen

    @Composable
    override fun Content() {
        var selectedTab by remember { mutableStateOf("Home") }

        var isMenuOpen by remember { mutableStateOf(false) }
        
        OmniMenuDrawer(
            isOpen = isMenuOpen,
            onClose = { isMenuOpen = false },
            onNavigate = { 
                selectedTab = it // For now mapping to tabs, later to full routes
                isMenuOpen = false
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
                val isMobile = maxWidth < 800.dp
                val showRightPanel = maxWidth > 1200.dp // Desktop wide mode
    
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // No Static Sidebar anymore, handled by Drawer
    
                            // MAIN CONTENT
                            Column(modifier = Modifier.weight(1f).padding(if (isMobile) 16.dp else 24.dp)) {
                                // Header
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Menu Trigger
                                         androidx.compose.material3.IconButton(onClick = { isMenuOpen = true }) {
                                             Icon(Icons.Filled.Menu, contentDescription = "Open Menu", tint = KaironexColors.InkBlack)
                                         }
                                         Spacer(Modifier.width(16.dp))
                                         
                                        Column {
                                            Text(
                                                text = "Good Morning, Student.",
                                                style = if(isMobile) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = KaironexColors.InkBlack
                                            )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Your focus score is stable.",
                                        style = MaterialTheme.typography.bodyMedium,
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

                        // Content Area using KxCard for unification
                            KxCard(
                            modifier = Modifier.fillMaxSize(),
                            variant = KxCardVariant.High,
                            backgroundColor = KaironexColors.CanvasWhite
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                when(selectedTab) {
                                    "Home" -> HomeScreen()
                                    "Chat" -> ChatScreen()
                                    "Analytics" -> AnalyticsScreen()
                                    "Settings" -> SettingsScreen()
                                    // Omni Menu mapping placeholders
                                    "StudyRoom" -> Text("Study Room Loading...", modifier = Modifier.align(Alignment.Center))
                                    "KnowledgeGraph" -> Text("Knowledge Graph Loading...", modifier = Modifier.align(Alignment.Center))
                                    "MockTest" -> Text("Mock Test Center Loading...", modifier = Modifier.align(Alignment.Center))
                                    "JobCampaign" -> Text("Job Campaign Loading...", modifier = Modifier.align(Alignment.Center))
                                    "ResumeDebugger" -> Text("Resume Debugger Loading...", modifier = Modifier.align(Alignment.Center))
                                    "Nutrition" -> Text("Nutrition Loading...", modifier = Modifier.align(Alignment.Center))
                                    "Sleep" -> Text("Sleep Tracking Loading...", modifier = Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                    
                    // RIGHT PANEL (Agent Deck / Mini PressureMap)
                    if (showRightPanel) {
                            RightPanel()
                    }
                }
                }
            }

                // BOTTOM NAVIGATION (Only on Mobile)
                if (isMobile) {
                    val items = listOf(
                        "Home" to Icons.Filled.Home,
                        "Chat" to Icons.Filled.Chat,
                        "Analytics" to Icons.Filled.Analytics,
                        "Settings" to Icons.Filled.Settings
                    )
                    
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = KaironexColors.Indigo600,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { (label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                selected = selectedTab == label,
                                onClick = { selectedTab = label },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KaironexColors.Indigo600,
                                    selectedTextColor = KaironexColors.Indigo600,
                                    unselectedIconColor = KaironexColors.Slate500,
                                    unselectedTextColor = KaironexColors.Slate500,
                                    indicatorColor = KaironexColors.Indigo50
                                )
                            )
                        }
                    }
                }
                }
            }
        }
    }

    @Composable
    fun Sidebar(selected: String, onSelect: (String) -> Unit) {
        Surface(
            modifier = Modifier.width(250.dp).fillMaxHeight(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = KaironexColors.Indigo600,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("K", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("KAIRONEX", fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                }

                Spacer(Modifier.height(48.dp))

                // Menu Items
                val items = listOf(
                    "Home" to Icons.Filled.Home,
                    "Chat" to Icons.Filled.Chat,
                    "Analytics" to Icons.Filled.Analytics
                )

                items.forEach { (label, icon) ->
                    SidebarItem(label, icon, selected == label) { onSelect(label) }
                    Spacer(Modifier.height(8.dp))
                }
                
                Spacer(Modifier.weight(1f))
                
                SidebarItem("Settings", Icons.Filled.Settings, selected == "Settings") { onSelect("Settings") }
            }
        }
    }

    @Composable
    fun SidebarItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
        val backgroundColor = if (isSelected) KaironexColors.Indigo50 else Color.Transparent
        val contentColor = if (isSelected) KaironexColors.Indigo600 else KaironexColors.Slate500

        Surface(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, color = contentColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }


    @Composable
    fun RightPanel() {
        Surface(
            modifier = Modifier.width(350.dp).fillMaxHeight(),
            color = KaironexColors.CloudGray, // Slight distinction
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Active Agents", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack)
                Spacer(Modifier.height(16.dp))
                
                // Real Active Agent Deck
                val dummyAgents = listOf(
                    Agent("1", "Exam Prep", "Math", "Active", "2 days left"),
                    Agent("2", "Fitness Coach", "Health", "Idle", "Tomorrow")
                )
                ActiveAgentDeck(agents = dummyAgents, modifier = Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(32.dp))
                
                Text("Academic Pressure", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack)
                Spacer(Modifier.height(16.dp))
                
                // Real Pressure Map
                KxCard(variant = KxCardVariant.Flat, modifier = Modifier.fillMaxWidth().height(250.dp), backgroundColor = KaironexColors.CanvasWhite) {
                     PressureMap(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
