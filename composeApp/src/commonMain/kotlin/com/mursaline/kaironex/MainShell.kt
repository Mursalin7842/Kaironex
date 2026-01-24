package com.mursaline.kaironex

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
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
import com.mursaline.kaironex.ui.components.KxOrb
import com.mursaline.kaironex.ui.components.KxOrbState

/**
 * MainShell - The Orb-Centric Navigation Shell
 *
 * Architecture: Portal-based navigation
 * - Command (Dashboard): The Portal - shows Cortex Hero + Life Tracks
 * - Orb (AI Assistant): The Executor - Gemini Live-like AI assistant
 * - Profile: Account/Settings access
 *
 * The Dashboard IS the router. Users enter Study/Tracks from the Hero Card,
 * not from redundant navbar items.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
object MainShellScreen : Screen {
    private fun readResolve(): Any = MainShellScreen

    @Composable
    override fun Content() {
        var isMenuOpen by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf("Command") }
        var isOrbExpanded by remember { mutableStateOf(false) }
        var isProfileOpen by remember { mutableStateOf(false) }

        Navigator(DashboardScreen) { navigator ->
            val currentRoute = navigator.lastItem

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
                            "Settings" -> isProfileOpen = true
                            else -> { /* Placeholder */ }
                        }
                    }
                ) {
                    if (isMobile) {
                        // MOBILE LAYOUT: Minimal Top Bar + Content + Orb-Centric Bottom Nav
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
                                    actions = {
                                        // Profile in top bar for quick access
                                        IconButton(onClick = { isProfileOpen = true }) {
                                            ProfileAvatar(size = 32.dp)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = KaironexColors.CanvasWhite
                                    )
                                )
                            },
                            bottomBar = {
                                // Orb-Centric Bottom Nav: Command | ORB | Profile
                                OrbCentricMobileNav(
                                    selectedTab = selectedTab,
                                    isOrbExpanded = isOrbExpanded,
                                    onCommandClick = {
                                        selectedTab = "Command"
                                        if (currentRoute !is DashboardScreen) navigator.replace(DashboardScreen)
                                    },
                                    onOrbClick = { isOrbExpanded = !isOrbExpanded },
                                    onProfileClick = { isProfileOpen = true }
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

                                // Profile Sheet
                                if (isProfileOpen) {
                                    ProfileSheet(
                                        onDismiss = { isProfileOpen = false },
                                        isMobile = true
                                    )
                                }
                            }
                        }
                    } else {
                        // DESKTOP LAYOUT: Minimal Side Rail with Orb
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Orb-Centric Desktop Rail
                            OrbCentricDesktopRail(
                                isOrbExpanded = isOrbExpanded,
                                onMenuClick = { isMenuOpen = true },
                                onCommandClick = {
                                    if (currentRoute !is DashboardScreen) navigator.replace(DashboardScreen)
                                },
                                onOrbClick = { isOrbExpanded = !isOrbExpanded },
                                onProfileClick = { isProfileOpen = true }
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

                                // Orb Expanded Panel for Desktop
                                if (isOrbExpanded) {
                                    AssistantPanel(
                                        onDismiss = { isOrbExpanded = false },
                                        isMobile = false,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                }

                                // Profile Sheet for Desktop
                                if (isProfileOpen) {
                                    ProfileSheet(
                                        onDismiss = { isProfileOpen = false },
                                        isMobile = false,
                                        modifier = Modifier.align(Alignment.TopEnd)
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

// ===== ORB-CENTRIC NAVIGATION (Minimal: Command | Orb | Profile) =====

/**
 * Mobile Bottom Navigation - Orb Centric
 * Only 3 elements: Command (Home), Orb (AI), Profile
 */
@Composable
fun OrbCentricMobileNav(
    selectedTab: String,
    isOrbExpanded: Boolean,
    onCommandClick: () -> Unit,
    onOrbClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. COMMAND (The Portal - Dashboard)
            NavItemMinimal(
                icon = Icons.Filled.Dashboard,
                label = "Command",
                isSelected = selectedTab == "Command",
                onClick = onCommandClick
            )

            // 2. CENTER ORB (The Executor - AI Assistant)
            Box(
                modifier = Modifier
                    .offset(y = (-20).dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isOrbExpanded)
                                listOf(KaironexColors.Purple600, KaironexColors.Indigo600)
                            else
                                listOf(KaironexColors.Indigo600, KaironexColors.GeminiBlurple)
                        )
                    )
                    .border(3.dp, Color.White, CircleShape)
                    .clickable(onClick = onOrbClick),
                contentAlignment = Alignment.Center
            ) {
                KxOrb(
                    size = 48.dp,
                    state = if (isOrbExpanded) KxOrbState.Active else KxOrbState.Idle
                )
            }

            // 3. PROFILE (Account/Settings)
            Column(
                modifier = Modifier
                    .clickable(onClick = onProfileClick)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatar(size = 28.dp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }
        }
    }
}

/**
 * Desktop Side Rail - Orb Centric
 * Minimal: Menu, Command, Orb, Profile
 */
@Composable
fun OrbCentricDesktopRail(
    isOrbExpanded: Boolean,
    onMenuClick: () -> Unit,
    onCommandClick: () -> Unit,
    onOrbClick: () -> Unit,
    onProfileClick: () -> Unit
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
            // Menu Button (Access to full navigation)
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = KaironexColors.InkBlack)
            }

            Spacer(Modifier.height(24.dp))

            // COMMAND (Dashboard Portal)
            NavigationRailItem(
                selected = true, // Always "selected" since it's the main view
                onClick = onCommandClick,
                icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Command") },
                label = { Text("Command", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = KaironexColors.ElectricBlue,
                    selectedTextColor = KaironexColors.ElectricBlue,
                    indicatorColor = KaironexColors.CloudGray
                )
            )

            Spacer(Modifier.weight(1f))

            // CENTER ORB (AI Assistant)
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                    size = 48.dp,
                    state = if (isOrbExpanded) KxOrbState.Active else KxOrbState.Idle
                )
            }

            Spacer(Modifier.weight(1f))

            // PROFILE (Account/Settings)
            Column(
                modifier = Modifier
                    .clickable(onClick = onProfileClick)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileAvatar(size = 36.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ===== HELPER COMPONENTS =====

@Composable
fun NavItemMinimal(
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
            modifier = Modifier.size(24.dp),
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
fun ProfileAvatar(size: androidx.compose.ui.unit.Dp) {
    Surface(
        shape = CircleShape,
        color = KaironexColors.GeminiBlurple,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "S",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = if (size > 30.dp) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall
            )
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
    val panelHeight = if (isMobile) Modifier.height(450.dp) else Modifier.fillMaxHeight()

    Surface(
        modifier = modifier.then(panelWidth).then(panelHeight).padding(if (isMobile) 16.dp else 0.dp),
        color = KaironexColors.CanvasWhite,
        shape = if (isMobile) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
               else RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
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
                    "Kaironex AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", color = KaironexColors.SlateGray)
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
                shape = RoundedCornerShape(16.dp)
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
            .clip(RoundedCornerShape(12.dp))
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

// ===== PROFILE SHEET =====

@Composable
fun ProfileSheet(
    onDismiss: () -> Unit,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    val sheetWidth = if (isMobile) Modifier.fillMaxWidth() else Modifier.width(320.dp)
    val sheetHeight = if (isMobile) Modifier.fillMaxHeight(0.7f) else Modifier.fillMaxHeight()

    Surface(
        modifier = modifier.then(sheetWidth).then(sheetHeight),
        color = KaironexColors.CanvasWhite,
        shape = if (isMobile) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
               else RoundedCornerShape(bottomStart = 16.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile & Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.InkBlack
                )
                TextButton(onClick = onDismiss) {
                    Text("Done", color = KaironexColors.ElectricBlue)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Profile Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Student User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                    Text(
                        "student@university.edu",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Settings Options
            ProfileSettingItem("🔔", "Notifications")
            ProfileSettingItem("🎨", "Appearance")
            ProfileSettingItem("🔒", "Privacy & Security")
            ProfileSettingItem("📊", "Study Statistics")
            ProfileSettingItem("❓", "Help & Support")

            Spacer(Modifier.weight(1f))

            // Sign Out
            TextButton(
                onClick = { /* TODO: Sign out */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out", color = KaironexColors.AlertRed)
            }
        }
    }
}

@Composable
private fun ProfileSettingItem(emoji: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO */ }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = KaironexColors.InkBlack
        )
    }
}
