package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
// import com.multiplatform.webview.web.WebView
// import com.multiplatform.webview.web.rememberWebViewState
// import com.multiplatform.webview.web.rememberWebViewNavigator
// import com.multiplatform.webview.web.LoadingState

import com.mursaline.kaironex.utils.LocalFileServer

@Composable
actual fun InterviewWebView(
    modifier: Modifier,
    apiKey: String,
    userName: String,
    agentName: String,
    onInterviewComplete: () -> Unit,
    onAgentStateChange: (Boolean, Boolean) -> Unit,
    onProfileUpdate: (String, String) -> Unit
) {
    // 1. Start Local Server for Assets (Bypasses file:// CORS issues)
    val projectDir = File(System.getProperty("user.dir"))
    
    // Robust Path Detection:
    // Check if we are running inside 'composeApp' or at root
    val candidate1 = File(projectDir, "src/androidMain/assets")
    val candidate2 = File(projectDir, "composeApp/src/androidMain/assets")
    
    val assetsRoot = if (candidate1.exists()) candidate1 else candidate2
    
    println("Project Dir: ${projectDir.absolutePath}")
    println("Serving Assets from: ${assetsRoot.absolutePath}")
    
    // Lazy start (idempotent)
    LocalFileServer.start(assetsRoot)
    val port = LocalFileServer.port

    // 2. Construct HTTP URL
    val url = "http://localhost:$port/interviewer/index.html?userName=${userName}&agentName=${agentName}"
    
    /*
    val state = rememberWebViewState(url)
    val navigator = rememberWebViewNavigator()

    // JS Bridge: Inject shim to redirect window.Android calls to custom URL scheme
    val loadingState = state.loadingState
    LaunchedEffect(loadingState) {
        ...
    }

    WebView(
        state = state,
        navigator = navigator,
        modifier = modifier
    )
    */
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("WebView is temporarily disabled for debugging.")
    }
}
