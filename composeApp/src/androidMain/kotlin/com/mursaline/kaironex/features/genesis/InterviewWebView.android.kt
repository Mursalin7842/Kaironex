package com.mursaline.kaironex.features.genesis

import android.annotation.SuppressLint
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
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
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val triggerScript = """
                            window.ANDROID_API_KEY = '$apiKey';
                            // New React app doesn't use window.startInterview global anymore, it auto-starts via React lifecycle + Orbit click
                        """.trimIndent()
                        evaluateJavascript(triggerScript, null)
                    }
                }

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onAgentState(isTalking: Boolean, isConnected: Boolean) {
                        onAgentStateChange(isTalking, isConnected)
                    }

                    @android.webkit.JavascriptInterface
                    fun onProfileUpdate(field: String, value: String) {
                        onProfileUpdate(field, value)
                    }

                    @android.webkit.JavascriptInterface
                    fun onComplete() {
                        onInterviewComplete()
                    }
                }, "Android")

                // Load with URL Params
                val encodedUser = android.net.Uri.encode(userName)
                val encodedAgent = android.net.Uri.encode(agentName)
                loadUrl("file:///android_asset/interviewer/index.html?userName=${encodedUser}&agentName=${encodedAgent}")
            }
        },
        onRelease = { webView ->
            // CRITICAL: Stop the React App / Gemini Session when navigating away
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
    )
}
