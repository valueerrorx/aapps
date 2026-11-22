package solutions.xapient.moovr.domain

// Zurueck-Taste: das Spiel entscheidet ueber moovrBack() in moovr.js
object BackBridge {
    const val BACK_SCRIPT = "window.moovrBack ? moovrBack() : 'exit'"
    const val PAUSE_SCRIPT = "window.moovrPause && moovrPause()"
    const val RESUME_SCRIPT = "window.moovrResume && moovrResume()"

    // evaluateJavascript liefert JSON, z. B. "\"handled\""; alles andere beendet, damit niemand festhaengt
    fun shouldExit(jsResult: String?): Boolean =
        jsResult?.trim()?.removeSurrounding("\"") != "handled"
}
