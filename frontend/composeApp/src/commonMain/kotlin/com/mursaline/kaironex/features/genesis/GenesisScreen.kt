package com.mursaline.kaironex.features.genesis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen

/**
 * GenesisScreen - The AI Interview Screen
 *
 * Now receives user identity data from SystemSetupScreen
 * and goes directly to the interview (no more redundant identity collection)
 */
data class GenesisScreen(
    val userName: String,
    val addressAs: String
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // ViewModel for the interview
        val viewModel: GenesisViewModel = viewModel { GenesisViewModel() }

        // Set identity immediately from passed parameters
        LaunchedEffect(Unit) {
            val effectiveName = addressAs.ifBlank { userName }
            viewModel.startInterview(effectiveName)
        }

        Scaffold(
            containerColor = Color.White
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
            ) {
                var hasNavigated by remember { mutableStateOf(false) }
                GenesisInterviewScreen(
                    viewModel = viewModel,
                    userName = addressAs.ifBlank { userName },
                    agentName = "Kaironex",
                    onInterviewComplete = {
                        if (!hasNavigated) {
                            hasNavigated = true
                            navigator.push(com.mursaline.kaironex.features.dashboard.ProfileCalibrationScreen(isOnboarding = true))
                        }
                    }
                )
            }
        }
    }
}
