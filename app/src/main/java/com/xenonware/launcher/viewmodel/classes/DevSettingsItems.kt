package com.xenonware.launcher.viewmodel.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.values.LargestPadding
import com.xenonware.launcher.ui.res.SettingsTileContext
import com.xenonware.launcher.ui.res.XenonDialog
import com.xenonware.launcher.ui.res.XenonIcon
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel

@Composable
fun DevSettingsItems(
    settingsViewModel: SettingsViewModel,
    viewModel: DevSettingsViewModel,
) {
    val devModeEnabled by viewModel.devModeToggleState.collectAsState()
    val crashLogExists by viewModel.crashLogExists.collectAsState()
    
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var currentCrashLog by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(LargestPadding)) {
        SettingsSwitchTile(
            title = "Enable Developer Mode",
            checked = devModeEnabled,
            onCheckedChange = { viewModel.setDeveloperModeEnabled(it) }
        )

        val crashLogSubtitle = if (crashLogExists) "Recent crashes recorded." else "No crashes recorded."
        
        SettingsTileContext(
            title = "Crash Logs",
            subtitle = crashLogSubtitle,
            icon = { XenonIcon(Icons.Rounded.BugReport).Render(Modifier) },
            showContext = true,
            modifier = Modifier.padding(top = LargestPadding),
            contextContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { 
                            currentCrashLog = viewModel.readCrashLog()
                            showCrashLogDialog = true 
                        },
                        enabled = crashLogExists
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            XenonIcon(Icons.Rounded.Description).Render(Modifier)
                            Text("View")
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.shareCrashLog() },
                        enabled = crashLogExists
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            XenonIcon(Icons.Rounded.Share).Render(Modifier)
                            Text("Share")
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.clearCrashLog() },
                        enabled = crashLogExists,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            XenonIcon(Icons.Rounded.Delete).Render(Modifier)
                            Text("Clear")
                        }
                    }
                }
            }
        )
        
        if (showCrashLogDialog) {
            XenonDialog(
                onDismissRequest = { showCrashLogDialog = false },
                title = "Crash Log",
                confirmButtonText = "Close",
                onConfirmButtonClick = { showCrashLogDialog = false }
            ) {
                Text(
                    text = currentCrashLog,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
