package com.mursaline.kaironex.features.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.Image
import androidx.compose.ui.UiComposable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.features.genesis.GenesisIntroScreen
import com.mursaline.kaironex.features.onboarding.SystemSetupScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch

object SignupScreen : Screen {
    private fun readResolve(): Any = SignupScreen
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    @UiComposable
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

        AuthLayout(navToSignup = true) {
            @Suppress("UnusedBoxWithConstraintsScope")
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDesktop = with(LocalDensity.current) { constraints.maxWidth.toDp() > 800.dp }
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
                                // Branding
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KaironexColors.Indigo600), 
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("K", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Kaironex", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp, letterSpacing = (-1).sp)
                                }

                                // Hero Text
                                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Text(
                                        text = "Negotiated Autonomy.",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 56.sp
                                    )
                                    Text(
                                        text = "Kaironex is not a blocker. It is a Consensual Cognitive OS.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFFC7D2FE), 
                                        lineHeight = 28.sp
                                    )
                                }
                                
                                // Footer
                                Text("© 2026 Kaironex System v2.4.0", color = KaironexColors.Indigo600, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // RIGHT SIDE - FORM
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
                            Text("Create account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Get started with your free 14-day trial.", style = MaterialTheme.typography.bodyMedium, color = KaironexColors.Slate500)

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
                                Text("OR CONTINUE WITH EMAIL", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500, modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(modifier = Modifier.weight(1f).height(1.dp).background(KaironexColors.Slate100))
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Name
                            Text("Full Name", style = MaterialTheme.typography.labelMedium, color = KaironexColors.Slate800, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("John Doe", color = KaironexColors.Slate500.copy(alpha=0.7f)) },
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
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = KaironexColors.Slate500) }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

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
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = KaironexColors.Slate500) }
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Password
                            Text("Password", style = MaterialTheme.typography.labelMedium, color = KaironexColors.Slate800, fontWeight = FontWeight.Medium)
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
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = KaironexColors.Slate500) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = KaironexColors.Slate500)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Confirm Password
                            Text("Confirm Password", style = MaterialTheme.typography.labelMedium, color = KaironexColors.Slate800, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    passwordError = if (it != password && it.isNotEmpty()) "Passwords don't match" else null
                                },
                                placeholder = { Text("••••••••", color = KaironexColors.Slate500.copy(alpha=0.7f)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = KaironexColors.Slate50,
                                    focusedIndicatorColor = if (passwordError != null) KaironexColors.Rose500 else KaironexColors.Indigo600,
                                    unfocusedIndicatorColor = if (passwordError != null) KaironexColors.Rose500 else KaironexColors.Slate100,
                                    cursorColor = KaironexColors.Indigo600,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                singleLine = true,
                                isError = passwordError != null,
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (passwordError != null) KaironexColors.Rose500 else KaironexColors.Slate500) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = KaironexColors.Slate500)
                                    }
                                }
                            )

                            // Error message
                            if (passwordError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    passwordError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KaironexColors.Rose500
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Submit
                            Button(
                                onClick = {
                                     if (password != confirmPassword) {
                                         passwordError = "Passwords don't match"
                                         return@Button
                                     }
                                     scope.launch {
                                         val result = authRepo.signup(name, email, password)
                                         if (result.isSuccess) {
                                             navigator.replaceAll(SystemSetupScreen(userName = name))
                                         }
                                     }
                                },
                                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && passwordError == null,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
