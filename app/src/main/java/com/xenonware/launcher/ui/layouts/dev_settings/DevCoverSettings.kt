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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel
import com.xenonware.launcher.viewmodel.classes.DevSettingsItems
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun DevCoverSettings(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val hazeState = rememberHazeState()
    val context = LocalContext.current

    ActivityScreen(
        titleText = stringResource(id = R.string.developer_options_title),

        expandable = false,

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
