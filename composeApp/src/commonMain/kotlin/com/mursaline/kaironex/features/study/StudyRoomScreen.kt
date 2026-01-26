package com.mursaline.kaironex.features.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@Suppress("unused")
object StudyRoomScreen : Screen {
    @Suppress("unused")
    private fun readResolve(): Any = StudyRoomScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(KaironexColors.CloudGray)) {
            val isMobile = this.maxWidth < 800.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Common) - made responsive
                StudyHeader(
                     onBack = { navigator?.pop() },
                     title = if (isMobile) "Math: Calculus" else "Mathematics: Advanced Calculus",
                     timer = "00:45:00",
                     isMobile = isMobile
                )

                if (isMobile) {
                    // Mobile: Tabbed View (Resources | Viewer | Tools)
                    MobileStudyLayout()
                } else {
                    // Desktop: 3-Pane Layout
                    DesktopStudyLayout()
                }
                
                // Bottom Context Bar (Common)
                StudyContextBar(isMobile = isMobile)
            }
        }
    }
}

@Composable
fun StudyHeader(onBack: () -> Unit, title: String, timer: String, isMobile: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isMobile) 56.dp else 64.dp)
            .padding(horizontal = if (isMobile) 8.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KaironexColors.InkBlack)
            }
            if (!isMobile) Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = if (isMobile) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        Surface(
            color = KaironexColors.ElectricBlue.copy(alpha=0.1f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = timer,
                modifier = Modifier.padding(horizontal = if (isMobile) 8.dp else 12.dp, vertical = if (isMobile) 4.dp else 6.dp),
                color = KaironexColors.ElectricBlue,
                fontWeight = FontWeight.Bold,
                style = if (isMobile) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
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
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Resources", "Viewer", "Tools")

    val dummyResources = remember {
        listOf(
            Resource("1", "Ch 1: Limits", ResourceType.PDF, "45 min"),
            Resource("2", "Derivatives", ResourceType.VIDEO, "1h 20min"),
            Resource("3", "Practice", ResourceType.PDF, "30 min")
        )
    }
    var selectedResource by remember { mutableStateOf<Resource?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = KaironexColors.CanvasWhite,
            contentColor = KaironexColors.ElectricBlue
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(8.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Resources Tab
                    KxCard(
                        modifier = Modifier.fillMaxSize(),
                        variant = KxCardVariant.Flat,
                        backgroundColor = KaironexColors.CanvasWhite
                    ) {
                        ResourceIndex(
                            resources = dummyResources,
                            selectedResourceId = selectedResource?.id,
                            onResourceSelect = {
                                selectedResource = it
                                selectedTab = 1 // Switch to viewer
                            },
                            isMobile = true
                        )
                    }
                }
                1 -> {
                    // Viewer Tab
                    KxCard(
                        modifier = Modifier.fillMaxSize(),
                        variant = KxCardVariant.High,
                        backgroundColor = Color.Black
                    ) {
                        StudyViewer(resource = selectedResource, isMobile = true)
                    }
                }
                2 -> {
                    // Tools Tab
                    KxCard(
                        modifier = Modifier.fillMaxSize(),
                        variant = KxCardVariant.Flat,
                        backgroundColor = KaironexColors.CanvasWhite
                    ) {
                        StudyTools(isMobile = true)
                    }
                }
            }
        }
    }
}

@Composable
fun StudyContextBar(isMobile: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(if (isMobile) 48.dp else 56.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isMobile) 12.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = {}) {
                Text(
                    if (isMobile) "Break" else "Request Break",
                    color = KaironexColors.SlateGray,
                    style = if (isMobile) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
                )
            }
            
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.ElectricBlue),
                contentPadding = if (isMobile) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else ButtonDefaults.ContentPadding
            ) {
                Text(
                    if (isMobile) "Finish" else "Finish Session",
                    style = if (isMobile) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
