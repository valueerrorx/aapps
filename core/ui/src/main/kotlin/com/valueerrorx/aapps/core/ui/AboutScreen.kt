package com.valueerrorx.aapps.core.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

const val SOURCE_URL = "https://github.com/valueerrorx/aapps"

// Info-Seite: Version, Versprechen, Lizenz, Quellcode
@Composable
fun AboutScreen(
    appName: String,
    versionName: String,
    tagline: String,
    onBack: () -> Unit,
    notes: List<String> = emptyList(),
    icon: @Composable () -> Unit = {},
) {
    val c = Aapps.colors
    val context = LocalContext.current
    AappsScaffold {
        Column(Modifier.fillMaxWidth()) {
            AappsTopBar(null, onBack, stringResource(R.string.aapps_back))
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            ) {
                Box(Modifier.glow(c.accent, 90.dp, 0.22f)) { icon() }
                Spacer(Modifier.height(20.dp))
                BasicText(appName, style = Aapps.type.headline.copy(color = c.fg))
                Spacer(Modifier.height(4.dp))
                BasicText(
                    stringResource(R.string.aapps_version, versionName),
                    style = Aapps.type.number.copy(color = c.accent, fontSize = Aapps.type.label.fontSize * 1.1f),
                )
                Spacer(Modifier.height(16.dp))
                BasicText(tagline, style = Aapps.type.body.copy(color = c.fgMuted))
                Spacer(Modifier.height(28.dp))
                SettingsGroup(null) {
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Promise(stringResource(R.string.aapps_promise_ads))
                        Promise(stringResource(R.string.aapps_promise_tracking))
                        Promise(stringResource(R.string.aapps_promise_internet))
                    }
                }
                Spacer(Modifier.height(20.dp))
                SettingsGroup(null) {
                    SettingsAction(stringResource(R.string.aapps_source), SOURCE_URL.removePrefix("https://")) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // kein Browser installiert: nichts tun
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                val small = Aapps.type.body.copy(color = c.fgFaint, fontSize = Aapps.type.label.fontSize * 1.1f)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    notes.forEach { BasicText(it, style = small) }
                    BasicText(stringResource(R.string.aapps_license), style = small)
                    BasicText(stringResource(R.string.aapps_fonts), style = small)
                }
            }
        }
    }
}

@Composable
private fun Promise(text: String) {
    val c = Aapps.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).background(c.accent.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            AappsIcon(AappsIcons.Check, null, c.accent, size = 16.dp)
        }
        Spacer(Modifier.width(14.dp))
        BasicText(text, style = Aapps.type.body.copy(color = c.fg))
    }
}
