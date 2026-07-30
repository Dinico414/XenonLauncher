package com.xenonware.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenonware.launcher.viewmodel.LayoutType

val LocalLayoutType = staticCompositionLocalOf { LayoutType.COMPACT }

@Composable
fun ScreenEnvironment(
    persistedAppThemeIndex: Int,
    applyCoverTheme: Boolean,
    blackedOutEnabled: Boolean,
    content: @Composable (layoutType: LayoutType, isLandscape: Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    // Simple layout type logic for now
    val layoutType = if (applyCoverTheme) LayoutType.COVER else LayoutType.COMPACT
    
    CompositionLocalProvider(LocalLayoutType provides layoutType) {
        content(layoutType, isLandscape)
    }
}
