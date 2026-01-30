package com.mursaline.kaironex.features.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Suppress("unused")
object LoginScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = LoginScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val authRepo = remember { MockAuthRepository() }

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        // Floating animation
        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatOffset"
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.White)) {
            val isDesktop = maxWidth > 800.dp
            val contentPadding = if (maxWidth > 1200.dp) 56.dp else if (maxWidth > 800.dp) 48.dp else 32.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT SIDE - HERO (Desktop Only)
                if (isDesktop) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1a1a2e),
                                        Color(0xFF16213e),
                                        Color(0xFF0f3460)
                                    )
                                )
                            )
                    ) {
                        // Robot Image with floating effect
                        Image(
                            painter = painterResource(Res.drawable.robot_hero),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.5f)
                                .graphicsLayer { translationY = floatOffset * 0.5f },
                            contentScale = ContentScale.Crop
                        )

                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF1a1a2e).copy(alpha = 0.8f),
                                            Color(0xFF1a1a2e).copy(alpha = 0.95f)
                                        )
                                    )
                                )
                        )

                        // Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Branding
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Animated Logo
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .graphicsLayer {
                                            rotationZ = floatOffset * 0.5f
                                            scaleX = 1f + (floatOffset / 200f)
                                            scaleY = 1f + (floatOffset / 200f)
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    KaironexColors.GeminiBlurple,
                                                    Color(0xFF7c3aed)
                                                )
                                            )
                                        )
                                        .border(
                                            2.dp,
                                            Color.White.copy(alpha = 0.3f),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("K", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Kaironex",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        letterSpacing = (-1).sp
                                    )
                                    Text(
                                        "Student Life Operating System",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Hero Text with floating effect
                            Column(
                                modifier = Modifier.graphicsLayer { translationY = floatOffset },
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = "Welcome back.",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 56.sp
                                )

                                Text(
                                    text = "Your AI-powered academic companion is ready to help you achieve excellence.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Feature highlights
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    FeatureHighlight(
                                        icon = "🧠",
                                        title = "AI-Powered Learning",
                                        description = "Gemini 3 understands your unique learning style"
                                    )
                                    FeatureHighlight(
                                        icon = "📊",
                                        title = "Smart Pressure Management",
                                        description = "Balance academics, career, and life effortlessly"
                                    )
                                    FeatureHighlight(
                                        icon = "🎯",
                                        title = "Goal-Oriented System",
                                        description = "Track progress and stay motivated every day"
                                    )
                                }
                            }

                            // Stats Footer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("30K+", "Active Students")
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                                StatItem("95%", "Success Rate")
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                                StatItem("4.9★", "User Rating")
                            }
                        }
                    }
                }

                // RIGHT SIDE - FORM
                Box(
                    modifier = Modifier
                        .weight(if (isDesktop) 0.8f else 1f)
                        .fillMaxHeight()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .padding(horizontal = 32.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        // Mobile Logo & Robot
                        if (!isDesktop) {
                            // Floating Robot
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .graphicsLayer {
                                        translationY = -floatOffset
                                        scaleX = 1f + (floatOffset / 120f)
                                        scaleY = 1f + (floatOffset / 120f)
                                    }
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.robot_hero),
                                    contentDescription = "Kaironex",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Logo
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(KaironexColors.GeminiBlurple, Color(0xFF7c3aed))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Kaironex", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                                    Text("Student Life OS", fontSize = 11.sp, color = KaironexColors.Slate500)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Header
                        Text(
                            text = "Sign in to your account",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.Slate900,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Continue your learning journey",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KaironexColors.Slate500,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Google Sign In Button
                        OutlinedButton(
                            onClick = { /* Google Sign In */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, KaironexColors.BorderGray),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = KaironexColors.Slate800
                            )
                        ) {
                            // Google Logo (G in brand colors)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "G",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4285F4) // Google Blue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Continue with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Divider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = KaironexColors.BorderGray
                            )
                            Text(
                                text = "or",
                                style = MaterialTheme.typography.bodySmall,
                                color = KaironexColors.Slate500,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 1.dp,
                                color = KaironexColors.BorderGray
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Email address",
                                style = MaterialTheme.typography.labelLarge,
                                color = KaironexColors.Slate800,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; error = null },
                                placeholder = {
                                    Text(
                                        "Enter your email",
                                        color = KaironexColors.Slate500.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = KaironexColors.CloudGray,
                                    focusedBorderColor = KaironexColors.GeminiBlurple,
                                    unfocusedBorderColor = KaironexColors.BorderGray,
                                    cursorColor = KaironexColors.GeminiBlurple,
                                    focusedTextColor = KaironexColors.Slate900,
                                    unfocusedTextColor = KaironexColors.Slate900
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email,
                                        null,
                                        tint = KaironexColors.Slate500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Password Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Password",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KaironexColors.Slate800,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Forgot password?",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KaironexColors.GeminiBlurple,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; error = null },
                                placeholder = {
                                    Text(
                                        "Enter your password",
                                        color = KaironexColors.Slate500.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = KaironexColors.CloudGray,
                                    focusedBorderColor = KaironexColors.GeminiBlurple,
                                    unfocusedBorderColor = KaironexColors.BorderGray,
                                    cursorColor = KaironexColors.GeminiBlurple,
                                    focusedTextColor = KaironexColors.Slate900,
                                    unfocusedTextColor = KaironexColors.Slate900
                                ),
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Lock,
                                        null,
                                        tint = KaironexColors.Slate500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            null,
                                            tint = KaironexColors.Slate500,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // Error Message
                        if (error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = KaironexColors.AlertRed.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        null,
                                        tint = KaironexColors.AlertRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        error!!,
                                        color = KaironexColors.AlertRed,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Sign In Button
                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    authRepo.login(email, password)
                                        .onSuccess {
                                            navigator.replaceAll(MainShellScreen)
                                        }
                                        .onFailure {
                                            error = it.message
                                            isLoading = false
                                        }
                                }
                            },
                            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KaironexColors.GeminiBlurple,
                                contentColor = Color.White,
                                disabledContainerColor = KaironexColors.Slate100,
                                disabledContentColor = KaironexColors.Slate500
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 0.dp
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Sign Up Link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Don't have an account?",
                                color = KaironexColors.Slate500,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Sign up",
                                color = KaironexColors.GeminiBlurple,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable { navigator.push(SignupScreen) }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Judge Access (Shortcut)
                        Text(
                            "🔐 Hackathon Judge Access",
                            color = KaironexColors.Slate500.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.clickable { 
                                navigator.push(com.mursaline.kaironex.features.onboarding.SystemSetupScreen(userName = "Mursaline Huqe"))
                            }
                        )



                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

// Helper Composables
@Composable
private fun FeatureHighlight(icon: String, title: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
