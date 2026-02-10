package com.mursaline.kaironex.features.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxCard
import com.mursaline.kaironex.ui.components.KxCardVariant
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant

object MonthlyPlansScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<MonthlyPlansViewModel>()
        val plans by viewModel.plans.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Semester Plan", 
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.InkBlack
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = KaironexColors.InkBlack
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KaironexColors.CanvasWhite
                    )
                )
            },
            containerColor = KaironexColors.CanvasWhite // "White Theme"
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = KaironexColors.ElectricBlue
                    )
                } else if (plans.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(plans) { index, plan ->
                            // Staggered animation effect
                            var isVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(index) {
                                isVisible = true // Trigger locally
                            }
                            
                            AnimatedVisibility(
                                visible = isVisible,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) +
                                        slideInVertically(animationSpec = tween(300, delayMillis = index * 50)) { it / 2 }
                            ) {
                                MonthlyPlanCard(plan)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "📅",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No Semester Plan Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload your syllabus to generate a plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = KaironexColors.SlateGray
            )
        }
    }

    @Composable
    private fun MonthlyPlanCard(plan: MonthlyPlansRepository.MonthlyPlan) {
        var expanded by remember { mutableStateOf(false) }

        // Use a Surface with shadow elevation to create the "floating" effect
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), // Check corner radius of other cards, usually 12 or 16
            color = KaironexColors.CanvasWhite,
            shadowElevation = 4.dp // Add shadow back
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header: Month & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Derive Month Name from Start Date for accuracy
                        Text(
                            text = getMonthNameFromDate(plan.startDate) ?: getMonthName(plan.monthIndex),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.InkBlack
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatDate(plan.startDate)} - ${formatDate(plan.endDate)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = KaironexColors.SlateGray
                        )
                    }
                    
                    KxBadge(
                        text = plan.status.uppercase(),
                        variant = when(plan.status.lowercase()) {
                            "completed" -> KxBadgeVariant.Success
                            "active", "in_progress" -> KxBadgeVariant.Brand
                            else -> KxBadgeVariant.Neutral
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = KaironexColors.CloudGray)
                Spacer(Modifier.height(16.dp))

                // Goals Section
                Text(
                    text = "Key Objectives",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KaironexColors.ElectricBlue
                )
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = plan.goalsContext ?: "No specific goals set.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.InkBlack,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
                
                if (!plan.goalsContext.isNullOrBlank() && plan.goalsContext.length > 150) {
                   Spacer(Modifier.height(8.dp))
                   // Make "Read More" distinct and clickable
                   Row(
                       modifier = Modifier
                           .clip(RoundedCornerShape(4.dp))
                           .clickable { expanded = !expanded }
                           .padding(4.dp)
                   ) {
                       Text(
                           text = if (expanded) "Show Less" else "Read More",
                           style = MaterialTheme.typography.labelMedium,
                           color = KaironexColors.GeminiBlurple,
                           fontWeight = FontWeight.Bold
                       )
                   }
                }
            }
        }
    }

    private fun getMonthName(index: Int): String {
        return when(index) {
            1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
            5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
            9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
            else -> "Month $index"
        }
    }

    private fun getMonthNameFromDate(dateString: String): String? {
        // Expected format: YYYY-MM-DD...
        return try {
            val parts = dateString.split("-")
            if (parts.size >= 2) {
                val monthNum = parts[1].toIntOrNull()
                if (monthNum != null) getMonthName(monthNum) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            isoDate.split("T")[0] // Simplistic YYYY-MM-DD
        } catch (e: Exception) {
            isoDate
        }
    }
}
