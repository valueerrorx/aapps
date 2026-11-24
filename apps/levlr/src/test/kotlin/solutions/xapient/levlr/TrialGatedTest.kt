package solutions.xapient.levlr

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import solutions.xapient.aapps.core.billing.Owned
import solutions.xapient.aapps.core.billing.PurchaseSource
import solutions.xapient.aapps.core.billing.TrialGate
import solutions.xapient.aapps.core.billing.TrialPolicy
import solutions.xapient.aapps.core.ui.TrialGated

// TrialGated mit Fake statt Play Billing: Testphase, Ablauf, Wiederherstellen
@RunWith(RobolectricTestRunner::class)
class TrialGatedTest {
    // leere Activity fuer die Testregel anmelden (kein Test-Manifest im Projekt)
    @get:Rule(order = 0) val host = object : ExternalResource() {
        override fun before() {
            val app = RuntimeEnvironment.getApplication()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
        }
    }

    @get:Rule(order = 1) val compose = createComposeRule()

    private val defaultFactory = TrialGate.factory
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("trial-ui", Context.MODE_PRIVATE)
    private var owned: Owned? = Owned.NONE
    private var queries = 0

    private val fake = object : PurchaseSource {
        override var onUpdate: ((Owned) -> Unit)? = null
        override fun query(onResult: (Owned?) -> Unit) { queries++; onResult(owned) }
        override fun price(onResult: (String?) -> Unit) = onResult("€0.99")
        override fun buy(activity: Activity) = true
        override fun close() = Unit
    }

    @After fun restore() { TrialGate.factory = defaultFactory }

    private fun start(elapsed: Long) {
        prefs.edit().clear().putLong(TrialGate.KEY_FIRST, 0L).commit()
        TrialGate.factory = { TrialGate(prefs, fake) { elapsed } }
        compose.setContent { TrialGated(LevlrAccent) { BasicText("APP") } }
    }

    @Test fun duringTrialShowsApp() {
        start(TrialPolicy.TRIAL_MS - 1)
        compose.onNodeWithText("APP").assertExists()
        assertEquals(1, queries)
    }

    @Test fun afterTrialShowsThanksWithPrice() {
        start(TrialPolicy.TRIAL_MS)
        compose.onNodeWithText("APP").assertDoesNotExist()
        compose.onNodeWithText("UNLOCK FOR €0.99").assertExists()
    }

    @Test fun restoreUnlocks() {
        start(TrialPolicy.TRIAL_MS * 3)
        owned = Owned.PURCHASED
        compose.onNodeWithText("RESTORE PURCHASE").performClick()
        compose.onNodeWithText("APP").assertExists()
    }

    @Test fun cachedPurchaseOfflineShowsApp() {
        prefs.edit().clear().putLong(TrialGate.KEY_FIRST, 0L).putBoolean(TrialGate.KEY_PURCHASED, true).commit()
        owned = null
        TrialGate.factory = { TrialGate(prefs, fake) { TrialPolicy.TRIAL_MS * 9 } }
        compose.setContent { TrialGated(LevlrAccent) { BasicText("APP") } }
        compose.onNodeWithText("APP").assertExists()
    }
}
