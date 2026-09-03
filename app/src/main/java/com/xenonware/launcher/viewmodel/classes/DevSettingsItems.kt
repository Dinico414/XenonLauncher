package com.xenonware.launcher.viewmodel.classes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTileContext
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonIcon
import com.xenon.mylibrary.values.LargestPadding
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.DevSettingsViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun DevSettingsItems(
    settingsViewModel: SettingsViewModel,
    viewModel: DevSettingsViewModel,
) {
    val devModeEnabled by viewModel.devModeToggleState.collectAsState()
    val crashLogExists by viewModel.crashLogExists.collectAsState()
    
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var currentCrashLog by remember { mutableStateOf("") }

    var showMediaDumpDialog by remember { mutableStateOf(false) }
    var currentMediaDump by remember { mutableStateOf("") }
    val hazeState = rememberHazeState()


    Column(modifier = Modifier.padding(LargestPadding)) {
        SettingsSwitchTile(
            title = stringResource(R.string.enable_dev_mode),
            checked = devModeEnabled,
            onCheckedChange = { viewModel.setDeveloperModeEnabled(it) }
        )

        val crashLogSubtitle = if (crashLogExists) stringResource(R.string.recent_crashes) else stringResource(R.string.no_crashes)
        
        SettingsTileContext(
            title = stringResource(R.string.crash_logs),
            subtitle = crashLogSubtitle,
            icon = { XenonIcon(Icons.Rounded.BugReport).Render(Modifier) },
            showContext = true,
            modifier = Modifier.padding(top = LargestPadding),
            contextContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                currentCrashLog = viewModel.readCrashLog()
                                showCrashLogDialog = true
                            },
                            enabled = crashLogExists,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                XenonIcon(Icons.Rounded.Description).Render(Modifier)
                                Text(stringResource(R.string.view))
                            }
                        }

                        TextButton(
                            onClick = { viewModel.shareCrashLog() },
                            enabled = crashLogExists,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                XenonIcon(Icons.Rounded.Share).Render(Modifier)
                                Text(stringResource(R.string.share))
                            }
                        }

                        TextButton(
                            onClick = { viewModel.clearCrashLog() },
                            enabled = crashLogExists,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                XenonIcon(Icons.Rounded.Delete).Render(Modifier)
                                Text(stringResource(R.string.clear))
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.contactDeveloper() },
                        enabled = crashLogExists,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Email, null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.contact_developer))
                        }
                    }
                }
            }
        )

        SettingsTileContext(
            title = stringResource(R.string.media_debug),
            subtitle = stringResource(R.string.media_debug_description),
            icon = { XenonIcon(Icons.Rounded.MusicNote).Render(Modifier) },
            showContext = true,
            modifier = Modifier.padding(top = LargestPadding),
            contextContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            currentMediaDump = viewModel.dumpMediaControls()
                            showMediaDumpDialog = true
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            XenonIcon(Icons.Rounded.Description).Render(Modifier)
                            Text(stringResource(R.string.dump_media_state))
                        }
                    }
                }
            }
        )

        if (showCrashLogDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
            XenonDialog(
                onDismissRequest = { showCrashLogDialog = false },
                title = stringResource(R.string.crash_log),
                confirmButtonText = stringResource(R.string.close),
                onConfirmButtonClick = { showCrashLogDialog = false }
            ) {
                Text(
                    text = currentCrashLog,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }}
        }
        if (showMediaDumpDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
            XenonDialog(
                onDismissRequest = { showMediaDumpDialog = false },
                title = stringResource(R.string.media_state_dump),
                confirmButtonText = stringResource(R.string.close),
                onConfirmButtonClick = { showMediaDumpDialog = false }
            ) {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(scrollState)) {
                    Text(
                        text = currentMediaDump,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            }
        }
    }
}
