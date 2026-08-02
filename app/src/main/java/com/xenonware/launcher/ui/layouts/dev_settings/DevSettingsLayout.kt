package com.xenonware.launcher.ui.layouts.dev_settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xenon.mylibrary.theme.LayoutType
import com.xenonware.launcher.viewmodel.DevSettingsViewModel

@Composable
fun DevSettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    modifier: Modifier = Modifier,
    appSize: IntSize,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                DevCoverSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                )
            }

            else -> {
                DevDefaultSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = isLandscape,
                    appSize = appSize
                )
            }
        }
    }
}
