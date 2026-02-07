package com.mursaline.kaironex.features.campaign

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.util.Base64

/**
 * The Armory: Resume Builder & Optimizer
 */
import org.koin.compose.koinInject
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.features.campaign.CampaignSetupScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object ArmoryScreen : Screen {
    private fun readResolve(): Any = ArmoryScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val repo = koinInject<AppwriteStatsRepository>()
        val campaignState by repo.campaignState.collectAsState()
        val isLoading by repo.isLoading.collectAsState()
        val scope = rememberCoroutineScope()
        
        var activeMode by remember { mutableStateOf<String?>(null) } // "analyze", "generate", null
        var resumeText by remember { mutableStateOf("") }
        var jobDescription by remember { mutableStateOf("") }
        var projectsText by remember { mutableStateOf("") }
        var analysisResult by remember { mutableStateOf<AtsAnalysisResult?>(null) }
        var generatedResume by remember { mutableStateOf<GeneratedResume?>(null) }
        var isProcessing by remember { mutableStateOf(false) }
        var pdfFileName by remember { mutableStateOf<String?>(null) }
        
        // PDF File Picker
        val pdfPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        resumeText = "" // Clear text input
                        pdfFileName = uri.lastPathSegment ?: "resume.pdf"
                        
                        // Store base64 for later use
                        scope.launch {
                            isProcessing = true
                            val result = repo.analyzeResume(
                                resumeText = null,
                                jobDescription = jobDescription,
                                resumePdfBase64 = base64
                            )
                            analysisResult = result
                            isProcessing = false
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Save DOCX to Downloads
        fun saveDocxToDownloads(base64Data: String, filename: String) {
            try {
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ use MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, filename)
                        put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    
                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                        Toast.makeText(context, "Resume saved to Downloads!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Legacy storage
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, filename)
                    FileOutputStream(file).use { it.write(bytes) }
                    Toast.makeText(context, "Resume saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        val armory = campaignState.armory
        val inventory = armory.inventory
        val blueprints = armory.blueprints

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(
                        when (activeMode) {
                            "analyze" -> "Resume Analyzer"
                            "generate" -> "Resume Forge"
                            else -> "The Armory"
                        }, 
                        fontWeight = FontWeight.Bold
                    ) },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (activeMode != null) {
                                activeMode = null
                            } else {
                                navigator.pop() 
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        if (activeMode == null) {
                            IconButton(onClick = { navigator.push(CampaignSetupScreen(isEditMode=true)) }) {
                                Icon(Icons.Filled.Settings, "Edit")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = KaironexColors.CanvasWhite
                    )
                )
            },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
            when (activeMode) {
                "analyze" -> {
                    // Resume Analyzer UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Analyze Your Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Get an ATS score and actionable improvements", style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // PDF Upload Button
                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text(pdfFileName ?: "Upload Resume PDF")
                        }
                        
                        if (pdfFileName != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("PDF loaded: $pdfFileName", style = MaterialTheme.typography.bodySmall, color = Color(0xFF43A047))
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("— OR —", modifier = Modifier.align(Alignment.CenterHorizontally), color = KaironexColors.SlateGray)
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = resumeText,
                            onValueChange = { 
                                resumeText = it
                                pdfFileName = null // Clear PDF when typing
                            },
                            label = { Text("Paste Your Resume") },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            placeholder = { Text("Paste your resume text here...") }
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Job Description (Optional)") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            placeholder = { Text("Paste the job description for targeted analysis...") }
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    // Call backend API (text only since PDF triggers immediately)
                                    val result = repo.analyzeResume(
                                        resumeText = resumeText.takeIf { it.isNotBlank() },
                                        jobDescription = jobDescription,
                                        resumePdfBase64 = null
                                    )
                                    analysisResult = result
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = (resumeText.isNotBlank() || pdfFileName != null) && !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Filled.Search, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Analyze Resume")
                            }
                        }
                        
                        // Show Analysis Results
                        analysisResult?.let { result ->
                            Spacer(Modifier.height(24.dp))
                            AtsResultCard(result)
                        }
                    }
                }
                
                "generate" -> {
                    // Resume Generator UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Forge Your Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Generate a tailored resume from your projects", style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
                        
                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Target Job Description *") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            placeholder = { Text("Paste the job description you're targeting...") }
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = projectsText,
                            onValueChange = { projectsText = it },
                            label = { Text("Your Projects *") },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            placeholder = { Text("Describe your projects:\n\n1. Project Name\n- What you built\n- Technologies used\n- Impact/Results\n\n2. Another Project...") }
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    isProcessing = true
                                    generatedResume = repo.generateResume(jobDescription, projectsText)
                                    isProcessing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = jobDescription.isNotBlank() && projectsText.isNotBlank() && !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1))
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Filled.Create, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Forge Resume")
                            }
                        }
                        
                        // Show Generated Resume
                        generatedResume?.let { resume ->
                            Spacer(Modifier.height(24.dp))
                            
                            KxCard(
                                modifier = Modifier.fillMaxWidth(),
                                variant = KxCardVariant.Elevated
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Resume Generated!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF43A047))
                                        Spacer(Modifier.weight(1f))
                                        Text("ATS: ${resume.estimatedAtsScore}", fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    
                                    // Professional Summary Preview
                                    Text("Summary", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(resume.professionalSummary.take(200) + "...", style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
                                    
                                    Spacer(Modifier.height(12.dp))
                                    
                                    // Skills
                                    if (resume.skillsSection.isNotEmpty()) {
                                        Text("Skills", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text(resume.skillsSection.take(8).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                                    }
                                    
                                    Spacer(Modifier.height(16.dp))
                                    
                                    // Download Button
                                    if (resume.docxBase64 != null) {
                                        Button(
                                            onClick = {
                                                saveDocxToDownloads(
                                                    resume.docxBase64!!,
                                                    resume.docxFilename ?: "resume.docx"
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                        ) {
                                            Icon(Icons.Filled.Share, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Download as DOCX")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                else -> {
                    // Main Armory View
                    if (inventory.isEmpty() && blueprints.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(color = KaironexColors.ElectricBlue)
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Armory Empty", style = MaterialTheme.typography.titleMedium, color = KaironexColors.SlateGray)
                                    Button(onClick = { navigator.push(CampaignSetupScreen(isEditMode=true)) }) {
                                        Text("Initialize Strategy")
                                    }
                                }
                                
                                LaunchedEffect(Unit) {
                                    repo.refreshAll()
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Main Actions - Resume Tools
                            item {
                                Text("Resume Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { activeMode = "analyze" },
                                        modifier = Modifier.weight(1f).height(80.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.Search, null)
                                            Text("Analyze", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { activeMode = "generate" },
                                        modifier = Modifier.weight(1f).height(80.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.Create, null)
                                            Text("Generate", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }

                            // Current Loadout (Main Resume)
                            item {
                                Spacer(Modifier.height(16.dp))
                                Text("Equipped Armor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                
                                val equipped = inventory.find { it.status == "equipped" }
                                if (equipped != null) {
                                    ArmoryItemCard(equipped)
                                } else {
                                    Text("No artifact equipped.", style = MaterialTheme.typography.bodyMedium, color = KaironexColors.SlateGray)
                                }
                            }

                            // Inventory
                            item {
                                Spacer(Modifier.height(16.dp))
                                Text("Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            items(inventory.filter { it.status != "equipped" }) { item ->
                                ArmoryItemCard(item)
                            }
                            
                            // Blueprints
                            if (blueprints.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    Text("Available Blueprints", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                items(blueprints) { item ->
                                    ArmoryItemCard(item, isBlueprint = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AtsResultCard(result: AtsAnalysisResult) {
    KxCard(
        modifier = Modifier.fillMaxWidth(),
        variant = KxCardVariant.Elevated
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Score Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${result.atsScore}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        result.atsScore >= 80 -> Color(0xFF43A047)
                        result.atsScore >= 60 -> Color(0xFFFFA000)
                        else -> Color(0xFFE53935)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("ATS Score", style = MaterialTheme.typography.titleMedium)
                    Text(result.estimatedPassRate, style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            
            // Missing Keywords
            if (result.missingKeywords.isNotEmpty()) {
                Text("Missing Keywords", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    result.missingKeywords.take(5).forEach { keyword ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(keyword, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFFE53935))
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Checklist
            if (result.checklist.isNotEmpty()) {
                Text("Action Checklist", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                result.checklist.take(5).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (item.status == "completed") Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (item.status == "completed") Color(0xFF43A047) else Color(0xFFFFA000),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item.item, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = when (item.impact) {
                                "HIGH" -> Color(0xFFFFEBEE)
                                "MEDIUM" -> Color(0xFFFFF3E0)
                                else -> Color(0xFFE8F5E9)
                            }
                        ) {
                            Text(item.impact, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Overall Assessment
            Text("Assessment", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(result.overallAssessment, style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
        }
    }
}

@Composable
fun ArmoryItemCard(item: com.mursaline.kaironex.features.campaign.ArmoryItem, isBlueprint: Boolean = false) {
    KxCard(
        modifier = Modifier.fillMaxWidth(),
        variant = KxCardVariant.Elevated
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isBlueprint) Icons.Filled.Build else Icons.Filled.Star,
                contentDescription = null,
                tint = if (item.status == "equipped") Color(0xFFFFD700) else KaironexColors.SlateGray,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text(item.type, style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (isBlueprint) {
                    Text("Cost", style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
                    Text("${item.costXp} XP", fontWeight = FontWeight.Bold, color = KaironexColors.InkBlack)
                } else {
                    Text(item.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = KaironexColors.SuccessGreen)
                }
            }
        }
    }
}
