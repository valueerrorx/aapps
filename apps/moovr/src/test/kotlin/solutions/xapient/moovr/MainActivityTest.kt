package solutions.xapient.moovr

import android.net.Uri
import android.os.SystemClock
import android.view.WindowManager
import android.webkit.WebResourceRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import solutions.xapient.moovr.domain.BackBridge
import solutions.xapient.moovr.domain.UrlPolicy

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private fun start(): ActivityController<MainActivity> = Robolectric.buildActivity(MainActivity::class.java).setup()

    @Before
    fun resetRendererWindow() {
        MainActivity.lastRendererRestart = null
    }

    @Test
    fun rendererCrashRestartsOnceThenFinishes() {
        val first = start()
        var restarts = 0
        first.get().restart = { restarts++ }
        val client = first.get().webView.webViewClient as GameWebViewClient
        assertTrue(client.onRenderProcessGone(first.get().webView, null))
        assertEquals(1, restarts)
        assertFalse(first.get().isFinishing)

        // zerstoerte WebView wird bei Pause/Resume nicht mehr angefasst
        val shadow = shadowOf(first.get().webView)
        val evaluated = shadow.lastEvaluatedJavascript
        first.pause()
        assertFalse(shadow.wasOnPauseCalled())
        assertEquals(evaluated, shadow.lastEvaluatedJavascript)
        first.resume()
        assertEquals(evaluated, shadow.lastEvaluatedJavascript)

        // zweiter Absturz im 30-s-Fenster: beenden statt Schleife
        val second = start()
        second.get().restart = { restarts++ }
        (second.get().webView.webViewClient as GameWebViewClient).onRenderProcessGone(second.get().webView, null)
        assertEquals(1, restarts)
        assertTrue(second.get().isFinishing)
    }

    @Test
    fun rendererCrashAfterWindowRestartsAgain() {
        MainActivity.lastRendererRestart = SystemClock.elapsedRealtime() - MainActivity.RENDERER_RESTART_WINDOW_MS
        val controller = start()
        var restarts = 0
        controller.get().restart = { restarts++ }
        (controller.get().webView.webViewClient as GameWebViewClient).onRenderProcessGone(controller.get().webView, null)
        assertEquals(1, restarts)
        assertFalse(controller.get().isFinishing)
    }

    private fun request(url: String, mainFrame: Boolean = true, gesture: Boolean = false) = object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = mainFrame
        override fun isRedirect() = false
        override fun hasGesture() = gesture
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): Map<String, String> = emptyMap()
    }

    @Test
    fun loadsGameFromAssetHostWithHardenedSettings() {
        val activity = start().get()
        val web = activity.webView
        assertEquals(UrlPolicy.START_URL, shadowOf(web).lastLoadedUrl)
        with(web.settings) {
            assertTrue(javaScriptEnabled)
            assertTrue(domStorageEnabled)
            assertFalse(allowFileAccess)
            assertFalse(allowContentAccess)
            assertFalse(mediaPlaybackRequiresUserGesture)
            assertFalse(javaScriptCanOpenWindowsAutomatically)
        }
        val flags = activity.window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
    }

    @Test
    fun clientServesOwnAssetsAndRejectsEverythingElse() {
        val client = start().get().webView.webViewClient as GameWebViewClient

        val index = client.shouldInterceptRequest(null, request(UrlPolicy.START_URL))
        assertEquals(200, index.statusCode)
        assertEquals("text/html", index.mimeType)
        assertTrue(index.data.readBytes().decodeToString().contains("moovr.js"))

        val ogg = client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/assets/assets/sounds/bumper.ogg"))
        assertEquals(200, ogg.statusCode)
        assertTrue(ogg.data.readBytes().size > 1000)

        // fehlendes Asset: echte 404 statt leerer AssetLoader-Antwort
        assertEquals(404, client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/assets/nope.js")).statusCode)
        assertEquals(403, client.shouldInterceptRequest(null, request("https://example.com/x.js")).statusCode)
        assertEquals(403, client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/res/x")).statusCode)
    }

    @Test
    fun foreignNavigationIsBlocked() {
        val activity = start().get()
        val client = activity.webView.webViewClient as GameWebViewClient
        assertFalse(client.shouldOverrideUrlLoading(null, request(UrlPolicy.START_URL)))
        assertTrue(client.shouldOverrideUrlLoading(null, request("https://example.com/")))
        assertTrue(client.shouldOverrideUrlLoading(null, request("file:///sdcard/x.html", gesture = true)))
        assertNull(shadowOf(activity).peekNextStartedActivity())

        // Link nach Tipp oeffnet den Browser
        assertTrue(client.shouldOverrideUrlLoading(null, request("https://xapient.solutions/", gesture = true)))
        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Uri.parse("https://xapient.solutions/"), intent.data)
    }

    @Test
    fun backAsksTheGameAndExitsOnlyWhenItSaysSo() {
        val controller = start()
        val activity = controller.get()
        val client = activity.webView.webViewClient as GameWebViewClient
        client.onPageFinished(activity.webView, UrlPolicy.START_URL)
        assertTrue(activity.pageReady)

        activity.onBackPressedDispatcher.onBackPressed()
        val shadow = shadowOf(activity.webView)
        assertEquals(BackBridge.BACK_SCRIPT, shadow.lastEvaluatedJavascript)
        shadow.lastEvaluatedJavascriptCallback.onReceiveValue("\"handled\"")
        assertFalse(activity.isFinishing)

        activity.onBackPressedDispatcher.onBackPressed()
        shadow.lastEvaluatedJavascriptCallback.onReceiveValue("\"exit\"")
        assertTrue(activity.isFinishing)
    }

    @Test
    fun backBeforePageLoadExits() {
        val activity = start().get()
        activity.onBackPressedDispatcher.onBackPressed()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun pauseAndResumeReachWebViewAndGame() {
        val controller = start()
        val shadow = shadowOf(controller.get().webView)

        controller.pause()
        assertTrue(shadow.wasOnPauseCalled())
        assertEquals(BackBridge.PAUSE_SCRIPT, shadow.lastEvaluatedJavascript)

        controller.resume()
        assertTrue(shadow.wasOnResumeCalled())
        assertEquals(BackBridge.RESUME_SCRIPT, shadow.lastEvaluatedJavascript)

        controller.pause().stop().destroy()
        assertTrue(shadow.wasDestroyCalled())
    }
}
