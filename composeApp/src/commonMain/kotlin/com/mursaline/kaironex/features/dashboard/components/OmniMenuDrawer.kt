package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

data class MenuItem(
    val id: String,
    val title: String,
    val category: String,
    val icon: ImageVector? = null // Placeholder for now
)

@Composable
fun OmniMenuDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    if (isOpen) {
        ModalDrawerSheet(
             modifier = Modifier.fillMaxHeight().width(300.dp),
             drawerContainerColor = KaironexColors.CanvasWhite,
             drawerContentColor = KaironexColors.InkBlack
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Kaironex System",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.ElectricBlue
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Menu")
                    }
                }
                
                OmniMenuSection("🧠 Cortex (Focus & Learning)") {
                    OmniMenuItem("The Study Room", onClick = { onNavigate("StudyRoom") })
                    OmniMenuItem("Knowledge Graph", onClick = { onNavigate("KnowledgeGraph") })
                    OmniMenuItem("Mock Test Center", onClick = { onNavigate("MockTest") })
                }
                
                OmniMenuSection("💼 Campaign (Career)") {
                    OmniMenuItem("Job Campaign Manager", onClick = { onNavigate("JobCampaign") })
                    OmniMenuItem("Resume Debugger", onClick = { onNavigate("ResumeDebugger") })
                }
                
                OmniMenuSection("🧬 Vitality") {
                    OmniMenuItem("Nutritional Supply Chain", onClick = { onNavigate("Nutrition") })
                    OmniMenuItem("Sleep & Energy", onClick = { onNavigate("Sleep") })
                }
                 
                Spacer(Modifier.weight(1f))
                
                OmniMenuSection("⚙️ System") {
                    OmniMenuItem("Settings", onClick = { onNavigate("Settings") })
                }
            }
        }
    } else {
        // Just render content if drawer logic is external, but usually this wraps content or is used in a Scaffold.
        // For custom overlay approach:
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun OmniMenuSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = KaironexColors.SlateGray,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun OmniMenuItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .background(KaironexColors.CloudGray, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.Medium,
            color = KaironexColors.InkBlack
        )
    }
    Spacer(Modifier.height(8.dp))
}
