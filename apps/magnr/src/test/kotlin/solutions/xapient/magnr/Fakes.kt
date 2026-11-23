package solutions.xapient.magnr

import solutions.xapient.magnr.domain.Beeper
import solutions.xapient.magnr.domain.FieldListener
import solutions.xapient.magnr.domain.FieldSource

// Magnetometer ohne Hardware; emit liefert Werte wie der Sensor (Main-Thread)
class FakeSource(override val isAvailable: Boolean = true) : FieldSource {
    override val maxRange = 2000f
    private var listener: FieldListener? = null
    var starts = 0
    var t = 1_000_000_000L
    val running: Boolean get() = listener != null

    override fun start(listener: FieldListener) {
        this.listener = listener
        starts++
    }

    override fun stop() {
        listener = null
    }

    fun emit(z: Float, samples: Int = 1) = repeat(samples) {
        listener?.onField(0f, 0f, z, 3, t)
        t += 20_000_000L
    }
}

class FakeBeeper : Beeper {
    override var strength = 0f
    var running = false
    var starts = 0

    override fun start() {
        running = true
        starts++
    }

    override fun stop() {
        running = false
    }
}
