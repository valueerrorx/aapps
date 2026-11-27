package solutions.xapient.dividr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.Frames
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Ids
import solutions.xapient.dividr.domain.ImportSummary
import solutions.xapient.dividr.domain.Kind
import solutions.xapient.dividr.domain.Stamp
import solutions.xapient.dividr.domain.addMember
import solutions.xapient.dividr.domain.newGroup
import solutions.xapient.dividr.domain.putEntry
import solutions.xapient.dividr.qr.QrMatrix
import solutions.xapient.dividr.res.*
import solutions.xapient.dividr.ui.CameraUi
import solutions.xapient.dividr.ui.DividrAbout
import solutions.xapient.dividr.ui.DividrAccent
import solutions.xapient.dividr.ui.EntryScreen
import solutions.xapient.dividr.ui.GroupItem
import solutions.xapient.dividr.ui.GroupScreen
import solutions.xapient.dividr.ui.GroupsScreen
import solutions.xapient.dividr.ui.ImportScreen
import solutions.xapient.dividr.ui.QrScreen
import solutions.xapient.dividr.ui.ScanScreen
import solutions.xapient.dividr.ui.SettleScreen

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    GROUP, ENTRY, QR, SETTLE, IMPORT, GROUPS, SCAN, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// Toern-Beispiel wie Android (Demo in DividrScreenshotTest)
private object Demo {
    const val A = 0x2B1C_0001
    const val B = 0x5E7A_0002
    private var seq = 0
    private fun id(d: Int = A) = Ids.of(d, ++seq)

    fun group(today: Int): Group {
        seq = 0
        var g = newGroup(77L, "Kornaten 2026", "EUR", A)
        val ids = listOf("Anna", "Ben", "Clara", "David", "Eva", "Felix").map { n -> id().also { g = g.addMember(it, n, A) } }
        fun exp(payer: Int, cents: Long, title: String, day: Int, forIdx: List<Int>? = null, cur: String = "", rate: Long = 0) {
            val e = Entry(
                id(if (payer % 2 == 0) A else B), Stamp(1, A), title = title, amount = cents, payer = ids[payer],
                forAll = forIdx == null, forIds = forIdx?.map { ids[it] }?.toSet() ?: emptySet(), day = today - day, currency = cur, rateMicros = rate,
            )
            g = g.putEntry(e, A)
        }
        exp(0, 18_640, "Proviant Konzum", 3)
        exp(1, 7_200, "Marina Hvar", 3)
        exp(2, 4_350, "Diesel", 2)
        exp(3, 21_480, "Konoba Mate", 2)
        exp(4, 1_260, "Eis", 2, listOf(0, 4, 5))
        exp(5, 3_600, "Taxi", 1)
        exp(0, 1_840, "Kaffee", 1, listOf(0, 1))
        exp(1, 6_500, "Hafengebühr Vis", 1)
        exp(2, 9_870, "Fischmarkt", 1)
        exp(3, 2_400, "Bar", 0, listOf(1, 3))
        exp(4, 4_800, "Bäckerei", 0)
        exp(5, 1_200_000, "Thermalbad Hévíz", 0, cur = "HUF", rate = 2_530)
        exp(0, 5_100, "Proviant Lidl", 0)
        val pay = Entry(id(), Stamp(1, A), Kind.PAYMENT, amount = 5_000, payer = ids[1], forAll = false, forIds = setOf(ids[0]), day = today)
        g = g.putEntry(pay, A)
        exp(2, 3_300, "Eis", 0)
        return g
    }

    fun incomingNewPerson(g: Group, today: Int): Group {
        val d = 0x4411_0003
        val gina = Ids.of(d, 1)
        return g.addMember(gina, "Gina", d)
            .putEntry(Entry(Ids.of(d, 2), Stamp(1, d), title = "Fähre Split", amount = 8_400, payer = gina, day = today), d)
    }
}

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(DividrAccent) {
        val today = remember { todayEpochDay() }
        val g = remember { Demo.group(today) }
        val me = remember { g.members.values.minOf { it.id } }
        val codes = remember { Frames.pack(g).map { QrMatrix.encode(it) } }
        when (scene) {
            Scene.GROUP -> GroupScreen(g, me, codes, today, {}, {}, {}, {}, {}, {}, {})
            Scene.ENTRY -> EntryScreen(
                g, g.entries.values.first { it.currency == "HUF" }, me, today, listOf("Proviant Konzum", "Diesel", "Eis"),
                { 2_530L }, {}, {}, {}, focus = false, preset = null,
            )
            Scene.QR -> QrScreen(codes, stringResource(Res.string.let_them_scan), {}, animate = false)
            Scene.SETTLE -> SettleScreen(g, me, {}, {}, {})
            Scene.IMPORT -> ImportScreen(ImportSummary.of(g, Demo.incomingNewPerson(g, today)), { _, _ -> }, {})
            Scene.GROUPS -> {
                val other = newGroup(5L, "WG", "EUR", 1).addMember(Ids.of(1, 1), "Anna", 1)
                GroupsScreen(listOf(GroupItem("a", g, me), GroupItem("b", other, Ids.of(1, 1))), {}, {}, {}, {})
            }
            Scene.SCAN -> ScanScreen(CameraUi.ON, setOf(0, 1), 4, null, {}, {}) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A3338), Color(0xFF151B1F), Color(0xFF30393E)))))
            }
            Scene.ABOUT -> DividrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
