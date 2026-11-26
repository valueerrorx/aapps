package solutions.xapient.aapps.core.store

/** App Store: Testphase als eigenes Gratis-Produkt (Apple 3.1.1), danach einmaliger Kauf */
enum class StoreAccess { OFFER, TRIAL, FULL, EXPIRED }

/**
 * Stand aus StoreKit: [unlocked] aus Transaction.currentEntitlements, [revoked] = letzte Freischalt-Transaktion
 * nachweislich widerrufen, [trialStart] = erstes Kaufdatum des Testphasen-Produkts in ms
 */
data class Entitlements(val unlocked: Boolean, val trialStart: Long?, val revoked: Boolean = false)

data class StoreDecision(val access: StoreAccess, val unlocked: Boolean, val trialStart: Long?)

// Reine Entscheidungslogik, ohne StoreKit
object StorePolicy {
    const val TRIAL_MS = 24L * 60 * 60 * 1000

    /**
     * [query] null = StoreKit nicht abfragbar oder noch offen, dann gilt der Cache.
     * "Gekauft" geht nur mit Beleg verloren (Erstattung/Widerruf), nicht bei leerer Abfrage (offline, Geraetewechsel).
     * Testphasen-Start: der frueheste bekannte Wert (lokal beim Aktivieren oder aus der Transaktion).
     * Uhr zurueckgestellt (now < start) zaehlt bewusst als Testphase, kein Anti-Tamper.
     */
    fun decide(cachedUnlocked: Boolean, cachedTrialStart: Long?, query: Entitlements?, now: Long): StoreDecision {
        val unlocked = if (query == null) cachedUnlocked else query.unlocked || (cachedUnlocked && !query.revoked)
        val start = listOfNotNull(cachedTrialStart, query?.trialStart).minOrNull()
        val access = when {
            unlocked -> StoreAccess.FULL
            start == null -> StoreAccess.OFFER
            now - start < TRIAL_MS -> StoreAccess.TRIAL
            else -> StoreAccess.EXPIRED
        }
        return StoreDecision(access, unlocked, start)
    }
}
