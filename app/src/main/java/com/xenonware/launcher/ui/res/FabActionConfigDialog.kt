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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
        title = "Configure ${if (isDoubleTap) "Double Tap" else "Long Press"} Action",
        confirmButtonText = "Save",
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Select Action",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            val actions = FabAction.entries
            items(actions) { action ->
                val isSelected = selectedAction == action
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected && action != FabAction.OPEN_APP && action != FabAction.OPEN_LINK) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { selectedAction = action }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        RadioButton(selected = isSelected, onClick = { selectedAction = action })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = getActionName(action),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSelected && action == FabAction.OPEN_LINK) {
                        OutlinedTextField(
                            value = linkValue,
                            onValueChange = { linkValue = it },
                            label = { Text("Link") },
                            placeholder = { Text("https://...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            if (selectedAction == FabAction.OPEN_APP) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Select App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                items(apps) { app ->
                    val isAppSelected = selectedPackage == app.packageName
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isAppSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { selectedPackage = app.packageName }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isAppSelected,
                                onClick = { selectedPackage = app.packageName }
                            )

                            AppIcon(
                                app = app,
                                iconShape = iconShape,
                                showShadow = showShadow,
                                size = 32.dp
                            )

                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isAppSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAppSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
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
