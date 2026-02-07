package com.mursaline.kaironex.features.campaign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.theme.KaironexColors

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
        val repo = koinInject<AppwriteStatsRepository>()
        val campaignState by repo.campaignState.collectAsState()
        val isLoading by repo.isLoading.collectAsState()
        
        val armory = campaignState.armory
        val inventory = armory.inventory
        val blueprints = armory.blueprints

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("The Armory", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(CampaignSetupScreen(isEditMode=true)) }) {
                            Icon(Icons.Filled.Settings, "Edit") // Requires import
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = KaironexColors.CanvasWhite
                    )
                )
            },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
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
                           
                           // Auto-retry fetch if empty and not loading
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
                    // Current Loadout (Main Resume)
                    item {
                        Text("Equipped Armor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        val equipped = inventory.find { it.status == "equipped" }
                        if (equipped != null) {
                            ArmoryItemCard(equipped)
                        } else {
                            Text("No artifact equipped.", style = MaterialTheme.typography.bodyMedium, color = KaironexColors.SlateGray)
                        }
                    }

                    // Actions
                    item {
                        Button(
                            onClick = { /* Forge Logic */ },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Filled.Build, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Forge New Artifact")
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
