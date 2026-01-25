package com.mursaline.kaironex

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.transitions.SlideTransition
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.features.dashboard.DashboardScreen
import com.mursaline.kaironex.features.profile.ProfileScreen
import com.mursaline.kaironex.features.study.StudySessionsScreen
import com.mursaline.kaironex.features.agents.LifeSupportAgentsScreen
import com.mursaline.kaironex.ui.components.KxOrb


/**
 * MainShell - The Trinity Navigation Shell
 *
 * Architecture: Clean "Trinity" navigation
 * - Command (Home): The Dashboard Portal
 * - Orb (AI): Immersive full-screen AI assistant
 * - Profile: Dedicated settings/account page
 *
 * NO hamburger menu. The Dashboard IS the router.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
object MainShellScreen : Screen {
    private fun readResolve(): Any = MainShellScreen

    @Composable
    override fun Content() {
        var isOrbExpanded by remember { mutableStateOf(false) }

        Navigator(DashboardScreen) { navigator ->
            val currentRoute = navigator.lastItem

            // Determine active tab for UI highlighting
            val selectedTab = when (currentRoute) {
                is DashboardScreen -> "Home"
                is StudySessionsScreen -> "Study"
                is LifeSupportAgentsScreen -> "More"
                is ProfileScreen -> "Profile"
                else -> "Home" // Default to Home for detail screens
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
                val isMobile = maxWidth < 800.dp

                Scaffold(
                    bottomBar = {
                        if (isMobile) {
                            FiveItemNavBar(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    when (tab) {
                                        "Home" -> navigator.replaceAll(DashboardScreen)
                                        "Study" -> navigator.push(StudySessionsScreen)
                                        "More" -> navigator.push(com.mursaline.kaironex.features.agents.AgentSpaceScreen)
                                        "Profile" -> navigator.push(ProfileScreen)
                                    }
                                },
                                onOrbClick = { isOrbExpanded = true }
                            )
                        }
                    },
                    containerColor = KaironexColors.CloudGray
                ) { paddingValues ->
                    Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                        // DESKTOP: Navigation Rail (Left Side)
                        if (!isMobile) {
                            FiveItemNavRail(
                                selectedTab = selectedTab,
                                onTabSelected = { tab ->
                                    when (tab) {
                                        "Home" -> navigator.replaceAll(DashboardScreen)
                                        "Study" -> navigator.push(StudySessionsScreen)
                                        "More" -> navigator.push(com.mursaline.kaironex.features.agents.AgentSpaceScreen)
                                        "Profile" -> navigator.push(ProfileScreen)
                                    }
                                },
                                onOrbClick = { isOrbExpanded = true }
                            )
                        }

                        // MAIN CONTENT AREA
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            SlideTransition(navigator)
                        }
                    }
                }

// === THE IMMERSIVE ORB OVERLAY ===
                AnimatedVisibility(
                    visible = isOrbExpanded,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.fillMaxSize().zIndex(99f)
                ) {
                    // Handle back press to close orb instead of closing app
                    com.mursaline.kaironex.platform.PlatformBackHandler(enabled = isOrbExpanded) {
                        isOrbExpanded = false
                    }

                    ImmersiveAssistantPanel(
                        onDismiss = { isOrbExpanded = false },
                        isMobile = isMobile
                    )
                }
            }
        }
    }
}

// ===== 1. FIVE-ITEM NAVBAR (Mobile) =====
// Home | Study | ORB | More | Profile
@Composable
fun FiveItemNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOrbClick: () -> Unit
) {
    // Container with extra space for the floating orb
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp) // Extra height to accommodate floating orb
    ) {
        // The actual navbar surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .align(Alignment.BottomCenter),
            color = KaironexColors.CanvasWhite,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME
                NavIconItemCompact(
                    icon = Icons.Filled.Home,
                    label = "Home",
                    isSelected = selectedTab == "Home",
                    onClick = { onTabSelected("Home") }
                )

                // 2. STUDY
                NavIconItemCompact(
                    icon = Icons.Filled.School,
                    label = "Study",
                    isSelected = selectedTab == "Study",
                    onClick = { onTabSelected("Study") }
                )

                // Spacer for center orb
                Spacer(Modifier.width(56.dp))

                // 4. MORE (Agents/Life)
                NavIconItemCompact(
                    icon = Icons.Filled.MoreHoriz,
                    label = "Agents",
                    isSelected = selectedTab == "More",
                    onClick = { onTabSelected("More") }
                )

                // 5. PROFILE
                NavIconItemCompact(
                    icon = Icons.Filled.Person,
                    label = "Profile",
                    isSelected = selectedTab == "Profile",
                    onClick = { onTabSelected("Profile") }
                )
            }
        }

        // 3. CENTER ORB (Floating above the navbar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(KaironexColors.Indigo600, KaironexColors.GeminiBlurple)
                    )
                )
                .border(3.dp, KaironexColors.CanvasWhite, CircleShape)
                .clickable(onClick = onOrbClick),
            contentAlignment = Alignment.Center
        ) {
            KxOrb(
                isAgentSpeaking = false,
                isUserListening = false,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun NavIconItemCompact(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) KaironexColors.ElectricBlue else KaironexColors.SlateGray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) KaironexColors.ElectricBlue else KaironexColors.SlateGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ===== 2. FIVE-ITEM NAV RAIL (Desktop) =====
@Composable
fun FiveItemNavRail(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onOrbClick: () -> Unit
) {
    Surface(
        modifier = Modifier.width(80.dp).fillMaxHeight(),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. HOME
            NavIconItemCompact(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = selectedTab == "Home",
                onClick = { onTabSelected("Home") }
            )

            // 2. STUDY
            NavIconItemCompact(
                icon = Icons.Filled.School,
                label = "Study",
                isSelected = selectedTab == "Study",
                onClick = { onTabSelected("Study") }
            )

            // 3. ORB (Center)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(KaironexColors.Indigo600, KaironexColors.GeminiBlurple)
                        )
                    )
                    .border(2.dp, KaironexColors.CloudGray, CircleShape)
                    .clickable(onClick = onOrbClick),
                contentAlignment = Alignment.Center
            ) {
            KxOrb(
                isAgentSpeaking = false,
                isUserListening = false,
                modifier = Modifier.size(40.dp)
            )
            }

            // 4. MORE (Life Support Agents)
            NavIconItemCompact(
                icon = Icons.Filled.MoreHoriz,
                label = "More",
                isSelected = selectedTab == "More",
                onClick = { onTabSelected("More") }
            )

            // 5. PROFILE
            NavIconItemCompact(
                icon = Icons.Filled.Person,
                label = "Profile",
                isSelected = selectedTab == "Profile",
                onClick = { onTabSelected("Profile") }
            )
        }
    }
}

// ===== 3. IMMERSIVE ASSISTANT PANEL (Full-Screen Overlay) =====
@Composable
fun ImmersiveAssistantPanel(
    onDismiss: () -> Unit,
    isMobile: Boolean
) {
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val orbFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbFloat"
    )

    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    var inputText by remember { mutableStateOf("") }

    // Full-screen overlay with glassmorphism effect
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        KaironexColors.GeminiBlurple.copy(alpha = 0.95f),
                        KaironexColors.Indigo900.copy(alpha = 0.98f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { } // Consume clicks
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branding
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Kaironex", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("AI Assistant", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Close Button
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (isMobile) 48.dp else 80.dp))

            // Central Orb with floating effect
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = -orbFloat
                        scaleX = orbPulse
                        scaleY = orbPulse
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(if (isMobile) 200.dp else 260.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(if (isMobile) 160.dp else 200.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        )
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner orb
                    Box(
                        modifier = Modifier
                            .size(if (isMobile) 120.dp else 150.dp)
                            .shadow(24.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White,
                                        KaironexColors.CloudGray
                                    )
                                )
                            )
                            .border(
                                3.dp,
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color.White.copy(alpha = 0.5f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        KxOrb(
                            isAgentSpeaking = true,
                            isUserListening = true,
                            modifier = Modifier.size(if (isMobile) 100.dp else 130.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // Status Text
            Text(
                "I'm listening...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Speak naturally or type your request below",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
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

            Spacer(Modifier.height(24.dp))

            // Input Field with glassmorphism
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask me anything...", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        ),
                        singleLine = true
                    )

                    FloatingActionButton(
                        onClick = { },
                        containerColor = Color.White,
                        contentColor = KaironexColors.GeminiBlurple,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }

            Spacer(Modifier.height(if (isMobile) 16.dp else 32.dp))
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
