package com.mursaline.kaironex.features.study.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.features.study.FlashCard

@Composable
fun FlashcardDeck(
    flashCards: List<FlashCard>,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(400)
    )

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress
        Text(
            text = "Card ${currentIndex + 1} of ${flashCards.size}",
            style = MaterialTheme.typography.labelLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Card Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable { isFlipped = !isFlipped }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            contentAlignment = Alignment.Center
        ) {
            val card = flashCards.getOrNull(currentIndex)
            
            if (card != null) {
                // Front Side
                if (rotation <= 90f) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = card.front,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } 
                // Back Side
                else {
                    Card(
                        modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = card.back,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Difficulty: ${card.difficulty}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    if (currentIndex > 0) {
                        currentIndex--
                        isFlipped = false
                    }
                },
                enabled = currentIndex > 0
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }
            
            IconButton(
                onClick = { isFlipped = !isFlipped }
            ) {
                Icon(Icons.Default.Refresh, "Flip")
            }
            
            IconButton(
                onClick = { 
                    if (currentIndex < flashCards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                enabled = currentIndex < flashCards.size - 1
            ) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }
    }
}
