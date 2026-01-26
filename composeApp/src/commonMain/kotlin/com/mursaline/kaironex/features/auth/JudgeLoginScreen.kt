package com.mursaline.kaironex.features.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

object JudgeLoginScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val authRepo = remember { MockAuthRepository() }

        var accessCode by remember { mutableStateOf("0000") } // Pre-filled for convenience
        var error by remember { mutableStateOf<String?>(null) }

        AuthLayout {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Evaluation Protocols",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF6200EA), // Deep Purple accent
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Standard biometric locks engaged. Override required.",
                    color = Color(0xFF1A1A2E).copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Access Code
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = { accessCode = it },
                    label = { Text("Admin Access Code") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6200EA),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        cursorColor = Color(0xFF6200EA),
                        focusedLabelColor = Color(0xFF6200EA),
                        unfocusedLabelColor = Color.Gray,
                        focusedContainerColor = Color.White.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error!!, color = Color.Red, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login Action
                Button(
                    onClick = {
                        scope.launch {
                            authRepo.judgeLogin(accessCode)
                                .onSuccess {
                                    // For judges, skip to main app directly
                                    navigator.replaceAll(com.mursaline.kaironex.MainShellScreen)
                                    // Optionally push AgentSpaceScreen if MainShell handles tabs:
                                    // But since MainShell defaults to Dashboard, we might want to ensure they land on Agents.
                                    // For now, let's stick to MainShellScreen and rely on user clicking "Agents".
                                    // OR, if we want to force it:
                                    // navigator.replaceAll(com.mursaline.kaironex.features.agents.AgentSpaceScreen) 
                                    // BUT MainShell has the bottom bar. AgentSpaceScreen is just content.
                                    // The Judge Mode really wants full visibility.
                                    // Let's keep MainShellScreen for now as requested by user "Judges see a distinct architecture" by BROWSING to it.
                                    // Actually, let's verify if AgentSpaceScreen is a standalone screen or needs Shell.
                                    // It's a Screen object.
                                }
                                .onFailure { error = it.message }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EA).copy(alpha = 0.9f) // Purple button
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Override Security", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = { navigator.pop() }) {
                    Text("[Cancel Override]", color = Color(0xFF1A1A2E).copy(alpha = 0.5f))
                }
            }
        }
    }
}
