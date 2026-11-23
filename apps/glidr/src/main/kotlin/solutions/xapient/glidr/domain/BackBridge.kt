package solutions.xapient.glidr.domain

// Zurueck-Taste: das Spiel entscheidet ueber glidrBack() in glidr.js
object BackBridge {
    const val BACK_SCRIPT = "window.glidrBack ? glidrBack() : 'exit'"
    const val PAUSE_SCRIPT = "window.glidrPause && glidrPause()"
    const val RESUME_SCRIPT = "window.glidrResume && glidrResume()"

    // evaluateJavascript liefert JSON, z. B. "\"handled\""; alles andere beendet, damit niemand festhaengt
    fun shouldExit(jsResult: String?): Boolean =
        jsResult?.trim()?.removeSurrounding("\"") != "handled"
}
