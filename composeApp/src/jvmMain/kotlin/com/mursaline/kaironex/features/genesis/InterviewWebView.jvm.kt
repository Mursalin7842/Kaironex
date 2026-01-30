package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import java.io.File
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.LoadingState

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
    
    val state = rememberWebViewState(url)
    val navigator = rememberWebViewNavigator()

    // JS Bridge: Inject shim to redirect window.Android calls to custom URL scheme
    val loadingState = state.loadingState
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Finished) {
            // 1. Inject API Key
            navigator.evaluateJavaScript("window.ANDROID_API_KEY = '$apiKey';")
            
            // 2. Inject JS Shim for Callbacks
            val jsShim = """
                window.Android = {
                    onAgentState: function(isTalking, isConnected) {
                        window.location.href = 'kaironex://agentState?talking=' + isTalking + '&connected=' + isConnected;
                    },
                    onProfileUpdate: function(field, value) {
                        window.location.href = 'kaironex://profileUpdate?field=' + encodeURIComponent(field) + '&value=' + encodeURIComponent(value);
                    },
                    onComplete: function() {
                        window.location.href = 'kaironex://complete';
                    }
                };
            """.trimIndent()
            navigator.evaluateJavaScript(jsShim)
        }
    }

    // URL Interception for Callbacks
    val currentUrl = state.lastLoadedUrl
    LaunchedEffect(currentUrl) {
        if (currentUrl != null) println("WebView Loaded: $currentUrl")
        
        val uriStr = currentUrl ?: ""
        if (uriStr.startsWith("kaironex://")) {
            if (uriStr.startsWith("kaironex://agentState")) {
                val talking = uriStr.contains("talking=true")
                val connected = uriStr.contains("connected=true")
                onAgentStateChange(talking, connected)
            } else if (uriStr.startsWith("kaironex://profileUpdate")) {
                try {
                    val field = uriStr.substringAfter("field=").substringBefore("&")
                    val value = uriStr.substringAfter("value=")
                    val decodedField = java.net.URLDecoder.decode(field, "UTF-8")
                    val decodedValue = java.net.URLDecoder.decode(value, "UTF-8")
                    onProfileUpdate(decodedField, decodedValue)
                } catch (e: Exception) { println("Error parsing profile update: $e") }
            } else if (uriStr.startsWith("kaironex://complete")) {
                onInterviewComplete()
            }
        }
    }

    WebView(
        state = state,
        navigator = navigator,
        modifier = modifier
    )
}
