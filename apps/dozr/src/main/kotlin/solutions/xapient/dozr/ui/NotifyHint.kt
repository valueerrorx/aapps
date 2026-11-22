package solutions.xapient.dozr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import solutions.xapient.aapps.core.ui.Aapps
import solutions.xapient.aapps.core.ui.AappsPillButton
import solutions.xapient.dozr.R

// Eigene Erklaerung vor der zweiten System-Frage nach Benachrichtigungen
@Composable
fun NotifyHint(onContinue: () -> Unit, onDismiss: () -> Unit) {
    val c = Aapps.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .background(c.surface, RoundedCornerShape(24.dp))
                .border(1.dp, c.line, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(stringResource(R.string.notify_why), style = Aapps.type.body.copy(color = c.fg))
            AappsPillButton(stringResource(R.string.notify_ok), onContinue, active = true)
        }
    }
}
