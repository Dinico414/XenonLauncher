package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.CalendarInfo

@Composable
fun CalendarSelectionDialog(
    availableCalendars: List<CalendarInfo>,
    selectedCalendars: List<String>,
    onDismiss: () -> Unit,
    onToggleCalendar: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit
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
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = stringResource(R.string.select_calendars),
        confirmButtonText = stringResource(R.string.done),
        onConfirmButtonClick = onDismiss,
        actionButton1Text = stringResource(R.string.select_all),
        onActionButton1Click = onSelectAll,
        actionButton2Text = stringResource(R.string.clear_all),
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
                    stringResource(R.string.calendar_selection_description),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            if (availableCalendars.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no_calendars_found),
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(availableCalendars) { calendar ->
                    val isSelected = selectedCalendars.contains(calendar.id) || selectedCalendars.isEmpty()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleCalendar(calendar.id) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(calendar.color))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                calendar.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                calendar.accountName,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                            if (!calendar.syncEvents) {
                                Text(
                                    stringResource(R.string.sync_disabled),
                                    fontSize = 11.sp,
                                    color = colorScheme.error
                                )
                            }
                        }
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleCalendar(calendar.id) }
                        )
                    }
                }
            }
        }
    }
}
