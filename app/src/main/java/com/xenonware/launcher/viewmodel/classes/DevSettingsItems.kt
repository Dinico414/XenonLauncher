package com.xenonware.launcher.viewmodel.classes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.values.LargestPadding
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel

@Composable
fun DevSettingsItems(
    settingsViewModel: SettingsViewModel,
    viewModel: DevSettingsViewModel,
) {
    val devModeEnabled by viewModel.devModeToggleState.collectAsState()

    Column(modifier = Modifier.padding(LargestPadding)) {
        SettingsSwitchTile(
            title = "Enable Developer Mode",
            checked = devModeEnabled,
            onCheckedChange = { viewModel.setDeveloperModeEnabled(it) }
        )
        
        Text(text = "More developer options coming soon...", modifier = Modifier.padding(top = LargestPadding))
    }
}
