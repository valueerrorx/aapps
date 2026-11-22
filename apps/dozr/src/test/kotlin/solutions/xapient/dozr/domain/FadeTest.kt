package solutions.xapient.dozr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Lautstaerke wie am Geraet: Stufen 0..max, optional mit Untergrenze
private class FakeVolume(var value: Int, private val floor: Int = 0) : Volume {
    val writes = mutableListOf<Int>()
    override val current: Int get() = value
    override fun set(index: Int) {
        writes += index
        value = index.coerceAtLeast(floor)
    }
}

class FadeCurveTest {
    @Test fun endpoints() {
        assertEquals(1.0, FadeCurve.level(0.0), 1e-9)
        assertEquals(0.5, FadeCurve.level(0.5), 1e-9)
        assertEquals(0.0, FadeCurve.level(1.0), 1e-9)
        assertEquals(1.0, FadeCurve.level(-1.0), 1e-9)
        assertEquals(0.0, FadeCurve.level(2.0), 1e-9)
    }

    @Test fun monotonicAndSoftAtBothEnds() {
        var prev = 2.0
        for (i in 0..100) {
            val l = FadeCurve.level(i / 100.0)
            assertTrue(l <= prev)
            prev = l
        }
        // flach am Anfang und am Ende: erstes und letztes Prozent aendern kaum
        assertTrue(1.0 - FadeCurve.level(0.01) < 0.001)
        assertTrue(FadeCurve.level(0.99) < 0.001)
    }

    @Test fun indexSteps() {
        assertEquals(15, FadeCurve.index(15, 0.0))
        assertEquals(0, FadeCurve.index(15, 1.0))
        assertEquals(8, FadeCurve.index(15, 0.5))
        assertEquals(0, FadeCurve.index(0, 0.3))
    }
}

class FaderTest {
    // Ganzer Lauf: runter auf 0, danach exakt die Ausgangsstufe
    @Test fun fadesDownAndRestores() {
        val v = FakeVolume(12)
        val f = Fader(v)
        for (i in 0..20) f.step(i / 20.0)
        assertEquals(0, v.value)
        // nur abwaerts, keine Spruenge nach oben
        assertEquals(v.writes.sortedDescending(), v.writes)
        f.restore()
        assertEquals(12, v.value)
        assertNull(f.memo)
    }

    @Test fun startRemembersOriginal() {
        val v = FakeVolume(9)
        val f = Fader(v)
        assertTrue(f.step(0.0))
        assertEquals(FadeMemo(9, 9), f.memo)
        assertTrue(v.writes.isEmpty())
    }

    // Nutzer dreht waehrend des Ausblendens: ab dann nichts mehr anfassen, nichts zurueckstellen
    @Test fun userChangeDuringFadeWins() {
        val v = FakeVolume(10)
        val f = Fader(v)
        f.step(0.0)
        f.step(0.5)
        assertEquals(5, v.value)
        v.value = 7
        f.step(0.6)
        assertTrue(f.memo!!.touched)
        f.step(1.0)
        assertEquals(7, v.value)
        f.restore()
        assertEquals(7, v.value)
    }

    // Nutzer dreht leiser: auch das gilt
    @Test fun userTurnsDownIsRespected() {
        val v = FakeVolume(10)
        val f = Fader(v)
        f.step(0.0)
        f.step(0.3)
        v.value = 1
        f.step(1.0)
        f.restore()
        assertEquals(1, v.value)
    }

    // Nach dem Ausblenden selbst gedreht (z. B. Morgen danach, Prozess lebt noch): nicht ueberschreiben
    @Test fun changeAfterFadeIsNotOverwritten() {
        val v = FakeVolume(10)
        val f = Fader(v)
        f.step(0.0)
        f.step(1.0)
        v.value = 4
        f.restore()
        assertEquals(4, v.value)
    }

    // Verlaengern mitten im Ausblenden: sofort zurueck, beim naechsten Ausblenden neue Ausgangsstufe
    @Test fun extendDuringFadeRestoresAndRestarts() {
        val v = FakeVolume(10)
        val f = Fader(v)
        f.step(0.0)
        f.step(0.7)
        assertTrue(v.value < 10)
        assertTrue(f.restore())
        assertEquals(10, v.value)
        f.step(0.0)
        assertEquals(10, f.memo!!.orig)
    }

    // Prozess-Tod mitten im Ausblenden: neuer Fader mit gesichertem Memo macht weiter und stellt richtig zurueck
    @Test fun survivesProcessDeath() {
        val v = FakeVolume(14)
        val a = Fader(v)
        a.step(0.0)
        a.step(0.5)
        val saved = a.memo!!
        assertEquals(14, saved.orig)
        val b = Fader(v, saved)
        b.step(0.8)
        assertTrue(v.value < 7)
        b.step(1.0)
        b.restore()
        assertEquals(14, v.value)
    }

    // Prozess-Tod, danach gedreht: gesichertes Memo erkennt das
    @Test fun processDeathThenUserChangeIsRespected() {
        val v = FakeVolume(14)
        val a = Fader(v)
        a.step(0.0)
        a.step(0.5)
        v.value = 3
        val b = Fader(v, a.memo)
        b.restore()
        assertEquals(3, v.value)
    }

    // Geraet laesst nicht unter 1: letzte tatsaechliche Stufe zaehlt, kein falsches "Nutzer hat gedreht"
    @Test fun deviceFloorIsNotMistakenForUser() {
        val v = FakeVolume(6, floor = 1)
        val f = Fader(v)
        for (i in 0..10) f.step(i / 10.0)
        assertEquals(1, v.value)
        assertFalse(f.memo!!.touched)
        f.restore()
        assertEquals(6, v.value)
    }

    @Test fun silentStartNeedsNoWrites() {
        val v = FakeVolume(0)
        val f = Fader(v)
        f.step(0.0)
        f.step(1.0)
        f.restore()
        assertTrue(v.writes.isEmpty())
    }

    @Test fun restoreWithoutFadeDoesNothing() {
        val v = FakeVolume(5)
        assertFalse(Fader(v).restore())
        assertTrue(v.writes.isEmpty())
    }
}
