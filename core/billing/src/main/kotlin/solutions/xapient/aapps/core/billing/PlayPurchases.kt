package solutions.xapient.aapps.core.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

// Google Play Billing: spricht per IPC mit der Play-Store-App, kein eigener Server
internal class PlayPurchases(context: Context) : PurchaseSource, PurchasesUpdatedListener, BillingClientStateListener {
    private val main = Handler(Looper.getMainLooper())
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()
    private val waiting = mutableListOf<(Boolean) -> Unit>()
    private var connecting = false
    private var closed = false
    private var details: ProductDetails? = null

    override var onUpdate: ((Owned) -> Unit)? = null

    private fun ready(block: (Boolean) -> Unit) {
        if (closed) return
        if (client.isReady) return block(true)
        waiting += block
        if (connecting) return
        connecting = true
        try {
            client.startConnection(this)
        } catch (_: Exception) {
            // z. B. kein Play Store: wie Verbindungsfehler behandeln
            flush(false)
        }
    }

    private fun flush(ok: Boolean) {
        connecting = false
        val w = waiting.toList()
        waiting.clear()
        w.forEach { it(ok) }
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        main.post { flush(result.responseCode == BillingResponseCode.OK) }
    }

    override fun onBillingServiceDisconnected() {
        main.post { flush(false) }
    }

    override fun query(onResult: (Owned?) -> Unit) = ready { ok ->
        if (!ok) return@ready onResult(null)
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { r, list ->
            main.post {
                if (closed) return@post
                onResult(if (r.responseCode == BillingResponseCode.OK) handle(list) else null)
            }
        }
    }

    // Eigene Kaeufe bestaetigen (sonst erstattet Play nach 3 Tagen) und zusammenfassen
    private fun handle(list: List<Purchase>): Owned {
        val mine = list.filter { PRODUCT_ID in it.products }
        mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { p ->
            val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
            // Fehlschlag: beim naechsten Start erneut
            client.acknowledgePurchase(ack) { }
        }
        return TrialPolicy.combine(mine.map {
            when (it.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> Owned.PURCHASED
                Purchase.PurchaseState.PENDING -> Owned.PENDING
                else -> Owned.NONE
            }
        })
    }

    override fun price(onResult: (String?) -> Unit) = ready { ok ->
        if (!ok) return@ready onResult(null)
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID).setProductType(ProductType.INAPP).build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { r, result ->
            val d = if (r.responseCode == BillingResponseCode.OK) result.productDetailsList.firstOrNull() else null
            main.post {
                if (closed) return@post
                details = d
                onResult(d?.oneTimePurchaseOfferDetails?.formattedPrice)
            }
        }
    }

    override fun buy(activity: Activity): Boolean {
        val d = details ?: return false
        val item = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d)
        d.oneTimePurchaseOfferDetails?.offerToken?.let { item.setOfferToken(it) }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item.build())).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    // Kaufdialog fertig: Bestand neu abfragen, dabei wird auch bestaetigt
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val code = result.responseCode
        if (code != BillingResponseCode.OK && code != BillingResponseCode.ITEM_ALREADY_OWNED) return
        main.post { query { owned -> if (owned != null) onUpdate?.invoke(owned) } }
    }

    override fun close() {
        closed = true
        waiting.clear()
        client.endConnection()
    }
}
