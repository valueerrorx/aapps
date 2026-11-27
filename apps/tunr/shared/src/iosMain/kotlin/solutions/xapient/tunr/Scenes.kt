package solutions.xapient.tunr

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notes
import solutions.xapient.tunr.domain.Reading
import solutions.xapient.tunr.domain.Settings
import solutions.xapient.tunr.ui.InstrumentScreen
import solutions.xapient.tunr.ui.MicUi
import solutions.xapient.tunr.ui.TunerScreen
import solutions.xapient.tunr.ui.TunerState
import solutions.xapient.tunr.ui.TunrAbout
import solutions.xapient.tunr.ui.TunrAccent
import solutions.xapient.tunr.ui.TunrSettings
import kotlin.math.abs
import kotlin.math.pow

// Feste Zustaende fuer App-Store-Screenshots, wie die Roborazzi-Tests auf Android
internal enum class Scene {
    IN_TUNE, FLAT_GUITAR, REFERENCE_TONE, SHARP, LISTEN, MIC_ASK, INSTRUMENTS, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

private fun tone(midi: Int, cents: Float, string: Int = -1) = Reading(
    active = true, heard = true, hz = (Notes.hz(midi, 440f) * 2.0.pow(cents / 1200.0)).toFloat(),
    target = midi, cents = cents, string = string, inTune = abs(cents) <= 5f, locks = 1,
)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    // deutsche Notenschrift (H statt B) in deutscher Sprache, wie die Voreinstellung
    val notation = Settings.notationFor(Locale.current.language)
    val chromatic = Settings(notation = notation)
    val guitar = Settings(notation = notation, instrument = Instrument.GUITAR)
    AappsTheme(TunrAccent) {
        when (scene) {
            Scene.IN_TUNE -> Tuner(chromatic, tone(69, 1f), strobe = 4f, shine = 0.35f)
            Scene.FLAT_GUITAR -> Tuner(guitar, tone(45, -23f, string = 1), strobe = 21f, locked = 1)
            Scene.REFERENCE_TONE -> Tuner(guitar, Reading(heard = true, target = 45, string = 1), locked = 1, tone = 1)
            Scene.SHARP -> Tuner(chromatic, tone(54, 38f), strobe = 40f)
            Scene.LISTEN -> Tuner(chromatic)
            Scene.MIC_ASK -> Tuner(chromatic, mic = MicUi.ASK)
            Scene.INSTRUMENTS -> InstrumentScreen(Instrument.GUITAR, notation, {}, {})
            Scene.SETTINGS -> TunrSettings(chromatic, {}, {}, {})
            Scene.ABOUT -> TunrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

@Composable
private fun Tuner(s: Settings, r: Reading = Reading(), mic: MicUi = MicUi.ON, strobe: Float = 0f, shine: Float = 1f, locked: Int = -1, tone: Int = -1) {
    val state = TunerState(s.a4, s.instrument).apply {
        show(r, strobe, shine)
        if (tone >= 0) showTone(tone, 0.8f)
    }
    TunerScreen(state, s, mic, locked, null, {}, {}, {}, {}, animate = false)
}

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen("tunr", StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
