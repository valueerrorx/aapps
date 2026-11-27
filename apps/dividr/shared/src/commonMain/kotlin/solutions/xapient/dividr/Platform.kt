package solutions.xapient.dividr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import solutions.xapient.dividr.domain.Group
import solutions.xapient.dividr.domain.Locale
import solutions.xapient.dividr.ui.CameraUi

/** Gruppen-Dateien und lokale Angaben; Android Dateien + SharedPreferences, iOS Dateien + UserDefaults */
interface DividrStore {
    val device: Int
    fun newId(): Long
    fun newGroupId(): Long
    fun keyFor(g: Group): String = g.ids.min().toULong().toString(16)
    fun loadAll(): Map<String, Group>
    fun save(key: String, g: Group)
    fun delete(key: String)
    var lastKey: String?
    fun me(key: String): Long?
    fun setMe(key: String, id: Long?)
    fun lastRate(currency: String): Long?
    fun setLastRate(currency: String, micros: Long)
    var haptics: Boolean
    var cameraBlocked: Boolean
}

/** Kamera-Erlaubnis; [state] ist Compose-State */
interface CameraAccess {
    val state: CameraUi
    fun refresh()
    fun allow()
}

@Composable
expect fun rememberCameraAccess(store: DividrStore): CameraAccess

/** Kamera-Vorschau, meldet gelesene QR-Texte */
@Composable
expect fun CameraScan(onText: (String) -> Unit, onError: () -> Unit, modifier: Modifier = Modifier)

/** Text ueber das System teilen (Messenger) */
@Composable
expect fun rememberShare(): (String) -> Unit

/** Volle Bildschirmhelligkeit, solange sichtbar */
@Composable
expect fun FullBrightness()

@Composable
expect fun currentLocale(): Locale

/** Tage seit 1970 in der lokalen Zeitzone */
expect fun todayEpochDay(): Int

/** Datum kurz, im laufenden Jahr ohne Jahreszahl („Mo. 3. Nov.“ / „3. Nov. 2025“) */
expect fun formatDay(day: Int, today: Int, locale: Locale): String
