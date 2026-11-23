package solutions.xapient.glidr

import android.net.Uri
import android.view.WindowManager
import android.webkit.WebResourceRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import solutions.xapient.glidr.domain.BackBridge
import solutions.xapient.glidr.domain.UrlPolicy

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private fun start(): ActivityController<MainActivity> = Robolectric.buildActivity(MainActivity::class.java).setup()

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
        assertTrue(index.data.readBytes().decodeToString().contains("glidr.js"))

        val ogg = client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/assets/assets/sounds/bumper.ogg"))
        assertEquals(200, ogg.statusCode)
        assertTrue(ogg.data.readBytes().size > 1000)

        // Level-Daten laedt Phaser per XHR ueber denselben Host
        val lvl = client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/assets/assets/levels/47.json"))
        assertEquals(200, lvl.statusCode)
        assertTrue(lvl.data.readBytes().decodeToString().contains("\"layers\""))
        val levels = client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/assets/assets/levels/levels.js"))
        assertTrue(levels.data.readBytes().decodeToString().contains("var levels"))

        // fehlendes Asset: AssetLoader liefert Antwort ohne Daten, WebView meldet 404
        assertNull(client.shouldInterceptRequest(null, request("https://appassets.androidplatform.net/assets/nope.js")).data)
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
