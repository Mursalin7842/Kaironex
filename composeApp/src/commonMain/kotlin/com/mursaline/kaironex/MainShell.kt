package com.mursaline.kaironex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.animation.*
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.features.dashboard.components.OmniMenuDrawer
import com.mursaline.kaironex.features.dashboard.DashboardScreen

@OptIn(ExperimentalAnimationApi::class)
object MainShellScreen : Screen {
    private fun readResolve(): Any = MainShellScreen

    @Composable
    override fun Content() {
        var isMenuOpen by remember { mutableStateOf(false) }

        Navigator(DashboardScreen) { navigator ->
            val currentRoute = navigator.lastItem
            val selectedItem = when (currentRoute) {
                is DashboardScreen -> "Home"
                else -> "Home" // Default
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
                val isMobile = maxWidth < 800.dp

                OmniMenuDrawer(
                    isOpen = isMenuOpen,
                    onClose = { isMenuOpen = false },
                    onNavigate = { route ->
                        isMenuOpen = false
                        // Handle OmniMenu Navigation
                        when (route) {
                            "StudyRoom" -> navigator.push(com.mursaline.kaironex.features.study.StudyRoomScreen)
                            "Settings" -> { /* TODO */ }
                            else -> { /* Placeholder */ }
                        }
                    }
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {

                        // 1. Navigation Rail (Desktop Only)
                        if (!isMobile) {
                            NavigationRail(
                                containerColor = KaironexColors.CanvasWhite,
                                contentColor = KaironexColors.InkBlack,
                                modifier = Modifier.width(80.dp).fillMaxHeight(),
                                header = {
                                    IconButton(onClick = { isMenuOpen = true }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                    }
                                }
                            ) {
                                Spacer(Modifier.weight(1f))

                                ShellNavItem("Home", Icons.Filled.Home, selectedItem == "Home") {
                                    if (currentRoute !is DashboardScreen) navigator.replace(DashboardScreen)
                                }
                                Spacer(Modifier.height(12.dp))

                                // Placeholder for Chat Screen - for now just re-route to Dashboard (or handle internal tab)
                                ShellNavItem("Chat", Icons.Filled.Chat, selectedItem == "Chat") { /* navigator.push(ChatScreen) */ }
                                Spacer(Modifier.height(12.dp))

                                ShellNavItem("Stats", Icons.Filled.Analytics, selectedItem == "Stats") { /* navigator.push(AnalyticsScreen) */ }

                                Spacer(Modifier.weight(1f))

                                ShellNavItem("Settings", Icons.Filled.Settings, selectedItem == "Settings") { /* navigator.push(SettingsScreen) */ }
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // 2. Main Content - Renders the internal Navigator's current screen
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            androidx.compose.animation.AnimatedContent(
                                targetState = navigator.lastItem,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }
                            ) { screen ->
                                navigator.saveableState("shell_content", screen) {
                                    screen.Content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShellNavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationRailItem(
        selected = isSelected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = KaironexColors.ElectricBlue,
            selectedTextColor = KaironexColors.ElectricBlue,
            indicatorColor = KaironexColors.CloudGray
        )
    )
}
