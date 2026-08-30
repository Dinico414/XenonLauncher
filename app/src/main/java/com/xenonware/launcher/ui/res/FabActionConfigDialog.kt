package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.FabAction

@Composable
fun FabActionConfigDialog(
    isDoubleTap: Boolean,
    apps: List<AppInfo>,
    initialAction: FabAction,
    initialValue: String,
    iconShape: IconShape,
    showShadow: Boolean,
    onDismiss: () -> Unit,
    onSave: (FabAction, String) -> Unit,
) {
    var selectedAction by remember { mutableStateOf(initialAction) }
    var linkValue by remember { mutableStateOf(if (initialAction == FabAction.OPEN_LINK) initialValue else "") }
    var selectedPackage by remember { mutableStateOf(if (initialAction == FabAction.OPEN_APP) initialValue else "") }

    var showAppPicker by remember { mutableStateOf(false) }
    var showLinkInput by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    val actionType = stringResource(if (isDoubleTap) R.string.fab_double_tap else R.string.fab_long_press)

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.configure_action_format, actionType),
        confirmButtonText = stringResource(R.string.save),
        onConfirmButtonClick = {
            val finalValue = when (selectedAction) {
                FabAction.OPEN_APP -> selectedPackage
                FabAction.OPEN_LINK -> linkValue
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.select_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            val actions = FabAction.entries
            items(actions) { action ->
                val isSelected = selectedAction == action
                val isSubmenuAction = action == FabAction.OPEN_APP || action == FabAction.OPEN_LINK

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAction = action }
                            .padding(12.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedAction = action }
                        )
                        Spacer(Modifier.width(8.dp))
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
                                    else -> ""
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
                                .height(36.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        Box(
                            modifier = Modifier
                                .clickable {
                                    if (action == FabAction.OPEN_APP) showAppPicker = true
                                    else if (action == FabAction.OPEN_LINK) showLinkInput = true
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
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
}

@Composable
private fun getActionName(action: FabAction): String {
    return when (action) {
        FabAction.LOCK_DEVICE -> stringResource(R.string.action_lock_device)
        FabAction.TRIGGER_ASSISTANT -> stringResource(R.string.action_trigger_assistant)
        FabAction.OPEN_APP -> stringResource(R.string.action_open_app)
        FabAction.OPEN_LINK -> stringResource(R.string.action_open_link)
        FabAction.TOGGLE_FLASHLIGHT -> stringResource(R.string.action_toggle_flashlight)
        FabAction.NONE -> stringResource(R.string.action_none)
    }
}
