package org.lyaaz.fucklauncher

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.lyaaz.fucklauncher.ui.AppTheme as Theme

class SettingsActivity : ComponentActivity() {

    private var currentUiMode: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentUiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        setContent {
            Theme {
                SettingsScreen()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newUiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (newUiMode != currentUiMode) {
            recreate()
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    Theme {
        SettingsScreen()
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = Utils.getPrefs(context)
    val settings = Settings.getInstance(prefs)
    var enableDoubleTab2Sleep by remember {
        mutableStateOf(settings.enableDoubleTapToSleep())
    }
    var enableForceMonoIcon by remember {
        mutableStateOf(settings.enableForcedMonoIcon())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
    ) {
        item {
            SwitchPreferenceItem(
                title = R.string.title_enable_double_tap_to_sleep,
                checked = enableDoubleTab2Sleep,
                onCheckedChange = {
                    enableDoubleTab2Sleep = it
                    prefs.edit {
                        putBoolean(Settings.PREF_ENABLE_DOUBLE_TAP_TO_SLEEP, it)
                    }
                }
            )
        }
        item {
            SwitchPreferenceItem(
                title = R.string.title_enable_forced_mono_icon,
                summary = R.string.summary_enable_forced_mono_icon,
                checked = enableForceMonoIcon,
                onCheckedChange = {
                    enableForceMonoIcon = it
                    prefs.edit {
                        putBoolean(Settings.PREF_ENABLE_FORCED_MONO_ICON, it)
                    }
                }
            )
        }
    }
}

@Composable
fun SwitchPreferenceItem(
    @StringRes title: Int,
    @StringRes summary: Int? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    noSwitch: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(if (enabled) 1.0f else 0.6f)
                )
                if (summary != null) {
                    Text(
                        text = stringResource(id = summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(if (enabled) 1.0f else 0.6f)
                    )
                }
            }
            if (!noSwitch) {
                Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
            }
        }
    }
}