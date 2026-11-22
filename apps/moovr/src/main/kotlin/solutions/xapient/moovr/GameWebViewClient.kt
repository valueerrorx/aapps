package solutions.xapient.moovr

import android.net.Uri
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import solutions.xapient.moovr.domain.UrlPolicy
import java.io.ByteArrayInputStream

// Liefert nur eigene Assets aus, alles andere wird lokal abgewiesen
class GameWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val openExternal: (Uri) -> Unit,
    private val onPageReady: () -> Unit,
    private val onRendererGone: () -> Unit,
) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse =
        intercept(request.url)

    fun intercept(url: Uri): WebResourceResponse {
        if (!UrlPolicy.isOwnAsset(url.toString())) return status(403, "Forbidden")
        return assetLoader.shouldInterceptRequest(url) ?: status(404, "Not Found")
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        return when (UrlPolicy.navigation(request.url.toString(), request.hasGesture())) {
            UrlPolicy.Navigation.LOAD -> false
            UrlPolicy.Navigation.OPEN_EXTERNAL -> { openExternal(request.url); true }
            UrlPolicy.Navigation.BLOCK -> true
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (url != null && UrlPolicy.isOwnAsset(url)) onPageReady()
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        onRendererGone()
        return true
    }

    private fun status(code: Int, reason: String) =
        WebResourceResponse("text/plain", "utf-8", code, reason, emptyMap(), ByteArrayInputStream(ByteArray(0)))
}
