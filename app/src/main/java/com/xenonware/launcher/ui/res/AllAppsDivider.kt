package com.xenonware.launcher.ui.res

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AllAppsDivider(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 20.dp, bottom = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f), color = colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Text(
            text = "All apps",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = typography.labelMedium,
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f), color = colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
    }
}