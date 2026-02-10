package com.mursaline.kaironex.features.zones.vitality

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.delay
import cafe.adriel.voyager.koin.koinScreenModel

object MealDecisionScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<VitalityViewModel>()
        val state by viewModel.uiState.collectAsState()
        
        // Local State
        var step by remember { mutableStateOf<DecisionStep>(DecisionStep.Input) }
        var timeAvailable by remember { mutableStateOf(30) }
        var energyLevel by remember { mutableStateOf(50f) }

        // Transition logic
        LaunchedEffect(state.lastDecision) {
            if (state.lastDecision != null && step is DecisionStep.Loading) {
                step = DecisionStep.Result(
                    DecisionResult(
                        type = state.lastDecision!!.action.uppercase(),
                        emoji = if (state.lastDecision!!.action == "cook") "🍳" else "🛵",
                        title = if (state.lastDecision!!.action == "cook") "Cook Meal" else "Order Food",
                        color = if (state.lastDecision!!.action == "cook") KaironexColors.SuccessGreen else KaironexColors.AttentionOrange,
                        reason = state.lastDecision!!.reason,
                        suggestion = state.lastDecision!!.suggestion
                    )
                )
            }
        }

        Scaffold(
            containerColor = KaironexColors.CloudGray,
            topBar = {
                TopAppBar(
                    title = { Text("Meal Decision", fontWeight = FontWeight.Bold, color = KaironexColors.Slate900) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = KaironexColors.Slate900)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = KaironexColors.CloudGray)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val current = step) {
                    is DecisionStep.Input -> InputView(
                        time = timeAvailable,
                        energy = energyLevel,
                        onTimeChange = { timeAvailable = it },
                        onEnergyChange = { energyLevel = it },
                        onDecide = {
                            step = DecisionStep.Loading
                            viewModel.makeMealDecision(timeAvailable, energyLevel.toInt())
                        }
                    )
                    is DecisionStep.Loading -> LoadingView {
                         // Brain Logic is handled via ViewModel and LaunchedEffect
                    }
                    is DecisionStep.Result -> ResultView(
                        result = current.data,
                        onReset = { step = DecisionStep.Input }
                    )
                }
            }
        }
    }

    @Composable
    private fun InputView(
        time: Int,
        energy: Float,
        onTimeChange: (Int) -> Unit,
        onEnergyChange: (Float) -> Unit,
        onDecide: () -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("🤔 What should I eat?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
            }

            // Time Selection
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("⏰ TIME AVAILABLE", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(15, 30, 60).forEach { t ->
                                FilterChip(
                                    selected = time == t,
                                    onClick = { onTimeChange(t) },
                                    label = { Text("${t}m") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Energy Selection
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("🔋 ENERGY LEVEL", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("😫", fontSize = 24.sp)
                            Slider(
                                value = energy,
                                onValueChange = onEnergyChange,
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = KaironexColors.GeminiBlurple,
                                    activeTrackColor = KaironexColors.GeminiBlurple
                                )
                            )
                            Text("😊", fontSize = 24.sp)
                        }
                        Text(
                            when {
                                energy < 30 -> "Low Energy"
                                energy < 70 -> "Moderate"
                                else -> "High Energy"
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.Slate500
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onDecide,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.GeminiBlurple)
                ) {
                    Text("🎯 DECIDE FOR ME", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    @Composable
    private fun LoadingView(onFinished: suspend () -> Unit) {
        LaunchedEffect(Unit) {
            delay(2000)
            onFinished()
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = KaironexColors.GeminiBlurple)
            Spacer(Modifier.height(16.dp))
            Text("Brain is analyzing options...", color = KaironexColors.Slate500)
        }
    }

    @Composable
    private fun ResultView(result: DecisionResult, onReset: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Decision Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = result.color,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(result.emoji, fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(result.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(result.suggestion, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
                }
            }

            // Reasoning
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("📝 REASONING", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "\"${result.reason}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KaironexColors.Slate900,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KaironexColors.Slate900)
            ) {
                Text("Start Over")
            }
        }
    }

    sealed class DecisionStep {
        object Input : DecisionStep()
        object Loading : DecisionStep()
        data class Result(val data: DecisionResult) : DecisionStep()
    }

    data class DecisionResult(
        val type: String,
        val emoji: String,
        val title: String,
        val color: Color,
        val reason: String,
        val suggestion: String
    )
}
