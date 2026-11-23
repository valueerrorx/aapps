package solutions.xapient.moovr.domain

import java.net.URI

// Was die Spiel-WebView laden darf: nur die eigenen Assets
object UrlPolicy {
    const val HOST = "appassets.androidplatform.net"
    const val ASSET_PATH = "/assets/"
    const val START_URL = "https://$HOST${ASSET_PATH}index.html"

    enum class Navigation { LOAD, OPEN_EXTERNAL, BLOCK }

    fun isOwnAsset(url: String): Boolean {
        val uri = parse(url) ?: return false
        val path = uri.rawPath ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(HOST, ignoreCase = true) &&
            (uri.port == -1 || uri.port == 443) &&
            uri.rawUserInfo == null &&
            path.startsWith(ASSET_PATH) &&
            ".." !in path && "%2e" !in path.lowercase()
    }

    // Navigation im Hauptfenster; fremde Links nur nach Tipp im Browser
    fun navigation(url: String, userGesture: Boolean): Navigation {
        if (isOwnAsset(url)) return Navigation.LOAD
        val uri = parse(url) ?: return Navigation.BLOCK
        val web = uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrEmpty()
        return if (web && userGesture) Navigation.OPEN_EXTERNAL else Navigation.BLOCK
    }

    private fun parse(url: String): URI? = try {
        URI(url.trim())
    } catch (_: Exception) {
        null
    }
}
