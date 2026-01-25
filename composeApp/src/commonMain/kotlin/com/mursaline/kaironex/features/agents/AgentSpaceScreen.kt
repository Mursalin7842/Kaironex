package com.mursaline.kaironex.features.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.core.stats.AgentStatus
import com.mursaline.kaironex.core.stats.StatsProvider
import com.mursaline.kaironex.ui.theme.KaironexColors

object AgentSpaceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val stats = remember { StatsProvider.getMoreStats() }
        val homeStats = remember { StatsProvider.getHomeStats() } // For Genesis monitoring if needed

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
                .padding(16.dp)
        ) {
            Text(
                "Agent Space (Judge View)",
                style = MaterialTheme.typography.headlineMedium,
                color = KaironexColors.Slate900,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Monitoring active autonomous agents",
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.Slate500
            )

            Spacer(Modifier.height(24.dp))

            // System Health
            SystemHealthCard(score = stats.lifeStability.overallScore)

            Spacer(Modifier.height(24.dp))

            // Agents Grid
            val agents = listOf(
                AgentSummary("Genesis", "Identity & Memory", AgentStatus.OPTIMAL, "Interviewer"),
                AgentSummary("Campaign", "Career & Growth", stats.campaign.agentStatus, "Hunter"),
                AgentSummary("Vitality", "Health & Finance", stats.vitality.agentStatus, "Guardian"),
                AgentSummary("Radius", "Environment & Social", stats.radius.agentStatus, "Scout")
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(agents) { agent ->
                    AgentSummaryCard(agent) {
                        navigator.push(AgentDetailScreen(agent.name))
                    }
                }
            }
        }
    }
}

data class AgentSummary(
    val name: String,
    val role: String,
    val status: AgentStatus,
    val archetype: String
)

@Composable
fun SystemHealthCard(score: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = KaironexColors.Slate900),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("System Stability", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Global Confidence Score", style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
            }
            Text(
                "$score%", 
                style = MaterialTheme.typography.displayMedium, 
                color = if (score > 70) KaironexColors.SuccessGreen else KaironexColors.AttentionOrange,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AgentSummaryCard(agent: AgentSummary, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(agent.archetype.uppercase(), style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
                Icon(
                    imageVector = Icons.Default.Warning, // Allow icon based on status
                    contentDescription = null,
                    tint = Color(agent.status.color),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(agent.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(agent.role, style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
            
            Spacer(Modifier.height(16.dp))
            
            Surface(
                color = Color(agent.status.color).copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    agent.status.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(agent.status.color),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
