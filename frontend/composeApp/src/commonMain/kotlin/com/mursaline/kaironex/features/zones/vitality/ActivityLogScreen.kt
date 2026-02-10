package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.ui.components.KxTextField

object ActivityLogScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        
        var activityType by remember { mutableStateOf("") }
        var durationMins by remember { mutableStateOf("30") }
        var intensity by remember { mutableStateOf("Medium") }
        val intensityOptions = listOf("Low", "Medium", "High")
        
        var isSubmitting by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = KaironexColors.CloudGray,
            topBar = {
                TopAppBar(
                    title = { Text("Log Activity", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KaironexColors.CloudGray)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                KxTextField(
                    value = activityType,
                    onValueChange = { activityType = it },
                    label = "Activity Type (e.g. Running, Gym)",
                    modifier = Modifier.fillMaxWidth()
                )

                KxTextField(
                    value = durationMins,
                    onValueChange = { if (it.all { char -> char.isDigit() }) durationMins = it },
                    label = "Duration (Minutes)",
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = KaironexColors.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Intensity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            intensityOptions.forEach { level ->
                                FilterChip(
                                    selected = intensity == level,
                                    onClick = { intensity = level },
                                    label = { Text(level) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KaironexColors.SuccessGreen,
                                        selectedLabelColor = KaironexColors.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.logActivity(activityType, durationMins.toIntOrNull() ?: 0, intensity)
                        navigator.pop()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.Slate900),
                    enabled = !isSubmitting && activityType.isNotBlank()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = KaironexColors.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
