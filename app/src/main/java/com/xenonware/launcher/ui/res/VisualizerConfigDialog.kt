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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.xenonware.launcher.util.LocalMainFontFamily
import com.xenonware.launcher.util.LocalSubFontFamily

@Composable
fun VisualizerConfigDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    var localStyle by remember { mutableIntStateOf(VisualizerConfig.visualizerStyle) }
    var localReactivity by remember { mutableIntStateOf(VisualizerConfig.reactivity) }
    var localGeometry by remember { mutableIntStateOf(VisualizerConfig.geometricStyle) }
    var localWaves by remember { mutableIntStateOf(VisualizerConfig.waves) }
    var localColorProfile by remember { mutableIntStateOf(VisualizerConfig.colorProfile) }

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
        onConfirmButtonClick = {
            VisualizerConfig.visualizerStyle = localStyle
            VisualizerConfig.reactivity = localReactivity
            VisualizerConfig.geometricStyle = localGeometry
            VisualizerConfig.waves = localWaves
            VisualizerConfig.colorProfile = localColorProfile
            VisualizerConfig.save(context)
            onDismiss()
        },
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
                    subtitle = VisualizerConfig.getStyleName(localStyle),
                    onClick = { pickingStyle = true },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            val isOscilloscope = localStyle == VisualizerStyle.OSCILLOSCOPE
            item {
                SettingsTile(
                    modifier = if (isOscilloscope) Modifier.alpha(0.38f) else Modifier,
                    title = "Visualizer Reactivity",
                    subtitle = if (isOscilloscope) "Ignored by Oscilloscope" else VisualizerConfig.getReactivityName(localReactivity),
                    onClick = if (isOscilloscope) null else { { pickingReactivity = true } },
                    shape = standaloneShape,
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            item {
                SettingsTile(
                    title = "Geometric Style",
                    subtitle = VisualizerConfig.getGeometryName(localGeometry),
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
                    subtitle = if (localWaves == 1) "Enabled" else "Disabled",
                    checked = localWaves == 1,
                    onCheckedChange = { 
                        localWaves = if (it) 1 else 0
                    },
                    onClick = {
                        localWaves = if (localWaves == 1) 0 else 1
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
                    subtitle = VisualizerConfig.getColorProfileName(localColorProfile),
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
            selectedOption = localStyle,
            getName = { VisualizerConfig.getStyleName(it) },
            onSelect = { 
                localStyle = it
            },
            onDismiss = { pickingStyle = false }
        )
    }

    if (pickingReactivity) {
        VisualizerOptionPickerDialog(
            title = "Visualizer Reactivity",
            options = listOf(0, 1, 2, 3, 4),
            selectedOption = localReactivity,
            getName = { VisualizerConfig.getReactivityName(it) },
            onSelect = { 
                localReactivity = it
            },
            onDismiss = { pickingReactivity = false }
        )
    }

    if (pickingGeometry) {
        VisualizerOptionPickerDialog(
            title = "Geometric Style",
            options = listOf(0, 1, 2),
            selectedOption = localGeometry,
            getName = { VisualizerConfig.getGeometryName(it) },
            onSelect = { 
                localGeometry = it
            },
            onDismiss = { pickingGeometry = false }
        )
    }

    if (pickingColorProfile) {
        VisualizerOptionPickerDialog(
            title = "Color Profile",
            options = listOf(0, 1, 2),
            selectedOption = localColorProfile,
            getName = { VisualizerConfig.getColorProfileName(it) },
            onSelect = { 
                localColorProfile = it
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
