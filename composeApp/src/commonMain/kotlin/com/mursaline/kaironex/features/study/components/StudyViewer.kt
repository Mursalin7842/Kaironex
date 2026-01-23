package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun StudyViewer(
    resource: Resource?,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black), // Dark background for content viewer
        contentAlignment = Alignment.Center
    ) {
        if (resource == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (isMobile) "Select a resource" else "Select a resource to begin studying",
                    color = Color.White.copy(alpha = 0.6f),
                    style = if (isMobile) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium
                )
            }
        } else {
            when(resource.type) {
                ResourceType.PDF -> PDFViewer(resource, isMobile)
                ResourceType.VIDEO -> VideoPlayer(resource, isMobile)
                ResourceType.CODE -> CodeViewer(resource, isMobile)
            }
        }
    }
}

@Composable
fun PDFViewer(resource: Resource, isMobile: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().padding(if (isMobile) 16.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📄 PDF Viewer",
                color = Color.White,
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = resource.title,
                color = Color.White.copy(alpha = 0.7f),
                style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "PDF rendering will be implemented here",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun VideoPlayer(resource: Resource, isMobile: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().padding(if (isMobile) 16.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🎥 Video Player",
                color = Color.White,
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = resource.title,
                color = Color.White.copy(alpha = 0.7f),
                style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Video player will be implemented",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CodeViewer(resource: Resource, isMobile: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().padding(if (isMobile) 16.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💻 Code Viewer",
                color = Color.White,
                style = if (isMobile) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = resource.title,
                color = Color.White.copy(alpha = 0.7f),
                style = if (isMobile) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Code viewer will be implemented",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
