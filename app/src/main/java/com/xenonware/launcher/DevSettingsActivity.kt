package com.xenonware.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.ui.layouts.dev_settings.DevSettingsLayout
import com.xenonware.launcher.ui.theme.ScreenEnvironment
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel

class DevSettingsActivity : ComponentActivity() {

    private lateinit var devSettingsViewModel: DevSettingsViewModel
    private lateinit var mainSettingsViewModel: SettingsViewModel
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        mainSettingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.SettingsViewModelFactory(application)
        )[SettingsViewModel::class.java]

        devSettingsViewModel = ViewModelProvider(this)[DevSettingsViewModel::class.java]
        sharedPreferenceManager = SharedPreferenceManager(applicationContext)

        val initialThemePref = sharedPreferenceManager.theme
        val initialCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val initialBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        updateAppCompatDelegateTheme(initialThemePref)

        lastAppliedTheme = initialThemePref
        lastAppliedCoverThemeEnabled = initialCoverThemeEnabledSetting
        lastAppliedBlackedOutMode = initialBlackedOutMode

        setContent {
            val currentContainerSize = LocalWindowInfo.current.containerSize
            
            val activeNightMode by mainSettingsViewModel.activeNightModeFlag.collectAsState()
            LaunchedEffect(activeNightMode) {
                AppCompatDelegate.setDefaultNightMode(activeNightMode)
            }

            val themePref by mainSettingsViewModel.persistedThemeIndex.collectAsState()
            val blackedOut by mainSettingsViewModel.blackedOutModeEnabled.collectAsState()
            val coverThemeEnabled by mainSettingsViewModel.enableCoverTheme.collectAsState()

            val applyCoverTheme = remember(currentContainerSize, coverThemeEnabled) {
                mainSettingsViewModel.applyCoverTheme(currentContainerSize)
            }

            ScreenEnvironment(
                themePreference = themePref,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOut
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

    override fun onResume() {
        super.onResume()
        devSettingsViewModel.updateCrashLogStatus()

        val currentThemePref = sharedPreferenceManager.theme
        val currentCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val currentBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        if (currentThemePref != lastAppliedTheme ||
            currentCoverThemeEnabledSetting != lastAppliedCoverThemeEnabled ||
            currentBlackedOutMode != lastAppliedBlackedOutMode
        ) {
            if (currentThemePref != lastAppliedTheme) {
                updateAppCompatDelegateTheme(currentThemePref)
            }

            lastAppliedTheme = currentThemePref
            lastAppliedCoverThemeEnabled = currentCoverThemeEnabledSetting
            lastAppliedBlackedOutMode = currentBlackedOutMode

            recreate()
        }
    }

    private fun updateAppCompatDelegateTheme(themePref: Int) {
        if (themePref >= 0 && themePref < sharedPreferenceManager.themeFlag.size) {
            AppCompatDelegate.setDefaultNightMode(sharedPreferenceManager.themeFlag[themePref])
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
