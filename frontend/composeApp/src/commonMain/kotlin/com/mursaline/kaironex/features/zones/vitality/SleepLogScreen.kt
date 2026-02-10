package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.launch

object SleepLogScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        
        var sleepHours by remember { mutableStateOf(7.5f) }
        var sleepQuality by remember { mutableStateOf("Good") }
        val qualityOptions = listOf("Poor", "Fair", "Good", "Excellent")
        
        var isSubmitting by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = KaironexColors.CloudGray,
            topBar = {
                TopAppBar(
                    title = { Text("Log Sleep", fontWeight = FontWeight.Bold) },
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
                // Hours Slider
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Duration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${sleepHours} hours", 
                            style = MaterialTheme.typography.displayMedium, 
                            color = KaironexColors.GeminiBlurple,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = sleepHours,
                            onValueChange = { sleepHours = it },
                            valueRange = 0f..14f,
                            steps = 27, // 0.5 increments
                            colors = SliderDefaults.colors(
                                thumbColor = KaironexColors.GeminiBlurple,
                                activeTrackColor = KaironexColors.GeminiBlurple
                            )
                        )
                    }
                }

                // Quality Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            qualityOptions.forEach { quality ->
                                FilterChip(
                                    selected = sleepQuality == quality,
                                    onClick = { sleepQuality = quality },
                                    label = { Text(quality) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = KaironexColors.GeminiBlurple,
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
                        viewModel.logSleep(sleepHours.toDouble(), sleepQuality)
                        navigator.pop()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.Slate900),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = KaironexColors.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Sleep Log", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
