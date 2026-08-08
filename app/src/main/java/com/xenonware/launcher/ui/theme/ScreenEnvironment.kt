@file:Suppress("DEPRECATION")

package com.xenonware.launcher.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.mylibrary.theme.LayoutType

@Composable
fun ScreenEnvironment(
    themePreference: Int,
    coverTheme: Boolean,
    blackedOutModeEnabled: Boolean,
    statusBarDarkIconsOverride: Boolean? = null,
    navigationBarDarkIconsOverride: Boolean? = null,
    content: @Composable (layoutType: LayoutType, isLandscape: Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val useDynamicColor = true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val dimensionForLayout = if (isLandscape) screenHeight else screenWidth

        val layoutType = when {
            coverTheme -> LayoutType.COVER
            dimensionForLayout < 320.dp -> LayoutType.SMALL
            dimensionForLayout < 600.dp -> LayoutType.COMPACT
            dimensionForLayout < 840.dp -> LayoutType.MEDIUM
            else -> LayoutType.EXPANDED
        }

        val appIsDarkTheme = when {
            layoutType == LayoutType.COVER -> true
            else -> when (themePreference) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }
        }

        XenonTheme(
            darkTheme = appIsDarkTheme,
            useBlackedOutDarkTheme = if (appIsDarkTheme) blackedOutModeEnabled else false,
            dynamicColor = useDynamicColor,
            isCoverMode = layoutType == LayoutType.COVER
        ) {
            val systemUiController = rememberSystemUiController()
            val view = LocalView.current

            val darkIconsForSystemBars =
                if (layoutType == LayoutType.COVER) false else !appIsDarkTheme

            if (!view.isInEditMode) {
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = statusBarDarkIconsOverride ?: darkIconsForSystemBars
                    )
                    systemUiController.setNavigationBarColor(
                        color = Color.Transparent,
                        darkIcons = navigationBarDarkIconsOverride ?: darkIconsForSystemBars,
                        navigationBarContrastEnforced = false
                    )
                }
            }
            content(layoutType, isLandscape)
        }
    }
}
