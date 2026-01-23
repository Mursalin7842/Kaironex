package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxTextField

@Composable
fun StudyTools(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = modifier) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = KaironexColors.CanvasWhite,
            contentColor = KaironexColors.ElectricBlue
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("AI Assistant") },
                icon = { Icon(Icons.Filled.Chat, contentDescription = "AI") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Flashcards") },
                icon = { Icon(Icons.Filled.Style, contentDescription = "Flashcards") }
            )
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when(selectedTab) {
                0 -> AIAssistant()
                1 -> FlashcardReviewer()
            }
        }
    }
}

@Composable
fun AIAssistant() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(KaironexColors.CloudGray.copy(alpha = 0.3f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI conversation will appear here",
                color = KaironexColors.SlateGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KxTextField(
                value = "",
                onValueChange = {},
                placeholder = "Ask a question about this topic...",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun FlashcardReviewer() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = MaterialTheme.shapes.medium,
            color = KaironexColors.CloudGray,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Flashcard content will appear here",
                    color = KaironexColors.InkBlack,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.AlertRed)
            ) {
                Text("Hard")
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.AttentionOrange)
            ) {
                Text("Medium")
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.SuccessGreen)
            ) {
                Text("Easy")
            }
        }
    }
}
