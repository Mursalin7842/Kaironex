package com.mursaline.kaironex.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen
import com.mursaline.kaironex.features.resources.FileRepository
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class DataIngestionScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val fileRepository = koinInject<FileRepository>()
        val scope = rememberCoroutineScope()
        
        var driveLink by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }
        var uploadStatus by remember { mutableStateOf("") }

        // File Picker
        val launcher = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("pdf", "docx", "txt", "md")),
            mode = PickerMode.Single
        ) { file ->
            if (file != null) {
                scope.launch {
                    isUploading = true
                    uploadStatus = "Uploading ${file.name}..."
                    val success = fileRepository.uploadFile(file)
                    if (success) {
                        uploadStatus = "✅ ${file.name} Uploaded!"
                    } else {
                        uploadStatus = "❌ Upload Failed"
                    }
                    isUploading = false
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E)) // Dark bg
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Feed the Brain 🧠",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "To personalize your learning, Kairo needs access to your academic materials. Upload files or connect Google Drive.",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // OPTION 1: Google Drive
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF4285F4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Drive Link", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = driveLink,
                            onValueChange = { driveLink = it },
                            placeholder = { Text("Paste folder or file link...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (driveLink.isNotBlank()) {
                                    scope.launch {
                                        isUploading = true
                                        val success = fileRepository.saveDriveLink(driveLink)
                                        uploadStatus = if (success) "✅ Link Saved!" else "❌ Save Failed"
                                        isUploading = false
                                    }
                                }
                            },
                            enabled = driveLink.isNotBlank() && !isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                        ) {
                            Text("Connect Drive")
                        }
                    }
                }

                // OPTION 2: File Upload
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF0F9D58))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manual Upload", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Supported: PDF, DOCX, TXT", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { launcher.launch() },
                            enabled = !isUploading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                        ) {
                            Text("Select Files")
                        }
                    }
                }

                if (uploadStatus.isNotEmpty()) {
                    Text(
                        text = uploadStatus,
                        color = if (uploadStatus.contains("❌")) Color.Red else Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // NEXT BUTTON
            Button(
                onClick = { navigator.push(MainShellScreen) },
                modifier = Modifier.align(Alignment.BottomEnd),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Finish Setup", color = Color.Black)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
            }
        }
    }
}
