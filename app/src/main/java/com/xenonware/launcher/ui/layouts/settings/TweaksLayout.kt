package com.xenonware.launcher.ui.layouts.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun TweaksLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize,
) {
    DeviceConfigProvider(appSize = appSize) {

        val configuration = LocalConfiguration.current
        val isCompact =
            LocalDeviceConfig.current.isCommunicator || LocalDeviceConfig.current.isMindOne
        val appHeight = configuration.screenHeightDp.dp

        val isAppBarExpandable = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> !isLandscape && !isCompact && appHeight >= 460.dp
            LayoutType.MEDIUM -> true
            LayoutType.EXPANDED -> true
        }

        val hazeState = rememberHazeState()

        ActivityScreen(
            titleText = stringResource(id = R.string.tweaks),

            expandable = isAppBarExpandable,

            navigationIconStartPadding = MediumPadding,
            navigationIconPadding = MediumPadding,
            navigationIconSpacing = NoSpacing,
            navigationIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back_description),
                    modifier = Modifier.size(24.dp)
                )
            },
            onNavigationIconClick = onNavigateBack,
            hasNavigationIconExtraContent = false,
            actions = {},
            modifier = Modifier.hazeSource(hazeState),
            content = { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = LargestPadding,
                            end = LargestPadding,
                            top = LargestPadding,
                            bottom = WindowInsets.safeDrawing.asPaddingValues()
                                .calculateBottomPadding() + LargestPadding
                        )
                ) {
                    TweaksItems(
                        viewModel = viewModel,
                        layoutType = layoutType
                    )
                }
            })
    }
}
