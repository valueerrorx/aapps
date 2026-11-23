package solutions.xapient.glidr

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import solutions.xapient.glidr.domain.BackBridge
import solutions.xapient.glidr.domain.UrlPolicy

// Huelle fuer das Phaser-Spiel: vollflaechige WebView, immersiv, Querformat
class MainActivity : ComponentActivity() {

    internal lateinit var webView: WebView
        private set
    internal var pageReady = false
        private set
    private var rendererGone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        webView = createWebView()
        root.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(root)

        // Spiel nicht unter die Kamera-Aussparung legen, Knoepfe liegen am Rand
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val cut = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(cut.left, cut.top, cut.right, cut.bottom)
            WindowInsetsCompat.CONSUMED
        }
        hideSystemBars()
        holdSplashUntilReady(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })
        webView.loadUrl(UrlPolicy.START_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val assets = WebViewAssetLoader.Builder()
            .setDomain(UrlPolicy.HOST)
            .addPathHandler(UrlPolicy.ASSET_PATH, WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        return WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isHapticFeedbackEnabled = false
            isLongClickable = false
            setOnLongClickListener { true }
            settings.apply {
                // JS nur fuer die eigenen Assets, alles andere blockt der Client
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                setGeolocationEnabled(false)
                textZoom = 100
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            webViewClient = GameWebViewClient(
                assetLoader = assets,
                openExternal = ::openExternal,
                onPageReady = { pageReady = true; window.decorView.invalidate() },
                onRendererGone = ::onRendererGone,
            )
        }
    }

    // Splash bleibt, bis die Seite geladen ist (hoechstens 2 s)
    private fun holdSplashUntilReady(root: View) {
        val start = SystemClock.uptimeMillis()
        root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!pageReady && SystemClock.uptimeMillis() - start < SPLASH_MAX_MS) return false
                root.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    internal fun onBack() {
        if (!pageReady || rendererGone) {
            finish()
            return
        }
        webView.evaluateJavascript(BackBridge.BACK_SCRIPT) { handleBackResult(it) }
    }

    internal fun handleBackResult(result: String?) {
        if (BackBridge.shouldExit(result) && !isFinishing) finish()
    }

    override fun onResume() {
        super.onResume()
        if (rendererGone) return
        webView.onResume()
        webView.evaluateJavascript(BackBridge.RESUME_SCRIPT, null)
    }

    override fun onPause() {
        // erst Spiel und Audio anhalten, dann die WebView
        if (!rendererGone) {
            webView.evaluateJavascript(BackBridge.PAUSE_SCRIPT, null)
            webView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            // kein Browser: ignorieren
        }
    }

    // Renderer vom System beendet: neu aufbauen statt abzustuerzen
    private fun onRendererGone() {
        rendererGone = true
        recreate()
    }

    private companion object {
        const val SPLASH_MAX_MS = 2000L
    }
}
