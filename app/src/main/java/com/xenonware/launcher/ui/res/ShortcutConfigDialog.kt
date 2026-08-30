package com.xenonware.launcher.ui.res

//import com.xenon.mylibrary.res.XenonDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.viewmodel.LauncherViewModel

@Composable
fun ShortcutConfigDialog(
    type: LauncherViewModel.ShortcutType,
    apps: List<AppInfo>,
    initialValue: String,
    iconShape: IconShape,
    showShadow: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var linkValue by remember {
        mutableStateOf(
            if (initialValue.startsWith("link:")) initialValue.substring(
                5
            ) else ""
        )
    }
    var selectedPackage by remember {
        mutableStateOf(
            if (initialValue.startsWith("app:")) initialValue.substring(
                4
            ) else ""
        )
    }
    var selectionMode by remember {
        mutableStateOf(if (initialValue.startsWith("app:")) "app" else "link")
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var showLinkInput by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    val typeName = when (type) {
        LauncherViewModel.ShortcutType.TIME -> stringResource(R.string.time)
        LauncherViewModel.ShortcutType.DATE -> stringResource(R.string.date)
        LauncherViewModel.ShortcutType.WEATHER -> stringResource(R.string.weather)
    }

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.configure_shortcut_format, typeName),
        confirmButtonText = stringResource(R.string.save),
        onConfirmButtonClick = {
            when (selectionMode) {
                "link" if linkValue.isNotEmpty() -> {
                    onSave("link:$linkValue")
                }

                "app" if selectedPackage.isNotEmpty() -> {
                    onSave("app:$selectedPackage")
                }

                else -> {
                    onSave("")
                }
            }
        },
        contentManagesScrolling = true,
        externalShowTopDivider = showTopDivider,
        externalShowBottomDivider = showBottomDivider
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isLinkSelected = selectionMode == "link"
            // Link Tile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLinkSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectionMode = "link" }
                        .padding(12.dp)
                ) {
                    RadioButton(
                        selected = isLinkSelected,
                        onClick = { selectionMode = "link" }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.link),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isLinkSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLinkSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLinkSelected && linkValue.isNotEmpty()) linkValue else stringResource(R.string.not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLinkSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(36.dp),
                    color = if (isLinkSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                Box(
                    modifier = Modifier
                        .clickable {
                            selectionMode = "link"
                            showLinkInput = true
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isLinkSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            val isAppSelected = selectionMode == "app"
            // App Tile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isAppSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectionMode = "app" }
                        .padding(12.dp)
                ) {
                    RadioButton(
                        selected = isAppSelected,
                        onClick = { selectionMode = "app" }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.select_app),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isAppSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAppSelected && selectedPackage.isNotEmpty()) (apps.find { it.packageName == selectedPackage }?.label ?: selectedPackage) else stringResource(R.string.not_set),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAppSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .height(36.dp),
                    color = if (isAppSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                Box(
                    modifier = Modifier
                        .clickable {
                            selectionMode = "app"
                            showAppPicker = true
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isAppSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
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
                selectionMode = "app"
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
                selectionMode = "link"
                showLinkInput = false
            }
        )
    }
}
