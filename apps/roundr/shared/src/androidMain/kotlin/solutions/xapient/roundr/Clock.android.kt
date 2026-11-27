package solutions.xapient.roundr

import android.os.SystemClock

actual fun runClock(): Long = SystemClock.elapsedRealtime()
