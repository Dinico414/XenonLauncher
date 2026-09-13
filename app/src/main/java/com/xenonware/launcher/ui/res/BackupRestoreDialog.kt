package com.xenonware.launcher.ui.res

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.BiggestPadding
import com.xenon.mylibrary.values.IconSizeMedium
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargeMediumSpacer
import com.xenon.mylibrary.values.LargestSpacer
import com.xenon.mylibrary.values.MediumSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.BackupInfo
import com.xenonware.launcher.viewmodel.SettingsViewModel

@Composable
fun BackupRestoreDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val backups by viewModel.backups.collectAsState()
    val isSyncing by viewModel.isSyncingBackups.collectAsState()
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

    XenonDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.backup_and_restore),
        properties = DialogProperties(usePlatformDefaultWidth = true),
        confirmButtonText = null,
        contentManagesScrolling = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            if (currentUser == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.sign_in_to_backup),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.startBackup() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LargeMediumCornerRadius),
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(LargeMediumPadding)
                ) {
                    Text(stringResource(R.string.start_backup))
                }

                Spacer(modifier = Modifier.height(LargestSpacer))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MediumSpacing)
                ) {
                    if (isSyncing) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(LargeMediumCornerRadius),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(LargeMediumPadding),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(IconSizeMedium)
                                            .rotate(rotation),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(LargeMediumSpacer))
                                    Text(
                                        text = stringResource(R.string.syncing_with_cloud),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (backups.isEmpty() && !isSyncing) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = BiggestPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_backups_found),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(backups) { backup ->
                            BackupItem(
                                backup = backup,
                                onRestore = { viewModel.restoreBackup(backup) }
                            ) { viewModel.deleteBackup(backup) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupItem(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LargeMediumCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(LargeMediumPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${backup.date} - ${backup.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = backup.device,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = stringResource(R.string.restore),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
