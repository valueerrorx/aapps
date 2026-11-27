package solutions.xapient.hudr

import solutions.xapient.hudr.domain.Fix

// GPS-Quelle hinter einer Schnittstelle, damit die App ohne Geraet testbar ist; Rueckrufe auf dem Hauptthread
interface GpsSource {
    var onFix: ((Fix) -> Unit)?
    var onSatellites: ((used: Int, visible: Int) -> Unit)?
    var onEnabled: ((Boolean) -> Unit)?

    // Standort im System eingeschaltet
    fun enabled(): Boolean

    // false: kein GPS oder kein Recht
    fun start(): Boolean
    fun stop()
}
