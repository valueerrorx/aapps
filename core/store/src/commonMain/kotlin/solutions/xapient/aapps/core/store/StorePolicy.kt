package solutions.xapient.aapps.core.store

/** App Store: Testphase als eigenes Gratis-Produkt (Apple 3.1.1), danach einmaliger Kauf */
enum class StoreAccess { OFFER, TRIAL, FULL, EXPIRED }

/** Stand aus StoreKit (Transaction.currentEntitlements); [trialStart] = Kaufdatum des Testphasen-Produkts in ms */
data class Entitlements(val unlocked: Boolean, val trialStart: Long?)

data class StoreDecision(val access: StoreAccess, val unlocked: Boolean, val trialStart: Long?)

// Reine Entscheidungslogik, ohne StoreKit
object StorePolicy {
    const val TRIAL_MS = 24L * 60 * 60 * 1000

    /**
     * [query] null = StoreKit nicht abfragbar oder noch offen, dann gilt der Cache.
     * Nur eine erfolgreiche Abfrage setzt "gekauft" zurueck (z. B. nach Erstattung).
     * Testphasen-Start: der frueheste bekannte Wert (lokal beim Aktivieren oder aus der Transaktion).
     */
    fun decide(cachedUnlocked: Boolean, cachedTrialStart: Long?, query: Entitlements?, now: Long): StoreDecision {
        val unlocked = query?.unlocked ?: cachedUnlocked
        val start = listOfNotNull(cachedTrialStart, query?.trialStart).minOrNull()
        val access = when {
            unlocked -> StoreAccess.FULL
            start == null -> StoreAccess.OFFER
            // Uhr zurueckgestellt (now < start) zaehlt bewusst als Testphase, kein Anti-Tamper
            now - start < TRIAL_MS -> StoreAccess.TRIAL
            else -> StoreAccess.EXPIRED
        }
        return StoreDecision(access, unlocked, start)
    }
}
