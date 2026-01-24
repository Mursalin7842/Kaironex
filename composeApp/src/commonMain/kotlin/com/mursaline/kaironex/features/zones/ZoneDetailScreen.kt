package com.mursaline.kaironex.features.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * Zone Detail Screen - The Command Center for each Life Track
 *
 * ============================================================
 * KAIRONEX ZONE ARCHITECTURE
 * ============================================================
 *
 * When a user clicks a Track on the Dashboard:
 * - CAMPAIGN → Shows Skill Tree, Quest Board, Armory, Simulacrum
 * - VITALITY → Shows Bio-Fuel, Resource Monitor, Regen Mode
 * - RADIUS   → Shows Signal Decoder, Safehouse, Local Scan, Admin Protocol
 */
data class ZoneDetailScreen(val track: LifeTrack) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
        ) {
            // 1. HERO HEADER (Track-colored)
            Surface(
                color = track.color,
                contentColor = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Back Button
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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
                items(getSubFeaturesForTrack(track)) { feature ->
                    ZoneFeatureCard(
                        feature = feature,
                        trackColor = track.color
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))

                    // Quick Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { /* TODO: Start primary action */ },
                            colors = ButtonDefaults.buttonColors(containerColor = track.color),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(getQuickActionLabel(track))
                        }
                        OutlinedButton(
                            onClick = { /* TODO: View history */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View History")
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
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
fun ZoneFeatureCard(feature: ZoneFeature, trackColor: Color) {
    KxCard(
        modifier = Modifier.fillMaxWidth(),
        variant = KxCardVariant.Elevated,
        onClick = { /* TODO: Open feature detail */ }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Emoji Icon Container
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = trackColor.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(feature.emoji, style = MaterialTheme.typography.headlineMedium)
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
                    Surface(
                        color = trackColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = feature.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = trackColor,
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
