package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.MediumLargeSpacing
import com.xenon.mylibrary.values.MediumSpacer
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.theme.LocalMainFontFamily
import com.xenonware.launcher.ui.theme.LocalSubFontFamily

@Composable
fun VisualizerConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    var pickingStyle by remember { mutableStateOf(false) }
    var pickingReactivity by remember { mutableStateOf(false) }
    var pickingGeometry by remember { mutableStateOf(false) }
    var pickingColorProfile by remember { mutableStateOf(false) }

    XenonDialog(
        onDismissRequest = onDismiss,
        title = "Music Visualizer",
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        confirmButtonText = stringResource(R.string.done),
        onConfirmButtonClick = onDismiss,
        contentManagesScrolling = true
    ) {
        val standaloneShape = RoundedCornerShape(ExtraLargerCornerRadius)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
            verticalArrangement = Arrangement.spacedBy(MediumSpacer)
        ) {
            item {
                SettingsTile(
                    title = "Visualizer Style",
                    subtitle = VisualizerConfig.getStyleName(VisualizerConfig.visualizerStyle),
                    onClick = { pickingStyle = true },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            item {
                SettingsTile(
                    title = "Visualizer Reactivity",
                    subtitle = VisualizerConfig.getReactivityName(VisualizerConfig.reactivity),
                    onClick = { pickingReactivity = true },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            item {
                SettingsTile(
                    title = "Geometric Style",
                    subtitle = VisualizerConfig.getGeometryName(VisualizerConfig.geometricStyle),
                    onClick = { pickingGeometry = true },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            item {
                SettingsSwitchTile(
                    title = "Visualizer Waves",
                    subtitle = if (VisualizerConfig.waves == 1) "Enabled" else "Disabled",
                    checked = VisualizerConfig.waves == 1,
                    onCheckedChange = { 
                        VisualizerConfig.waves = if (it) 1 else 0
                        VisualizerConfig.save(context)
                    },
                    onClick = {
                        VisualizerConfig.waves = if (VisualizerConfig.waves == 1) 0 else 1
                        VisualizerConfig.save(context)
                    },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            item {
                SettingsTile(
                    title = "Color Profile",
                    subtitle = VisualizerConfig.getColorProfileName(VisualizerConfig.colorProfile),
                    onClick = { pickingColorProfile = true },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }
        }
    }

    if (pickingStyle) {
        VisualizerOptionPickerDialog(
            title = "Visualizer Style",
            options = listOf(0, 1, 2, 3, 4, 5),
            selectedOption = VisualizerConfig.visualizerStyle,
            getName = { VisualizerConfig.getStyleName(it) },
            onSelect = { 
                VisualizerConfig.visualizerStyle = it
                VisualizerConfig.save(context)
            },
            onDismiss = { pickingStyle = false }
        )
    }

    if (pickingReactivity) {
        VisualizerOptionPickerDialog(
            title = "Visualizer Reactivity",
            options = listOf(0, 1, 2, 3, 4),
            selectedOption = VisualizerConfig.reactivity,
            getName = { VisualizerConfig.getReactivityName(it) },
            onSelect = { 
                VisualizerConfig.reactivity = it
                VisualizerConfig.save(context)
            },
            onDismiss = { pickingReactivity = false }
        )
    }

    if (pickingGeometry) {
        VisualizerOptionPickerDialog(
            title = "Geometric Style",
            options = listOf(0, 1, 2),
            selectedOption = VisualizerConfig.geometricStyle,
            getName = { VisualizerConfig.getGeometryName(it) },
            onSelect = { 
                VisualizerConfig.geometricStyle = it
                VisualizerConfig.save(context)
            },
            onDismiss = { pickingGeometry = false }
        )
    }

    if (pickingColorProfile) {
        VisualizerOptionPickerDialog(
            title = "Color Profile",
            options = listOf(0, 1, 2),
            selectedOption = VisualizerConfig.colorProfile,
            getName = { VisualizerConfig.getColorProfileName(it) },
            onSelect = { 
                VisualizerConfig.colorProfile = it
                VisualizerConfig.save(context)
            },
            onDismiss = { pickingColorProfile = false }
        )
    }
}

@Composable
private fun VisualizerOptionPickerDialog(
    title: String,
    options: List<Int>,
    selectedOption: Int,
    getName: (Int) -> String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    XenonDialog(
        onDismissRequest = onDismiss,
        title = title,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        confirmButtonText = stringResource(R.string.done),
        onConfirmButtonClick = onDismiss,
        contentManagesScrolling = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(MediumLargeSpacing)
            ) {
                items(options) { option ->
                    val isSelected = option == selectedOption
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LargestCornerRadius))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceBright
                            )
                            .clickable { 
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { 
                                onSelect(option)
                                onDismiss()
                            }
                        )
                        Spacer(Modifier.size(MediumSpacer))
                        Text(
                            text = getName(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
