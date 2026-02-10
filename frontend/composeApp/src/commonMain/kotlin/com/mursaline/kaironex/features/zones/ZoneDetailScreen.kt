package com.mursaline.kaironex.features.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.koin.compose.koinInject
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

import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch

/**
 * Zone Detail Screen - The Command Center for each Life Track
 */
data class ZoneDetailScreen(val track: LifeTrack) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var showLogDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        
        // Access Stats for Gating
        val statsRepo = koinInject<com.mursaline.kaironex.core.stats.AppwriteStatsRepository>()
        val moreStats by statsRepo.moreStats.collectAsState()
        val isCalibrated = moreStats.campaign.isCalibrated

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(KaironexColors.CloudGray)
            ) {
                // 1. HERO HEADER (Track-colored)
                Surface(
                    color = track.color,
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Top Bar: Back + Settings
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                            }
                            
                            // Edit/Recalibrate Button (Only for Campaign for now)
                            if (track == LifeTrack.Campaign) {
                                IconButton(onClick = { navigator.push(com.mursaline.kaironex.features.campaign.CampaignSetupScreen(isEditMode = true)) }) {
                                    Icon(Icons.Default.Settings, "Recalibrate", tint = Color.White)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Track Identity
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.emoji,
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = track.subtitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Tagline
                        Text(
                            text = track.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                // 2. SUB-FEATURES LIST
                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Available Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.SlateGray
                        )
                    }

                    // Dynamically render sub-features based on track
                    if (track == LifeTrack.Campaign && !isCalibrated) {
                        // 1. CTA to Setup
                        item {
                            ZoneFeatureCard(
                                feature = ZoneFeature(
                                    name = "Setup Campaign Strategy",
                                    description = "Initialize your career profile to unlock the Campaign Agent features.",
                                    emoji = "🚀",
                                    status = "Action Required"
                                ),
                                trackColor = track.color,
                                isLocked = false
                            )
                        }
                        
                        // 2. Locked Tools
                        items(getSubFeaturesForTrack(track)) { feature ->
                            ZoneFeatureCard(
                                feature = feature,
                                trackColor = track.color,
                                isLocked = true,
                                onLockedClick = { 
                                     scope.launch { snackbarHostState.showSnackbar("Setup Your Profile to Unlock ${feature.name}") }
                                }
                            )
                        }
                    } else {
                        items(getSubFeaturesForTrack(track)) { feature ->
                            ZoneFeatureCard(
                                feature = feature,
                                trackColor = track.color
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(80.dp)) // Extra space
                    }
                }
            }
        }

        if (showLogDialog) {
            ZoneLogDialog(track, onDismiss = { showLogDialog = false })
        }
    }
}

@Composable
fun ZoneLogDialog(track: LifeTrack, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log ${track.title} Activity") },
        text = {
            Column {
                Text("Update your stats manually. (This would connect to ${track.title} Agent)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                
                // Dynamic Fields based on Track
                when(track) {
                    LifeTrack.Vitality -> {
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Sleep Hours") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Energy Level (1-10)") }, modifier = Modifier.fillMaxWidth())
                    }
                    LifeTrack.Campaign -> {
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Goal Update") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Hours Spent") }, modifier = Modifier.fillMaxWidth())
                    }
                    LifeTrack.Radius -> {
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Current Location") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = "", onValueChange = {}, label = { Text("Environment Mode") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = track.color)
            ) {
                Text("Log Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ===== DATA MODEL FOR ZONE FEATURES =====
data class ZoneFeature(
    val name: String,
    val description: String,
    val emoji: String,
    val status: String = "Ready"
)

// ===== GET SUB-FEATURES FOR EACH TRACK =====
fun getSubFeaturesForTrack(track: LifeTrack): List<ZoneFeature> {
    return when(track) {
        // THE CAMPAIGN - Career & Growth
        LifeTrack.Campaign -> listOf(
            ZoneFeature(
                name = CampaignFeature.SkillTree.title,
                description = CampaignFeature.SkillTree.description,
                emoji = CampaignFeature.SkillTree.emoji,
                status = "Active"
            ),
            ZoneFeature(
                name = CampaignFeature.QuestBoard.title,
                description = CampaignFeature.QuestBoard.description,
                emoji = CampaignFeature.QuestBoard.emoji,
                status = "3 New Quests"
            ),
            ZoneFeature(
                name = CampaignFeature.Armory.title,
                description = CampaignFeature.Armory.description,
                emoji = CampaignFeature.Armory.emoji,
                status = "Ready"
            ),
            ZoneFeature(
                name = CampaignFeature.Simulacrum.title,
                description = CampaignFeature.Simulacrum.description,
                emoji = CampaignFeature.Simulacrum.emoji,
                status = "Voice Mode"
            )
        )

        // THE VITALITY - Food & Finance
        LifeTrack.Vitality -> listOf(
            ZoneFeature(
                name = VitalityFeature.BioFuel.title,
                description = VitalityFeature.BioFuel.description,
                emoji = VitalityFeature.BioFuel.emoji,
                status = "Scan Fridge"
            ),
            ZoneFeature(
                name = VitalityFeature.ResourceMonitor.title,
                description = VitalityFeature.ResourceMonitor.description,
                emoji = VitalityFeature.ResourceMonitor.emoji,
                status = "23 Days Runway"
            ),
            ZoneFeature(
                name = VitalityFeature.RegenMode.title,
                description = VitalityFeature.RegenMode.description,
                emoji = VitalityFeature.RegenMode.emoji,
                status = "Sleep: 7.2h avg"
            )
        )

        // THE RADIUS - Habitat & Culture
        LifeTrack.Radius -> listOf(
            ZoneFeature(
                name = RadiusFeature.SignalDecoder.title,
                description = RadiusFeature.SignalDecoder.description,
                emoji = RadiusFeature.SignalDecoder.emoji,
                status = "12 Slang Terms"
            ),
            ZoneFeature(
                name = RadiusFeature.Safehouse.title,
                description = RadiusFeature.Safehouse.description,
                emoji = RadiusFeature.Safehouse.emoji,
                status = "3 Listings"
            ),
            ZoneFeature(
                name = RadiusFeature.LocalScan.title,
                description = RadiusFeature.LocalScan.description,
                emoji = RadiusFeature.LocalScan.emoji,
                status = "GPS Active"
            ),
            ZoneFeature(
                name = RadiusFeature.AdminProtocol.title,
                description = RadiusFeature.AdminProtocol.description,
                emoji = RadiusFeature.AdminProtocol.emoji,
                status = "Visa: 89 Days"
            )
        )
    }
}

// ===== QUICK ACTION LABEL PER TRACK =====
fun getQuickActionLabel(track: LifeTrack): String {
    return when(track) {
        LifeTrack.Campaign -> "Start Career Quest"
        LifeTrack.Vitality -> "Quick Budget Check"
        LifeTrack.Radius -> "Scan Nearby"
    }
}

// ===== ZONE FEATURE CARD COMPONENT =====
@Composable
fun ZoneFeatureCard(
    feature: ZoneFeature, 
    trackColor: Color,
    isLocked: Boolean = false,
    onLockedClick: () -> Unit = {}
) {
    val navigator = LocalNavigator.currentOrThrow
    
    KxCard(
        modifier = Modifier.fillMaxWidth().alpha(if (isLocked) 0.6f else 1f),
        variant = KxCardVariant.Elevated,
        onClick = {
            if (isLocked) {
                onLockedClick()
            } else {
                when (feature.name) {
                    // Campaign Features
                    "Setup Campaign Strategy" -> navigator.push(com.mursaline.kaironex.features.campaign.CampaignSetupScreen(isEditMode = false))
                    CampaignFeature.QuestBoard.title -> navigator.push(com.mursaline.kaironex.features.campaign.QuestBoardScreen)
                    CampaignFeature.Armory.title -> navigator.push(com.mursaline.kaironex.features.campaign.ArmoryScreen)
                    CampaignFeature.SkillTree.title -> navigator.push(com.mursaline.kaironex.features.campaign.SkillTreeScreen)
                    CampaignFeature.Simulacrum.title -> navigator.push(com.mursaline.kaironex.features.campaign.SimulacrumScreen)
                    
                    // Other tracks (Placeholder)
                    else -> { /* No action yet */ }
                }
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Emoji Icon Container
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isLocked) Color.Gray.copy(alpha=0.1f) else trackColor.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLocked && feature.name != "Setup Campaign Strategy") {
                         Text("🔒", style = MaterialTheme.typography.headlineMedium)
                    } else {
                         Text(feature.emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        feature.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack
                    )

                    // Status Badge
                    val statusColor = if (feature.status == "Action Required") Color.Red else trackColor
                    
                    Surface(
                        color = if (isLocked) Color.Gray.copy(alpha=0.2f) else statusColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (isLocked) "Locked" else feature.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLocked) Color.Gray else statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = KaironexColors.SlateGray,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                )
            }
        }
    }
}
