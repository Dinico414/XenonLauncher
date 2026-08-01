package com.xenonware.launcher.ui.res

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xenonware.launcher.model.AppInfo

@Composable
fun NotificationManagerDialog(
    allApps: List<AppInfo>,
    visibleApps: List<String>,
    onDismiss: () -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    iconShape: IconShape,
    showShadow: Boolean
) {
    val listState = rememberLazyListState()
    val showTopDivider by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }
    val showBottomDivider by remember {
        derivedStateOf { listState.canScrollForward }
    }

    XenonDialog(
        onDismissRequest = onDismiss,
        title = "Notification Manager",
        properties = DialogProperties(usePlatformDefaultWidth = true),
        confirmButtonText = "Done",
        onConfirmButtonClick = onDismiss,
        actionButton1Text = "Select All",
        onActionButton1Click = onSelectAll,
        actionButton2Text = "Clear All",
        onActionButton2Click = onClearAll,
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
                    "Choose which apps are allowed to show notifications in the launcher.",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(allApps) { app ->
                val isSelected = visibleApps.contains(app.packageName) || visibleApps.isEmpty()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleApp(app.packageName) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        app = app,
                        iconShape = iconShape,
                        showShadow = showShadow,
                        size = 32.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        app.label,
                        fontSize = 16.sp,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}
