package com.mursaline.kaironex.features.agents

import android.annotation.SuppressLint
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mursaline.kaironex.brain.AgentType
import com.mursaline.kaironex.features.campaign.CampaignState
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * 📞 AGENT CALL WEBVIEW (Android)
 * ================================
 * Loads the dedicated Caller Agent React app for agent-initiated calls.
 *
 * The Caller Agent is DIFFERENT from the WebOrb:
 * - Agent speaks FIRST (proactive outreach)
 * - Injects call reason and context from the brain
 * - Negotiates with user and reports back
 * - Full Gemini Live voice integration
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun AgentCallWebView(
    modifier: Modifier,
    apiKey: String,
    userId: String,
    agentType: AgentType,
    agentName: String,
    callReason: String,
    campaignState: CampaignState,
    onCallEnded: () -> Unit,
    onAgentStateChange: (isTalking: Boolean, isConnected: Boolean) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                }

                setBackgroundColor(0xFF0D1117.toInt()) // Solid dark background to prevent white flash

                val assetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context))
                    .build()

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        // Grant microphone permission for voice calls
                        request.grant(request.resources)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: android.webkit.WebResourceRequest
                    ): android.webkit.WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        // Inject config into the Caller Agent React app
                        val escapedReason = callReason.replace("'", "\\'").replace("\n", " ")

                        // Serialize campaign state to JSON
                        val campaignStateJson = try {
                            Json.encodeToString(campaignState)
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", "\\n")
                                .replace("\r", "")
                        } catch (e: Exception) {
                            println("❌ Failed to serialize campaign state: ${e.message}")
                            "{}"
                        }

                        val initScript = """
                            window.ANDROID_API_KEY = '$apiKey';
                            window.AGENT_CALL_CONFIG = {
                                agentType: '${agentType.name}',
                                agentName: '$agentName',
                                callReason: '$escapedReason',
                                callContext: 'User ID: $userId',
                                userProfile: '$campaignStateJson'
                            };
                            console.log('📞 Agent Call Config injected:', window.AGENT_CALL_CONFIG);
                        """.trimIndent()
                        evaluateJavascript(initScript, null)
                    }
                }

                // JavaScript interface for callbacks from React app
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onAgentState(isTalking: Boolean, isConnected: Boolean) {
                        onAgentStateChange(isTalking, isConnected)
                    }

                    @android.webkit.JavascriptInterface
                    fun onCallEnded() {
                        onCallEnded()
                    }

                    @android.webkit.JavascriptInterface
                    fun log(message: String) {
                        println("📞 AgentCallWebView: $message")
                    }
                }, "Android")

                // Load the Caller Agent web app
                val encodedAgent = android.net.Uri.encode(agentName)
                val encodedReason = android.net.Uri.encode(callReason)
                loadUrl("https://appassets.androidplatform.net/assets/call_agent/index.html?agentType=${agentType.name}&agentName=$encodedAgent&callReason=$encodedReason")
            }
        },
        onRelease = { webView ->
            // CRITICAL: Clean up WebView when navigating away
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
    )
}






