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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenonware.launcher.R

@Composable
fun AllAppsDivider(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = 20.dp, bottom = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f), color = colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Text(
            text = stringResource(R.string.all_apps),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = typography.labelMedium,
            color = colorScheme.onSurface.copy(alpha = 0.8f)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f), color = colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}