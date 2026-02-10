package com.mursaline.kaironex.features.zones.radius

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.features.zones.components.*
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * 📡 RADIUS DASHBOARD SCREEN
 * ============================
 * External awareness and survival systems:
 * - Admin Protocol (Visa & Documentation)
 * - Signal Decoder (Cultural & Language Guide)
 * - Safehouse (Housing & Utilities)
 * - Local Scan (Nearby Resources)
 *
 * Designed for international students navigating
 * unfamiliar environments.
 */
object RadiusDashboardScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<RadiusViewModel>()
        val state by viewModel.uiState.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        Scaffold(
            containerColor = KaironexColors.CloudGray,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Radius Agent",
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.Slate900
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaironexColors.Slate900)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KaironexColors.CloudGray,
                        titleContentColor = KaironexColors.Slate900
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF9C27B0))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Card — Overall Radius Status
                    item {
                        RadiusHeroCard(state = state)
                    }

                    // Admin Protocol (Visa & Documentation)
                    item {
                        AdminProtocolCard(
                            visaDaysRemaining = state.visaDaysRemaining,
                            workHoursUsed = state.workHoursUsed,
                            workHourLimit = state.workHourLimit,
                            advisorContact = state.advisorContact,
                            alerts = state.adminAlerts,
                            onContactAdvisor = {
                                // Future: Open email/phone intent
                            }
                        )
                    }

                    // Signal Decoder (Cultural & Language Guide)
                    item {
                        SignalDecoderCard(
                            learnedTerms = state.termsLearned,
                            totalTerms = state.totalTerms,
                            recentTerms = getSampleSlangTerms(),
                            fluencyScore = state.languageFluencyScore,
                            onViewAll = {
                                // Future: Navigate to full glossary
                            }
                        )
                    }

                    // Local Scan (Nearby Resources)
                    item {
                        LocalScanCard(
                            resources = getSampleLocalResources(),
                            safeZoneAwareness = state.safeZoneAwareness,
                            onScan = {
                                // Future: GPS-based local scan
                            }
                        )
                    }

                    // Safehouse (Housing & Utilities)
                    item {
                        SafehouseCard(
                            listings = getSampleListings(),
                            housingStabilityScore = state.housingStabilityScore,
                            utilityReadiness = state.utilityReadiness,
                            onSearch = {
                                // Future: Housing search
                            }
                        )
                    }

                    // Bottom padding
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// === HERO CARD ===
@Composable
private fun RadiusHeroCard(state: RadiusUiState) {
    val overallScore = (
        (state.housingStabilityScore * 0.25f) +
        (state.languageFluencyScore * 0.2f) +
        (state.safeZoneAwareness * 100 * 0.2f) +
        (state.culturalComfort * 100 * 0.15f) +
        (state.localKnowledgeScore * 0.2f)
    ).toInt().coerceIn(0, 100)

    val (statusEmoji, statusLabel) = when {
        overallScore >= 80 -> "✅" to "Fully Adapted"
        overallScore >= 60 -> "🟢" to "Adapting Well"
        overallScore >= 40 -> "⚠️" to "Needs Attention"
        else -> "🚨" to "Critical Setup Needed"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.9f),
                            Color(0xFF7B1FA2).copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "📡 Radius Agent",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "External Awareness & Survival",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$statusEmoji", fontSize = 24.sp)
                            Text(
                                statusLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Quick stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStat("🏠", "Housing", "${state.housingStabilityScore}%")
                    QuickStat("🗣️", "Fluency", "${state.languageFluencyScore}%")
                    QuickStat("🔍", "Local", "${state.localKnowledgeScore}%")
                    QuickStat("👥", "Social", "${state.socialInteractionCount}/wk")
                }
            }
        }
    }
}

@Composable
private fun QuickStat(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

// === SAMPLE DATA (for hackathon demo) ===

private fun getSampleSlangTerms() = listOf(
    SlangTerm("No cap", "Not lying / for real", "\"That exam was hard, no cap\"", "US", true),
    SlangTerm("Slay", "Doing something exceptionally well", "\"You slayed that presentation\"", "US", true),
    SlangTerm("Lit", "Very exciting or excellent", "\"The campus event was lit\"", "US", false),
    SlangTerm("Brekkie", "Breakfast", "\"Let's grab brekkie before class\"", "AUS", false),
    SlangTerm("Cheers", "Thank you / goodbye", "\"Cheers for the notes!\"", "UK", true)
)

private fun getSampleLocalResources() = listOf(
    LocalResource("Library", "Study Space", "0.3 km", "📚"),
    LocalResource("Halal Kitchen", "Restaurant", "0.8 km", "🍽️"),
    LocalResource("FX Center", "Currency", "1.2 km", "💱"),
    LocalResource("Student Hub", "Support", "0.1 km", "🏫"),
    LocalResource("Pharmacy", "Health", "0.5 km", "💊"),
    LocalResource("Gym", "Fitness", "0.4 km", "🏋️")
)

private fun getSampleListings() = listOf(
    SafehouseListing("1", "Studio near Campus", "University Quarter", "$850/mo", 4.5f, true),
    SafehouseListing("2", "Shared 2BR Apt", "Downtown Core", "$650/mo", 4.2f, false)
)
