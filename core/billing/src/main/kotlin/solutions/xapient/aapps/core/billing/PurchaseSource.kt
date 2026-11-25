package solutions.xapient.aapps.core.billing

import android.app.Activity

/** Kaufquelle (Play Billing oder Fake im Test); Rueckrufe immer auf dem Main-Thread */
interface PurchaseSource {
    /** Kaeufe ausserhalb einer Abfrage (Kaufdialog, ausstehend -> gekauft) */
    var onUpdate: ((Owned) -> Unit)?

    /** null = nicht abfragbar (offline, kein Play Store) */
    fun query(onResult: (Owned?) -> Unit)

    /** Formatierter Preis aus ProductDetails, null = unbekannt */
    fun price(onResult: (String?) -> Unit)

    /** false = Kaufdialog nicht startbar (ProductDetails fehlen) */
    fun buy(activity: Activity): Boolean

    fun close()
}
