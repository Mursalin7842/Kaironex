package com.mursaline.kaironex

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.mursaline.kaironex.features.agents.AgentDashboardScreen
import com.mursaline.kaironex.features.agents.IncomingAgentCallOverlay
import com.mursaline.kaironex.features.agents.AgentVoiceCallScreen
import com.mursaline.kaironex.brain.AgentCall


import kotlinx.coroutines.launch
import org.koin.compose.koinInject


/**
 * MainShell - The Trinity Navigation Shell
 *
 * Architecture: Clean "Trinity" navigation
 * - Command (Home): The Dashboard Portal
 * - Orb (AI): Immersive full-screen AI assistant
 * - Profile: Dedicated settings/account page
 *
 * Features:
 * - Real-time brain sync
 * - Autonomous agent calling
 *
 * NO hamburger menu. The Dashboard IS the router.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Suppress("unused")
object MainShellScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = MainShellScreen

    @Composable
    override fun Content() {
        var isOrbExpanded by remember { mutableStateOf(false) }
        var activeAgentCall by remember { mutableStateOf<com.mursaline.kaironex.brain.AgentCall?>(null) }

        // Get brain client from DI
        val brainClient: com.mursaline.kaironex.brain.BrainApiClient = org.koin.compose.koinInject()
        val appwriteBridge: com.mursaline.kaironex.brain.AppwriteBridge = org.koin.compose.koinInject()
        val httpClient: io.ktor.client.HttpClient = org.koin.compose.koinInject()

        // Session manager for data sync
        val sessionManager = com.mursaline.kaironex.core.rememberKaironexSession(
            brainClient = brainClient,
            appwriteBridge = appwriteBridge,
            httpClient = httpClient
        )

        Navigator(DashboardScreen) { navigator ->
            val currentRoute = navigator.lastItem

            // Initialize session
            LaunchedEffect(Unit) {
                // Use authenticated user identity from CurrentUser
                val userId = com.mursaline.kaironex.core.CurrentUser.userId.ifBlank { 
                    // Fallback: If somehow we got here without auth, use session manager's existing userId
                    sessionManager.userId.value ?: "uninitialized"
                }
                
                // Initialize session only if not already initialized
                if (!sessionManager.isInitialized.value) {
                    sessionManager.initialize(userId)
                }
            }

            // Cleanup on dispose
            DisposableEffect(Unit) {
                onDispose {
                    sessionManager.cleanup()
                }
            }

            // Determine active tab for UI highlighting
            val selectedTab = when (currentRoute) {
                is DashboardScreen -> "Home"
                is StudySessionsScreen -> "Study"
                is LifeSupportAgentsScreen -> "More"
                is ProfileScreen -> "Profile"
                else -> "Home" // Default to Home for detail screens
            }

            @Suppress("UnusedBoxWithConstraintsScope")
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
                                        "More" -> navigator.push(LifeSupportAgentsScreen)
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
                                        "More" -> navigator.push(LifeSupportAgentsScreen)
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
                        isMobile = isMobile,
                        onStartVoiceCall = {
                            isOrbExpanded = false
                            // Voice call triggers agent call now
                            sessionManager.triggerAgentCall()
                        },
                        onOpenAgentDashboard = {
                            isOrbExpanded = false
                            navigator.push(AgentDashboardScreen)
                        }
                    )
                }

                // === INCOMING AGENT CALL OVERLAY ===
                // This is the MAIN FEATURE: agents call the user autonomously
                val incomingCall by sessionManager.agentCallService.incomingCall.collectAsState()
                incomingCall?.let { call ->
                    IncomingAgentCallOverlay(
                        call = call,
                        onAccept = {
                            sessionManager.agentCallService.acceptCall(call.id)
                            // Open dedicated agent call screen (agent speaks first)
                            activeAgentCall = call
                        },
                        onDismiss = {
                            sessionManager.agentCallService.dismissCall(call.id)
                        },
                        modifier = Modifier.fillMaxSize().zIndex(100f)
                    )
                }

                // === ACTIVE AGENT VOICE CALL SCREEN ===
                // Full-screen call experience where agent speaks first
                activeAgentCall?.let { call ->
                    AgentVoiceCallScreen(
                        call = call,
                        onEndCall = {
                            activeAgentCall = null
                        },
                        modifier = Modifier.fillMaxSize().zIndex(101f)
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
                .border(3.dp, KaironexColors.CanvasWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            com.mursaline.kaironex.ui.components.VoiceOrb(
                isListening = false,
                onClick = onOrbClick,
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
                    .border(2.dp, KaironexColors.CloudGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
            com.mursaline.kaironex.ui.components.VoiceOrb(
                isListening = false,
                onClick = onOrbClick,
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
// Beautiful, audio-focused interface for user-initiated conversations with Kaironex
@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
@Composable
fun ImmersiveAssistantPanel(
    onDismiss: () -> Unit,
    isMobile: Boolean,
    onStartVoiceCall: () -> Unit = {}, 
    onOpenAgentDashboard: () -> Unit = {}
) {
    val sessionManager = com.mursaline.kaironex.core.LocalKaironexSession.current
    val scope = rememberCoroutineScope()

    // Ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val orbFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbFloat"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // Deep space gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        // Animated background particles/stars effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starCount = 50
            for (i in 0 until starCount) {
                val x = (size.width * ((i * 7) % 100) / 100f)
                val y = (size.height * ((i * 13) % 100) / 100f)
                val alpha = 0.3f + (i % 5) * 0.1f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 1.5f + (i % 3),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Minimal top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4ADE80))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Kaironex Online",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Close button - minimal
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.15f))

            // Central Orb Area - The star of the show
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = -orbFloat },
                contentAlignment = Alignment.Center
            ) {
                // Outer rotating ring
                Box(
                    modifier = Modifier
                        .size(if (isMobile) 320.dp else 400.dp)
                        .graphicsLayer { rotationZ = ringRotation }
                        .drawBehind {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1).copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color(0xFF8B5CF6).copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color(0xFF6366F1).copy(alpha = 0.3f)
                                    )
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                )

                // Middle glow ring
                Box(
                    modifier = Modifier
                        .size(if (isMobile) 280.dp else 350.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF6366F1).copy(alpha = glowPulse * 0.4f),
                                    Color(0xFF8B5CF6).copy(alpha = glowPulse * 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // The WebOrb - Full audio experience
                com.mursaline.kaironex.ui.components.WebOrb(
                    apiKey = com.mursaline.kaironex.PlatformSecrets.apiKey,
                    modifier = Modifier.size(if (isMobile) 240.dp else 300.dp),
                    onProfileUpdate = { field, value ->
                        scope.launch {
                            sessionManager?.getStatsRepository()?.updateProfileField(field, value)
                        }
                    },
                    onAgentState = { _ -> }
                )
            }

            Spacer(Modifier.height(48.dp))

            // Elegant status text
            Text(
                "I'm listening",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Ask me anything about your studies, schedule, or goals",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(Modifier.weight(0.2f))

            // Subtle hint at bottom
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = if (isMobile) 32.dp else 48.dp)
            ) {
                // Waveform hint
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { i ->
                        val height by infiniteTransition.animateFloat(
                            initialValue = 8f,
                            targetValue = 24f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600 + i * 100, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wave$i"
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.6f))
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Speak naturally • I understand context",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
