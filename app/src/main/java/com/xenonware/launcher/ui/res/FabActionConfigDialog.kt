package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.BiggestBiggerSpacing
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargeMediumSpacing
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.MediumSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.AppWidgetGroup
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.model.WidgetPickerItemData
import com.xenonware.launcher.ui.theme.LocalMainFontFamily
import com.xenonware.launcher.ui.theme.LocalSubFontFamily
import com.xenonware.launcher.viewmodel.FabConfigMode

@Composable
fun FabActionConfigDialog(
    configMode: FabConfigMode,
    apps: List<AppInfo>,
    installedShortcuts: Map<AppWidgetGroup, List<WidgetPickerItemData>>,
    initialAction: FabAction,
    initialValue: String,
    iconShape: IconShape,
    showShadow: Boolean,
    onDismiss: () -> Unit,
    onSave: (FabAction, String) -> Unit,
    onPickShortcut: (WidgetPickerItemData) -> Unit,
) {
    var selectedAction by remember { mutableStateOf(initialAction) }
    var linkValue by remember { mutableStateOf(if (initialAction == FabAction.OPEN_LINK) initialValue else "") }
    var selectedPackage by remember { mutableStateOf(if (initialAction == FabAction.OPEN_APP) initialValue else "") }
    var shortcutName by remember { mutableStateOf(if (initialAction == FabAction.OPEN_SHORTCUT) initialValue.substringBefore("|") else "") }

    var showAppPicker by remember { mutableStateOf(false) }
    var showLinkInput by remember { mutableStateOf(false) }
    var showShortcutPicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    val actionType = when (configMode) {
        FabConfigMode.DOUBLE -> stringResource(R.string.fab_double_tap)
        FabConfigMode.LONG -> stringResource(R.string.fab_long_press)
        FabConfigMode.SINGLE -> stringResource(R.string.fab_single_tap)
        FabConfigMode.SWIPE_UP -> stringResource(R.string.fab_swipe_up)
        else -> ""
    }
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        title = stringResource(R.string.configure_action_format, actionType),
        confirmButtonText = stringResource(R.string.save),
        onConfirmButtonClick = {
            val finalValue = when (selectedAction) {
                FabAction.OPEN_APP -> selectedPackage
                FabAction.OPEN_LINK -> linkValue
                FabAction.OPEN_SHORTCUT -> initialValue // Value is handled by the shortcut launcher result
                else -> ""
            }
            onSave(selectedAction, finalValue)
        },
        contentManagesScrolling = true,
        externalShowTopDivider = showTopDivider,
        externalShowBottomDivider = showBottomDivider
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            verticalArrangement = Arrangement.spacedBy(MediumSpacing)
        ) {
            item {
                Text(
                    text = stringResource(R.string.select_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = LargeMediumSpacing, top = MediumSpacing, bottom = MediumSpacing)
                )
            }

            val actions = FabAction.entries
            items(actions) { action ->
                val isSelected = selectedAction == action
                val isSubmenuAction = action == FabAction.OPEN_APP || action == FabAction.OPEN_LINK || action == FabAction.OPEN_SHORTCUT

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LargestCornerRadius))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAction = action }
                            .padding(LargeMediumPadding)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedAction = action }
                        )
                        Spacer(Modifier.width(MediumSpacer))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getActionName(action),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSubmenuAction) {
                                val subtitle = when (action) {
                                    FabAction.OPEN_APP -> apps.find { it.packageName == selectedPackage }?.label ?: stringResource(R.string.not_set)
                                    FabAction.OPEN_LINK -> linkValue.ifEmpty { stringResource(R.string.not_set) }
                                    FabAction.OPEN_SHORTCUT -> if (initialAction == FabAction.OPEN_SHORTCUT) shortcutName else stringResource(R.string.not_set)
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (isSubmenuAction) {
                        VerticalDivider(
                            modifier = Modifier
                                .height(BiggestBiggerSpacing),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        Box(
                            modifier = Modifier
                                .clickable {
                                    when (action) {
                                        FabAction.OPEN_APP -> showAppPicker = true
                                        FabAction.OPEN_LINK -> showLinkInput = true
                                        FabAction.OPEN_SHORTCUT -> showShortcutPicker = true
                                    }
                                }
                                .padding(LargeMediumPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(ExtraLargerSpacing),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = apps,
            selectedPackage = selectedPackage,
            iconShape = iconShape,
            showShadow = showShadow,
            onDismiss = { showAppPicker = false },
            onAppSelected = {
                selectedPackage = it
                selectedAction = FabAction.OPEN_APP
                showAppPicker = false
            }
        )
    }

    if (showLinkInput) {
        LinkInputDialog(
            initialValue = linkValue,
            onDismiss = { showLinkInput = false },
            onSave = {
                linkValue = it
                selectedAction = FabAction.OPEN_LINK
                showLinkInput = false
            }
        )
    }

    if (showShortcutPicker) {
        WidgetSelectorDialog(
            installedWidgets = installedShortcuts,
            title = stringResource(R.string.select_shortcut),
            onDismiss = { showShortcutPicker = false },
            onWidgetSelected = { item ->
                onPickShortcut(item)
                showShortcutPicker = false
            }
        )
    }
}

@Composable
private fun getActionName(action: FabAction): String {
    return when (action) {
        FabAction.LOCK_DEVICE -> stringResource(R.string.action_lock_device)
        FabAction.TRIGGER_ASSISTANT -> stringResource(R.string.action_trigger_assistant)
        FabAction.OPEN_APP -> stringResource(R.string.action_open_app)
        FabAction.OPEN_LINK -> stringResource(R.string.action_open_link)
        FabAction.TOGGLE_FLASHLIGHT -> stringResource(R.string.action_toggle_flashlight)
        FabAction.OPEN_APP_DRAWER -> stringResource(R.string.action_open_app_drawer)
        FabAction.OPEN_SHORTCUT -> stringResource(R.string.action_open_shortcut)
        FabAction.NONE -> stringResource(R.string.action_none)
    }
}
