package solutions.xapient.hudr.ui

import org.jetbrains.compose.resources.StringResource
import solutions.xapient.hudr.res.Res
import solutions.xapient.hudr.res.gps_off_text_ios
import solutions.xapient.hudr.res.loc_blocked_text_ios
import solutions.xapient.hudr.res.note_location_ios

internal actual object HudrTexts {
    actual val noteLocation: StringResource get() = Res.string.note_location_ios
    actual val locBlocked: StringResource get() = Res.string.loc_blocked_text_ios
    actual val gpsOff: StringResource get() = Res.string.gps_off_text_ios
}
