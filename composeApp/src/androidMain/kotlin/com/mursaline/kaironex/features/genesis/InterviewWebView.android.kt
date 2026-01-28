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
    onInterviewComplete: () -> Unit
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
                        // Inject API Key into window object
                        // React code checks: (window as any).ANDROID_API_KEY
                        evaluateJavascript(
                            "window.ANDROID_API_KEY = '$apiKey';",
                            null
                        )
                    }
                }

                // Load the local asset
                // The React app was built with base: './' so asset loading works relative to this index.html
                loadUrl("file:///android_asset/interviewer/index.html")
            }
        }
    )
}
