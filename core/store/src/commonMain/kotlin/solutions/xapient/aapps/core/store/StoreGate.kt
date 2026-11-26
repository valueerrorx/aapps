package solutions.xapient.aapps.core.store

/** Hinweis auf dem Angebots-/Dankes-Bildschirm nach einer Nutzeraktion */
enum class StoreNotice { OFFLINE, NOT_FOUND, UNAVAILABLE, FAILED }

data class StoreState(
    val access: StoreAccess,
    val price: String? = null,
    val trialAvailable: Boolean = false,
    val pending: Boolean = false,
    val busy: Boolean = false,
    val notice: StoreNotice? = null,
)

/** Testphase + Kaufstatus einer iOS-App; [start] bei jedem Start, Rueckrufe auf dem Main-Thread */
class StoreGate(
    private val cache: StoreCache,
    private val bridge: StoreBridge,
    private val clock: () -> Long,
) : StoreListener {
    var state: StoreState = StoreState(decide(null).access)
        private set
    var onChange: ((StoreState) -> Unit)? = null
    private var restoring = false
    private var started = false

    private fun decide(query: Entitlements?) =
        StorePolicy.decide(cache.unlocked, cache.trialStart, query, clock())

    private fun set(s: StoreState) {
        if (s == state) return
        state = s
        onChange?.invoke(s)
    }

    /** Einmal: Updates hoeren; danach wie [refresh] */
    fun start() {
        if (!started) {
            started = true
            bridge.start(this)
        }
        refresh()
    }

    /** Kaufstatus abfragen, bis dahin gilt der Cache; auch beim Zurueckkehren in die App (Ablauf) */
    fun refresh() {
        set(state.copy(access = decide(null).access))
        bridge.refresh()
    }

    fun startTrial() {
        if (!state.trialAvailable) return unavailable()
        set(state.copy(busy = true, notice = null))
        bridge.purchase(trial = true)
    }

    fun buy() {
        if (state.price == null) return unavailable()
        set(state.copy(busy = true, notice = null))
        bridge.purchase(trial = false)
    }

    /** "Kauf wiederherstellen" */
    fun restore() {
        restoring = true
        set(state.copy(busy = true, notice = null))
        bridge.restore()
        if (state.price == null) bridge.loadProducts()
    }

    /** Nur Debug-Builds: Testphase ablaufen lassen oder lokalen Stand loeschen */
    fun debug(cmd: String) {
        when (cmd) {
            "expire" -> cache.trialStart = clock() - StorePolicy.TRIAL_MS
            "reset" -> {
                cache.trialStart = null
                cache.unlocked = false
            }
            else -> return
        }
        set(state.copy(access = decide(null).access))
    }

    fun close() {
        onChange = null
        if (started) bridge.stop()
        started = false
    }

    private fun unavailable() {
        set(state.copy(notice = StoreNotice.UNAVAILABLE))
        bridge.loadProducts()
    }

    /** ms bis zum Ende der Testphase, null ausserhalb der Testphase (fuer einen Ablauf-Timer bei offener App) */
    fun trialRemainingMillis(): Long? {
        if (state.access != StoreAccess.TRIAL) return null
        val start = cache.trialStart ?: return null
        return (start + StorePolicy.TRIAL_MS - clock()).coerceAtLeast(0)
    }

    override fun onEntitlements(unlocked: Boolean, revoked: Boolean, trialStartMillis: Long) {
        val d = decide(Entitlements(unlocked, trialStartMillis.takeIf { it >= 0 }, revoked))
        // Cache nur bei erfolgreicher Abfrage aendern, damit offline gekauft bleibt
        cache.unlocked = d.unlocked
        cache.trialStart = d.trialStart
        val notice = if (restoring && !d.unlocked) StoreNotice.NOT_FOUND else null
        restoring = false
        // ausstehender Kauf (Ask to Buy): abgelehnt oder offen sieht gleich aus, Knopf wieder frei geben
        set(state.copy(access = d.access, pending = false, busy = false, notice = notice))
        if (d.access != StoreAccess.FULL && state.price == null) bridge.loadProducts()
    }

    override fun onQueryFailed() {
        val notice = if (restoring) StoreNotice.OFFLINE else state.notice
        restoring = false
        set(state.copy(access = decide(null).access, busy = false, notice = notice))
        if (state.access != StoreAccess.FULL && state.price == null) bridge.loadProducts()
    }

    override fun onProducts(unlockPrice: String?, trialAvailable: Boolean) {
        set(state.copy(price = unlockPrice ?: state.price, trialAvailable = trialAvailable || state.trialAvailable))
    }

    override fun onPurchasePending() = set(state.copy(pending = true, busy = false))

    override fun onPurchaseCancelled() = set(state.copy(busy = false))

    override fun onPurchaseFailed() = set(state.copy(busy = false, notice = StoreNotice.FAILED))

    override fun onRestoreFailed() {
        restoring = false
        set(state.copy(busy = false, notice = StoreNotice.OFFLINE))
    }
}
