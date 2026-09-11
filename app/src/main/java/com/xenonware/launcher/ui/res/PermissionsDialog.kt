package com.xenonware.launcher.ui.res

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.PermissionStatus

@Composable
fun PermissionsDialog(
    permissions: List<PermissionStatus>,
    onDismiss: () -> Unit,
    onOpenPermission: (String) -> Unit
) {
    val listState = rememberLazyListState()

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.permissions),
        confirmButtonText = stringResource(R.string.close),
        onConfirmButtonClick = onDismiss,
        contentManagesScrolling = true,
        externalShowTopDivider = listState.canScrollBackward,
        externalShowBottomDivider = listState.canScrollForward,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(permissions) { status ->
                PermissionItem(
                    status = status,
                    onClick = { onOpenPermission(status.permission) }
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    status: PermissionStatus,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
        leadingContent = null,
        trailingContent = {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (status.isGranted)
                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else
                            Color(0xFFF44336).copy(alpha = 0.15f),
                        contentColor = if (status.isGranted) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (status.isGranted) stringResource(R.string.granted) else stringResource(R.string.denied),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
        overlineContent = null,
        supportingContent = null,
        content =
        {
            Text(
                text = status.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
