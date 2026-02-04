package com.mursaline.kaironex.features.campaign

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

/**
 * Simulacrum: Mock Interview Simulator
 */
object SimulacrumScreen : Screen {
    private fun readResolve(): Any = SimulacrumScreen

// Imports
import androidx.compose.ui.platform.LocalContext
import android.view.WindowManager

// ...

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        var activeMode by remember { mutableStateOf<String?>(null) }
        
        // Keep Screen On during Simulation
        DisposableEffect(activeMode) {
            val activity = context as? android.app.Activity
            if (activeMode != null) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        
        // Fetch User Context
// ...
// (and remove the artifact line further down)
        val statsRepo = koinInject<AppwriteStatsRepository>()
        val profile by statsRepo.profile.collectAsState()
        
        val userName = (profile.name ?: "").takeIf { it.isNotEmpty() } ?: "Candidate"
        val targetRole = (profile.targetRole ?: "").takeIf { it.isNotEmpty() } ?: "Software Engineer"
        val company = "Tech Corp" // Placeholder or from profile if available
        
        // Base URL for React App (Local Asset)
        val baseUrl = "file:///android_asset/simulacrum/index.html"
        val apiKey = PlatformSecrets.apiKey

        Scaffold(
            topBar = {
                if (activeMode == null) {
                    CenterAlignedTopAppBar(
                        title = { Text("Simulacrum", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
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
            if (activeMode == null) {
                // Mode Selection
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Select Simulation Scenario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("For role: $targetRole", style = MaterialTheme.typography.bodyMedium, color = KaironexColors.SlateGray)
                    Spacer(Modifier.height(32.dp))

                    SimulationCard("Technical Recruiter", "Standard HR screening questions.", Color(0xFF1E88E5)) { activeMode = "Recruiter" }
                    Spacer(Modifier.height(16.dp))
                    SimulationCard("Hiring Manager", "High pressure conflict resolution.", Color(0xFFE53935)) { activeMode = "HiringManager" }
                    Spacer(Modifier.height(16.dp))
                    SimulationCard("Peer Review", "Casual culture fit check.", Color(0xFF43A047)) { activeMode = "Peer" }
                }
            } else {
                // Active Simulation (WebView)
                
                // Permission State
                var permissionsGranted by remember { mutableStateOf(false) }
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { result ->
                        permissionsGranted = result.values.all { it }
                    }
                )

                // Check and Request on Launch
                LaunchedEffect(Unit) {
                    launcher.launch(arrayOf(
                        "android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO"
                    ))
                }

                if (permissionsGranted) {
                    val encodedDesc = "Standard Job Description for $targetRole".run { 
                        this.replace(" ", "%20")
                    }
                    
                    val finalUrl = "$baseUrl?userName=$userName&jobTitle=$targetRole&jobCompany=$company&persona=$activeMode&jobDesc=$encodedDesc&apiKey=$apiKey"
                    
                    SimulacrumWebView(
                        url = finalUrl,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        onClose = { activeMode = null }
                    )
                } else {
                    // Permission Rationale UI
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Camera & Microphone Required", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { 
                            launcher.launch(arrayOf(
                                "android.permission.CAMERA",
                                "android.permission.RECORD_AUDIO"
                            ))
                        }) {
                            Text("Grant Permissions")
                        }
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
