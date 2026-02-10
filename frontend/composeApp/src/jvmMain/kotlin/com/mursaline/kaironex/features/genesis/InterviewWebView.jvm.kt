package com.mursaline.kaironex.features.genesis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.datlag.kcef.KCEF
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefRendering
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefPermissionHandler
import org.cef.callback.CefMediaAccessCallback
import org.cef.CefSettings
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

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

    // 2. Construct HTTP URL with proper URL encoding
    val encodedUser = java.net.URLEncoder.encode(userName, "UTF-8")
    val encodedAgent = java.net.URLEncoder.encode(agentName, "UTF-8")
    val encodedApiKey = java.net.URLEncoder.encode(apiKey, "UTF-8")
    val url = "http://localhost:$port/interviewer/index.html?userName=$encodedUser&agentName=$encodedAgent&apiKey=$encodedApiKey"

    // 3. Browser state
    var browserComponent by remember { mutableStateOf<CefBrowser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPageReady by remember { mutableStateOf(false) }

    // Create browser when KCEF is ready
    LaunchedEffect(Unit) {
        try {
            // KCEF should already be initialized in main.kt
            kotlinx.coroutines.delay(100)

            val client = KCEF.newClientOrNull()
            if (client != null) {
                // Create message router for JavaScript-to-Kotlin communication
                val messageRouter = CefMessageRouter.create()
                messageRouter.addHandler(object : CefMessageRouterHandlerAdapter() {
                    override fun onQuery(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        queryId: Long,
                        request: String?,
                        persistent: Boolean,
                        callback: CefQueryCallback?
                    ): Boolean {
                        println("📨 JS Message: $request")
                        when {
                            request == "interviewComplete" -> {
                                println("✅ Interview Complete callback received!")
                                onInterviewComplete()
                                callback?.success("ok")
                                return true
                            }
                            request?.startsWith("profileUpdate:") == true -> {
                                val parts = request.removePrefix("profileUpdate:").split("=", limit = 2)
                                if (parts.size == 2) {
                                    onProfileUpdate(parts[0], parts[1])
                                    callback?.success("ok")
                                    return true
                                }
                            }
                            request?.startsWith("agentState:") == true -> {
                                val parts = request.removePrefix("agentState:").split(",")
                                if (parts.size == 2) {
                                    onAgentStateChange(parts[0].toBoolean(), parts[1].toBoolean())
                                    callback?.success("ok")
                                    return true
                                }
                            }
                        }
                        return false
                    }
}, true)
                client.addMessageRouter(messageRouter)

                // Add permission handler to auto-grant media permissions (microphone, camera)
                client.addPermissionHandler(object : CefPermissionHandler {
                    override fun onRequestMediaAccessPermission(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        requestingOrigin: String?,
                        requestedPermissions: Int,
                        callback: CefMediaAccessCallback?
                    ): Boolean {
                        println("🎤 Media Permission Request from: $requestingOrigin, permissions: $requestedPermissions")
                        // Grant all requested media permissions (microphone, camera, etc.)
                        callback?.Continue(requestedPermissions)
                        println("✅ Media permissions granted")
                        return true
                    }
                })

                // Add display handler to capture JavaScript console logs
                client.addDisplayHandler(object : CefDisplayHandlerAdapter() {
                    override fun onConsoleMessage(
                        browser: CefBrowser?,
                        level: CefSettings.LogSeverity?,
                        message: String?,
                        source: String?,
                        line: Int
                    ): Boolean {
                        val levelStr = when (level) {
                            CefSettings.LogSeverity.LOGSEVERITY_ERROR -> "❌ ERROR"
                            CefSettings.LogSeverity.LOGSEVERITY_WARNING -> "⚠️ WARN"
                            else -> "📝 LOG"
                        }
                        println("🌐 JS $levelStr: $message")
                        return false // Let CEF also handle the message
                    }
                })

                // Add load handler to inject API key when page loads
                client.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                        if (frame?.isMain == true) {
                            println("✅ Page loaded with status: $httpStatusCode")

                            // Inject API key immediately when page finishes loading
                            browser?.executeJavaScript(
                                """
                                (function() {
                                    // 1. Inject API Key
                                    window.ANDROID_API_KEY = '$apiKey';
                                    console.log('Kaironex Desktop: API Key injected successfully');
                                    console.log('API Key available:', !!window.ANDROID_API_KEY);
                                    
                                    // Also set process.env fallback for React apps
                                    if (!window.process) window.process = { env: {} };
                                    window.process.env.API_KEY = '$apiKey';
                                    window.process.env.GEMINI_API_KEY = '$apiKey';
                                    
                                    // 2. Create Android-compatible interface for Desktop
                                    // This mimics the Android WebView JavaScript interface
                                    // Uses CEF's cefQuery for Kotlin callbacks
                                    window.Android = {
                                        onAgentState: function(isTalking, isConnected) {
                                            console.log('Desktop: Agent State - Talking:', isTalking, 'Connected:', isConnected);
                                            if (window.cefQuery) {
                                                window.cefQuery({ request: 'agentState:' + isTalking + ',' + isConnected });
                                            }
                                        },
                                        onProfileUpdate: function(field, value) {
                                            console.log('Desktop: Profile Update -', field, ':', value);
                                            // Store profile updates in localStorage for persistence
                                            try {
                                                var profile = JSON.parse(localStorage.getItem('kaironex_profile') || '{}');
                                                profile[field] = value;
                                                localStorage.setItem('kaironex_profile', JSON.stringify(profile));
                                            } catch(e) { console.error('Failed to save profile:', e); }
                                            // Callback to Kotlin
                                            if (window.cefQuery) {
                                                window.cefQuery({ request: 'profileUpdate:' + field + '=' + value });
                                            }
                                        },
                                        onComplete: function() {
                                            console.log('Desktop: Interview Complete!');
                                            // Callback to Kotlin
                                            if (window.cefQuery) {
                                                window.cefQuery({ request: 'interviewComplete' });
                                            }
                                        }
                                    };
                                    console.log('Kaironex Desktop: Android interface mock created');
                                    
                                    // 3. Dispatch event to notify React app that API key is ready
                                    window.dispatchEvent(new CustomEvent('apiKeyReady', { detail: { apiKey: '$apiKey' } }));
                                })();
                                """.trimIndent(),
                                frame.url,
                                0
                            )
                            isPageReady = true
                        }
                    }

                    override fun onLoadError(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        errorCode: org.cef.handler.CefLoadHandler.ErrorCode?,
                        errorText: String?,
                        failedUrl: String?
                    ) {
                        if (frame?.isMain == true) {
                            println("❌ Page load error: $errorCode - $errorText for $failedUrl")
                        }
                    }
                })

                val browser = client.createBrowser(url, CefRendering.DEFAULT)
                browserComponent = browser
                isLoading = false
                println("✅ KCEF Browser created for: $url")
            } else {
                errorMessage = "Failed to create KCEF client"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "WebView Error: ${e.message}"
            isLoading = false
            e.printStackTrace()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            browserComponent?.close(true)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading WebView...",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
            errorMessage != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ $errorMessage",
                        color = Color.Red
                    )
                    Text(
                        text = "WebView could not be initialized.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            browserComponent != null -> {
                // Swing Panel to host the CEF browser
                SwingPanel(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        JPanel(BorderLayout()).apply {
                            add(browserComponent!!.uiComponent, BorderLayout.CENTER)
                        }
                    }
                )
            }
        }
    }
}
