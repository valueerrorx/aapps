package solutions.xapient.aapps.core.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/** Hinweis auf dem Dankes-Bildschirm nach einer Nutzeraktion */
enum class Notice { OFFLINE, NOT_FOUND, UNAVAILABLE }

data class GateState(
    val access: Access,
    val price: String? = null,
    val pending: Boolean = false,
    val busy: Boolean = false,
    val notice: Notice? = null,
)

/** Testphase + Kaufstatus einer App; einmal pro Activity, [refresh] bei jedem Start */
class TrialGate(
    private val prefs: SharedPreferences,
    private val source: PurchaseSource,
    // Schluessel-App, bei jeder Entscheidung live (kein Cache)
    private val bundle: () -> Boolean = { false },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var state: GateState
        private set
    var onChange: ((GateState) -> Unit)? = null

    init {
        if (!prefs.contains(KEY_FIRST)) prefs.edit().putLong(KEY_FIRST, clock()).apply()
        state = GateState(decide(null).access)
        source.onUpdate = { owned -> apply(owned, restoring = false) }
    }

    private fun decide(query: Owned?) = TrialPolicy.decide(
        prefs.getLong(KEY_FIRST, clock()), clock(), prefs.getBoolean(KEY_PURCHASED, false), query, bundle(),
    )

    private fun set(s: GateState) {
        if (s == state) return
        state = s
        onChange?.invoke(s)
    }

    private fun apply(query: Owned?, restoring: Boolean) {
        val d = decide(query)
        // Cache nur bei erfolgreicher Abfrage aendern, damit offline gekauft bleibt
        if (query != null) prefs.edit().putBoolean(KEY_PURCHASED, d.purchased).apply()
        val notice = when {
            !restoring -> null
            query == null -> Notice.OFFLINE
            query == Owned.NONE -> Notice.NOT_FOUND
            else -> null
        }
        set(state.copy(access = d.access, pending = d.pending, busy = false, notice = notice))
        if (d.access == Access.EXPIRED && state.price == null) loadPrice()
    }

    private fun loadPrice() = source.price { p -> if (p != null) set(state.copy(price = p)) }

    /** Bei jedem Start: Kaufstatus abfragen, bis dahin gilt der Cache */
    fun refresh() {
        set(state.copy(access = decide(null).access))
        source.query { apply(it, restoring = false) }
    }

    /** "Kauf wiederherstellen" */
    fun restore() {
        set(state.copy(busy = true, notice = null))
        source.query { apply(it, restoring = true) }
        if (state.price == null) loadPrice()
    }

    fun buy(activity: Activity) {
        if (source.buy(activity)) {
            set(state.copy(notice = null))
        } else {
            set(state.copy(notice = Notice.UNAVAILABLE))
            loadPrice()
        }
    }

    /** Nur Debug-Builds: adb ... --es aapps_trial expire|reset */
    fun handleDebug(intent: Intent?) {
        val cmd = intent?.getStringExtra(DEBUG_EXTRA) ?: return
        if (DebugTrial.apply(cmd, prefs, clock())) set(state.copy(access = decide(null).access))
    }

    fun close() {
        source.onUpdate = null
        onChange = null
        source.close()
    }

    companion object {
        const val PREFS = "aapps_billing"
        const val KEY_FIRST = "first_start"
        const val KEY_PURCHASED = "purchased"
        const val DEBUG_EXTRA = "aapps_trial"

        /** Nur aus dem Cache plus Bundle, ohne Play-Abfrage: fuer Einstiege ohne Activity (z. B. Kachel) */
        fun expired(context: Context): Boolean {
            val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!p.contains(KEY_FIRST)) return false
            val now = System.currentTimeMillis()
            val d = TrialPolicy.decide(
                p.getLong(KEY_FIRST, now), now, p.getBoolean(KEY_PURCHASED, false), null, BundleKey.installed(context),
            )
            return d.access == Access.EXPIRED
        }

        /** Im Test ersetzbar (Fake statt Play Billing) */
        var factory: (Context) -> TrialGate = { context ->
            val app = context.applicationContext
            TrialGate(app.getSharedPreferences(PREFS, Context.MODE_PRIVATE), PlayPurchases(app), bundle = { BundleKey.installed(app) })
        }
    }
}
