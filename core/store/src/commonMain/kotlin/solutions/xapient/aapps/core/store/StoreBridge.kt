package solutions.xapient.aapps.core.store

/**
 * StoreKit 2 (nur in Swift erreichbar). Die Swift-Seite implementiert das und meldet ueber [StoreListener],
 * immer auf dem Main-Thread. Produkte: <Bundle-ID>.full_unlock und <Bundle-ID>.trial (beide nicht verbrauchbar).
 */
interface StoreBridge {
    /** Transaction.updates hoeren (jede Transaktion mit finish() abschliessen) */
    fun start(listener: StoreListener)

    /** Transaction.currentEntitlements -> onEntitlements oder onQueryFailed */
    fun refresh()

    /** Product.products -> onProducts */
    fun loadProducts()

    /** Kauf starten: Testphase (Preis 0) oder Freischaltung */
    fun purchase(trial: Boolean)

    /** AppStore.sync(), danach refresh(); Fehler -> onRestoreFailed */
    fun restore()

    fun stop()
}

/** Rueckmeldungen von StoreKit; trialStartMillis -1 = keine Testphase */
interface StoreListener {
    fun onEntitlements(unlocked: Boolean, trialStartMillis: Long)
    fun onQueryFailed()
    fun onProducts(unlockPrice: String?, trialAvailable: Boolean)
    fun onPurchasePending()
    fun onPurchaseCancelled()
    fun onPurchaseFailed()
    fun onRestoreFailed()
}

/** Lokaler Cache, damit die App offline und vor der ersten Abfrage richtig startet */
interface StoreCache {
    var unlocked: Boolean
    var trialStart: Long?
}
