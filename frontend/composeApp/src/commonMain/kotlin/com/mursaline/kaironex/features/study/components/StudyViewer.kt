package com.mursaline.kaironex.features.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.core.AppConfig

@Composable
fun StudyViewer(
    resource: Resource?,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (resource == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
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
                ResourceType.CcVideo -> VideoPlayer(resource, isMobile) // Re-use Video Player
                ResourceType.IMAGE -> ImageViewer(resource, isMobile) // Need to create this or placeholder
                ResourceType.LINK -> LinkViewer(resource, isMobile) // Need to create this or placeholder
                ResourceType.ARTICLE -> ArticleViewer(resource, isMobile)
            }
        }
    }
}

@Composable
fun ArticleViewer(resource: Resource, isMobile: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(if (isMobile) 16.dp else 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew, // Or Article icon
                contentDescription = null,
                tint = KaironexColors.ElectricBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = resource.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Metadata
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {},
                label = { Text("AI Generated") },
                colors = AssistChipDefaults.assistChipColors(containerColor = KaironexColors.ElectricBlue.copy(alpha=0.1f)),
                border = null
            )
            Spacer(Modifier.width(8.dp))
            if (resource.duration != null) {
                Text(
                    text = "• ${resource.duration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Content
        if (!resource.content.isNullOrBlank()) {
             // TODO: Use a real Markdown renderer here
             androidx.compose.foundation.text.selection.SelectionContainer {
                 Text(
                    text = resource.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.InkBlack,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.6
                 )
             }
        } else {
            Text(
                "No content available.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun PDFViewer(resource: Resource, isMobile: Boolean = false) {
    // Build the file URL from Appwrite
    val fileUrl = remember(resource.id) {
        "${AppConfig.Appwrite.ENDPOINT}/storage/buckets/${AppConfig.Appwrite.STORAGE_BUCKET_ID}/files/${resource.id}/view?project=${AppConfig.Appwrite.PROJECT_ID}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.InkBlack)
            .padding(if (isMobile) 16.dp else 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Surface(
            color = KaironexColors.ElectricBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = KaironexColors.ElectricBlue,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PDF Document",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // PDF Info Card
        Surface(
            color = KaironexColors.SlateGray.copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Document Ready",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Open in browser to view the full PDF",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(24.dp))

                // Open button - this would use platform-specific URL opener
                Button(
                    onClick = {
                        // Platform-specific URL opening would go here
                        println("📄 Opening PDF: $fileUrl")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open PDF")
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(resource: Resource, isMobile: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.InkBlack)
            .padding(if (isMobile) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Video thumbnail area
        Surface(
            color = KaironexColors.SlateGray.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = resource.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = resource.duration ?: "Video",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { println("▶️ Playing video: ${resource.id}") },
            colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Play Video")
        }
    }
}

@Composable
fun CodeViewer(resource: Resource, isMobile: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // VS Code dark theme
            .padding(if (isMobile) 12.dp else 16.dp)
    ) {
        // File header
        Surface(
            color = Color(0xFF2D2D2D),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = KaironexColors.ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = resource.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // Code content area
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Code Preview",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Open file to view code",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ImageViewer(resource: Resource, isMobile: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.InkBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = resource.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Image Preview Not Implemented",
                color = KaironexColors.SlateGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LinkViewer(resource: Resource, isMobile: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaironexColors.InkBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = KaironexColors.ElectricBlue,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = resource.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
             Spacer(Modifier.height(16.dp))
            Button(
                onClick = { /* Open Link */ },
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
            ) {
                Text("Open Link")
            }
        }
    }
}
