package solutions.xapient.aapps.core.billing

/** Einheitliche Produkt-ID des einmaligen Kaufs in allen Apps */
const val PRODUCT_ID = "full_unlock"

/** Ergebnis einer erfolgreichen Kaufabfrage fuer [PRODUCT_ID] */
enum class Owned { PURCHASED, PENDING, NONE }

enum class Access { FULL, TRIAL, EXPIRED }

data class Decision(val access: Access, val purchased: Boolean, val pending: Boolean)

// Reine Entscheidungslogik, ohne Android
object TrialPolicy {
    const val TRIAL_MS = 24L * 60 * 60 * 1000

    /**
     * [query] null = Abfrage fehlgeschlagen oder noch offen, dann gilt [cachedPurchased].
     * Nur eine erfolgreiche Abfrage setzt den Cache zurueck (z. B. nach Erstattung).
     * [bundle] = Schluessel-App installiert: frei, zaehlt aber nicht als eigener Kauf.
     */
    fun decide(firstStart: Long, now: Long, cachedPurchased: Boolean, query: Owned?, bundle: Boolean = false): Decision {
        val purchased = if (query == null) cachedPurchased else query == Owned.PURCHASED
        val access = when {
            purchased || bundle -> Access.FULL
            // Uhr zurueckgestellt (now < firstStart) zaehlt bewusst als Testphase, kein Anti-Tamper
            now - firstStart < TRIAL_MS -> Access.TRIAL
            else -> Access.EXPIRED
        }
        return Decision(access, purchased, query == Owned.PENDING)
    }

    /** Mehrere Kaeufe desselben Produkts: gekauft schlaegt ausstehend */
    fun combine(states: List<Owned>): Owned = when {
        Owned.PURCHASED in states -> Owned.PURCHASED
        Owned.PENDING in states -> Owned.PENDING
        else -> Owned.NONE
    }
}
