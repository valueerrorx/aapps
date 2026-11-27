package solutions.xapient.dividr

import solutions.xapient.dividr.res.*
import solutions.xapient.aapps.core.ui.BackEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.rememberHaptics
import solutions.xapient.dividr.domain.Entry
import solutions.xapient.dividr.domain.FrameCollector
import solutions.xapient.dividr.domain.Frames
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.ImportSummary
import solutions.xapient.dividr.domain.Kind
import solutions.xapient.dividr.domain.Locale
import solutions.xapient.dividr.domain.Money
import solutions.xapient.dividr.domain.ScanResult
import solutions.xapient.dividr.domain.Settle
import solutions.xapient.dividr.domain.Stamp
import solutions.xapient.dividr.domain.addMember
import solutions.xapient.dividr.domain.adopt
import solutions.xapient.dividr.domain.balances
import solutions.xapient.dividr.domain.deleteEntry
import solutions.xapient.dividr.domain.displayName
import solutions.xapient.dividr.domain.mergeMembers
import solutions.xapient.dividr.domain.newGroup
import solutions.xapient.dividr.domain.putEntry
import solutions.xapient.dividr.domain.removeMember
import solutions.xapient.dividr.domain.rename
import solutions.xapient.dividr.domain.setFromStart
import solutions.xapient.dividr.domain.setMeta
import solutions.xapient.dividr.domain.totalSpent
import solutions.xapient.dividr.domain.unmerge
import solutions.xapient.dividr.domain.visibleEntries
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
import solutions.xapient.dividr.ui.JoinChoice
import solutions.xapient.dividr.ui.JoinScreen
import solutions.xapient.dividr.ui.NewGroupScreen
import solutions.xapient.dividr.ui.PeopleScreen
import solutions.xapient.dividr.ui.QrScreen
import solutions.xapient.dividr.ui.ScanScreen
import solutions.xapient.dividr.ui.SettleScreen

const val APP_NAME = "dividr"

private enum class Screen { GROUPS, NEW_GROUP, GROUP, ENTRY, QR, SCAN, IMPORT, JOIN, SETTLE, PEOPLE, SETTINGS, ABOUT }

@Composable
fun DividrApp(store: DividrStore, versionName: String) {
    val locale = currentLocale()
    // Texte fuer Rueckmeldungen ausserhalb der Komposition
    val upToDate = stringResource(Res.string.up_to_date)
    val letThemScan = stringResource(Res.string.let_them_scan)
    val nowScanMe = stringResource(Res.string.now_scan_me)
    val notOurs = stringResource(Res.string.not_ours)
    val newerCode = stringResource(Res.string.newer_code)
    val brokenCode = stringResource(Res.string.broken_code)
    val shareTotal = stringResource(Res.string.share_total, "%1\$s")
    val allSquare = stringResource(Res.string.all_square)
    val shareFooter = stringResource(Res.string.share_footer)
    val share = rememberShare()
    var groups by remember { mutableStateOf(store.loadAll()) }
    var key by remember { mutableStateOf(store.lastKey?.takeIf { it in groups } ?: groups.keys.sorted().firstOrNull()) }
    var screen by remember { mutableStateOf(if (key != null) Screen.GROUP else Screen.GROUPS) }
    var hapticsOn by remember { mutableStateOf(store.haptics) }
    var editing by remember { mutableLongStateOf(0L) }
    var qrHint by remember { mutableStateOf<String?>(null) }
    var incoming by remember { mutableStateOf<Group?>(null) }
    var summary by remember { mutableStateOf<ImportSummary?>(null) }
    var importKey by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var meVersion by remember { mutableLongStateOf(0L) }
    val group = key?.let { groups[it] }
    val me = key?.let { k -> meVersion.let { store.me(k) } }
    val today = todayEpochDay()

    LaunchedEffect(key) { store.lastKey = key }
    LaunchedEffect(notice) { if (notice != null) { delay(2500); notice = null } }

    fun save(k: String, g: Group) {
        groups = groups + (k to g)
        store.save(k, g)
    }

    fun edit(f: (Group) -> Group) {
        val k = key ?: return
        val g = groups[k] ?: return
        val n = f(g)
        if (n != g) save(k, n)
    }

    fun open(k: String?) {
        key = k
        screen = if (k != null) Screen.GROUP else Screen.GROUPS
    }

    // eigener Code: normal genau einer; leer = zu gross
    val codes = remember(group) { group?.let { g -> Frames.pack(g).map { QrMatrix.encode(it) } } ?: emptyList() }

    // Kamera-Erlaubnis erst beim Scannen
    val camera = rememberCameraAccess(store)
    var cameraError by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        camera.refresh()
        onPauseOrDispose { }
    }

    val collector = remember { FrameCollector() }
    var scanHave by remember { mutableStateOf(emptySet<Int>()) }
    var scanTotal by remember { mutableIntStateOf(0) }
    var scanMsg by remember { mutableStateOf<String?>(null) }

    AappsTheme(DividrAccent) {
        val haptics = rememberHaptics(hapticsOn)

        fun showMine(hint: String) {
            qrHint = hint
            screen = Screen.QR
        }

        fun receive(inc: Group) {
            val local = groups.entries.firstOrNull { it.value.sameGroup(inc) }
            if (local != null) {
                val s = ImportSummary.of(local.value, inc)
                if (s.upToDate) {
                    key = local.key
                    notice = upToDate
                    screen = Screen.GROUP
                } else {
                    summary = s
                    importKey = local.key
                    screen = Screen.IMPORT
                }
            } else {
                incoming = inc
                screen = Screen.JOIN
            }
        }

        fun scanned(text: String) {
            when (val r = collector.accept(text)) {
                ScanResult.NotOurs -> scanMsg = notOurs
                ScanResult.Newer -> scanMsg = newerCode
                ScanResult.Broken -> scanMsg = brokenCode
                is ScanResult.Progress -> {
                    if (r.have.size > scanHave.size) haptics.step()
                    scanHave = r.have
                    scanTotal = r.total
                    scanMsg = null
                }
                is ScanResult.Done -> {
                    haptics.tick()
                    receive(r.group)
                }
            }
        }

        fun startScan() {
            collector.reset()
            scanHave = emptySet()
            scanTotal = 0
            scanMsg = null
            cameraError = false
            camera.refresh()
            screen = Screen.SCAN
        }

        when (screen) {
            Screen.GROUPS -> {
                if (group != null) BackEffect { screen = Screen.GROUP }
                GroupsScreen(
                    items = groups.map { (k, g) -> GroupItem(k, g, store.me(k)) }.sortedBy { it.group.name.lowercase() },
                    onOpen = { open(it) },
                    onNew = { screen = Screen.NEW_GROUP },
                    onScan = ::startScan,
                    onBack = if (group != null) ({ screen = Screen.GROUP }) else null,
                )
            }
            Screen.NEW_GROUP -> {
                BackEffect { screen = Screen.GROUPS }
                NewGroupScreen(Money.defaultCurrency(locale), onCreate = { name, meName, cur ->
                    val meId = store.newId()
                    val g = newGroup(store.newGroupId(), name, cur, store.device).addMember(meId, meName, store.device)
                    val k = store.keyFor(g)
                    save(k, g)
                    store.setMe(k, meId)
                    haptics.confirm()
                    open(k)
                }, onBack = { screen = Screen.GROUPS })
            }
            Screen.GROUP -> {
                val g = group
                if (g == null) {
                    Reset { open(null) }
                } else {
                    GroupScreen(
                        group = g, me = me, codes = codes, today = today,
                        onGroups = { screen = Screen.GROUPS },
                        onSettings = { screen = Screen.SETTINGS },
                        onSettle = { screen = Screen.SETTLE },
                        onAdd = { editing = 0L; screen = Screen.ENTRY },
                        onEntry = { editing = it; screen = Screen.ENTRY },
                        onQr = { showMine(letThemScan) },
                        onScan = ::startScan,
                        notice = notice,
                    )
                }
            }
            Screen.ENTRY -> {
                val g = group ?: return@AappsTheme Reset { open(null) }
                BackEffect { screen = Screen.GROUP }
                val initial = g.entries[editing]?.takeIf { !it.deleted }
                EntryScreen(
                    group = g, initial = initial, me = me, today = today,
                    recentTitles = remember(g) { g.visibleEntries().map { it.title }.filter { it.isNotEmpty() }.distinct().take(12) },
                    lastRate = { store.lastRate(it) },
                    onSave = { e ->
                        val id = if (e.id == 0L) store.newId() else e.id
                        edit { it.putEntry(e.copy(id = id), store.device) }
                        if (e.currency != g.currency && e.rateMicros > 0) store.setLastRate(e.currency, e.rateMicros)
                        haptics.press()
                        screen = Screen.GROUP
                    },
                    onDelete = {
                        edit { it.deleteEntry(editing, store.device) }
                        haptics.press()
                        screen = Screen.GROUP
                    },
                    onBack = { screen = Screen.GROUP },
                )
            }
            Screen.QR -> {
                BackEffect { screen = Screen.GROUP }
                QrScreen(codes, qrHint ?: letThemScan, onBack = { screen = Screen.GROUP })
            }
            Screen.SCAN -> {
                BackEffect { screen = if (group != null) Screen.GROUP else Screen.GROUPS }
                ScanScreen(
                    camera = if (camera.state == CameraUi.ON && cameraError) CameraUi.ERROR else camera.state,
                    have = scanHave, total = scanTotal, message = scanMsg,
                    onAllow = camera::allow,
                    onBack = { screen = if (group != null) Screen.GROUP else Screen.GROUPS },
                ) {
                    CameraScan(::scanned, onError = { cameraError = true })
                }
            }
            Screen.IMPORT -> {
                val s = summary
                val k = importKey
                if (s == null || k == null) {
                    Reset { open(key) }
                } else {
                    BackEffect { open(key) }
                    ImportScreen(s, onApply = { fromStart, same ->
                        save(k, s.apply(fromStart, same, store.device))
                        summary = null
                        key = k
                        haptics.confirm()
                        showMine(nowScanMe)
                    }, onCancel = { summary = null; open(key) })
                }
            }
            Screen.JOIN -> {
                val inc = incoming
                if (inc == null) {
                    Reset { open(key) }
                } else {
                    BackEffect { open(key) }
                    JoinScreen(
                        incoming = inc,
                        mergeWith = group?.takeIf { it.currency == inc.currency },
                        onJoin = { choice ->
                            var g: Group = inc
                            val meId = when (choice) {
                                is JoinChoice.Existing -> choice.id
                                is JoinChoice.New -> store.newId().also { g = g.addMember(it, choice.name, store.device, choice.fromStart) }
                                JoinChoice.Watch -> null
                            }
                            val k = store.keyFor(g)
                            save(k, g)
                            store.setMe(k, meId)
                            incoming = null
                            key = k
                            haptics.confirm()
                            showMine(nowScanMe)
                        },
                        onMerge = {
                            val cur = group
                            val k = key
                            if (cur != null && k != null) {
                                summary = ImportSummary.of(cur, cur.adopt(inc))
                                importKey = k
                                incoming = null
                                screen = Screen.IMPORT
                            }
                        },
                        onCancel = { incoming = null; open(key) },
                    )
                }
            }
            Screen.SETTLE -> {
                val g = group ?: return@AappsTheme Reset { open(null) }
                BackEffect { screen = Screen.GROUP }
                SettleScreen(
                    g, me,
                    onDone = { t ->
                        val e = Entry(
                            store.newId(), Stamp(1, store.device), Kind.PAYMENT, amount = t.amount, currency = g.currency,
                            payer = t.from, forAll = false, forIds = setOf(t.to), day = today,
                        )
                        edit { it.putEntry(e, store.device) }
                        haptics.confirm()
                    },
                    onShare = { share(shareText(g, locale, shareTotal, allSquare, shareFooter)) },
                    onBack = { screen = Screen.GROUP },
                )
            }
            Screen.PEOPLE -> {
                val g = group ?: return@AappsTheme Reset { open(null) }
                val k = key!!
                BackEffect { screen = Screen.SETTINGS }
                PeopleScreen(
                    g, me,
                    onRename = { id, n -> edit { it.rename(id, n, store.device) } },
                    onFromStart = { id, v -> edit { it.setFromStart(id, v, store.device) }; haptics.step() },
                    onMerge = { a, b -> edit { it.mergeMembers(a, b, store.device) }; haptics.confirm() },
                    onUnmerge = { id -> edit { it.unmerge(id, store.device) } },
                    onRemove = { id -> edit { it.removeMember(id, store.device) } },
                    onMe = { id -> store.setMe(k, id); meVersion++ },
                    onAdd = { n -> edit { it.addMember(store.newId(), n, store.device) } },
                    onBack = { screen = Screen.SETTINGS },
                )
            }
            Screen.SETTINGS -> {
                val g = group ?: return@AappsTheme Reset { open(null) }
                BackEffect { screen = Screen.GROUP }
                DividrSettings(
                    g, hapticsOn,
                    onHaptics = { hapticsOn = it; store.haptics = it },
                    onRename = { n -> edit { it.setMeta(n, it.currency, store.device) } },
                    onCurrency = { c -> edit { it.setMeta(it.name, c, store.device) } },
                    onPeople = { screen = Screen.PEOPLE },
                    onGroups = { screen = Screen.GROUPS },
                    onLeave = {
                        val k = key
                        if (k != null) {
                            store.delete(k)
                            groups = groups - k
                        }
                        open(groups.keys.sorted().firstOrNull())
                    },
                    onAbout = { screen = Screen.ABOUT },
                    onBack = { screen = Screen.GROUP },
                )
            }
            Screen.ABOUT -> {
                BackEffect { screen = Screen.SETTINGS }
                DividrAbout(versionName) { screen = Screen.SETTINGS }
            }
        }
    }
}

// Zustand passt nicht (z. B. Gruppe weg): nach dem Zeichnen zuruecksetzen, nicht waehrenddessen
@Composable
private fun Reset(action: () -> Unit) {
    LaunchedEffect(Unit) { action() }
}

// Abrechnung als Klartext fuer den Messenger; total = Vorlage mit %1$s
fun shareText(g: Group, locale: Locale, total: String, allSquare: String, footer: String): String {
    val plan = Settle.plan(g.balances())
    val lines = ArrayList<String>()
    lines += g.name
    lines += total.replace("%1\$s", Money.format(g.totalSpent(), g.currency, locale))
    lines += ""
    if (plan.isEmpty()) {
        lines += allSquare
    } else {
        plan.forEach { t -> lines += "${g.displayName(t.from)} → ${g.displayName(t.to)}: ${Money.format(t.amount, g.currency, locale)}" }
    }
    lines += ""
    lines += footer
    return lines.joinToString("\n")
}
