package com.mursaline.kaironex.features.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.ui.components.SimulacrumWebView
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.PlatformSecrets
import kotlinx.coroutines.launch

/**
 * Simulacrum: Mock Interview Simulator with AI-Designed Interviews
 */
object SimulacrumScreen : Screen {
    private fun readResolve(): Any = SimulacrumScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        
        var screenState by remember { mutableStateOf<String>("setup") } // "setup", "mode_select", "active"
        var activeMode by remember { mutableStateOf<String?>(null) }
        
        // Interview Setup Fields
        var jobDescription by remember { mutableStateOf("") }
        var resumeText by remember { mutableStateOf("") }
        var selectedDifficulty by remember { mutableStateOf("medium") }
        var isDesigning by remember { mutableStateOf(false) }
        var interviewDesign by remember { mutableStateOf<InterviewDesign?>(null) }
        
        // Keep Screen On during Simulation
        // TODO: Implement platform-specific screen control
        /*
        DisposableEffect(activeMode) {
            val activity = context as? android.app.Activity
            if (activeMode != null) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        */
        
        // Fetch User Context
        val statsRepo = koinInject<AppwriteStatsRepository>()
        val profile by statsRepo.profile.collectAsState()
        
        val userName = (profile.name ?: "").takeIf { it.isNotEmpty() } ?: "Candidate"
        val targetRole = (profile.targetRole ?: "").takeIf { it.isNotEmpty() } ?: "Software Engineer"
        val company = "Tech Corp"
        
        // Base URL for React App (Local Asset)
        val baseUrl = "https://appassets.androidplatform.net/assets/simulacrum/index.html"
        val apiKey = PlatformSecrets.apiKey

        Scaffold(
            topBar = {
                if (screenState != "active") {
                    CenterAlignedTopAppBar(
                        title = { Text(
                            when (screenState) {
                                "setup" -> "Interview Setup"
                                "mode_select" -> "Select Interviewer"
                                else -> "Simulacrum"
                            },
                            fontWeight = FontWeight.Bold
                        ) },
                        navigationIcon = {
                            IconButton(onClick = { 
                                when (screenState) {
                                    "mode_select" -> screenState = "setup"
                                    else -> navigator.pop()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = KaironexColors.CanvasWhite
                        )
                    )
                }
            },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
            when (screenState) {
                "setup" -> {
                    // Interview Setup UI
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Design Your Interview", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Kairo will create a personalized mock interview", style = MaterialTheme.typography.bodyMedium, color = KaironexColors.SlateGray)
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Job Description Input
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Job Description *") },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            placeholder = { Text("Paste the job description you're preparing for...") }
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Resume Input (Optional)
                        OutlinedTextField(
                            value = resumeText,
                            onValueChange = { resumeText = it },
                            label = { Text("Your Resume (Optional)") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            placeholder = { Text("Paste your resume for personalized questions...") }
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Difficulty Selection
                        Text("Difficulty", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("easy" to "Friendly", "medium" to "Standard", "hard" to "Tough", "brutal" to "Brutal").forEach { (value, label) ->
                                FilterChip(
                                    selected = selectedDifficulty == value,
                                    onClick = { selectedDifficulty = value },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (value) {
                                            "easy" -> Color(0xFF43A047)
                                            "medium" -> Color(0xFF1E88E5)
                                            "hard" -> Color(0xFFFFA000)
                                            else -> Color(0xFFE53935)
                                        }
                                    )
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Design Interview Button
                        Button(
                            onClick = {
                                scope.launch {
                                    isDesigning = true
                                    val design = statsRepo.designInterview(
                                        jobDescription = jobDescription,
                                        resumeText = resumeText.takeIf { it.isNotBlank() },
                                        difficulty = selectedDifficulty
                                    )
                                    interviewDesign = design
                                    isDesigning = false
                                    if (design != null) {
                                        screenState = "mode_select"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = jobDescription.isNotBlank() && !isDesigning,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EA))
                        ) {
                            if (isDesigning) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Designing Interview...")
                            } else {
                                Text("Design Interview")
                            }
                        }
                        
                        // Show prep notes if design available
                        interviewDesign?.let { design ->
                            Spacer(Modifier.height(24.dp))
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Interview Ready!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Spacer(Modifier.height(8.dp))
                                    Text("${design.questionBank.size} questions prepared", style = MaterialTheme.typography.bodySmall)
                                    Text("Duration: ~${design.durationMinutes} minutes", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Prep Notes:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    design.candidatePrepNotes.forEach { note ->
                                        Text("• $note", style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
                                    }
                                }
                            }
                        }
                        
                        // Quick Start Option
                        Spacer(Modifier.height(32.dp))
                        TextButton(
                            onClick = { screenState = "mode_select" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip Setup - Use Default Interview", color = KaironexColors.SlateGray)
                        }
                    }
                }
                
                "mode_select" -> {
                    // Mode Selection
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select Interviewer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("For role: $targetRole", style = MaterialTheme.typography.bodyMedium, color = KaironexColors.SlateGray)
                        Spacer(Modifier.height(32.dp))

                        SimulationCard("Technical Recruiter", "Standard HR screening questions.", Color(0xFF1E88E5)) { 
                            activeMode = "Recruiter"
                            screenState = "active"
                        }
                        Spacer(Modifier.height(16.dp))
                        SimulationCard("Hiring Manager", "High pressure conflict resolution.", Color(0xFFE53935)) { 
                            activeMode = "HiringManager"
                            screenState = "active"
                        }
                        Spacer(Modifier.height(16.dp))
                        SimulationCard("Peer Review", "Casual culture fit check.", Color(0xFF43A047)) { 
                            activeMode = "Peer"
                            screenState = "active"
                        }
                    }
                }
                
                "active" -> {
                    // Active Simulation (WebView)
                    
                    // Permission State
                    // var permissionsGranted by remember { mutableStateOf(false) }
                    // val launcher = rememberLauncherForActivityResult(
                    //     contract = ActivityResultContracts.RequestMultiplePermissions(),
                    //     onResult = { result ->
                    //         permissionsGranted = result.values.all { it }
                    //     }
                    // )

                    // Check and Request on Launch
                    // LaunchedEffect(Unit) {
                    //    launcher.launch(arrayOf(
                    //        "android.permission.CAMERA",
                    //        "android.permission.RECORD_AUDIO"
                    //    ))
                    // }

                    // Assume permissions are handled by platform code or user manually for now
                    val permissionsGranted = true

                    if (permissionsGranted) {
                         // Build URL with interview context
                        val encodedDesc = (interviewDesign?.geminiLivePrompt ?: "Standard Job Description for $targetRole").run { 
                            this.replace(" ", "%20").take(2000)
                        }
                        
                        val finalUrl = "$baseUrl?userName=$userName&jobTitle=$targetRole&jobCompany=$company&persona=$activeMode&jobDesc=$encodedDesc&apiKey=$apiKey&difficulty=$selectedDifficulty"
                        
                        SimulacrumWebView(
                            url = finalUrl,
                            modifier = Modifier.fillMaxSize().padding(padding),
                            onClose = { 
                                activeMode = null
                                screenState = "setup"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimulationCard(title: String, description: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                 // Placeholder Icon
                 Text(title.first().toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = KaironexColors.SlateGray)
            }
        }
    }
}
