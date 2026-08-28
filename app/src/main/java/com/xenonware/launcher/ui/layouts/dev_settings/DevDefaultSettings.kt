package com.xenonware.launcher.ui.layouts.dev_settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel
import com.xenonware.launcher.viewmodel.classes.DevSettingsItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun DevDefaultSettings(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize,
) {
    DeviceConfigProvider(appSize = appSize) {

        val hazeState = rememberHazeState()
        val context = LocalContext.current

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

        ActivityScreen(
            titleText = stringResource(id = R.string.developer_options_title),

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
            actions = {
                IconButton(onClick = {
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(R.string.restart_app)
                    )
                }
            },
            modifier = Modifier.hazeSource(hazeState),
            content = { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    DevSettingsItems(
                        settingsViewModel = settingsViewModel,
                        viewModel = viewModel,
                    )
                }
            }

        )
    }
}
