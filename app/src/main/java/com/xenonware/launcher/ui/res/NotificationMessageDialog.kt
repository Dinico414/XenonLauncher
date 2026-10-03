package com.xenonware.launcher.ui.res

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.launcher.R
import com.xenonware.launcher.util.LocalMainFontFamily
import com.xenonware.launcher.util.LocalSubFontFamily

@Composable
fun NotificationMessageDialog(
    selectedType: Int,
    onDismiss: () -> Unit,
    onSelectType: (Int) -> Unit
) {
    var currentType by remember(selectedType) { mutableIntStateOf(selectedType) }
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current

    val options = listOf(0, 1, 2)

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        title = stringResource(R.string.notification_message),
        confirmButtonText = stringResource(R.string.done),
        onConfirmButtonClick = {
            onSelectType(currentType)
            onDismiss()
        },
        contentManagesScrolling = true
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(options) { type ->
                val isSelected = type == currentType
                val label = when (type) {
                    0 -> stringResource(R.string.notification_message_none)
                    1 -> stringResource(R.string.notification_message_no_notification)
                    else -> stringResource(R.string.notification_message_up_to_date)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LargeMediumCornerRadius))
                        .clickable { currentType = type }
                        .padding(vertical = MediumPadding, horizontal = SmallPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { currentType = type }
                    )
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = MediumPadding)
                    )
                }
            }
        }
    }
}
