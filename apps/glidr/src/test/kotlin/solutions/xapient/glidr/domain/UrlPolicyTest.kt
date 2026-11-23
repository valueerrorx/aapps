package solutions.xapient.glidr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.glidr.domain.UrlPolicy.Navigation

class UrlPolicyTest {

    @Test
    fun startUrlIsOwnAsset() {
        assertTrue(UrlPolicy.isOwnAsset(UrlPolicy.START_URL))
        assertTrue(UrlPolicy.isOwnAsset("https://appassets.androidplatform.net/assets/assets/sounds/splash.ogg"))
        assertTrue(UrlPolicy.isOwnAsset("https://APPASSETS.androidplatform.net:443/assets/game.js?v=1"))
    }

    @Test
    fun foreignOrTrickyUrlsAreNotOwnAssets() {
        listOf(
            "http://appassets.androidplatform.net/assets/index.html",
            "https://appassets.androidplatform.net/res/drawable/splash.xml",
            "https://appassets.androidplatform.net/assets/../res/x",
            "https://appassets.androidplatform.net/assets/%2E%2E/x",
            "https://appassets.androidplatform.net:8443/assets/index.html",
            "https://evil@appassets.androidplatform.net/assets/index.html",
            "https://appassets.androidplatform.net.evil.com/assets/index.html",
            "https://example.com/assets/index.html",
            "file:///android_asset/index.html",
            "content://solutions.xapient.glidr/x",
            "javascript:alert(1)",
            "data:text/html,hi",
            "not a url",
            "",
        ).forEach { assertFalse(it, UrlPolicy.isOwnAsset(it)) }
    }

    @Test
    fun navigationOnlyLoadsOwnAssets() {
        assertEquals(Navigation.LOAD, UrlPolicy.navigation(UrlPolicy.START_URL, userGesture = false))
        assertEquals(Navigation.BLOCK, UrlPolicy.navigation("https://example.com/", userGesture = false))
        assertEquals(Navigation.BLOCK, UrlPolicy.navigation("file:///sdcard/x.html", userGesture = true))
        assertEquals(Navigation.BLOCK, UrlPolicy.navigation("intent://x#Intent;end", userGesture = true))
    }

    @Test
    fun webLinksOpenExternallyOnlyAfterTap() {
        assertEquals(Navigation.OPEN_EXTERNAL, UrlPolicy.navigation("https://xapient.solutions/", userGesture = true))
        assertEquals(Navigation.OPEN_EXTERNAL, UrlPolicy.navigation("http://phaser.io", userGesture = true))
    }
}
