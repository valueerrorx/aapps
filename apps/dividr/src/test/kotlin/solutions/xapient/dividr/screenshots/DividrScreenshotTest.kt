package solutions.xapient.dividr.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.Frames
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Ids
import solutions.xapient.dividr.domain.ImportSummary
import solutions.xapient.dividr.domain.Kind
import solutions.xapient.dividr.domain.Stamp
import solutions.xapient.dividr.domain.WireCodec
import solutions.xapient.dividr.domain.addMember
import solutions.xapient.dividr.domain.deleteEntry
import solutions.xapient.dividr.domain.newGroup
import solutions.xapient.dividr.domain.putEntry
import solutions.xapient.dividr.domain.setFromStart
import solutions.xapient.dividr.qr.QrMatrix
import solutions.xapient.dividr.ui.CameraUi
import solutions.xapient.dividr.ui.DividrAbout
import solutions.xapient.dividr.ui.DividrAccent
import solutions.xapient.dividr.ui.DividrSettings
import solutions.xapient.dividr.ui.EntryScreen
import solutions.xapient.dividr.ui.GroupItem
import solutions.xapient.dividr.ui.GroupScreen
import solutions.xapient.dividr.ui.GroupsScreen
import solutions.xapient.dividr.ui.ImportScreen
import solutions.xapient.dividr.ui.JoinScreen
import solutions.xapient.dividr.ui.NewGroupScreen
import solutions.xapient.dividr.ui.PeopleScreen
import solutions.xapient.dividr.ui.QrScreen
import solutions.xapient.dividr.ui.ScanScreen
import solutions.xapient.dividr.ui.SettleScreen

// Toern-Beispiel: 6 Personen, 15 Eintraege
object Demo {
    const val TODAY = 20_420
    private const val A = 0x2B1C_0001
    private const val B = 0x5E7A_0002
    private var seq = 0
    private fun id(d: Int = A) = Ids.of(d, ++seq)

    val people = listOf("Anna", "Ben", "Clara", "David", "Eva", "Felix")

    fun group(): Group {
        seq = 0
        var g = newGroup(77L, "Kornaten 2026", "EUR", A)
        val ids = people.map { n -> id().also { g = g.addMember(it, n, A) } }
        fun exp(payer: Int, cents: Long, title: String, day: Int, forIdx: List<Int>? = null, cur: String = "", rate: Long = 0) {
            val e = Entry(
                id(if (payer % 2 == 0) A else B), Stamp(1, A), title = title, amount = cents, payer = ids[payer],
                forAll = forIdx == null, forIds = forIdx?.map { ids[it] }?.toSet() ?: emptySet(), day = TODAY - day, currency = cur, rateMicros = rate,
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
        val pay = Entry(id(), Stamp(1, A), Kind.PAYMENT, amount = 5_000, payer = ids[1], forAll = false, forIds = setOf(ids[0]), day = TODAY)
        g = g.putEntry(pay, A)
        exp(2, 3_300, "Eis", 0)
        return g
    }

    fun anna(g: Group) = g.members.values.minOf { it.id }

    fun codes(g: Group) = Frames.pack(g).map { QrMatrix.encode(it) }

    fun incomingNewPerson(g: Group): Group {
        val d = 0x4411_0003
        val gina = Ids.of(d, 1)
        var o = g.addMember(gina, "Gina", d)
        o = o.putEntry(Entry(Ids.of(d, 2), Stamp(1, d), title = "Fähre Split", amount = 8_400, payer = gina, day = TODAY), d)
        return o
    }

    fun incomingUpdate(g: Group): Group {
        val d = 0x4411_0004
        val diesel = g.entries.values.first { it.title == "Diesel" }
        val taxi = g.entries.values.first { it.title == "Taxi" }
        return g.putEntry(diesel.copy(amount = 4_950), d).deleteEntry(taxi.id, d)
            .putEntry(Entry(Ids.of(d, 1), Stamp(1, d), title = "Pizza", amount = 6_300, payer = g.members.values.maxOf { it.id }, day = TODAY), d)
    }

    fun incomingSameName(g: Group): Group {
        val d = 0x4411_0005
        return g.addMember(Ids.of(d, 1), "eva", d)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class DividrScreenshotTest {
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))

    private fun shot(name: String, qualifiers: String? = null, fontScale: Float = 1f, content: @Composable () -> Unit) {
        if (qualifiers != null) RuntimeEnvironment.setQualifiers(qualifiers)
        if (fontScale != 1f) RuntimeEnvironment.setFontScale(fontScale)
        captureRoboImage("screenshots/$name.png", options) { AappsTheme(DividrAccent) { content() } }
    }

    private val g = Demo.group()
    private val me = Demo.anna(g)

    @Composable
    private fun Main(group: Group = g, codes: List<QrMatrix> = Demo.codes(group), meId: Long? = me) =
        GroupScreen(group, meId, codes, Demo.TODAY, {}, {}, {}, {}, {}, {}, {})

    @Test fun group() = shot("group") { Main() }
    @Test fun group_de() = shot("group_de", "+de") { Main() }
    @Test fun group_land() = shot("group_land", "+land") { Main() }
    @Test fun group_font200() = shot("group_font200", fontScale = 2f) { Main() }
    @Test fun group_multi() = shot("group_multi") {
        Main(codes = Frames.pack(WireCodec.encode(g), chunk = 300).map { QrMatrix.encode(it) })
    }
    @Test fun group_empty() = shot("group_empty") {
        val e = newGroup(5L, "WG Küche", "EUR", 1).addMember(Ids.of(1, 1), "Anna", 1).addMember(Ids.of(1, 2), "Ben", 1)
        Main(e, meId = Ids.of(1, 1))
    }

    @Test fun groups() = shot("groups") {
        val other = newGroup(5L, "WG Küche", "EUR", 1).addMember(Ids.of(1, 1), "Anna", 1)
        GroupsScreen(listOf(GroupItem("a", g, me), GroupItem("b", other, Ids.of(1, 1))), {}, {}, {}, {})
    }
    @Test fun start() = shot("start") { GroupsScreen(emptyList(), {}, {}, {}, null) }
    @Test fun new_group() = shot("new_group") { NewGroupScreen("EUR", { _, _, _ -> }, {}, "Kornaten 2026", "Anna") }

    @Composable
    private fun EntryShot(preset: Entry?, initial: Entry? = null) =
        EntryScreen(g, initial, me, Demo.TODAY, listOf("Proviant Konzum", "Diesel", "Eis"), { 2_530L }, {}, {}, {}, focus = false, preset = preset)

    private val dinner get() = g.entries.values.first { it.title == "Konoba Mate" }

    @Test fun entry() = shot("entry") { EntryShot(null, dinner) }
    @Test fun entry_de() = shot("entry_de", "+de") { EntryShot(null, dinner) }
    @Test fun entry_land() = shot("entry_land", "+land") { EntryShot(null, dinner) }
    @Test fun entry_font200() = shot("entry_font200", fontScale = 2f) { EntryShot(null, dinner) }
    @Test fun entry_some() = shot("entry_some") { EntryShot(null, g.entries.values.first { it.title == "Eis" && !it.forAll }) }
    @Test fun entry_foreign() = shot("entry_foreign") { EntryShot(null, g.entries.values.first { it.currency == "HUF" }) }
    @Test fun entry_payment() = shot("entry_payment") { EntryShot(g.entries.values.first { it.kind == Kind.PAYMENT }) }

    @Test fun qr_fullscreen() = shot("qr_fullscreen") { QrScreen(Demo.codes(g), "Let the other phone scan this code", {}, animate = false) }

    // Store DE wie EN
    @Test fun qr_fullscreen_de() = shot("qr_fullscreen_de", "+de") {
        QrScreen(Demo.codes(g), "Lass das andere Handy diesen Code scannen", {}, animate = false)
    }
    @Test fun group_land_de() = shot("group_land_de", "+de-land") { Main() }
    @Test fun entry_foreign_de() = shot("entry_foreign_de", "+de") { EntryShot(null, g.entries.values.first { it.currency == "HUF" }) }
    @Test fun import_update_de() = shot("import_update_de", "+de") { ImportScreen(ImportSummary.of(g, Demo.incomingUpdate(g)), { _, _ -> }, {}) }

    @Test fun scan() = shot("scan") {
        ScanScreen(CameraUi.ON, setOf(0, 1), 4, null, {}, {}) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A3338), Color(0xFF151B1F), Color(0xFF30393E)))))
        }
    }
    @Test fun scan_permission() = shot("scan_permission") { ScanScreen(CameraUi.ASK, emptySet(), 0, null, {}, {}) {} }

    @Composable private fun ImportNew() = ImportScreen(ImportSummary.of(g, Demo.incomingNewPerson(g)), { _, _ -> }, {})

    @Test fun import_new_person() = shot("import_new_person") { ImportNew() }
    @Test fun import_new_person_de() = shot("import_new_person_de", "+de") { ImportNew() }
    @Test fun import_new_person_land() = shot("import_new_person_land", "+land") { ImportNew() }
    @Test fun import_new_person_font200() = shot("import_new_person_font200", fontScale = 2f) { ImportNew() }
    @Test fun import_update() = shot("import_update") { ImportScreen(ImportSummary.of(g, Demo.incomingUpdate(g)), { _, _ -> }, {}) }
    @Test fun import_same_name() = shot("import_same_name") { ImportScreen(ImportSummary.of(g, Demo.incomingSameName(g)), { _, _ -> }, {}) }

    @Test fun join() = shot("join") {
        val mine = newGroup(9L, "Kornaten", "EUR", 1).addMember(Ids.of(1, 1), "Gina", 1)
        JoinScreen(g, mine, {}, {}, {}, initialName = "Gina")
    }

    @Composable private fun Settle() = SettleScreen(g, me, {}, {}, {})

    @Test fun settle() = shot("settle") { Settle() }
    @Test fun settle_de() = shot("settle_de", "+de") { Settle() }
    @Test fun settle_land() = shot("settle_land", "+land") { Settle() }
    @Test fun settle_font200() = shot("settle_font200", fontScale = 2f) { Settle() }
    @Test fun settle_done() = shot("settle_done") {
        val even = newGroup(5L, "WG Küche", "EUR", 1).addMember(Ids.of(1, 1), "Anna", 1).addMember(Ids.of(1, 2), "Ben", 1)
        SettleScreen(even, Ids.of(1, 1), {}, {}, {})
    }

    @Test fun people() = shot("people") {
        val withLate = g.setFromStart(g.members.values.maxOf { it.id }, false, 1)
        PeopleScreen(withLate, me, { _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {}, open = g.members.values.sortedBy { it.id }[1].id)
    }

    @Test fun settings() = shot("settings") { DividrSettings(g, true, {}, {}, {}, {}, {}, {}, {}, {}) }
    @Test fun about() = shot("about") { DividrAbout("0.1.0") {} }
    @Test fun about_de() = shot("about_de", "+de") { DividrAbout("0.1.0") {} }
}
