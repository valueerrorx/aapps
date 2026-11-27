package solutions.xapient.hudr.ui

import org.jetbrains.compose.resources.StringResource
import solutions.xapient.hudr.res.Res
import solutions.xapient.hudr.res.gps_off_text
import solutions.xapient.hudr.res.loc_blocked_text
import solutions.xapient.hudr.res.note_location

internal actual object HudrTexts {
    actual val noteLocation: StringResource get() = Res.string.note_location
    actual val locBlocked: StringResource get() = Res.string.loc_blocked_text
    actual val gpsOff: StringResource get() = Res.string.gps_off_text
}
