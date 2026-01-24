package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxTextField

@Composable
fun StudyTools(modifier: Modifier = Modifier, isMobile: Boolean = false) {
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
                text = {
                    Text(
                        if (isMobile) "AI" else "AI Assistant",
                        style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
                    )
                },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "AI",
                        modifier = Modifier.size(if (isMobile) 18.dp else 24.dp)
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        if (isMobile) "Cards" else "Flashcards",
                        style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
                    )
                },
                icon = {
                    Icon(
                        Icons.Filled.Style,
                        contentDescription = "Flashcards",
                        modifier = Modifier.size(if (isMobile) 18.dp else 24.dp)
                    )
                }
            )
            // NEW: Knowledge Gatekeeper Tab
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        if (isMobile) "Gate" else "Gatekeeper",
                        style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
                    )
                },
                icon = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Gatekeeper",
                        modifier = Modifier.size(if (isMobile) 18.dp else 24.dp)
                    )
                }
            )
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(if (isMobile) 12.dp else 16.dp)) {
            when(selectedTab) {
                0 -> AIAssistant(isMobile = isMobile)
                1 -> FlashcardReviewer(isMobile = isMobile)
                2 -> KnowledgeGatekeeperStatus(isMobile = isMobile)
            }
        }
    }
}

@Composable
fun AIAssistant(isMobile: Boolean = false) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(KaironexColors.CloudGray.copy(alpha = 0.3f))
                .padding(if (isMobile) 12.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isMobile) "AI chat here" else "AI conversation will appear here",
                color = KaironexColors.SlateGray,
                style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(if (isMobile) 8.dp else 12.dp))

        // Input Area
        if (isMobile) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KxTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Ask a question...",
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send")
                }
            }
        } else {
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
}

@Composable
fun FlashcardReviewer(isMobile: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isMobile) 150.dp else 200.dp),
            shape = MaterialTheme.shapes.medium,
            color = KaironexColors.CloudGray,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMobile) "Flashcard content" else "Flashcard content will appear here",
                    color = KaironexColors.InkBlack,
                    style = if (isMobile) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isMobile) 8.dp else 12.dp)
        ) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.AlertRed),
                contentPadding = if (isMobile) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else ButtonDefaults.ContentPadding
            ) {
                Text(
                    "Hard",
                    style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
                )
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.AttentionOrange),
                contentPadding = if (isMobile) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else ButtonDefaults.ContentPadding
            ) {
                Text(
                    "Medium",
                    style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
                )
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.SuccessGreen),
                contentPadding = if (isMobile) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else ButtonDefaults.ContentPadding
            ) {
                Text(
                    "Easy",
                    style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Knowledge Gatekeeper Status
 *
 * Based on the Kaironex Motivation: The "Knowledge Gatekeeper" innovation
 * Students must pass a micro-quiz to unlock distractions (Netflix, etc.)
 * This connects study completion to reward access.
 */
@Composable
fun KnowledgeGatekeeperStatus(isMobile: Boolean = false) {
    var conceptMastery by remember { mutableStateOf(0.3f) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (conceptMastery >= 0.7f) KaironexColors.SuccessGreen.copy(alpha = 0.1f)
                   else KaironexColors.AttentionOrange.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Gatekeeper",
                    tint = if (conceptMastery >= 0.7f) KaironexColors.SuccessGreen else KaironexColors.AttentionOrange,
                    modifier = Modifier.size(if (isMobile) 24.dp else 32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (conceptMastery >= 0.7f) "Gatekeeper Satisfied" else "Gatekeeper Active",
                        fontWeight = FontWeight.Bold,
                        color = if (conceptMastery >= 0.7f) KaironexColors.SuccessGreen else KaironexColors.AttentionOrange,
                        style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (conceptMastery >= 0.7f)
                            "You've earned your break!"
                        else
                            "Pass a micro-quiz to unlock distractions",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }
        }

        Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

        // Mastery Progress
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Concept Mastery",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KaironexColors.InkBlack
                )
                Text(
                    "${(conceptMastery * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.ElectricBlue
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { conceptMastery },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isMobile) 8.dp else 10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = KaironexColors.ElectricBlue,
                trackColor = KaironexColors.CloudGray
            )
        }

        Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

        // Locked Apps Section
        Text(
            "Locked Until Mastery",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = KaironexColors.SlateGray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LockedAppChip("Netflix", "🎬", isMobile)
            LockedAppChip("YouTube", "📺", isMobile)
            LockedAppChip("TikTok", "🎵", isMobile)
        }

        Spacer(Modifier.weight(1f))

        // Take Quiz Button
        Button(
            onClick = {
                // Simulate quiz completion - in production this would open a quiz dialog
                conceptMastery = (conceptMastery + 0.2f).coerceAtMost(1f)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.GeminiBlurple
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = if (isMobile) PaddingValues(12.dp) else PaddingValues(16.dp)
        ) {
            Text(
                if (conceptMastery >= 0.7f) "Take Another Quiz" else "Take Micro-Quiz",
                style = if (isMobile) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LockedAppChip(name: String, emoji: String, isMobile: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = KaironexColors.AlertRed.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isMobile) 8.dp else 12.dp, vertical = if (isMobile) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(4.dp))
            Text(
                name,
                style = if (isMobile) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = KaironexColors.AlertRed,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

