package solutions.xapient.thinkr.ui

import android.content.Context
import android.provider.Settings

// Animationsskala 0 in den Entwickleroptionen/Bedienungshilfen = reduzierte Bewegung
fun Motion.read(context: Context) {
    reduced = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
