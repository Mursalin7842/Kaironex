package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.mursaline.kaironex.features.study.StudyViewModel
import com.mursaline.kaironex.features.study.components.Resource
import com.mursaline.kaironex.features.study.components.ResourceType
import com.mursaline.kaironex.ui.theme.KaironexColors
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch



@Composable
fun FilesContent(
    isMobile: Boolean,
    viewModel: StudyViewModel
) {
    val resources by viewModel.resources.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(if (isMobile) 12.dp else 24.dp)) {
        
        // Header Row with Upload Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Files",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.SlateGray
            )

            Button(
                onClick = {
                    scope.launch {
                        try {
                            val file = FileKit.pickFile(
                                type = PickerType.ImageAndVideo, // Extend to PDF logic if needed or use custom type
                                mode = PickerMode.Single
                            )
                            if (file != null) {
                                viewModel.uploadFiles(listOf(file))
                            }
                        } catch (e: Exception) {
                            println("File Picker Error: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple),
                shape = RoundedCornerShape(8.dp),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload")
                }
            }
        }

        if (resources.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.InsertDriveFile, 
                        contentDescription = null, 
                        tint = KaironexColors.SlateGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No files uploaded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KaironexColors.SlateGray
                    )
                     Spacer(Modifier.height(8.dp))
                    Text(
                        "Upload documents to use them in your study sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray.copy(alpha = 0.7f)
                    )
                 }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(resources) { file ->
                    FileItem(file)
                }
                
                // Placeholder "Upload More" item as requested
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = KaironexColors.CloudGray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { /* Placeholder action */ }
                    ) {
                         Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                             Icon(Icons.Default.Add, null, tint = KaironexColors.SlateGray)
                             Spacer(Modifier.width(8.dp))
                             Text("Upload more files (Placeholder)", color = KaironexColors.SlateGray, fontWeight = FontWeight.Medium)
                         }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(resource: Resource) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            val icon = when (resource.type) {
                ResourceType.PDF -> Icons.AutoMirrored.Filled.Article
                ResourceType.VIDEO -> Icons.Default.Movie
                ResourceType.CcVideo -> Icons.Default.Movie
                ResourceType.IMAGE -> Icons.Default.Image
                ResourceType.LINK -> Icons.AutoMirrored.Filled.OpenInNew
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            
            val iconColor = when (resource.type) {
                ResourceType.PDF -> KaironexColors.ErrorRed
                ResourceType.VIDEO -> KaironexColors.EventsOrange
                else -> KaironexColors.GeminiBlurple
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    resource.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KaironexColors.SlateGray
                )
                Text(
                    resource.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray.copy(alpha = 0.6f)
                )
            }
        }
    }
}
