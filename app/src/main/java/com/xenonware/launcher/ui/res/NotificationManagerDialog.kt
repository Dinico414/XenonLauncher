package com.xenonware.launcher.ui.res

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonTextField
import com.xenon.mylibrary.values.IconSizeLarge
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargeMediumSpacer
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.util.LocalMainFontFamily
import com.xenonware.launcher.util.LocalSubFontFamily

@Composable
fun NotificationManagerDialog(
    allApps: List<AppInfo>,
    visibleApps: List<String>,
    title: String = stringResource(R.string.notification_manager),
    description: String = stringResource(R.string.notification_manager_description),
    emptyMeansAllSelected: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
    iconShape: IconShape,
    showShadow: Boolean
) {
    val allPackageNames = remember(allApps) { allApps.map { it.packageName }.toSet() }
    var selectedPackages by remember(visibleApps, allPackageNames, emptyMeansAllSelected) {
        mutableStateOf(
            if (emptyMeansAllSelected) {
                if (visibleApps.isEmpty()) {
                    allPackageNames
                } else if (visibleApps.contains("__NONE__")) {
                    emptySet()
                } else {
                    visibleApps.toSet()
                }
            } else {
                visibleApps.toSet()
            }
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(searchQuery) {
        if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
            listState.scrollToItem(0)
        }
    }

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        title = title,
        confirmButtonText = stringResource(R.string.done),
        onConfirmButtonClick = {
            val result = if (emptyMeansAllSelected) {
                if (selectedPackages.size >= allPackageNames.size) {
                    emptyList()
                } else if (selectedPackages.isEmpty()) {
                    listOf("__NONE__")
                } else {
                    selectedPackages.toList()
                }
            } else {
                selectedPackages.toList()
            }
            onSave(result)
            onDismiss()
        },
        actionButton1Text = stringResource(R.string.select_all),
        onActionButton1Click = { selectedPackages = allPackageNames },
        actionButton2Text = stringResource(R.string.clear_all),
        onActionButton2Click = { selectedPackages = emptySet() },
        contentManagesScrolling = true,
        externalShowTopDivider = showTopDivider,
        externalShowBottomDivider = showBottomDivider
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .heightIn(max = 400.dp)
        ) {
            item {
                Text(
                    description,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = LargestPadding)
                )
            }

            item {
                XenonTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = LargeMediumPadding),
                    mainContextFont = mainFont,
                    subContextFont = subFont
                )
            }

            if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = LargestPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_results),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(filteredApps, key = { "${it.packageName}/${it.className}" }) { app ->
                val isSelected = app.packageName in selectedPackages

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LargeMediumCornerRadius))
                        .clickable {
                            selectedPackages = if (isSelected) {
                                selectedPackages - app.packageName
                            } else {
                                selectedPackages + app.packageName
                            }
                        }
                        .padding(vertical = MediumPadding, horizontal = SmallPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        app = app,
                        iconShape = iconShape,
                        showShadow = showShadow,
                        size = IconSizeLarge
                    )
                    Spacer(Modifier.width(LargeMediumSpacer))
                    Text(
                        app.label,
                        fontSize = 16.sp,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            selectedPackages = if (checked) {
                                selectedPackages + app.packageName
                            } else {
                                selectedPackages - app.packageName
                            }
                        }
                    )
                }
            }
        }
    }
}
