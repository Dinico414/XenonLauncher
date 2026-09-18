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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestBiggerSpacing
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacer
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppMenuItem
import com.xenonware.launcher.ui.theme.LocalMainFontFamily
import com.xenonware.launcher.ui.theme.LocalSubFontFamily

@Composable
fun AppMenuOrderDialog(
    initialOrder: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val items = remember { mutableStateListOf<String>().apply { addAll(initialOrder) } }
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        title = stringResource(R.string.app_menu_order),
        confirmButtonText = stringResource(R.string.save),
        onConfirmButtonClick = {
            onSave(items.toList())
        },
        actionButton1Text = stringResource(R.string.cancel),
        onActionButton1Click = onDismiss,
        contentManagesScrolling = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_menu_order_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = LargePadding)
            )

            FilledTonalButton(
                onClick = {
                    items.clear()
                    items.addAll(AppMenuItem.DEFAULT_ORDER.map { it.id })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LargePadding),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(LargeMediumCornerRadius)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.padding(end = MediumPadding).size(LargestBiggerSpacing))
                Text(stringResource(R.string.reset_to_default), fontWeight = FontWeight.SemiBold)
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, itemId ->
                    val menuItem = AppMenuItem.fromId(itemId) ?: return@itemsIndexed
                    val label = when (menuItem) {
                        AppMenuItem.UNINSTALL -> stringResource(R.string.uninstall)
                        AppMenuItem.APP_INFO -> stringResource(R.string.app_info)
                        AppMenuItem.EDIT -> stringResource(R.string.edit)
                        AppMenuItem.HIDE -> stringResource(R.string.hide)
                        AppMenuItem.SPLIT_SCREEN -> stringResource(R.string.split_screen)
                    }
                    val icon = when (menuItem) {
                        AppMenuItem.UNINSTALL -> Icons.Rounded.Delete
                        AppMenuItem.APP_INFO -> Icons.Rounded.Info
                        AppMenuItem.EDIT -> Icons.Rounded.Edit
                        AppMenuItem.HIDE -> Icons.Rounded.Visibility
                        AppMenuItem.SPLIT_SCREEN -> Icons.Rounded.VerticalSplit
                    }

                    OrderItem(
                        label = label,
                        icon = icon,
                        onMoveUp = if (index > 0) { { items.move(index, index - 1) } } else null,
                        onMoveDown = if (index < items.size - 1) { { items.move(index, index + 1) } } else null
                    )
                }
            }
        }
    }
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val item = removeAt(from)
    add(to, item)
}

@Composable
private fun OrderItem(
    label: String,
    icon: ImageVector,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LargestCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(LargeMediumPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(MediumSpacer))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Row {
            if (onMoveUp != null) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onMoveUp)
                        .padding(4.dp)
                )
            } else {
                Spacer(Modifier.size(32.dp))
            }
            
            if (onMoveDown != null) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(onClick = onMoveDown)
                        .padding(4.dp)
                )
            } else {
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}
