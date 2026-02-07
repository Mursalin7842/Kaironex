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
import cafe.adriel.voyager.koin.koinScreenModel
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.features.study.StudyViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.BorderStroke

class DataIngestionScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<com.mursaline.kaironex.features.study.StudyViewModel>()
        
        val resources by viewModel.resources.collectAsState()
        val isUploading by viewModel.isUploading.collectAsState()

        // FileKit Picker
        val pickerLauncher = io.github.vinceglb.filekit.compose.rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("pdf", "docx", "txt", "md")),
            mode = PickerMode.Multiple()
        ) { files ->
            if (files != null && files.isNotEmpty()) {
                viewModel.uploadFiles(files)
            }
        }

        Scaffold(
            bottomBar = {
                // NEXT BUTTON
                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = { 
                                if (resources.isNotEmpty()) {
                                    viewModel.triggerInitialScheduleGeneration()
                                }
                                navigator.push(MainShellScreen) 
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.InkBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Finish Setup", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KaironexColors.CanvasWhite)
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Text(
                        "Feed the Brain 🧠",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )
                    Text(
                        "Upload your syllabus, slides, and notes. The Brain will read everything to build your master plan.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KaironexColors.SlateGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 1. Upload Section
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAFAFA), // very light gray
                        border = BorderStroke(1.dp, Color(0xFFF0F0F0)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CloudUpload, 
                                contentDescription = null, 
                                tint = KaironexColors.ElectricBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Drag & Drop or Tap to Upload",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = KaironexColors.ElectricBlue)
                                Spacer(Modifier.height(8.dp))
                                Text("Ingesting knowledge...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Button(
                                    onClick = { pickerLauncher.launch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.InkBlack),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("Select Files")
                                }
                            }
                        }
                    }
                }

                // 2. Resource List
                if (resources.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text("No documents yet.", color = Color.LightGray)
                        }
                    }
                } else {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ingested Documents", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${resources.size} files", style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
                        }
                    }
                    
                    items(resources) { res ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = KaironexColors.ElectricBlue.copy(alpha = 0.05f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { 
                                    Text("📄", style = MaterialTheme.typography.titleMedium) 
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(res.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Ready for analysis", style = MaterialTheme.typography.labelSmall, color = KaironexColors.SuccessGreen)
                            }
                            androidx.compose.material3.Icon(Icons.Default.CheckCircle, "Active", tint = KaironexColors.SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
                
                item {
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}
