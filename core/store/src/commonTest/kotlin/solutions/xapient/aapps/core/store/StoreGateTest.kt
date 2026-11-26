package solutions.xapient.aapps.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DAY = StorePolicy.TRIAL_MS

class StorePolicyTest {
    @Test fun noTrialYetOffers() {
        assertEquals(StoreAccess.OFFER, StorePolicy.decide(false, null, Entitlements(false, null), 5L).access)
    }

    @Test fun trialRunsOneDay() {
        assertEquals(StoreAccess.TRIAL, StorePolicy.decide(false, 100L, null, 100L + DAY - 1).access)
        assertEquals(StoreAccess.EXPIRED, StorePolicy.decide(false, 100L, null, 100L + DAY).access)
    }

    @Test fun clockBackStaysTrial() {
        assertEquals(StoreAccess.TRIAL, StorePolicy.decide(false, 1000L, null, 10L).access)
    }

    @Test fun earliestTrialStartWins() {
        // Neuinstallation: lokaler Start fehlt, Transaktion liefert ihn
        val d = StorePolicy.decide(false, null, Entitlements(false, 50L), 50L + DAY)
        assertEquals(StoreAccess.EXPIRED, d.access)
        assertEquals(50L, d.trialStart)
        assertEquals(40L, StorePolicy.decide(false, 40L, Entitlements(false, 50L), 60L).trialStart)
    }

    @Test fun purchaseBeatsExpiredTrial() {
        assertEquals(StoreAccess.FULL, StorePolicy.decide(false, 0L, Entitlements(true, 0L), 9 * DAY).access)
    }

    @Test fun cacheOnlyWhenQueryFails() {
        assertEquals(StoreAccess.FULL, StorePolicy.decide(true, 0L, null, 9 * DAY).access)
    }

    @Test fun emptyQueryKeepsCachedPurchase() {
        // offline nach Geraetewechsel: currentEntitlements leer, aber kein Beleg fuer Erstattung
        assertEquals(StoreAccess.FULL, StorePolicy.decide(true, 0L, Entitlements(false, 0L), 9 * DAY).access)
    }

    @Test fun revokedLocksAgain() {
        assertEquals(StoreAccess.EXPIRED, StorePolicy.decide(true, 0L, Entitlements(false, 0L, revoked = true), 9 * DAY).access)
    }
}

class StoreGateTest {
    private class MemCache : StoreCache {
        override var unlocked = false
        override var trialStart: Long? = null
    }

    private class FakeBridge : StoreBridge {
        var listener: StoreListener? = null
        val calls = mutableListOf<String>()
        override fun start(listener: StoreListener) { this.listener = listener; calls += "start" }
        override fun refresh() { calls += "refresh" }
        override fun loadProducts() { calls += "products" }
        override fun purchase(trial: Boolean) { calls += if (trial) "buyTrial" else "buyUnlock" }
        override fun restore() { calls += "restore" }
        override fun stop() { calls += "stop" }
    }

    private val cache = MemCache()
    private val bridge = FakeBridge()
    private var now = 1_000L
    private val gate = StoreGate(cache, bridge) { now }

    @Test fun startsWithOfferAndQueries() {
        gate.start()
        assertEquals(StoreAccess.OFFER, gate.state.access)
        assertEquals(listOf("start", "refresh"), bridge.calls)
    }

    @Test fun startTwiceListensOnce() {
        gate.start(); gate.start()
        assertEquals(1, bridge.calls.count { it == "start" })
    }

    @Test fun trialPurchaseStoresStartAndUnlocksForOneDay() {
        gate.start()
        gate.onProducts("1,49 €", true)
        gate.startTrial()
        assertTrue(gate.state.busy)
        assertEquals("buyTrial", bridge.calls.last())
        gate.onEntitlements(false, false, now)
        assertEquals(StoreAccess.TRIAL, gate.state.access)
        assertEquals(now, cache.trialStart)
        assertFalse(gate.state.busy)
        now += DAY
        gate.refresh()
        assertEquals(StoreAccess.EXPIRED, gate.state.access)
    }

    @Test fun trialWithoutProductShowsUnavailable() {
        gate.startTrial()
        assertEquals(StoreNotice.UNAVAILABLE, gate.state.notice)
        assertEquals("products", bridge.calls.last())
    }

    @Test fun buyWithoutPriceShowsUnavailable() {
        gate.buy()
        assertEquals(StoreNotice.UNAVAILABLE, gate.state.notice)
        assertFalse(gate.state.busy)
    }

    @Test fun expiredLoadsPrice() {
        cache.trialStart = 0L
        now = 2 * DAY
        gate.start()
        gate.onEntitlements(false, false, 0L)
        assertEquals(StoreAccess.EXPIRED, gate.state.access)
        assertTrue("products" in bridge.calls)
    }

    @Test fun purchaseUnlocksAndCaches() {
        cache.trialStart = 0L
        now = 2 * DAY
        gate.onProducts("1,49 €", true)
        gate.buy()
        gate.onEntitlements(true, false, 0L)
        assertEquals(StoreAccess.FULL, gate.state.access)
        assertTrue(cache.unlocked)
    }

    @Test fun cachedPurchaseOfflineStaysFull() {
        cache.unlocked = true
        cache.trialStart = 0L
        now = 9 * DAY
        val g = StoreGate(cache, bridge) { now }
        assertEquals(StoreAccess.FULL, g.state.access)
        g.start()
        g.onQueryFailed()
        assertEquals(StoreAccess.FULL, g.state.access)
        assertTrue(cache.unlocked)
    }

    @Test fun queryFailedStillLoadsProducts() {
        gate.start()
        gate.onQueryFailed()
        assertEquals(StoreAccess.OFFER, gate.state.access)
        assertTrue("products" in bridge.calls)
    }

    @Test fun refundLocksAgain() {
        cache.unlocked = true
        cache.trialStart = 0L
        now = 9 * DAY
        gate.onEntitlements(false, false, 0L)
        assertEquals(StoreAccess.FULL, gate.state.access)
        gate.onEntitlements(false, true, 0L)
        assertEquals(StoreAccess.EXPIRED, gate.state.access)
        assertFalse(cache.unlocked)
    }

    @Test fun restoreWithoutPurchaseShowsNotFound() {
        cache.trialStart = 0L
        now = 2 * DAY
        gate.restore()
        assertTrue(gate.state.busy)
        gate.onEntitlements(false, false, 0L)
        assertEquals(StoreNotice.NOT_FOUND, gate.state.notice)
        assertFalse(gate.state.busy)
        // normaler Refresh danach loescht den Hinweis
        gate.onEntitlements(false, false, 0L)
        assertNull(gate.state.notice)
    }

    @Test fun restoreFailureShowsOffline() {
        gate.restore()
        gate.onRestoreFailed()
        assertEquals(StoreNotice.OFFLINE, gate.state.notice)
        assertFalse(gate.state.busy)
    }

    @Test fun pendingClearsOnNextQuery() {
        gate.onProducts("1,49 €", true)
        gate.buy()
        gate.onPurchasePending()
        assertTrue(gate.state.pending)
        assertFalse(gate.state.busy)
        // Ask to Buy abgelehnt: naechste Abfrage ohne Kauf gibt den Knopf wieder frei
        gate.onEntitlements(false, false, now)
        assertFalse(gate.state.pending)
        gate.buy()
        gate.onPurchasePending()
        gate.onEntitlements(true, false, now)
        assertFalse(gate.state.pending)
        assertEquals(StoreAccess.FULL, gate.state.access)
    }

    @Test fun trialRemainingOnlyDuringTrial() {
        assertNull(gate.trialRemainingMillis())
        gate.onEntitlements(false, false, now)
        assertEquals(DAY, gate.trialRemainingMillis())
        now += DAY - 10
        assertEquals(10L, gate.trialRemainingMillis())
        now += 10
        gate.refresh()
        assertNull(gate.trialRemainingMillis())
    }

    @Test fun cancelAndFailure() {
        gate.onProducts("1,49 €", true)
        gate.buy()
        gate.onPurchaseCancelled()
        assertFalse(gate.state.busy)
        assertNull(gate.state.notice)
        gate.buy()
        gate.onPurchaseFailed()
        assertEquals(StoreNotice.FAILED, gate.state.notice)
    }

    @Test fun debugExpireAndReset() {
        cache.trialStart = now
        gate.refresh()
        assertEquals(StoreAccess.TRIAL, gate.state.access)
        gate.debug("expire")
        assertEquals(StoreAccess.EXPIRED, gate.state.access)
        gate.debug("reset")
        assertEquals(StoreAccess.OFFER, gate.state.access)
    }

    @Test fun onChangeFiresOnlyOnChange() {
        var n = 0
        gate.onChange = { n++ }
        gate.onProducts("1,49 €", true)
        gate.onProducts("1,49 €", true)
        assertEquals(1, n)
    }

    @Test fun closeStopsBridge() {
        gate.start()
        gate.close()
        assertEquals("stop", bridge.calls.last())
    }
}
