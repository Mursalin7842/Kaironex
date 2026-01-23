package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.components.KxIconButton
import com.mursaline.kaironex.ui.theme.KaironexColors

data class Agent(
    val id: String,
    val name: String,
    val subject: String,
    val status: String,
    val nextDeadline: String
)

@Composable
fun ActiveAgentDeck(
    agents: List<Agent>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(agents) { agent ->
                AgentCard(agent)
            }
        }
    }
}

@Composable
fun AgentCard(agent: Agent) {
    KxCard(
        modifier = Modifier.width(260.dp),
        variant = KxCardVariant.Elevated,
        onClick = { /* TODO: Open Agent Detail */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                KxBadge(
                    text = agent.subject,
                    variant = KxBadgeVariant.Brand
                )
                KxIconButton(
                    icon = Icons.Filled.MoreVert,
                    onClick = { /* Check menu */ }
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = agent.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.Slate900
            )
            
            Text(
                text = agent.status,
                style = MaterialTheme.typography.bodySmall,
                color = KaironexColors.Slate500
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Deadline: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.Slate500
                )
                Text(
                    text = agent.nextDeadline,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = KaironexColors.Rose500
                )
            }
        }
    }
}
