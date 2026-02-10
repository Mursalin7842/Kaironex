
package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import android.widget.FrameLayout

import android.webkit.JavascriptInterface

@Composable
actual fun SimulacrumWebView(
    url: String, 
    modifier: Modifier,
    onClose: () -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                
                // Allow local file loading (Fixes White Screen / CORS)
                settings.allowFileAccess = true
                
                val assetLoader = androidx.webkit.WebViewAssetLoader.Builder()
                    .addPathHandler("/assets/", androidx.webkit.WebViewAssetLoader.AssetsPathHandler(context))
                    .build()
                
                // Bridge React -> Android
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onComplete() {
                        onClose()
                    }

                    @JavascriptInterface
                    fun onAgentState(isTalking: Boolean, isConnected: Boolean) {
                        // Optional: Update Android UI state if needed
                    }
                    
                    @JavascriptInterface
                    fun onProfileUpdate(field: String, value: String) {
                        // Optional: Sync back to repo immediately
                    }
                }, "Android")
                
                // Allow Camera/Mic
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
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
                
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) {
               webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        },
        modifier = modifier
    )
}
