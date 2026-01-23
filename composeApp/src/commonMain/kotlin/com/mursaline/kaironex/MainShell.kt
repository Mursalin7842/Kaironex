package com.mursaline.kaironex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.animation.*
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.features.dashboard.components.OmniMenuDrawer
import com.mursaline.kaironex.features.dashboard.DashboardScreen

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
object MainShellScreen : Screen {
    private fun readResolve(): Any = MainShellScreen

    @Composable
    override fun Content() {
        var isMenuOpen by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf("Home") }

        Navigator(DashboardScreen) { navigator ->
            val currentRoute = navigator.lastItem
            val selectedItem = when (currentRoute) {
                is DashboardScreen -> "Home"
                else -> "Home"
            }

            @Suppress("UnusedBoxWithConstraintsScope")
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
                val isMobile = this.maxWidth < 800.dp

                OmniMenuDrawer(
                    isOpen = isMenuOpen,
                    onClose = { isMenuOpen = false },
                    onNavigate = { route ->
                        isMenuOpen = false
                        when (route) {
                            "StudyRoom" -> navigator.push(com.mursaline.kaironex.features.study.StudyRoomScreen)
                            "Settings" -> { /* TODO */ }
                            else -> { /* Placeholder */ }
                        }
                    }
                ) {
                    if (isMobile) {
                        // MOBILE LAYOUT: Top Bar + Content + Bottom Nav
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            "Kaironex",
                                            fontWeight = FontWeight.Bold,
                                            color = KaironexColors.InkBlack
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { isMenuOpen = true }) {
                                            Icon(
                                                Icons.Filled.Menu,
                                                contentDescription = "Menu",
                                                tint = KaironexColors.InkBlack
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = KaironexColors.CanvasWhite
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = KaironexColors.CanvasWhite,
                                    contentColor = KaironexColors.InkBlack,
                                    tonalElevation = 8.dp
                                ) {
                                    MobileNavItem("Home", Icons.Filled.Home, selectedTab == "Home") {
                                        selectedTab = "Home"
                                        if (currentRoute !is DashboardScreen) navigator.replace(DashboardScreen)
                                    }
                                    MobileNavItem("Chat", Icons.AutoMirrored.Filled.Chat, selectedTab == "Chat") {
                                        selectedTab = "Chat"
                                    }
                                    MobileNavItem("Stats", Icons.Filled.Analytics, selectedTab == "Stats") {
                                        selectedTab = "Stats"
                                    }
                                    MobileNavItem("Settings", Icons.Filled.Settings, selectedTab == "Settings") {
                                        selectedTab = "Settings"
                                    }
                                }
                            },
                            containerColor = KaironexColors.CloudGray
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                AnimatedContent(
                                    targetState = navigator.lastItem,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                                ) { screen ->
                                    navigator.saveableState("shell_content", screen) {
                                        screen.Content()
                                    }
                                }
                            }
                        }
                    } else {
                        // DESKTOP LAYOUT: Side Rail + Content
                        Row(modifier = Modifier.fillMaxSize()) {
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

                                ShellNavItem("Chat", Icons.AutoMirrored.Filled.Chat, selectedItem == "Chat") { }
                                Spacer(Modifier.height(12.dp))

                                ShellNavItem("Stats", Icons.Filled.Analytics, selectedItem == "Stats") { }

                                Spacer(Modifier.weight(1f))

                                ShellNavItem("Settings", Icons.Filled.Settings, selectedItem == "Settings") { }
                                Spacer(Modifier.height(24.dp))
                            }

                            // Main Content
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AnimatedContent(
                                    targetState = navigator.lastItem,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
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
}

@Composable
fun RowScope.MobileNavItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = KaironexColors.ElectricBlue,
            selectedTextColor = KaironexColors.ElectricBlue,
            unselectedIconColor = KaironexColors.SlateGray,
            unselectedTextColor = KaironexColors.SlateGray,
            indicatorColor = KaironexColors.CloudGray
        )
    )
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
