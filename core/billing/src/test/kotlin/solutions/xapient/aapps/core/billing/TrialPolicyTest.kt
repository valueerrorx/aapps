package solutions.xapient.aapps.core.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import solutions.xapient.aapps.core.billing.TrialPolicy.TRIAL_MS
import solutions.xapient.aapps.core.billing.TrialPolicy.decide

class TrialPolicyTest {
    private val t0 = 1_700_000_000_000L

    @Test fun firstStartIsTrial() =
        assertEquals(Decision(Access.TRIAL, purchased = false, pending = false), decide(t0, t0, false, null))

    @Test fun justBefore24hIsTrial() = assertEquals(Access.TRIAL, decide(t0, t0 + TRIAL_MS - 1, false, Owned.NONE).access)

    @Test fun exactly24hIsExpired() = assertEquals(Access.EXPIRED, decide(t0, t0 + TRIAL_MS, false, Owned.NONE).access)

    @Test fun clockTurnedBackIsTrialNotCrash() {
        assertEquals(Access.TRIAL, decide(t0, t0 - TRIAL_MS * 10, false, null).access)
        assertEquals(Access.TRIAL, decide(t0, 0, false, Owned.NONE).access)
    }

    @Test fun purchasedQueryUnlocksAndCaches() {
        val d = decide(t0, t0 + TRIAL_MS * 5, false, Owned.PURCHASED)
        assertEquals(Access.FULL, d.access)
        assertTrue(d.purchased)
    }

    @Test fun cachedPurchasedOfflineStaysFull() {
        val d = decide(t0, t0 + TRIAL_MS * 100, true, null)
        assertEquals(Access.FULL, d.access)
        assertTrue(d.purchased)
    }

    @Test fun pendingDoesNotUnlock() {
        val expired = decide(t0, t0 + TRIAL_MS, false, Owned.PENDING)
        assertEquals(Access.EXPIRED, expired.access)
        assertTrue(expired.pending)
        assertFalse(expired.purchased)
        assertEquals(Access.TRIAL, decide(t0, t0 + 1, false, Owned.PENDING).access)
    }

    @Test fun refundedResetsCacheOnSuccessfulQuery() {
        val d = decide(t0, t0 + TRIAL_MS * 3, true, Owned.NONE)
        assertEquals(Access.EXPIRED, d.access)
        assertFalse(d.purchased)
    }

    @Test fun refundedWithinTrialIsTrial() = assertEquals(Access.TRIAL, decide(t0, t0 + 10, true, Owned.NONE).access)

    @Test fun failedQueryKeepsCacheEitherWay() {
        assertFalse(decide(t0, t0 + TRIAL_MS, false, null).purchased)
        assertTrue(decide(t0, t0 + TRIAL_MS, true, null).purchased)
    }

    @Test fun bundleUnlocksButIsNoPurchase() {
        val d = decide(t0, t0 + TRIAL_MS * 3, false, Owned.NONE, bundle = true)
        assertEquals(Access.FULL, d.access)
        assertFalse(d.purchased)
        assertEquals(Access.FULL, decide(t0, t0 + TRIAL_MS * 3, false, null, bundle = true).access)
        assertEquals(Access.EXPIRED, decide(t0, t0 + TRIAL_MS * 3, false, null, bundle = false).access)
    }

    @Test fun combinePrefersPurchased() {
        assertEquals(Owned.NONE, TrialPolicy.combine(emptyList()))
        assertEquals(Owned.PENDING, TrialPolicy.combine(listOf(Owned.NONE, Owned.PENDING)))
        assertEquals(Owned.PURCHASED, TrialPolicy.combine(listOf(Owned.PENDING, Owned.PURCHASED)))
    }
}
