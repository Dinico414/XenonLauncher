package com.xenonware.launcher.ui.res

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenon.mylibrary.res.XenonTextField
import com.xenonware.launcher.R
import com.xenonware.launcher.util.LocalMainFontFamily
import com.xenonware.launcher.util.LocalSubFontFamily

@Composable
fun LinkInputDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var linkValue by remember { mutableStateOf(initialValue) }
    val mainFont = LocalMainFontFamily.current
    val subFont = LocalSubFontFamily.current
    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        mainContextFont = mainFont,
        subContextFont = subFont,
        title = stringResource(R.string.action_open_link),
        confirmButtonText = stringResource(R.string.ok),
        onConfirmButtonClick = {
            onSave(linkValue)
        },
        contentManagesScrolling = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            XenonTextField(
                value = linkValue,
                onValueChange = { linkValue = it },
                placeholder = { Text(stringResource(R.string.link)) },
                modifier = Modifier.fillMaxWidth(),
                mainContextFont = mainFont,
                subContextFont = subFont
            )
        }
    }
}
