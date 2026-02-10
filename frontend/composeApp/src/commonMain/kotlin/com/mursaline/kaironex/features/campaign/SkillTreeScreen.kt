package com.mursaline.kaironex.features.campaign

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * Skill Tree: Visual Learning Roadmap
 */
import org.koin.compose.koinInject
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.features.campaign.CampaignSetupScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object SkillTreeScreen : Screen {
    private fun readResolve(): Any = SkillTreeScreen

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repo = koinInject<AppwriteStatsRepository>()
        val campaignState by repo.campaignState.collectAsState()
        val skills = campaignState.skillTree

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Skill Tree", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(CampaignSetupScreen(isEditMode=true)) }) {
                            Icon(Icons.Filled.Settings, "Edit")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = KaironexColors.CanvasWhite
                    )
                )
            },
            containerColor = KaironexColors.CloudGray
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
               if (skills.isEmpty()) {
                   Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                       Column(horizontalAlignment = Alignment.CenterHorizontally) {
                           Text("No Skills Mapped", style = MaterialTheme.typography.titleMedium, color = KaironexColors.SlateGray)
                           Button(onClick = { navigator.push(CampaignSetupScreen(isEditMode=true)) }) {
                               Text("Setup Strategy")
                           }
                       }
                   }
               } else {
                   // Timeline / Tree Visualization
                   LazyColumn(
                       modifier = Modifier.fillMaxSize(),
                       contentPadding = PaddingValues(24.dp),
                       horizontalAlignment = Alignment.CenterHorizontally
                   ) {
                       item { Text("Path: ${campaignState.questBoard.firstOrNull()?.tags?.firstOrNull() ?: "Career"}", style = MaterialTheme.typography.titleMedium, color = KaironexColors.SlateGray) }
                       item { Spacer(Modifier.height(32.dp)) }

                       items(skills) { skill ->
                           SkillNodeView(skill)
                           if (skill != skills.last()) {
                               ConnectorLine(dashed = skill.status == "locked")
                           }
                       }
                   }
               }
            }
        }
    }
}

@Composable
fun SkillNodeView(skill: com.mursaline.kaironex.features.campaign.SkillNode) {
    val unlocked = skill.status != "locked"
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (unlocked) KaironexColors.CanvasWhite else Color.LightGray.copy(alpha = 0.5f),
        shadowElevation = if (unlocked) 4.dp else 0.dp,
        modifier = Modifier.width(280.dp).wrapContentHeight()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (unlocked) Color(0xFF5E35B1) else Color.Gray,
                modifier = Modifier.size(12.dp)
            ) {}
            Spacer(Modifier.width(16.dp))
            Column {
                Text(skill.name, fontWeight = FontWeight.Bold, color = if (unlocked) KaironexColors.InkBlack else Color.Gray)
                Text(skill.description, style = MaterialTheme.typography.bodySmall, color = if (unlocked) KaironexColors.SlateGray else Color.Gray)
                if (skill.xpCost > 0 && !unlocked) {
                    Text("Requires ${skill.xpCost} XP", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ConnectorLine(dashed: Boolean = false) {
    Box(
        modifier = Modifier.height(40.dp).width(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.Gray,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 4f,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
            )
        }
    }
}
