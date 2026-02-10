package com.mursaline.kaironex.features.dashboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.components.KxIconButton
import com.mursaline.kaironex.ui.components.KxIconButtonVariant


import com.mursaline.kaironex.ui.components.KxTextField
import com.mursaline.kaironex.ui.theme.KaironexColors

import com.mursaline.kaironex.core.gemini.ChatMessage

@Composable
fun ChatScreen() {
    var messageText by remember { mutableStateOf("") }
    // Dummy history
    val messages = remember {
        listOf(
            ChatMessage(sender = "user", content = "Hello, Kaironex. I need help focusing."),
            ChatMessage(sender = "ai", content = "I am here. Activating focus protocols. What is the subject?")
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // AI Presence Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.mursaline.kaironex.ui.components.VoiceOrb(
                isListening = true, 
                onClick = {},
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Kaironex AI", fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                Text("Active • Monitoring", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = KaironexColors.Indigo600)
            }
        }
        
        // Chat Area
        KxCard(
            variant = KxCardVariant.Flat,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            backgroundColor = Color.White
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Input Area
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                 KxTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = "Type your command...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(8.dp))
            KxIconButton(
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = { /* Send */ },
                variant = KxIconButtonVariant.Filled
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isFromUser = message.sender == "user"
    val align = if (isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isFromUser) KaironexColors.Indigo50 else KaironexColors.Slate100
    val textColor = if (isFromUser) KaironexColors.Indigo900 else KaironexColors.Slate900
    
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
        androidx.compose.material3.Surface(
            color = bgColor,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
    }
}
