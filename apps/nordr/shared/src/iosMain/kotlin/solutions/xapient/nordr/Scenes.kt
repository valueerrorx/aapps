package solutions.xapient.nordr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import solutions.xapient.aapps.core.store.StoreAccess
import solutions.xapient.aapps.core.store.StoreState
import solutions.xapient.aapps.core.ui.AappsTheme
import solutions.xapient.aapps.core.ui.StoreScreen
import solutions.xapient.aapps.core.ui.decimalSeparator
import solutions.xapient.nordr.domain.Accuracy
import solutions.xapient.nordr.domain.CompassReading
import solutions.xapient.nordr.domain.NorthRef
import solutions.xapient.nordr.domain.Region
import solutions.xapient.nordr.domain.Regions
import solutions.xapient.nordr.domain.Wmm
import solutions.xapient.nordr.ui.CompassScreen
import solutions.xapient.nordr.ui.CompassState
import solutions.xapient.nordr.ui.NordrAbout
import solutions.xapient.nordr.ui.NordrSettings
import solutions.xapient.nordr.ui.RegionPicker

// Feste Zustaende fuer App-Store-Screenshots (Simulator hat keinen Kompass), wie die Roborazzi-Tests auf Android
internal enum class Scene {
    HEADING, COURSE, ON_COURSE, TRUE_NORTH, CALIBRATE, INTERFERENCE, REGION, SETTINGS, ABOUT, OFFER, THANKS;

    companion object {
        fun of(name: String?): Scene? = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

// fester Zeitpunkt, damit die Deklination im Bild stabil bleibt (1. Juni 2026)
private const val FIXED_TIME = 1_780_272_000_000L

private fun declination(r: Region) = Wmm.declination(r.lat, r.lon, FIXED_TIME)

@Composable
internal fun SceneContent(scene: Scene, versionName: String) {
    AappsTheme(NordrAccent) {
        val vienna = remember { Regions.byId("vienna")!! }
        when (scene) {
            // 247° WSW, Rose schwingt noch nach, Handy leicht gekippt
            Scene.HEADING -> Compass(remember {
                state(CompassReading(valid = true, heading = 247f, magnetic = 247f)).apply {
                    physics.setTilt(0.12f, 0.3f)
                    physics.dial.pose(253f, -18f)
                }
            })
            Scene.COURSE -> Compass(remember {
                state(CompassReading(valid = true, heading = 232f, magnetic = 232f, course = 250f, deviation = 18f)).apply {
                    physics.setTilt(0f, 0.2f)
                }
            })
            Scene.ON_COURSE -> Compass(
                remember { state(CompassReading(valid = true, heading = 250f, magnetic = 250f, course = 250f, onCourse = true)) },
                shine = 0.4f,
            )
            Scene.TRUE_NORTH -> Compass(
                remember { state(CompassReading(valid = true, heading = 4f, magnetic = 359f)) },
                NorthRef.TRUE,
            )
            Scene.CALIBRATE -> Compass(remember {
                state(CompassReading(valid = true, heading = 81f, magnetic = 81f, accuracy = Accuracy.LOW))
            })
            Scene.INTERFERENCE -> Compass(
                remember { state(CompassReading(valid = true, heading = 140f, magnetic = 140f, accuracy = Accuracy.INTERFERENCE)) },
                phase = 0.25f,
            )
            Scene.REGION -> RegionPicker(vienna, ::declination, {}, {})
            Scene.SETTINGS -> NordrSettings(
                northRef = NorthRef.TRUE, onNorthRef = {}, region = vienna, declination = declination(vienna),
                onRegion = {}, haptics = true, onHaptics = {}, onShowCalibration = {}, onAbout = {}, onBack = {},
            )
            Scene.ABOUT -> NordrAbout(versionName) {}
            Scene.OFFER -> Store(StoreAccess.OFFER)
            Scene.THANKS -> Store(StoreAccess.EXPIRED)
        }
    }
}

private fun state(r: CompassReading) = CompassState().apply { show(r) }

@Composable
private fun Compass(state: CompassState, ref: NorthRef = NorthRef.MAGNETIC, shine: Float = -1f, phase: Float = 0.3f) =
    CompassScreen(state, ref, null, {}, {}, animate = false, shineOverride = shine, hintPhase = phase)

@Composable
private fun Store(access: StoreAccess) {
    // Preis wie StoreKit ihn liefern wuerde, passend zur Sprache
    val price = if (decimalSeparator() == ',') "1,49 €" else "€1.49"
    StoreScreen(APP_NAME, StoreState(access, price = price, trialAvailable = true), {}, {}, {})
}
