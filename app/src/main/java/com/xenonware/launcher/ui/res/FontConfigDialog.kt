package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonSingleChoiceButtonGroup
import com.xenon.mylibrary.values.BiggestBiggerSpacing
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestBiggerSpacing
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.LargestSpacing

import com.xenon.mylibrary.values.MediumLargeSpacing
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSmallSpacer
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.SmallSpacer
import com.xenon.mylibrary.values.SmallestStroke
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.theme.AxisDef
import com.xenonware.launcher.ui.theme.FontAxes
import com.xenonware.launcher.ui.theme.FontType
import com.xenonware.launcher.ui.theme.createCustomFontFamily
import kotlin.math.roundToInt

@Composable
fun FontConfigDialog(
    initialMainFontType: Int,
    initialSecondaryFontType: Int,
    initialRobotoSettings: String,
    initialGoogleSansSettings: String,
    onDismiss: () -> Unit,
    onSave: (mainFontType: Int, secondaryFontType: Int, robotoSettings: String, googleSansSettings: String) -> Unit
) {
    var selectedTargetTab by remember { mutableIntStateOf(0) } // 0: Main (Titles/Quicksand), 1: Secondary (Body/UI)
    var selectedMainType by remember { mutableIntStateOf(initialMainFontType) }
    var selectedSecondaryType by remember { mutableIntStateOf(initialSecondaryFontType) }

    val currentSelectedType = if (selectedTargetTab == 0) selectedMainType else selectedSecondaryType

    val robotoMap = remember {
        mutableStateMapOf<String, Float>().apply {
            putAll(FontAxes.parseSettings(initialRobotoSettings, FontAxes.ROBOTO_FLEX_AXES))
        }
    }

    val googleSansMap = remember {
        mutableStateMapOf<String, Float>().apply {
            putAll(FontAxes.parseSettings(initialGoogleSansSettings, FontAxes.GOOGLE_SANS_AXES))
        }
    }

    // Secondary sub-dialog state for adjusting variable font axes
    var editingFontType by remember { mutableStateOf<FontType?>(null) }

    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    val systemDefaultLabel = stringResource(R.string.font_system_default)
    val quicksandLabel = stringResource(R.string.font_quicksand)

    // Precompute sample families for each option so the items display in their own font
    val quicksandFont = remember {
        createCustomFontFamily(FontType.QUICKSAND, emptyMap(), emptyMap())
    }
    val systemFont = remember {
        FontFamily.Default
    }
    val robotoFont = remember(robotoMap.toMap()) {
        createCustomFontFamily(FontType.ROBOTO_FLEX, robotoMap.toMap(), emptyMap())
    }
    val googleSansFont = remember(googleSansMap.toMap()) {
        createCustomFontFamily(FontType.GOOGLE_SANS_FLEX, emptyMap(), googleSansMap.toMap())
    }

    data class FontOptionItem(
        val type: FontType,
        val label: String,
        val fontFamily: FontFamily,
        val isConfigurable: Boolean
    )

    val fontOptions = remember(systemDefaultLabel, quicksandLabel, robotoFont, googleSansFont) {
        listOf(
            FontOptionItem(FontType.SYSTEM, systemDefaultLabel, systemFont, false),
            FontOptionItem(FontType.QUICKSAND, quicksandLabel, quicksandFont, false),
            FontOptionItem(FontType.ROBOTO_FLEX, "Roboto Flex", robotoFont, true),
            FontOptionItem(FontType.GOOGLE_SANS_FLEX, "Google Sans Flex", googleSansFont, true)
        )
    }

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.font_settings),
        confirmButtonText = stringResource(R.string.save),
        onConfirmButtonClick = {
            onSave(
                selectedMainType,
                selectedSecondaryType,
                FontAxes.serializeSettings(robotoMap.toMap()),
                FontAxes.serializeSettings(googleSansMap.toMap())
            )
        },
        actionButton1Text = stringResource(R.string.cancel),
        onActionButton1Click = onDismiss,
        contentManagesScrolling = true,
        externalShowTopDivider = showTopDivider,
        externalShowBottomDivider = showBottomDivider
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Single choice button group: Main Content vs Secondary Content
            XenonSingleChoiceButtonGroup(
                options = listOf(0, 1),
                selectedOption = selectedTargetTab,
                onOptionSelect = { selectedTargetTab = it },
                label = { targetIndex ->
                    if (targetIndex == 0) {
                        stringResource(R.string.font_target_main)
                    } else {
                        stringResource(R.string.font_target_secondary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LargeMediumPadding)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(MediumLargeSpacing)
            ) {
                items(fontOptions) { item ->
                    val isSelected = currentSelectedType == item.type.id

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LargestCornerRadius))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceBright
                            )
                    ) {
                        // Radio button + Font name (Clickable area to select font)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (selectedTargetTab == 0) selectedMainType = item.type.id
                                    else selectedSecondaryType = item.type.id
                                }
                                .padding(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (selectedTargetTab == 0) selectedMainType = item.type.id
                                    else selectedSecondaryType = item.type.id
                                }
                            )
                            Spacer(Modifier.size(MediumSpacer))
                            Text(
                                text = item.label,
                                fontFamily = item.fontFamily,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider and Arrow Button for modifiable fonts
                        if (item.isConfigurable) {
                            VerticalDivider(
                                modifier = Modifier.height(BiggestBiggerSpacing),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        editingFontType = item.type
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(ExtraLargerSpacing),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog for customizing font axes
    editingFontType?.let { fontType ->
        FontAxesAdjustmentDialog(
            fontType = fontType,
            robotoMap = robotoMap,
            googleSansMap = googleSansMap,
            onDismiss = { editingFontType = null }
        )
    }
}

@Composable
private fun FontAxesAdjustmentDialog(
    fontType: FontType,
    robotoMap: MutableMap<String, Float>,
    googleSansMap: MutableMap<String, Float>,
    onDismiss: () -> Unit
) {
    val axes = if (fontType == FontType.ROBOTO_FLEX) FontAxes.ROBOTO_FLEX_AXES else FontAxes.GOOGLE_SANS_AXES
    val parentMap = if (fontType == FontType.ROBOTO_FLEX) robotoMap else googleSansMap

    // Work on a local copy so we can cancel changes
    val localMap = remember {
        mutableStateMapOf<String, Float>().apply {
            putAll(parentMap)
        }
    }

    val previewFontFamily = remember(fontType, localMap.toMap()) {
        createCustomFontFamily(
            fontType = fontType,
            robotoSettings = if (fontType == FontType.ROBOTO_FLEX) localMap.toMap() else emptyMap(),
            googleSansSettings = if (fontType == FontType.GOOGLE_SANS_FLEX) localMap.toMap() else emptyMap()
        )
    }

    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = fontType.title,
        confirmButtonText = stringResource(R.string.done),
        onConfirmButtonClick = {
            parentMap.clear()
            parentMap.putAll(localMap)
            onDismiss()
        },
        actionButton1Text = stringResource(R.string.cancel),
        onActionButton1Click = onDismiss,
        contentManagesScrolling = true,
        externalShowTopDivider = showTopDivider,
        externalShowBottomDivider = showBottomDivider
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pinned preview at the top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LargeMediumPadding)
                    .clip(RoundedCornerShape(LargestCornerRadius))
                    .border(
                        width = SmallestStroke,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(LargestCornerRadius)
                    )
                    .padding(LargePadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.font_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(MediumSmallSpacer))
                Text(
                    text = "The quick brown fox jumps over the lazy dog",
                    fontFamily = previewFontFamily,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(SmallSpacer))
                Text(
                    text = "0123456789 • 12:45 • Xenon Launcher",
                    fontFamily = previewFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Full-width tonal error reset button
            FilledTonalButton(
                onClick = {
                    localMap.clear()
                    localMap.putAll(axes.associate { it.tag to it.default })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LargeMediumPadding),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(LargeMediumCornerRadius)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.padding(end = MediumPadding).size(LargestBiggerSpacing))
                Text(stringResource(R.string.reset_all), fontWeight = FontWeight.SemiBold)
            }

            // Scrollable adjustments
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(MediumLargeSpacing)
            ) {
                items(axes) { axis ->
                    val currentValue = localMap[axis.tag] ?: axis.default
                    AxisSliderItem(
                        axis = axis,
                        value = currentValue,
                        onValueChange = { localMap[axis.tag] = it },
                        onReset = { localMap[axis.tag] = axis.default }
                    )
                }
            }
        }
    }
}

@Composable
private fun AxisSliderItem(
    axis: AxisDef,
    value: Float,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LargeMediumCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(LargeMediumPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(axis.nameRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${axis.min.roundToInt()} .. ${axis.max.roundToInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (axis.isInteger) "${value.roundToInt()}" else String.format(locale, "%.1f", value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                // Single option reset button
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.reset),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(LargestSpacing)
                    )
                }
            }
        }
        Slider(
            value = value,
            onValueChange = { onValueChange(if (axis.isInteger) it.roundToInt().toFloat() else it) },
            valueRange = axis.min..axis.max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
