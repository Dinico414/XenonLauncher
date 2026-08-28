package com.xenonware.launcher.ui.res

//import com.xenon.mylibrary.res.XenonDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    val typeName = type.name.lowercase().replaceFirstChar { it.uppercase() }

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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectionMode = "link" }
                        .padding(bottom = 16.dp)) {
                    RadioButton(
                        selected = selectionMode == "link", onClick = { selectionMode = "link" })
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = linkValue,
                        onValueChange = {
                            linkValue = it
                            selectionMode = "link"
                        },
                        label = { Text(stringResource(R.string.link)) },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectionMode == "link"
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.select_app),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }

            items(apps) { app ->
                val isSelected = selectionMode == "app" && selectedPackage == app.packageName
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable {
                            selectedPackage = app.packageName
                            selectionMode = "app"
                        }
                        .padding(vertical = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = isSelected, onClick = {
                                selectedPackage = app.packageName
                                selectionMode = "app"
                            })

                        AppIcon(
                            app = app,
                            iconShape = iconShape,
                            showShadow = showShadow,
                            size = 40.dp
                        )

                        Text(
                            text = app.label,
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
