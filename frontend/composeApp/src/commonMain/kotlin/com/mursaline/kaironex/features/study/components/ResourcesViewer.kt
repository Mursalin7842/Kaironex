package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.features.study.StudyResource

@Composable
fun ResourcesViewer(
    resources: List<StudyResource>,
    currentIndex: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val resource = resources.getOrNull(currentIndex) ?: return
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)  // WHITE BACKGROUND for readability
            .padding(16.dp)
    ) {
        // Resource navigation: "1 of 6"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onNavigate(currentIndex - 1) },
                enabled = currentIndex > 0
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous", tint = Color.Black)
            }
            
            Text(
                "${currentIndex + 1} of ${resources.size}",
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(
                onClick = { onNavigate(currentIndex + 1) },
                enabled = currentIndex < resources.size - 1
            ) {
                Icon(Icons.Default.ChevronRight, "Next", tint = Color.Black)
            }
        }
        
        // Resource title
        Text(
            text = resource.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
        
        // Resource type chip
        AssistChip(
            onClick = {},
            label = { Text(resource.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
            leadingIcon = {
                Icon(
                    imageVector = when (resource.type) {
                        "concept" -> Icons.Default.Lightbulb
                        "example" -> Icons.Default.Code
                        "key_points" -> Icons.AutoMirrored.Filled.List
                        "application" -> Icons.Default.Build
                        else -> Icons.AutoMirrored.Filled.Article
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Content with Markdown rendering (use your Markdown lib)
        // The content is Markdown-formatted for readability
        // For now, using Text as placeholder for MarkdownText
        Text(
            text = resource.content, 
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black.copy(alpha = 0.87f),
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.weight(1f))
        
        // Estimated read time
        Text(
            "📖 ${resource.estimatedReadTime} min read",
            color = Color.Gray,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
