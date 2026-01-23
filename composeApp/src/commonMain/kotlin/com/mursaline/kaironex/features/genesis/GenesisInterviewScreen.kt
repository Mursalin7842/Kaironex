package com.mursaline.kaironex.features.genesis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen
import com.mursaline.kaironex.ui.theme.KaironexColors

object GenesisInterviewScreen : Screen {
    private fun readResolve(): Any = GenesisInterviewScreen
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var messages by remember { mutableStateOf(listOf(
            "ai" to "To assist you effectively, I need to understand your reality. Let's start with your workflow.",
            "ai" to "How do you handle pressure? Do you pull all-nighters or do you prefer strict boundaries?"
        )) }
        var inputText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // Monitor IME visibility to scroll to bottom
        val imeInsets = WindowInsets.ime
        val density = LocalDensity.current
        val isImeVisible = imeInsets.getBottom(density) > 0
        
        LaunchedEffect(isImeVisible, messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.Slate50)
                .statusBarsPadding() // Safe area for top
                .navigationBarsPadding() // Safe area for bottom nav bar
                .imePadding() // Push up for keyboard
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).background(Color.White).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = KaironexColors.Indigo600, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("K", color = Color.White) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Genesis Interview", fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                        Text("Negotiating Autonomy", style = MaterialTheme.typography.bodySmall, color = KaironexColors.Indigo600)
                    }
                }
                TextButton(onClick = { navigator.replaceAll(MainShellScreen) }) { Text("Skip (Dev)", color = KaironexColors.Slate500) }
            }

            // Chat List (Scrollable)
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp)
            ) {
                items(messages) { (sender, text) ->
                    val isAi = sender == "ai"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
                    ) {
                        if (isAi) {
                            Surface(shape = CircleShape, color = Color.White, shadowElevation = 1.dp, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = KaironexColors.Indigo600, modifier = Modifier.size(20.dp)) }
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        
                        // Bubble
                        Surface(
                            shape = RoundedCornerShape(
                                if (isAi) 4.dp else 20.dp,
                                if (isAi) 20.dp else 4.dp,
                                20.dp,
                                20.dp
                            ),
                            color = if (isAi) Color.White else KaironexColors.Indigo600,
                            shadowElevation = 1.dp,
                            modifier = Modifier.widthIn(max = 480.dp)
                        ) {
                            Text(
                                text = text,
                                modifier = Modifier.padding(16.dp),
                                color = if (isAi) KaironexColors.Slate900 else Color.White,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            // Input Area
            Surface(color = Color.White, shadowElevation = 16.dp) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         // Voice Mode Toggle (Visual Only for now)
                        IconButton(onClick = {}) {
                             Icon(Icons.Default.CheckCircle, contentDescription = "Voice Mode", tint = KaironexColors.Slate500)
                        }
                        
                        Spacer(Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your answer...", color = KaironexColors.Slate500.copy(alpha=0.5f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = KaironexColors.Slate50,
                                unfocusedContainerColor = KaironexColors.Slate50,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent, 
                                focusedTextColor = KaironexColors.Slate900,
                                unfocusedTextColor = KaironexColors.Slate900
                            ),
                            shape = RoundedCornerShape(24.dp),
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (inputText.isNotBlank()) {
                                        messages = messages + ("user" to inputText)
                                        inputText = ""
                                        // Simulate AI Reply
                                        // In real app, this would be a suspend call
                                    }
                                }) { 
                                    Surface(shape = CircleShape, color = if(inputText.isNotBlank()) KaironexColors.Indigo600 else KaironexColors.Slate100, modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.ArrowForward, null, tint = if(inputText.isNotBlank()) Color.White else KaironexColors.Slate500, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Confirm Action (For Prototype Flow)
                    if (messages.size > 3) {
                        Button(
                            onClick = { navigator.replaceAll(MainShellScreen) }, 
                            modifier = Modifier.fillMaxWidth().height(48.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.Slate900),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Complete Interview & Initialize Workspace")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
