package solutions.xapient.pulsr.domain

import platform.Foundation.NSLock

actual class Lock actual constructor() {
    private val lock = NSLock()

    actual fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
