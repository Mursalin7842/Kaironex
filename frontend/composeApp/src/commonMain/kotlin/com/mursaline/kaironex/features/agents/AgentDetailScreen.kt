package com.mursaline.kaironex.features.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.core.stats.StatsProvider
import com.mursaline.kaironex.ui.theme.KaironexColors

data class AgentDetailScreen(val agentName: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scrollState = rememberScrollState()
        
        // Mock retrieving specific agent data based on name
        val stats = remember { StatsProvider.getMoreStats() }
        val rawData = when(agentName) {
            "Campaign" -> stats.campaign.toString()
            "Vitality" -> stats.vitality.toString()
            "Radius" -> stats.radius.toString()
            else -> "Genesis Agent Memory Log..."
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$agentName Protocol") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KaironexColors.Slate900, titleContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .background(KaironexColors.Slate900)
                    .padding(16.dp)
            ) {
                // Status Header
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Protocol Status", style = MaterialTheme.typography.labelMedium, color = KaironexColors.Slate500)
                        Text("ACTIVE - LISTENING", style = MaterialTheme.typography.titleLarge, color = KaironexColors.SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text("Memory Dump", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                
                // Raw Data Viewer
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatString(rawData),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = KaironexColors.ElectricBlue
                    )
                }
            }
        }
    }
    
    private fun formatString(str: String): String {
        return str.replace("(", "(\n  ")
            .replace(",", ",\n  ")
            .replace(")", "\n)")
    }
}
