package solutions.xapient.bundle

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

/** Eine xapient-App: Ordnername unter apps/ = Paketende, Akzentfarbe, gratis oder mit Kauf */
data class XApp(
    val id: String,
    val accent: Color,
    @param:DrawableRes val icon: Int,
    @param:StringRes val line: Int,
    val free: Boolean = false,
) {
    val pkg get() = "solutions.xapient.$id"
}

// Alle Apps aus settings.gradle.kts (Test prueft das); Bezahl-Apps zuerst, Gratis-Spiele am Ende
val Catalog = listOf(
    XApp("sleepr", Color(0xFF86B7CC), R.drawable.app_sleepr, R.string.line_sleepr),
    XApp("levlr", Color(0xFFC6F24E), R.drawable.app_levlr, R.string.line_levlr),
    XApp("nordr", Color(0xFF8AD8FF), R.drawable.app_nordr, R.string.line_nordr),
    XApp("spinr", Color(0xFFB9A2FF), R.drawable.app_spinr, R.string.line_spinr),
    XApp("scorr", Color(0xFFFFD24A), R.drawable.app_scorr, R.string.line_scorr),
    XApp("focusr", Color(0xFFFF6E5A), R.drawable.app_focusr, R.string.line_focusr),
    XApp("roundr", Color(0xFFFF6B3D), R.drawable.app_roundr, R.string.line_roundr),
    XApp("pulsr", Color(0xFFFF4FA0), R.drawable.app_pulsr, R.string.line_pulsr),
    XApp("loudr", Color(0xFFFF8A3D), R.drawable.app_loudr, R.string.line_loudr),
    XApp("tunr", Color(0xFF4DF0A2), R.drawable.app_tunr, R.string.line_tunr),
    XApp("magnr", Color(0xFFC08EFF), R.drawable.app_magnr, R.string.line_magnr),
    XApp("dozr", Color(0xFFFF9EC7), R.drawable.app_dozr, R.string.line_dozr),
    XApp("hertzr", Color(0xFF5B8DFF), R.drawable.app_hertzr, R.string.line_hertzr),
    XApp("hudr", Color(0xFF2EE6E6), R.drawable.app_hudr, R.string.line_hudr),
    XApp("thinkr", Color(0xFFF27BE8), R.drawable.app_thinkr, R.string.line_thinkr),
    XApp("dividr", Color(0xFF6EE87A), R.drawable.app_dividr, R.string.line_dividr),
    XApp("moovr", Color(0xFF8FDC5C), R.drawable.app_moovr, R.string.line_moovr, free = true),
    XApp("glidr", Color(0xFFCDE6FF), R.drawable.app_glidr, R.string.line_glidr, free = true),
)

/** Installierte xapient-Apps; Sichtbarkeit per <queries> im Manifest */
fun installedApps(context: Context): Set<String> {
    val pm = context.packageManager
    return Catalog.filter { app ->
        try {
            pm.getPackageInfo(app.pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }.map { it.id }.toSet()
}

fun openApp(context: Context, app: XApp) {
    val intent = context.packageManager.getLaunchIntentForPackage(app.pkg) ?: return
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // gerade deinstalliert: nichts tun
    }
}

/** Store-Seite, ohne Play Store im Browser; ohne beides nichts tun */
fun openStore(context: Context, app: XApp) {
    for (uri in listOf("market://details?id=${app.pkg}", "https://play.google.com/store/apps/details?id=${app.pkg}")) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            return
        } catch (_: ActivityNotFoundException) {
            // naechste Variante
        }
    }
}
