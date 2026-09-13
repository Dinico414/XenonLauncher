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
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.LargestPadding
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.theme.mainFontFamily

@Composable
fun AllAppsDivider(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(top = ExtraLargePadding, bottom = LargestPadding)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f), color = colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Text(
            text = stringResource(R.string.all_apps),
            modifier = Modifier.padding(horizontal = LargestPadding),
            style = typography.labelMedium.copy(
                fontFamily = mainFontFamily
            ),
            color = colorScheme.onSurface.copy(alpha = 0.8f)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f), color = colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}