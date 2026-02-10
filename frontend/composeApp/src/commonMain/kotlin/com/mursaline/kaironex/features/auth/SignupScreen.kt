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
import com.mursaline.kaironex.features.onboarding.SystemSetupScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Suppress("unused")
object SignupScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = SignupScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val authRepo = remember { MockAuthRepository() }

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }
        var passwordError by remember { mutableStateOf<String?>(null) }
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

        @Suppress("UnusedBoxWithConstraintsScope")
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.White)) {
            val isDesktop = maxWidth > 800.dp

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
                        // Robot Image
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
                                .padding(56.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Branding
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                                colors = listOf(KaironexColors.GeminiBlurple, Color(0xFF7c3aed))
                                            )
                                        )
                                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("K", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Kaironex", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 28.sp, letterSpacing = (-1).sp)
                                    Text("Student Life Operating System", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f), letterSpacing = 1.sp)
                                }
                            }

                            // Hero Text
                            Column(
                                modifier = Modifier.graphicsLayer { translationY = floatOffset },
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = "Start your journey.",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 56.sp
                                )

                                Text(
                                    text = "Join thousands of students transforming their academic life with AI-powered guidance.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // What you get
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    BenefitRow("✓", "AI study companion powered by Gemini 3")
                                    BenefitRow("✓", "Smart schedule & pressure management")
                                    BenefitRow("✓", "Career, finance & life support agents")
                                    BenefitRow("✓", "Personalized learning optimization")
                                }
                            }

                            // Testimonial
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⭐⭐⭐⭐⭐", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Rated 4.9/5", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "\"Kaironex changed how I manage my studies. It's like having a personal assistant that actually understands student life.\"",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("— Sarah K., Computer Science", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
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
                        Spacer(modifier = Modifier.height(32.dp))

                        // Mobile Logo
                        if (!isDesktop) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(colors = listOf(KaironexColors.GeminiBlurple, Color(0xFF7c3aed)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Kaironex", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                                    Text("Student Life OS", fontSize = 11.sp, color = KaironexColors.Slate500)
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        // Header
                        Text(
                            text = "Create your account",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.Slate900,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Get started with your AI companion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KaironexColors.Slate500,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Google Sign Up Button
                        OutlinedButton(
                            onClick = { /* Google Sign Up */ },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, KaironexColors.BorderGray),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = KaironexColors.Slate800)
                        ) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = KaironexColors.BorderGray)
                            Text("or", style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500, modifier = Modifier.padding(horizontal = 20.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = KaironexColors.BorderGray)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Name Field
                        FormField(
                            label = "Full name",
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Enter your name",
                            leadingIcon = Icons.Default.Person
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Field
                        FormField(
                            label = "Email address",
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Enter your email",
                            leadingIcon = Icons.Default.Email
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Password", style = MaterialTheme.typography.labelLarge, color = KaironexColors.Slate800, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordError = if (confirmPassword.isNotEmpty() && it != confirmPassword) "Passwords don't match" else null
                                },
                                placeholder = { Text("Min. 8 characters", color = KaironexColors.Slate500.copy(alpha = 0.6f)) },
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
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = KaironexColors.Slate500, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = KaironexColors.Slate500, modifier = Modifier.size(20.dp))
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Confirm Password Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Confirm password", style = MaterialTheme.typography.labelLarge, color = KaironexColors.Slate800, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    passwordError = if (it != password && it.isNotEmpty()) "Passwords don't match" else null
                                },
                                placeholder = { Text("Re-enter password", color = KaironexColors.Slate500.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = KaironexColors.CloudGray,
                                    focusedBorderColor = if (passwordError != null) KaironexColors.AlertRed else KaironexColors.GeminiBlurple,
                                    unfocusedBorderColor = if (passwordError != null) KaironexColors.AlertRed else KaironexColors.BorderGray,
                                    cursorColor = KaironexColors.GeminiBlurple,
                                    focusedTextColor = KaironexColors.Slate900,
                                    unfocusedTextColor = KaironexColors.Slate900
                                ),
                                singleLine = true,
                                isError = passwordError != null,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = if (passwordError != null) KaironexColors.AlertRed else KaironexColors.Slate500, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = KaironexColors.Slate500, modifier = Modifier.size(20.dp))
                                    }
                                }
                            )
                            if (passwordError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(passwordError!!, color = KaironexColors.AlertRed, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Create Account Button
                        Button(
                            onClick = {
                                if (password != confirmPassword) {
                                    passwordError = "Passwords don't match"
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    authRepo.signup(name, email, password)
                                        .onSuccess {
                                            // For hackathon: Always use demo_user_001
                                            com.mursaline.kaironex.core.CurrentUser.loginWithId(
                                                userId = "demo_user_001",
                                                name = name
                                            )
                                            navigator.replaceAll(SystemSetupScreen(userName = name))
                                        }
                                        .onFailure { passwordError = it.message; isLoading = false }
                                }
                            },
                            enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && passwordError == null && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KaironexColors.GeminiBlurple,
                                contentColor = Color.White,
                                disabledContainerColor = KaironexColors.Slate100,
                                disabledContentColor = KaironexColors.Slate500
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Terms
                        Text(
                            "By signing up, you agree to our Terms of Service and Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.Slate500,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign In Link
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Already have an account?", color = KaironexColors.Slate500, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign in", color = KaironexColors.GeminiBlurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.clickable { navigator.pop() })
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp, color = KaironexColors.SuccessGreen)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = KaironexColors.Slate800, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = KaironexColors.Slate500.copy(alpha = 0.6f)) },
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
            leadingIcon = { Icon(leadingIcon, null, tint = KaironexColors.Slate500, modifier = Modifier.size(20.dp)) }
        )
    }
}
