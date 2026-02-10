package com.mursaline.kaironex.ui.components

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
actual fun WebOrb(
    modifier: Modifier,
    apiKey: String,
    onProfileUpdate: (String, String) -> Unit,
    onAgentState: (String) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(0x00000000) // Transparent background
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccess = true
                }

                val assetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context))
                    .build()

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
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
                }

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun getApiKey(): String = apiKey

                    @android.webkit.JavascriptInterface
                    fun onProfileUpdate(field: String, value: String) {
                        onProfileUpdate(field, value)
                    }

                    @android.webkit.JavascriptInterface
                    fun onAgentState(state: String) {
                        onAgentState(state)
                    }

                    @android.webkit.JavascriptInterface
                    fun onComplete() {
                        // Optional: Handle session end
                    }
                }, "Android")
                
                // Load the orb-web app from assets with API key in URL to be sure
                loadUrl("https://appassets.androidplatform.net/assets/orb_web/index.html?apiKey=$apiKey")
            }
        },
        onRelease = { webView ->
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
    )
}
