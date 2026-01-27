package com.mursaline.kaironex.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GoogleDriveLinkScreen : Screen {
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var isConnecting by remember { mutableStateOf(false) }
        var isConnected by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = Color.White
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isConnected) KaironexColors.SuccessGreen.copy(alpha=0.1f) else KaironexColors.CloudGray,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CloudUpload, 
                            null, 
                            tint = if (isConnected) KaironexColors.SuccessGreen else KaironexColors.GeminiBlurple,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    if (isConnected) "Drive Connected!" else "Connect Resources",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.Slate900
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "Link your Google Drive so Kaironex can access your syllabus, notes, and past papers to help you study.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KaironexColors.Slate500,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(48.dp))
                
                if (!isConnected) {
                    Button(
                        onClick = {
                            isConnecting = true
                            // Mock connection delay
                            // In real app, launch Google Auth flow here
                            scope.launch {
                                delay(2000) 
                                isConnecting = false
                                isConnected = true
                            }
                        },
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                         if (isConnecting) {
                             CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                             Spacer(Modifier.width(12.dp))
                             Text("Connecting...")
                         } else {
                             Text("Link Google Drive", fontSize = 16.sp)
                         }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { navigator.replaceAll(MainShellScreen) }
                    ) {
                        Text("Skip for now", color = KaironexColors.Slate500)
                    }
                } else {
                    Button(
                        onClick = { navigator.replaceAll(MainShellScreen) },
                        colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.SuccessGreen),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Enter Dashboard", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
