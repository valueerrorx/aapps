package solutions.xapient.aapps.core.billing

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/** Schluessel-App "xapient bundle": installiert aus Google Play = alle Apps frei, ohne Cache */
object BundleKey {
    const val PACKAGE = "solutions.xapient.bundle"
    private const val PLAY = "com.android.vending"

    // TODO: SHA-256 des App-Signaturschluessels eintragen (Play Console > App integrity > App signing key, nach erstem Upload); leer = Bundle zaehlt nie
    const val BUNDLE_CERT_SHA256 = ""

    /** Bei jedem Start live pruefen: Paket da, aus Play installiert, mit dem Play-Schluessel signiert */
    fun installed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val pm = context.packageManager
        return check(Build.VERSION.SDK_INT, BUNDLE_CERT_SHA256, { installer(pm) }) { cert ->
            pm.hasSigningCertificate(PACKAGE, cert, PackageManager.CERT_INPUT_SHA256)
        }
    }

    /** Reine Entscheidung, im Test ohne PackageManager pruefbar */
    internal fun check(sdk: Int, certHex: String, installer: () -> String?, signed: (ByteArray) -> Boolean): Boolean {
        if (sdk < Build.VERSION_CODES.P) return false
        val cert = parseHex(certHex) ?: return false
        return try {
            installer() == PLAY && signed(cert)
        } catch (_: Exception) {
            // nicht installiert oder PackageManager-Fehler
            false
        }
    }

    /** "AB:CD:…" wie in der Play Console oder ohne Doppelpunkte; muss 32 Byte ergeben */
    internal fun parseHex(hex: String): ByteArray? {
        val s = hex.replace(":", "").trim()
        if (s.length != 64 || s.any { Character.digit(it, 16) < 0 }) return null
        return ByteArray(32) { i -> s.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    private fun installer(pm: PackageManager): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(PACKAGE).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(PACKAGE)
        }

    /** Store-Seite des Bundles; ohne Play Store still nichts tun */
    fun openStore(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            // kein Play Store
        }
    }
}
