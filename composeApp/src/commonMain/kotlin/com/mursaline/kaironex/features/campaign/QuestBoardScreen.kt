package com.mursaline.kaironex.features.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.Sort
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
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * Quest Board: Intelligent Job Aggregator
 */
import org.koin.compose.koinInject
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.features.campaign.CampaignSetupScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object QuestBoardScreen : Screen {
    private fun readResolve(): Any = QuestBoardScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo = koinInject<AppwriteStatsRepository>()
        var filter by remember { mutableStateOf("All") }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Quest Board", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(CampaignSetupScreen(isEditMode = true)) }) {
                            Icon(Icons.Filled.Settings, "Edit Strategy")
                        }
                        IconButton(onClick = { /* Refresh */ }) {
                            Icon(Icons.Filled.Refresh, "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = KaironexColors.CanvasWhite,
                        titleContentColor = KaironexColors.InkBlack
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { /* Filter */ },
                    containerColor = Color(0xFF5E35B1), // Campaign Purple
                    contentColor = Color.White
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "Filter")
                    Spacer(Modifier.width(8.dp))
                    Text("Filter")
                }
            },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter == "All",
                        onClick = { filter = "All" },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = filter == "Visa",
                        onClick = { filter = "Visa" },
                        label = { Text("Visa Sponsor") }
                    )
                    FilterChip(
                        selected = filter == "Remote",
                        onClick = { filter = "Remote" },
                        label = { Text("Remote") }
                    )
                }

                // Quest List
                val campaignState by repo.campaignState.collectAsState()
                val quests = campaignState.questBoard
                
                if (quests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                       Column(horizontalAlignment = Alignment.CenterHorizontally) {
                           Text("No Quests Active", style = MaterialTheme.typography.titleMedium, color = KaironexColors.SlateGray)
                           Button(onClick = { navigator.push(CampaignSetupScreen(isEditMode=true)) }) {
                               Text("Setup Strategy")
                           }
                       }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(quests) { quest ->
                            QuestCard(quest)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestCard(quest: com.mursaline.kaironex.features.campaign.Quest) {
    KxCard(
        modifier = Modifier.fillMaxWidth(),
        variant = KxCardVariant.Elevated,
        onClick = { /* Open Job Details */ }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo Placeholder - Use Type Initial
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = KaironexColors.CloudGray,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if(quest.type.isNotEmpty()) quest.type.first().toString() else "?", 
                        fontWeight = FontWeight.Bold, 
                        color = KaironexColors.SlateGray
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(quest.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(quest.type, style = MaterialTheme.typography.bodyMedium, color = KaironexColors.SlateGray)
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    quest.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = KaironexColors.SlateGray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                KxBadge(
                    text = "${quest.xp} XP",
                    variant = KxBadgeVariant.Success
                )
                Spacer(Modifier.height(4.dp))
                Text(quest.status, style = MaterialTheme.typography.labelSmall, color = KaironexColors.SlateGray)
            }
        }
    }
}
