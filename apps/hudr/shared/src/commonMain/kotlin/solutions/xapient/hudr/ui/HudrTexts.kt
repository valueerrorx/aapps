package solutions.xapient.hudr.ui

import org.jetbrains.compose.resources.StringResource

// Texte, die je Plattform anders lauten (Berechtigungen, Einstellungen)
internal expect object HudrTexts {
    val noteLocation: StringResource
    val locBlocked: StringResource
    val gpsOff: StringResource
}
