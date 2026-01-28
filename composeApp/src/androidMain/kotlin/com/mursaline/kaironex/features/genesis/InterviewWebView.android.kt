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
                        // Grant all permissions (audio) specifically for the local agent
                        // In production, check request.resources includes AUDIO_CAPTURE
                        request.grant(request.resources)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val triggerScript = """
                            window.ANDROID_API_KEY = '$apiKey';
                            window.ANDROID_AUTO_START = true;
                            if (window.startInterview) {
                                window.startInterview();
                            } else {
                                // Retry after short delay in case React hasn't mounted
                                setTimeout(function() {
                                    if(window.startInterview) window.startInterview();
                                }, 1000);
                            }
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

                // Load the local asset
                // The React app was built with base: './' so asset loading works relative to this index.html
                loadUrl("file:///android_asset/interviewer/index.html")
            }
        }
    )
}
