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
    val addressAs: String,
    val wakeWord: String
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // ViewModel for the interview
        val viewModel: GenesisViewModel = viewModel { GenesisViewModel() }

        // Set identity immediately from passed parameters
        LaunchedEffect(Unit) {
            viewModel.setIdentity(userName, wakeWord, addressAs)
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
                GenesisInterviewScreen(
                    viewModel = viewModel,
                    onInterviewComplete = {
                        navigator.replaceAll(MainShellScreen)
                    }
                )
            }
        }
    }
}
