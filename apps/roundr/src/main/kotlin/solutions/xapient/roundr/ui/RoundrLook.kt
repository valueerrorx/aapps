package solutions.xapient.roundr.ui

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
import solutions.xapient.roundr.R
import solutions.xapient.roundr.domain.Phase
import solutions.xapient.roundr.domain.Workout

// Glut fuer Arbeit, Eis fuer Pause; Glut ist zugleich die App-Farbe
val Ember = Color(0xFFFF6B3D)
val Ice = Color(0xFF4FC3F7)
val Amber = Color(0xFFF2B84E)
val RoundrAccent = Ember

// Dunkle Schrift auf den Farbflaechen
val Ink = Color(0xFF0E1318)
val Bg = Color(0xFF10161B)

fun phaseColor(p: Phase): Color = when (p) {
    Phase.PREP -> Amber
    Phase.WORK -> Ember
    Phase.REST -> Ice
    Phase.SET_REST -> Ice
}

// Geleerter Teil der Flaeche: Phasenfarbe tief abgedunkelt
fun drained(c: Color): Color = lerp(Bg, c, 0.16f)

// Angehalten: Farbe zurueckgenommen
fun dimmed(c: Color): Color = lerp(Bg, c, 0.62f)

@Composable
fun phaseLabel(p: Phase): String = stringResource(
    when (p) {
        Phase.PREP -> R.string.phase_prep
        Phase.WORK -> R.string.phase_work
        Phase.REST -> R.string.phase_rest
        Phase.SET_REST -> R.string.phase_set_rest
    },
)

// Vorlagen-Namen lokalisiert, eigene wie gespeichert, ohne Namen „Eigenes"
@Composable
fun workoutName(w: Workout): String = when (w.name) {
    "@tabata" -> stringResource(R.string.preset_tabata)
    "@tabata3" -> stringResource(R.string.preset_tabata3)
    "@hiit" -> stringResource(R.string.preset_hiit)
    "@emom" -> stringResource(R.string.preset_emom)
    "@boxing" -> stringResource(R.string.preset_boxing)
    "" -> stringResource(R.string.custom)
    else -> w.name
}

// Linien-Icons nur fuer roundr, gleicher Strich wie AappsIcons
object RoundrIcons {
    val Play: ImageVector by lazy { line("play") { moveTo(8f, 5.5f); lineTo(18.5f, 12f); lineTo(8f, 18.5f); close() } }
    val Pause: ImageVector by lazy { line("pause") { moveTo(9f, 6f); lineTo(9f, 18f); moveTo(15f, 6f); lineTo(15f, 18f) } }
    val Next: ImageVector by lazy {
        line("next") { moveTo(6.5f, 6.5f); lineTo(14f, 12f); lineTo(6.5f, 17.5f); close(); moveTo(17.5f, 6f); lineTo(17.5f, 18f) }
    }
    val Prev: ImageVector by lazy {
        line("prev") { moveTo(17.5f, 6.5f); lineTo(10f, 12f); lineTo(17.5f, 17.5f); close(); moveTo(6.5f, 6f); lineTo(6.5f, 18f) }
    }
    val Close: ImageVector by lazy { line("close") { moveTo(7f, 7f); lineTo(17f, 17f); moveTo(17f, 7f); lineTo(7f, 17f) } }
    val Plus: ImageVector by lazy { line("plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Minus: ImageVector by lazy { line("minus") { moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Down: ImageVector by lazy { line("down") { moveTo(6.5f, 9.5f); lineTo(12f, 15f); lineTo(17.5f, 9.5f) } }

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
