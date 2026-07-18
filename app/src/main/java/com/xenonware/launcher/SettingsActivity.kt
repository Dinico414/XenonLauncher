package com.xenonware.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import com.xenonware.launcher.ui.layouts.settings.SettingsLayout
import com.xenonware.launcher.ui.theme.XenonLauncherTheme
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState

class SettingsActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XenonLauncherTheme {
                val hazeState = remember { HazeState() }
                SettingsLayout(
                    onNavigateBack = { finish() },
                    viewModel = viewModel,
                    hazeState = hazeState
                )
            }
        }
    }
}
