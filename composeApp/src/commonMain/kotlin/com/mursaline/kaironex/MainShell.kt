package com.mursaline.kaironex

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.core.screen.Screen
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.features.dashboard.components.OmniMenuDrawer
import com.mursaline.kaironex.features.dashboard.DashboardScreen
import com.mursaline.kaironex.features.study.StudyRoomScreen
import com.mursaline.kaironex.ui.components.KxOrb
import com.mursaline.kaironex.ui.components.KxOrbState

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
object MainShellScreen : Screen {
    private fun readResolve(): Any = MainShellScreen

    @Composable
    override fun Content() {
        var isMenuOpen by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf("Home") }
        var isOrbExpanded by remember { mutableStateOf(false) }

        Navigator(DashboardScreen) { navigator ->
            val currentRoute = navigator.lastItem
            val selectedItem = when (currentRoute) {
                is DashboardScreen -> "Home"
                StudyRoomScreen -> "Study"
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
                            "StudyRoom" -> navigator.push(StudyRoomScreen)
                            "Settings" -> { /* TODO */ }
                            else -> { /* Placeholder */ }
                        }
                    }
                ) {
                    if (isMobile) {
                        // MOBILE LAYOUT: Top Bar + Content + Bottom Nav with Center Orb
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
                                // Custom Bottom Nav with Center Orb
                                MobileNavBarWithOrb(
                                    selectedTab = selectedTab,
                                    isOrbExpanded = isOrbExpanded,
                                    onTabSelected = { tab ->
                                        selectedTab = tab
                                        when (tab) {
                                            "Home" -> if (currentRoute !is DashboardScreen) navigator.replace(DashboardScreen)
                                            "Study" -> if (currentRoute != StudyRoomScreen) navigator.push(StudyRoomScreen)
                                        }
                                    },
                                    onOrbClick = { isOrbExpanded = !isOrbExpanded }
                                )
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

                                // Orb Expanded Panel (Gemini Live-like overlay)
                                AnimatedVisibility(
                                    visible = isOrbExpanded,
                                    enter = fadeIn() + slideInVertically { it },
                                    exit = fadeOut() + slideOutVertically { it },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    AssistantPanel(
                                        onDismiss = { isOrbExpanded = false },
                                        isMobile = true
                                    )
                                }
                            }
                        }
                    } else {
                        // DESKTOP LAYOUT: Side Rail with Center Orb + Content
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Enhanced Navigation Rail with Center Orb
                            DesktopNavRailWithOrb(
                                selectedItem = selectedItem,
                                isOrbExpanded = isOrbExpanded,
                                onMenuClick = { isMenuOpen = true },
                                onNavItemClick = { item ->
                                    when (item) {
                                        "Home" -> if (currentRoute !is DashboardScreen) navigator.replace(DashboardScreen)
                                        "Chat" -> { /* TODO: Chat overlay */ }
                                        "Study" -> if (currentRoute != StudyRoomScreen) navigator.push(StudyRoomScreen)
                                        "Settings" -> { /* TODO */ }
                                    }
                                },
                                onOrbClick = { isOrbExpanded = !isOrbExpanded }
                            )

                            // Main Content with Assistant Overlay
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                AnimatedContent(
                                    targetState = navigator.lastItem,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                                ) { screen ->
                                    navigator.saveableState("shell_content", screen) {
                                        screen.Content()
                                    }
                                }

                                // Orb Expanded Panel for Desktop (Gemini Live-like overlay)
                                if (isOrbExpanded) {
                                    AssistantPanel(
                                        onDismiss = { isOrbExpanded = false },
                                        isMobile = false,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
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

// ===== ENHANCED NAVIGATION WITH CENTER ORB =====

@Composable
fun MobileNavBarWithOrb(
    selectedTab: String,
    isOrbExpanded: Boolean,
    onTabSelected: (String) -> Unit,
    onOrbClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            MobileNavItemCompact(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = selectedTab == "Home",
                onClick = { onTabSelected("Home") }
            )

            // Chat
            MobileNavItemCompact(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "Chat",
                isSelected = selectedTab == "Chat",
                onClick = { onTabSelected("Chat") }
            )

            // CENTER ORB - AI Assistant
            Box(
                modifier = Modifier
                    .offset(y = (-16).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isOrbExpanded)
                                listOf(KaironexColors.Purple600, KaironexColors.Indigo600)
                            else
                                listOf(KaironexColors.Indigo600, KaironexColors.GeminiBlurple)
                        )
                    )
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(onClick = onOrbClick),
                contentAlignment = Alignment.Center
            ) {
                KxOrb(
                    size = 40.dp,
                    state = if (isOrbExpanded) KxOrbState.Active else KxOrbState.Idle
                )
            }

            // Study Room
            MobileNavItemCompact(
                icon = Icons.Filled.School,
                label = "Study",
                isSelected = selectedTab == "Study",
                onClick = { onTabSelected("Study") }
            )

            // Settings
            MobileNavItemCompact(
                icon = Icons.Filled.Settings,
                label = "Settings",
                isSelected = selectedTab == "Settings",
                onClick = { onTabSelected("Settings") }
            )
        }
    }
}

@Composable
fun MobileNavItemCompact(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) KaironexColors.ElectricBlue else KaironexColors.SlateGray
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) KaironexColors.ElectricBlue else KaironexColors.SlateGray
        )
    }
}

@Composable
fun DesktopNavRailWithOrb(
    selectedItem: String,
    isOrbExpanded: Boolean,
    onMenuClick: () -> Unit,
    onNavItemClick: (String) -> Unit,
    onOrbClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(80.dp).fillMaxHeight(),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Menu Button
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = KaironexColors.InkBlack)
            }

            Spacer(Modifier.height(24.dp))

            // Home
            ShellNavItem("Home", Icons.Filled.Home, selectedItem == "Home") {
                onNavItemClick("Home")
            }
            Spacer(Modifier.height(8.dp))

            // Chat
            ShellNavItem("Chat", Icons.AutoMirrored.Filled.Chat, selectedItem == "Chat") {
                onNavItemClick("Chat")
            }

            Spacer(Modifier.weight(1f))

            // CENTER ORB - AI Assistant (in the middle of the rail)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isOrbExpanded)
                                listOf(KaironexColors.Purple600, KaironexColors.Indigo600)
                            else
                                listOf(KaironexColors.Indigo600, KaironexColors.GeminiBlurple)
                        )
                    )
                    .border(2.dp, KaironexColors.CloudGray, CircleShape)
                    .clickable(onClick = onOrbClick),
                contentAlignment = Alignment.Center
            ) {
                KxOrb(
                    size = 40.dp,
                    state = if (isOrbExpanded) KxOrbState.Active else KxOrbState.Idle
                )
            }

            Spacer(Modifier.weight(1f))

            // Study Room
            ShellNavItem("Study", Icons.Filled.School, selectedItem == "Study") {
                onNavItemClick("Study")
            }
            Spacer(Modifier.height(8.dp))

            // Settings
            ShellNavItem("Settings", Icons.Filled.Settings, selectedItem == "Settings") {
                onNavItemClick("Settings")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ===== ASSISTANT PANEL (Gemini Live-like) =====

@Composable
fun AssistantPanel(
    onDismiss: () -> Unit,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    val panelWidth = if (isMobile) Modifier.fillMaxWidth() else Modifier.width(400.dp)
    val panelHeight = if (isMobile) Modifier.height(400.dp) else Modifier.fillMaxHeight()

    Surface(
        modifier = modifier.then(panelWidth).then(panelHeight).padding(if (isMobile) 16.dp else 0.dp),
        color = KaironexColors.CanvasWhite,
        shape = if (isMobile) androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
               else androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Kaironex Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Settings, // Close would be better
                        contentDescription = "Close",
                        tint = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Main Orb - Larger and interactive
            Box(
                modifier = Modifier
                    .size(if (isMobile) 120.dp else 150.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                KaironexColors.Purple600,
                                KaironexColors.Indigo600,
                                KaironexColors.GeminiBlurple
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                KxOrb(
                    size = if (isMobile) 100.dp else 130.dp,
                    state = KxOrbState.Active
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Tap to speak or type below",
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.SlateGray
            )

            Spacer(Modifier.weight(1f))

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AssistantQuickAction("🎤", "Voice") { }
                AssistantQuickAction("📷", "Camera") { }
                AssistantQuickAction("🖥️", "Screen") { }
            }

            Spacer(Modifier.height(16.dp))

            // Input Field
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Ask me anything...") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KaironexColors.ElectricBlue,
                    unfocusedBorderColor = KaironexColors.CloudGray
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun AssistantQuickAction(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(KaironexColors.CloudGray)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
    }
}

