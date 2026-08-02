package com.xenonware.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.ViewModelProvider
import com.xenonware.launcher.ui.layouts.dev_settings.DevSettingsLayout
import com.xenonware.launcher.ui.theme.ScreenEnvironment
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel

class DevSettingsActivity : ComponentActivity() {

    private lateinit var devSettingsViewModel: DevSettingsViewModel
    private lateinit var mainSettingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainSettingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.SettingsViewModelFactory(application)
        )[SettingsViewModel::class.java]

        devSettingsViewModel = ViewModelProvider(this)[DevSettingsViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            val activeNightMode by mainSettingsViewModel.activeNightModeFlag.collectAsState()
            val currentContainerSize = LocalWindowInfo.current.containerSize
            LaunchedEffect(activeNightMode) {
                AppCompatDelegate.setDefaultNightMode(activeNightMode)
            }

            val persistedAppThemeIndex by mainSettingsViewModel.persistedThemeIndex.collectAsState()
            val blackedOutEnabled by mainSettingsViewModel.blackedOutModeEnabled.collectAsState()
            val coverThemeEnabled by mainSettingsViewModel.enableCoverTheme.collectAsState()
            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = mainSettingsViewModel.applyCoverTheme(containerSize)


            ScreenEnvironment(
                persistedAppThemeIndex, applyCoverTheme, blackedOutEnabled
            ) { layoutType, isLandscape ->
                DevSettingsLayout(
                    onNavigateBack = { finish() },
                    viewModel = devSettingsViewModel,
                    isLandscape = isLandscape,
                    layoutType = layoutType,
                    appSize = currentContainerSize
                )
            }
        }
    }
}
