package solutions.xapient.aapps.core.ui

import androidx.compose.runtime.Composable

// tick/confirm melden Ereignisse, step/press sind Touch-Feedback (Regeln in docs/AGENTS.md)
expect class Haptics {
    fun tick()
    fun confirm()
    fun step()
    fun press()
}

@Composable
expect fun rememberHaptics(enabled: Boolean = true): Haptics

// Entprellung: hoechstens alle minGapMs, nur ein starkes Ereignis darf ein frisches schwaches ueberholen
internal class HapticGate(private val minGapMs: Long = MIN_GAP_MS) {
    private var lastAt = 0L
    private var last: Boolean? = null

    fun allow(strong: Boolean, nowMs: Long): Boolean {
        val free = last == null || nowMs - lastAt >= minGapMs
        if (!free && !(strong && last == false)) return false
        lastAt = nowMs
        last = strong
        return true
    }

    companion object {
        const val MIN_GAP_MS = 120L
    }
}

