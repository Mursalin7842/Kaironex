package com.mursaline.kaironex.features.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.features.study.components.*

object StudyRoomScreen : Screen {
    private fun readResolve(): Any = StudyRoomScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = maxWidth < 800.dp
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Common)
                StudyHeader(
                     onBack = { navigator?.pop() },
                     title = "Mathematics: Advanced Calculus",
                     timer = "00:45:00"
                )

                if (isMobile) {
                    // Mobile: Tabbed View (Resources | Viewer | Tools)
                    MobileStudyLayout()
                } else {
                    // Desktop: 3-Pane Layout
                    DesktopStudyLayout()
                }
                
                // Bottom Context Bar (Common)
                StudyContextBar()
            }
        }
    }
}

@Composable
fun StudyHeader(onBack: () -> Unit, title: String, timer: String) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = KaironexColors.InkBlack)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )
        }
        
        Surface(
            color = KaironexColors.ElectricBlue.copy(alpha=0.1f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = timer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = KaironexColors.ElectricBlue,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun DesktopStudyLayout() {
    var selectedResource by remember { mutableStateOf<Resource?>(null) }
    
    val dummyResources = remember {
        listOf(
            Resource("1", "Chapter 1: Limits and Continuity", ResourceType.PDF, "45 min"),
            Resource("2", "Derivatives Tutorial", ResourceType.VIDEO, "1h 20min"),
            Resource("3", "Integration Practice Problems", ResourceType.PDF, "30 min"),
            Resource("4", "Calculus Concepts Overview", ResourceType.VIDEO, "55 min"),
            Resource("5", "Sample Code: Numerical Integration", ResourceType.CODE)
        )
    }
    
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        // Left Pane: Resource Index
        KxCard(
            modifier = Modifier.width(250.dp).fillMaxHeight().padding(bottom = 16.dp),
            variant = KxCardVariant.Flat,
            backgroundColor = KaironexColors.CanvasWhite
        ) {
            ResourceIndex(
                resources = dummyResources,
                selectedResourceId = selectedResource?.id,
                onResourceSelect = { selectedResource = it }
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        // Center Pane: Content Viewer
        KxCard(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(bottom = 16.dp),
            variant = KxCardVariant.High,
            backgroundColor = Color.Black
        ) {
            StudyViewer(resource = selectedResource)
        }
        
        Spacer(Modifier.width(16.dp))
        
        // Right Pane: Tools
        KxCard(
            modifier = Modifier.width(300.dp).fillMaxHeight().padding(bottom = 16.dp),
            variant = KxCardVariant.Flat,
            backgroundColor = KaironexColors.CanvasWhite
        ) {
            StudyTools()
        }
    }
}

@Composable
fun MobileStudyLayout() {
    // Placeholder for tabs
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Mobile Study Layout (Tabs)", color = KaironexColors.InkBlack)
    }
}

@Composable
fun StudyContextBar() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {}) {
                Text("Request Break", color = KaironexColors.SlateGray)
            }
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue)
            ) {
                Text("Finish Session")
            }
        }
    }
}
