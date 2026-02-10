package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.features.study.FlashCard
import com.mursaline.kaironex.features.study.GatekeeperQuiz
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun StudyTools(
    flashCards: List<FlashCard> = emptyList(),
    quiz: GatekeeperQuiz? = null,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false,
    onRequestQuiz: () -> Unit = {}
) {
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
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
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
                0 -> FlashcardReviewer(flashCards = flashCards, isMobile = isMobile)
                1 -> KnowledgeGatekeeperStatus(quiz = quiz, isMobile = isMobile, onRequestQuiz = onRequestQuiz)
            }
        }
    }
}

@Composable
fun FlashcardReviewer(
    flashCards: List<FlashCard>,
    isMobile: Boolean = false
) {
    if (flashCards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No flashcards available for this task.",
                color = KaironexColors.SlateGray,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val currentCard = flashCards[currentIndex]

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        Text(
            "Card ${currentIndex + 1} of ${flashCards.size}",
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray
        )
        Spacer(Modifier.height(8.dp))

        // Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { isFlipped = !isFlipped },
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = KaironexColors.CloudGray),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFlipped) "ANSWER" else "QUESTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFlipped) KaironexColors.SuccessGreen else KaironexColors.ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isFlipped) currentCard.back else currentCard.front,
                        color = KaironexColors.InkBlack,
                        style = if (isMobile) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

        // Controls
        if (isFlipped) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        isFlipped = false
                        currentIndex = (currentIndex + 1) % flashCards.size
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.AlertRed)
                ) {
                    Text("Hard")
                }
                Button(
                    onClick = {
                        isFlipped = false
                        currentIndex = (currentIndex + 1) % flashCards.size
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.SuccessGreen)
                ) {
                    Text("Easy")
                }
            }
        } else {
             Button(
                onClick = { isFlipped = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
            ) {
                Text("Show Answer")
            }
        }
    }
}

@Composable
fun KnowledgeGatekeeperStatus(
    quiz: GatekeeperQuiz?,
    isMobile: Boolean = false,
    onRequestQuiz: () -> Unit
) {
    if (quiz == null) {
         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("No quiz data available.", color = KaironexColors.SlateGray)
         }
         return
    }

    var conceptMastery by remember { mutableStateOf(0.0f) } // Mock mastery for now

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
                            "Pass the quiz to unlock apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }
        }

        Spacer(Modifier.height(if (isMobile) 16.dp else 24.dp))

        // Quiz Info
        Text(
             "Quiz Subject",
             style = MaterialTheme.typography.labelSmall,
             color = KaironexColors.SlateGray
        )
        Text(
             quiz.title,
             style = MaterialTheme.typography.titleMedium,
             fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text("${quiz.questions.size} Questions • Needs ${(quiz.passThreshold * 100).toInt()}% to pass", color = KaironexColors.SlateGray)

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
        }

        Spacer(Modifier.weight(1f))

        // Take Quiz Button
        Button(
            onClick = onRequestQuiz,
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.GeminiBlurple
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = if (isMobile) PaddingValues(12.dp) else PaddingValues(16.dp)
        ) {
            Text(
                "Take Quiz",
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

