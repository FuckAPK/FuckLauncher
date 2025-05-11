package org.lyaaz.fucklauncher

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import org.lyaaz.ui.SwitchPreferenceItem
import org.lyaaz.ui.theme.AppTheme as Theme


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

    var enableAutoHide by remember {
        mutableStateOf(settings.enableAutoHide())
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
        item {
            SwitchPreferenceItem(
                title = R.string.title_enable_auto_hide,
                summary = R.string.summary_enable_auto_hide,
                checked = enableAutoHide,
                onCheckedChange = {
                    enableAutoHide = it
                    prefs.edit {
                        putBoolean(Settings.PREF_ENABLE_AUTO_HIDE, it)
                    }
                }
            )
        }
    }
}