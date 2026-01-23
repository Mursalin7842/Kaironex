package com.mursaline.kaironex.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.alpha
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.features.genesis.GenesisIntroScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch

object LoginScreen : Screen {
    private fun readResolve(): Any = LoginScreen
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @UiComposable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val authRepo = remember { MockAuthRepository() }
        val density = LocalDensity.current

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AuthLayout {
            @Suppress("UnusedBoxWithConstraintsScope")
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDesktop = constraints.maxWidth > with(density) { 800.dp.roundToPx() }
                Row(modifier = Modifier.fillMaxSize()) {
                    
                    // LEFT SIDE - HERO (Desktop Only)
                    if (isDesktop) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(KaironexColors.Slate900)
                        ) {
                             // Robot Image
                             Image(
                                 painter = painterResource(Res.drawable.robot_hero),
                                 contentDescription = null,
                                 modifier = Modifier.fillMaxSize().alpha(0.6f),
                                 contentScale = ContentScale.Crop
                             )

                            // Background Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                KaironexColors.IndigoGradientStart.copy(alpha=0.4f),
                                                KaironexColors.IndigoGradientEnd.copy(alpha=0.9f)
                                            )
                                        )
                                    )
                            )
                            
                            // Content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(48.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
// ... (Branding Same)
                                // Branding
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KaironexColors.Indigo600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "K",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Kaironex",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        letterSpacing = (-1).sp
                                    )
                                }

                                // Hero Text
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Text(
                                        text = "Welcome back, Architect.",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 56.sp
                                    )
                                    Text(
                                        text = "System status: Optimal. Your cognitive workspace is ready.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFC7D2FE), // Indigo-200
                                        lineHeight = 28.sp
                                    )
                                }
                                
                                // Footer
                                Text("© 2026 Kaironex System v2.4.0", color = KaironexColors.Indigo600, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // RIGHT SIDE - FORM (Always Visible)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.White)
                            .padding(if (isDesktop) 48.dp else 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.widthIn(max = 400.dp).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.Start
                        ) {
                            
                            // Mobile Logo
                            if (!isDesktop) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    modifier = Modifier.padding(bottom = 32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KaironexColors.Indigo600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("K", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kaironex", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                                }
                            }

                            // Header
                            Text(
                                text = "Sign in",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = KaironexColors.Slate900
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter your details to access your workspace.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KaironexColors.Slate500
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Social Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(
                                    onClick = { /* TODO */ },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KaironexColors.Slate100),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KaironexColors.Slate500)
                                ) {
                                    // Placeholder Icon
                                    Text("Google", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { /* TODO */ },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, KaironexColors.Slate100),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KaironexColors.Slate500)
                                ) {
                                    Text("Github", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Divider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(KaironexColors.Slate100))
                                Text(
                                    text = "OR CONTINUE WITH EMAIL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KaironexColors.Slate500,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(KaironexColors.Slate100))
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Form
                            // Email
                            Text("Email Address", style = MaterialTheme.typography.labelMedium, color = KaironexColors.Slate800, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("name@kaironex.com", color = KaironexColors.Slate500.copy(alpha=0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = KaironexColors.Slate50,
                                    focusedIndicatorColor = KaironexColors.Indigo600,
                                    unfocusedIndicatorColor = KaironexColors.Slate100,
                                    cursorColor = KaironexColors.Indigo600,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = KaironexColors.Slate500)
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Password
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Password", style = MaterialTheme.typography.labelMedium, color = KaironexColors.Slate800, fontWeight = FontWeight.Medium)
                                Text(
                                    "Forgot password?", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = KaironexColors.Indigo600,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {}
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("••••••••", color = KaironexColors.Slate500.copy(alpha=0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = KaironexColors.Slate50,
                                    focusedIndicatorColor = KaironexColors.Indigo600,
                                    unfocusedIndicatorColor = KaironexColors.Slate100,
                                    cursorColor = KaironexColors.Indigo600,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = KaironexColors.Slate500)
                                },
                                trailingIcon = {
                                    val image = if (passwordVisible)
                                        Icons.Filled.Visibility
                                    else
                                        Icons.Filled.VisibilityOff

                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = null, tint = KaironexColors.Slate500)
                                    }
                                }
                            )

                            if (error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = error!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Submit Button
                            Button(
                                onClick = {
                                     scope.launch {
                                         val result = authRepo.login(email, password)
                                         if (result.isSuccess) {
                                             navigator.replaceAll(GenesisIntroScreen)
                                         } else {
                                             error = result.exceptionOrNull()?.message
                                         }
                                     }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KaironexColors.Indigo600
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp,
                                    focusedElevation = 4.dp,
                                    hoveredElevation = 6.dp,
                                    disabledElevation = 0.dp
                                )
                            ) {
                                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Footer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Don't have an account? ", color = Color(0xFF64748B))
                                Text(
                                    "Sign up", 
                                    color = Color(0xFF4F46E5),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { navigator.push(SignupScreen) }
                                )
                            }
                            
                             Spacer(modifier = Modifier.height(16.dp))
                             // Judge Link
                             TextButton(onClick = { navigator.push(JudgeLoginScreen) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("Judge Access >>", color = Color(0xFF00B0FF))
                            }
                        }
                    }
                }
            }
        }
    }
}
