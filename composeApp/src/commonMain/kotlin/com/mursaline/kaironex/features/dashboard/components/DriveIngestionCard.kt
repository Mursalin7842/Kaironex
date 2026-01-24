package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * Drive Ingestion & File Upload Card
 *
 * Features:
 * - Google Drive integration
 * - Manual file upload (drag & drop)
 * - Recent uploads display
 * - Ingestion status
 */

data class UploadedFile(
    val id: String,
    val name: String,
    val type: FileType,
    val size: String,
    val uploadedAt: String,
    val status: IngestionStatus
)

enum class FileType(val emoji: String, val color: Long) {
    PDF("📄", 0xFFE53935),
    DOC("📝", 0xFF1E88E5),
    VIDEO("🎬", 0xFF8E24AA),
    AUDIO("🎧", 0xFF43A047),
    IMAGE("🖼️", 0xFFFF9800),
    PPT("📊", 0xFFFF5722),
    OTHER("📁", 0xFF607D8B)
}

enum class IngestionStatus(val label: String, val color: Long) {
    PENDING("Pending", 0xFFFF9800),
    PROCESSING("Processing", 0xFF2196F3),
    INDEXED("Indexed", 0xFF4CAF50),
    FAILED("Failed", 0xFFF44336)
}

@Composable
fun DriveIngestionCard(
    files: List<UploadedFile>,
    isDriveConnected: Boolean,
    isMobile: Boolean,
    onConnectDrive: () -> Unit = {},
    onUploadFiles: () -> Unit = {},
    onFileClick: (UploadedFile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(if (isMobile) 12.dp else 16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = KaironexColors.ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Study Materials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                }

                // Drive connection status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDriveConnected)
                        KaironexColors.SuccessGreen.copy(alpha = 0.1f)
                        else KaironexColors.AttentionOrange.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isDriveConnected) KaironexColors.SuccessGreen
                                   else KaironexColors.AttentionOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isDriveConnected) "Connected" else "Connect",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDriveConnected) KaironexColors.SuccessGreen
                                   else KaironexColors.AttentionOrange
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Upload area (Drop zone)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isMobile) 80.dp else 100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color = KaironexColors.ElectricBlue.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(KaironexColors.ElectricBlue.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Upload button
                        FilledTonalButton(
                            onClick = onUploadFiles,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = KaironexColors.ElectricBlue.copy(alpha = 0.15f)
                            )
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Files")
                        }

                        if (!isMobile && !isDriveConnected) {
                            // Connect Drive button
                            OutlinedButton(onClick = onConnectDrive) {
                                Icon(
                                    Icons.Default.Cloud,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Connect Drive")
                            }
                        }
                    }

                    if (!isMobile) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Drop PDF, PPT, Video, or Audio files here",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }
            }

            // Recent uploads
            if (files.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Text(
                    "Recent Uploads",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = KaironexColors.SlateGray
                )

                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    files.take(3).forEach { file ->
                        UploadedFileItem(
                            file = file,
                            onClick = { onFileClick(file) }
                        )
                    }
                }

                if (files.size > 3) {
                    TextButton(
                        onClick = { /* View all files */ },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("View all ${files.size} files")
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadedFileItem(
    file: UploadedFile,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = KaironexColors.CloudGray
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Text(
                file.type.emoji,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.width(10.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = KaironexColors.InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${file.size} • ${file.uploadedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }

            // Status indicator
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(file.status.color).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.status == IngestionStatus.PROCESSING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 2.dp,
                            color = Color(file.status.color)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        file.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(file.status.color)
                    )
                }
            }
        }
    }
}

// Sample data
fun getSampleUploadedFiles(): List<UploadedFile> = listOf(
    UploadedFile(
        id = "1",
        name = "DSA_Complete_Notes.pdf",
        type = FileType.PDF,
        size = "2.4 MB",
        uploadedAt = "2 hours ago",
        status = IngestionStatus.INDEXED
    ),
    UploadedFile(
        id = "2",
        name = "System_Design_Lecture.mp4",
        type = FileType.VIDEO,
        size = "145 MB",
        uploadedAt = "5 hours ago",
        status = IngestionStatus.PROCESSING
    ),
    UploadedFile(
        id = "3",
        name = "Interview_Prep_Slides.pptx",
        type = FileType.PPT,
        size = "8.2 MB",
        uploadedAt = "Yesterday",
        status = IngestionStatus.INDEXED
    )
)
