package solutions.xapient.aapps.core.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Fake ohne Play Store: Ergebnisse werden per Feld vorgegeben */
class FakePurchases(var owned: Owned? = Owned.NONE, var priceText: String? = "0,99 €") : PurchaseSource {
    override var onUpdate: ((Owned) -> Unit)? = null
    var buys = 0
    override fun query(onResult: (Owned?) -> Unit) = onResult(owned)
    override fun price(onResult: (String?) -> Unit) = onResult(priceText)
    override fun buy(activity: Activity): Boolean { buys++; return priceText != null }
    override fun close() = Unit
}

@RunWith(RobolectricTestRunner::class)
class TrialGateTest {
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("t", Context.MODE_PRIVATE)
    private var now = 1_000_000_000L
    private val fake = FakePurchases()

    @Before fun clear() = prefs.edit().clear().commit().let { }

    private fun gate() = TrialGate(prefs, fake) { now }

    @Test fun storesFirstStartOnce() {
        gate()
        assertEquals(now, prefs.getLong(TrialGate.KEY_FIRST, -1))
        now += 5_000
        gate()
        assertEquals(now - 5_000, prefs.getLong(TrialGate.KEY_FIRST, -1))
    }

    @Test fun expiresAfter24hAndLoadsPrice() {
        val g = gate()
        assertEquals(Access.TRIAL, g.state.access)
        now += TrialPolicy.TRIAL_MS
        g.refresh()
        assertEquals(Access.EXPIRED, g.state.access)
        assertEquals("0,99 €", g.state.price)
    }

    @Test fun purchaseCachedAndWorksOffline() {
        fake.owned = Owned.PURCHASED
        gate().refresh()
        assertTrue(prefs.getBoolean(TrialGate.KEY_PURCHASED, false))
        fake.owned = null
        now += TrialPolicy.TRIAL_MS * 30
        val g = gate()
        assertEquals(Access.FULL, g.state.access)
        g.refresh()
        assertEquals(Access.FULL, g.state.access)
    }

    @Test fun refundResetsCache() {
        prefs.edit().putLong(TrialGate.KEY_FIRST, now).putBoolean(TrialGate.KEY_PURCHASED, true).commit()
        now += TrialPolicy.TRIAL_MS * 2
        val g = gate()
        fake.owned = Owned.NONE
        g.refresh()
        assertEquals(Access.EXPIRED, g.state.access)
        assertFalse(prefs.getBoolean(TrialGate.KEY_PURCHASED, true))
    }

    @Test fun pendingThenPurchasedViaUpdate() {
        val g = gate()
        now += TrialPolicy.TRIAL_MS
        fake.owned = Owned.PENDING
        g.refresh()
        assertEquals(Access.EXPIRED, g.state.access)
        assertTrue(g.state.pending)
        fake.onUpdate!!(Owned.PURCHASED)
        assertEquals(Access.FULL, g.state.access)
        assertFalse(g.state.pending)
    }

    @Test fun restoreNotices() {
        val g = gate()
        now += TrialPolicy.TRIAL_MS
        g.restore()
        assertEquals(Notice.NOT_FOUND, g.state.notice)
        fake.owned = null
        g.restore()
        assertEquals(Notice.OFFLINE, g.state.notice)
        assertFalse(g.state.busy)
        fake.owned = Owned.PURCHASED
        g.restore()
        assertEquals(Access.FULL, g.state.access)
        assertNull(g.state.notice)
    }

    @Test fun noPriceMeansNoPurchaseFlow() {
        fake.priceText = null
        val g = gate()
        now += TrialPolicy.TRIAL_MS
        g.refresh()
        assertNull(g.state.price)
        g.buy(Robolectric.buildActivity(Activity::class.java).get())
        assertEquals(Notice.UNAVAILABLE, g.state.notice)
    }

    @Test fun onChangeFires() {
        val g = gate()
        val seen = mutableListOf<Access>()
        g.onChange = { seen += it.access }
        now += TrialPolicy.TRIAL_MS
        g.refresh()
        assertEquals(Access.EXPIRED, seen.last())
    }

    @Test fun debugSwitchOnlyInDebugVariant() {
        val probe = RuntimeEnvironment.getApplication().getSharedPreferences("probe", Context.MODE_PRIVATE)
        val debug = DebugTrial.apply("reset", probe, 0)
        val g = gate()
        g.handleDebug(Intent().putExtra(TrialGate.DEBUG_EXTRA, "expire"))
        assertEquals(if (debug) Access.EXPIRED else Access.TRIAL, g.state.access)
        g.handleDebug(Intent().putExtra(TrialGate.DEBUG_EXTRA, "reset"))
        assertEquals(Access.TRIAL, g.state.access)
    }
}
