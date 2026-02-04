package com.mursaline.kaironex.features.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors

import androidx.compose.ui.unit.sp

/**
 * Simulacrum: Mock Interview Simulator
 */
object SimulacrumScreen : Screen {
    private fun readResolve(): Any = SimulacrumScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var activeMode by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
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
                    Spacer(Modifier.height(32.dp))

                    SimulationCard("Technical Recruiter", "Standard HR screening questions.", Color(0xFF1E88E5)) { activeMode = "Recruiter" }
                    Spacer(Modifier.height(16.dp))
                    SimulationCard("Angry Manager", "High pressure conflict resolution.", Color(0xFFE53935)) { activeMode = "Manager" }
                    Spacer(Modifier.height(16.dp))
                    SimulationCard("System Design", "Whiteboard architecture session.", Color(0xFF43A047)) { activeMode = "System Design" }
                }
            } else {
                // Active Simulation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Black), // Immersive mode
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                           .fillMaxWidth()
                           .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { activeMode = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Exit", tint = Color.White)
                        }
                        Text(activeMode!!, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(48.dp))
                    }
                    
                    // Central Visualizer (Mock)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF5E35B1).copy(alpha = 0.5f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                         Text("AI Speaking...", color = Color.White.copy(alpha = 0.7f))
                    }
                    
                    // Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = { /* Toggle Mic */ },
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(Icons.Filled.Mic, "Speak", modifier = Modifier.size(32.dp))
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
