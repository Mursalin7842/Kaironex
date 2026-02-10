package com.mursaline.kaironex.features.campaign

import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
// import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch
// import java.io.File
// import java.io.FileOutputStream
// import android.util.Base64

/**
 * The Armory: Resume Builder & Optimizer
 */
import org.koin.compose.koinInject
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.features.campaign.CampaignSetupScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box

object ArmoryScreen : Screen {
    private fun readResolve(): Any = ArmoryScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // val context = LocalContext.current
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
        var pdfBase64 by remember { mutableStateOf<String?>(null) }
        
        // Load most recent resume data when entering analyze mode
        LaunchedEffect(activeMode, campaignState.resumeHistory) {
            if (activeMode == "analyze" && campaignState.resumeHistory.isNotEmpty()) {
                val mostRecent = campaignState.resumeHistory.maxByOrNull { it.created_at.ifEmpty { "0" } }
                mostRecent?.let { historyItem ->
                    // Pre-populate with the most recent analysis if no result is currently shown
                    if (analysisResult == null && historyItem.ats_score > 0) {
                        analysisResult = AtsAnalysisResult(
                            atsScore = historyItem.ats_score,
                            matchedKeywords = historyItem.matched_keywords,
                            missingKeywords = historyItem.missing_keywords,
                            checklist = historyItem.improvement_checklist.map {
                                AtsChecklistItem(
                                    item = it.item,
                                    status = it.status,
                                    impact = it.impact
                                )
                            },
                            overallAssessment = "Target Job: ${historyItem.target_job}",
                            estimatedPassRate = when {
                                historyItem.ats_score >= 80 -> "High Pass Rate (80%+)"
                                historyItem.ats_score >= 60 -> "Moderate Pass Rate (60-80%)"
                                else -> "Low Pass Rate (<60%)"
                            }
                        )
                    }
                }
            }
        }

        // PDF File Picker
        val pdfPickerLauncher = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("pdf")),
            mode = PickerMode.Single
        ) { file ->
            file?.let {
                scope.launch {
                    pdfFileName = it.name
                    // TODO: Implement file reading logic for KMP if needed
                    // For now, we just capture the name for UI feedback
                    
                    // Note: Actual byte reading and Base64 encoding requires KMP compatible libraries
                    // which we might not have set up for Base64 (java.util.Base64 is JVM only)
                }
            }
        }
        
        // Save DOCX to Downloads
        fun saveDocxToDownloads(base64Data: String, filename: String) {
             // TODO: Implement KMP File Saving
             println("Saving file $filename (Base64 length: ${base64Data.length}) - Not implemented on this platform yet")
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
                            onClick = { pdfPickerLauncher.launch() },
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
                                    // Call backend API
                                    val result = repo.analyzeResume(
                                        resumeText = resumeText.takeIf { it.isNotBlank() },
                                        jobDescription = jobDescription,
                                        resumePdfBase64 = pdfBase64
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

                            // Resume History Section
                            if (campaignState.resumeHistory.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Resume History",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = KaironexColors.ElectricBlue.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                "${campaignState.resumeHistory.size} Analysis",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KaironexColors.ElectricBlue
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }

                                items(campaignState.resumeHistory.sortedByDescending { it.created_at.ifEmpty { "0" } }) { historyItem ->
                                    ResumeHistoryCard(
                                        historyItem = historyItem,
                                        onClick = {
                                            // Convert resume history to AtsAnalysisResult format
                                            analysisResult = AtsAnalysisResult(
                                                atsScore = historyItem.ats_score,
                                                matchedKeywords = historyItem.matched_keywords,
                                                missingKeywords = historyItem.missing_keywords,
                                                checklist = historyItem.improvement_checklist.map {
                                                    AtsChecklistItem(
                                                        item = it.item,
                                                        status = it.status,
                                                        impact = it.impact
                                                    )
                                                },
                                                overallAssessment = "Target Job: ${historyItem.target_job}\nVersion: ${historyItem.version}\nStatus: ${historyItem.status}",
                                                estimatedPassRate = when {
                                                    historyItem.ats_score >= 80 -> "High Pass Rate (80%+)"
                                                    historyItem.ats_score >= 60 -> "Moderate Pass Rate (60-80%)"
                                                    else -> "Low Pass Rate (<60%)"
                                                }
                                            )

                                            // Pre-populate resume text from sections if available
                                            historyItem.sections?.let { sections ->
                                                val resumeTextBuilder = StringBuilder()
                                                resumeTextBuilder.append("PROFESSIONAL SUMMARY\n")
                                                resumeTextBuilder.append(sections.professional_summary)
                                                resumeTextBuilder.append("\n\n")

                                                if (sections.education.isNotEmpty()) {
                                                    resumeTextBuilder.append("EDUCATION\n")
                                                    resumeTextBuilder.append(sections.education)
                                                    resumeTextBuilder.append("\n\n")
                                                }

                                                if (sections.experience.isNotEmpty()) {
                                                    resumeTextBuilder.append("EXPERIENCE\n")
                                                    sections.experience.forEach { exp ->
                                                        resumeTextBuilder.append("${exp.title} - ${exp.company}\n")
                                                        resumeTextBuilder.append("${exp.duration}\n")
                                                        exp.highlights.forEach { highlight ->
                                                            resumeTextBuilder.append("• $highlight\n")
                                                        }
                                                        resumeTextBuilder.append("\n")
                                                    }
                                                }

                                                if (sections.projects.isNotEmpty()) {
                                                    resumeTextBuilder.append("PROJECTS\n")
                                                    sections.projects.forEach { proj ->
                                                        resumeTextBuilder.append("${proj.name} - ${proj.tech}\n")
                                                        resumeTextBuilder.append("${proj.description}\n\n")
                                                    }
                                                }

                                                if (sections.skills.isNotEmpty()) {
                                                    resumeTextBuilder.append("SKILLS\n")
                                                    resumeTextBuilder.append(sections.skills.joinToString(", "))
                                                }

                                                resumeText = resumeTextBuilder.toString()
                                            }

                                            jobDescription = historyItem.target_job
                                            activeMode = "analyze"
                                        }
                                    )
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

@Composable
fun ResumeHistoryCard(historyItem: ResumeHistoryItem, onClick: () -> Unit) {
    val atsScore = historyItem.ats_score
    val targetJob = historyItem.target_job.ifEmpty { "Resume Analysis" }
    val missingKeywordsCount = historyItem.missing_keywords.size
    val checklistCount = historyItem.improvement_checklist.count { it.status == "pending" }

    KxCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        variant = KxCardVariant.Elevated
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score circle
            Surface(
                shape = CircleShape,
                color = when {
                    atsScore >= 80 -> Color(0xFF43A047).copy(alpha = 0.1f)
                    atsScore >= 60 -> Color(0xFFFFA000).copy(alpha = 0.1f)
                    else -> Color(0xFFE53935).copy(alpha = 0.1f)
                },
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$atsScore",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            atsScore >= 80 -> Color(0xFF43A047)
                            atsScore >= 60 -> Color(0xFFFFA000)
                            else -> Color(0xFFE53935)
                        }
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    targetJob,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    "Version ${historyItem.version} • ${historyItem.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatTimestamp(historyItem.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "View",
                tint = KaironexColors.ElectricBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        // Quick stats
        if (missingKeywordsCount > 0 || checklistCount > 0) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (missingKeywordsCount > 0) {
                    Column {
                        Text(
                            "$missingKeywordsCount",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                        Text(
                            "Missing Keywords",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }
                if (checklistCount > 0) {
                    Column {
                        Text(
                            "$checklistCount",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA000)
                        )
                        Text(
                            "Action Items",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.SlateGray
                        )
                    }
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: String): String {
    // Simple timestamp formatting - can be enhanced with actual date parsing
    return try {
        val parts = timestamp.split("T")
        if (parts.size >= 2) {
            val date = parts[0]
            val time = parts[1].split(".")[0]
            "$date at ${time.substring(0, 5)}"
        } else {
            timestamp
        }
    } catch (e: Exception) {
        timestamp
    }
}

