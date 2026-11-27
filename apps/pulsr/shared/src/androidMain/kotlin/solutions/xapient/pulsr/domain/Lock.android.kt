package solutions.xapient.pulsr.domain

actual class Lock actual constructor() {
    actual fun <T> locked(block: () -> T): T = synchronized(this) { block() }
}
