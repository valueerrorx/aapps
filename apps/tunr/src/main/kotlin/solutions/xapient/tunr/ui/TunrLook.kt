package solutions.xapient.tunr.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import solutions.xapient.tunr.R
import solutions.xapient.tunr.domain.Instrument
import solutions.xapient.tunr.domain.Notation
import solutions.xapient.tunr.domain.Notes
import kotlin.math.abs

// Gruen laut Markenfarben: gestimmt; Bernstein und Koralle zeigen die Abweichung
val TunrGreen = Color(0xFF4DF0A2)
val TunrAccent = TunrGreen
val Amber = Color(0xFFF2B84E)
val Coral = Color(0xFFFF7B6B)
val Bg = Color(0xFF10161B)
val FgFaint = Color(0xFF55626D)
val FgMuted = Color(0xFF8A97A2)

// Zustandsfarbe stufenlos: bis 5 Cent gruen, dann bernstein, ab 35 Cent koralle
fun stateColor(cents: Float): Color {
    val a = abs(cents)
    return when {
        a <= 5f -> TunrGreen
        a <= 15f -> lerp(TunrGreen, Amber, (a - 5f) / 10f)
        a <= 35f -> lerp(Amber, Coral, (a - 15f) / 20f)
        else -> Coral
    }
}

// Ruhezustand (kein Ton): Strobe und Skala gedimmt, aber >= 3:1 zum Grund (ContrastTest)
const val IDLE_MIX = 0.66f
fun idleLine(): Color = lerp(Bg, FgMuted, IDLE_MIX)
// Zone "gestimmt" mit Ton, noch nicht eingerastet (ohne Ton: idleLine)
const val ZONE_DIM = 0.55f
fun zoneDim(): Color = lerp(Bg, TunrGreen, ZONE_DIM)

// Lichthof hinter dem Hero: Grund, Ton, eingerastet; Summe haelt Text >= 4,5:1
const val GLOW_BASE = 0.02f
const val GLOW_TONE = 0.04f
const val GLOW_LOCK = 0.06f

@Composable
fun instrumentLabel(i: Instrument): String = stringResource(
    when (i) {
        Instrument.CHROMATIC -> R.string.inst_chromatic
        Instrument.GUITAR -> R.string.inst_guitar
        Instrument.GUITAR_DROP_D -> R.string.inst_drop_d
        Instrument.BASS -> R.string.inst_bass
        Instrument.UKULELE -> R.string.inst_ukulele
        Instrument.VIOLIN -> R.string.inst_violin
    },
)

// "E2 A2 D3 …" in der gewaehlten Schreibweise
fun stringsText(i: Instrument, n: Notation): String = i.strings.joinToString("  ") { Notes.name(it, n).toString() }

// Linien-Icons nur fuer tunr, gleicher Strich wie AappsIcons
object TunrIcons {
    // Stimmgabel: zwei Zinken laufen in einen Stiel zusammen
    val Fork: ImageVector by lazy {
        line("fork") {
            moveTo(8.5f, 3.5f); lineTo(8.5f, 10.5f)
            arcTo(3.5f, 3.5f, 0f, false, false, 15.5f, 10.5f)
            lineTo(15.5f, 3.5f)
            moveTo(12f, 14f); lineTo(12f, 20.5f)
        }
    }
    val Mic: ImageVector by lazy {
        line("mic") {
            moveTo(9f, 6.5f)
            arcTo(3f, 3f, 0f, false, true, 15f, 6.5f)
            lineTo(15f, 11.5f)
            arcTo(3f, 3f, 0f, false, true, 9f, 11.5f)
            close()
            moveTo(5.5f, 11f)
            arcTo(6.5f, 6.5f, 0f, false, false, 18.5f, 11f)
            moveTo(12f, 17.5f); lineTo(12f, 20.5f)
        }
    }
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Minus: ImageVector by lazy { line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) } }

    private fun line(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        ).build()
}
