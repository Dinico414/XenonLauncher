package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.LargeIconSize
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.MassiveCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.SmallSpacer
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.ui.theme.LocalMainFontFamily
import com.xenonware.launcher.ui.theme.LocalSubFontFamily

@Composable
fun AppPickerDialog(
    apps: List<AppInfo>,
    selectedPackage: String,
    iconShape: IconShape,
    showShadow: Boolean,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var currentSelection by remember { mutableStateOf(selectedPackage) }
    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        title = stringResource(R.string.action_open_app),
        confirmButtonText = stringResource(R.string.ok),
        onConfirmButtonClick = {
            onAppSelected(currentSelection)
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
            verticalArrangement = Arrangement.spacedBy(SmallSpacer)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search), fontFamily = subFont) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = mainFont),
                    singleLine = true,
                    shape = RoundedCornerShape(LargeMediumCornerRadius)
                )
            }
            val filteredApps = apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
            items(filteredApps) { app ->
                val isAppSelected = currentSelection == app.packageName
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MassiveCornerRadius))
                        .background(if (isAppSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable {
                            currentSelection = app.packageName
                        }
                        .padding(horizontal = LargeMediumPadding, vertical = MediumPadding),
                    horizontalArrangement = Arrangement.spacedBy(MediumSpacer)
                ) {
                    RadioButton(
                        selected = isAppSelected,
                        onClick = {
                            currentSelection = app.packageName
                        }
                    )
                    AppIcon(
                        app = app,
                        iconShape = iconShape,
                        showShadow = showShadow,
                        size = LargeIconSize
                    )
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isAppSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontFamily = mainFont
                    )
                }
            }
        }
    }
}
