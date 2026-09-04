package com.xenonware.launcher.ui.layouts.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.res.SettingsTileContext
import com.xenon.mylibrary.res.XenonSingleChoiceButtonGroup
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import com.xenonware.launcher.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TweaksItems(
    viewModel: SettingsViewModel,
    layoutType: LayoutType = LayoutType.COMPACT,
    innerGroupRadius: Dp = 4.dp,
    outerGroupRadius: Dp = 24.dp,
    innerGroupSpacing: Dp = 2.dp,
    outerGroupSpacing: Dp = ExtraLargeSpacing,
    tileBackgroundColor: Color = MaterialTheme.colorScheme.surfaceBright,
    tileContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tileSubtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tileShapeOverride: Shape? = null,
    useGroupStyling: Boolean = true,
) {
    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else 0.dp
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else 0.dp
    val actualInnerGroupSpacing = if (useGroupStyling) innerGroupSpacing else 0.dp
    val actualOuterGroupSpacing = outerGroupSpacing

    val topShape = if (useGroupStyling) RoundedCornerShape(
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius,
        topStart = actualOuterGroupRadius,
        topEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val standaloneShape = if (useGroupStyling) RoundedCornerShape(actualOuterGroupRadius)
    else RoundedCornerShape(NoCornerRadius)

    val middleShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val bottomShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualOuterGroupRadius,
        bottomEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val showClock by viewModel.showClockAtAGlance.collectAsState()
    val notificationIndicatorType by viewModel.notificationIndicatorType.collectAsState()
    val notificationMessageType by viewModel.notificationMessageType.collectAsState()
    val hideAtAGlance by viewModel.hideAtAGlance.collectAsState()
    
    var moveWebSearch by remember { mutableStateOf(false) }
    var hideDockScrolling by remember { mutableStateOf(false) }
    var hideDockWidgets by remember { mutableStateOf(false) }
    var hideActionButton by remember { mutableStateOf(false) }
    var showMuteNotifications by remember { mutableStateOf(false) }
    var showPermanentNotifications by remember { mutableStateOf(false) }
    var disableGrouping by remember { mutableStateOf(false) }

    Column {
        // --- At a Glance Tweaks ---
        Column {
            SettingsSwitchTile(
                title = stringResource(R.string.show_clock_at_a_glance),
                subtitle = stringResource(R.string.show_clock_at_a_glance_description),
                checked = showClock,
                onCheckedChange = { viewModel.setShowClockAtAGlance(it) },
                icon = { Icon(Icons.Rounded.WatchLater, null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsTileContext(
                title = stringResource(R.string.notification_indicator),
                icon = { Icon(Icons.Rounded.Notifications, null, tint = tileSubtitleColor) },
                showContext = true,
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                enableRipple = false,
                contextContent = {
                    val entries = listOf(0, 1, 2)
                    val interactionSources = remember { entries.map { MutableInteractionSource() } }
                    val pressedStates = remember { mutableStateListOf<Boolean>().apply { repeat(entries.size) { add(false) } } }

                    entries.forEachIndexed { index, _ ->
                        LaunchedEffect(interactionSources[index]) {
                            var pressStartTime = 0L
                            interactionSources[index].interactions.collect { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> {
                                        pressedStates[index] = true
                                        pressStartTime = System.currentTimeMillis()
                                    }
                                    is PressInteraction.Release -> {
                                        val duration = System.currentTimeMillis() - pressStartTime
                                        if (duration < 200) delay((200 - duration).milliseconds)
                                        pressedStates[index] = false
                                    }
                                    is PressInteraction.Cancel -> pressedStates[index] = false
                                }
                            }
                        }
                    }

                    val pressedIndex = pressedStates.indexOfFirst { it }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                    alpha = if (LocalIsDarkTheme.current) 0.5f else 1f
                                )
                            )
                            .padding(vertical = 12.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .height(64.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            entries.forEachIndexed { index, type ->
                                val isSelected = notificationIndicatorType == type
                                val isPressed = pressedStates[index]
                                val isNeighborPressed = pressedIndex != -1 && abs(index - pressedIndex) == 1

                                val targetWidth = when {
                                    isPressed -> {
                                        val neighbors = if (index == 0 || index == entries.size - 1) 1 else 2
                                        64.dp + (if (neighbors == 1) 6.dp else 12.dp)
                                    }
                                    isNeighborPressed -> 58.dp
                                    else -> 64.dp
                                }

                                val containerWidth by animateDpAsState(
                                    targetValue = targetWidth,
                                    label = "indicatorWidth",
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )

                                val containerRadius by animateDpAsState(
                                    targetValue = when {
                                        isPressed -> 6.dp
                                        isSelected -> 16.dp
                                        else -> 32.dp
                                    }, label = "indicatorRadius", animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )

                                val containerShape = RoundedCornerShape(containerRadius)
                                val icon = when (type) {
                                    0 -> Icons.Rounded.Block
                                    1 -> Icons.Rounded.Check
                                    else -> Icons.Rounded.EmojiEvents
                                }

                                Box(
                                    modifier = Modifier
                                        .width(containerWidth)
                                        .fillMaxHeight()
                                        .clip(containerShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        .border(width = 2.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = containerShape)
                                        .clickable(interactionSource = interactionSources[index], indication = null) { viewModel.setNotificationIndicatorType(type) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else tileSubtitleColor
                                    )
                                }
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsTileContext(
                title = stringResource(R.string.notification_message),
                icon = { Icon(Icons.Rounded.TableRows, null, tint = tileSubtitleColor) },
                showContext = true,
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                enableRipple = false,
                contextContent = {
                    XenonSingleChoiceButtonGroup(
                        options = listOf(0, 1, 2),
                        selectedOption = notificationMessageType,
                        onOptionSelect = { viewModel.setNotificationMessageType(it) },
                        label = { type ->
                            when (type) {
                                0 -> stringResource(R.string.notification_message_none)
                                1 -> stringResource(R.string.notification_message_no_notification)
                                else -> stringResource(R.string.notification_message_up_to_date)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    )
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(R.string.hide_at_a_glance),
                subtitle = stringResource(R.string.hide_at_a_glance_description),
                checked = hideAtAGlance,
                onCheckedChange = { viewModel.setHideAtAGlance(it) },
                icon = { Icon(Icons.Rounded.VisibilityOff, null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Search Tweaks ---
        SettingsSwitchTile(
            title = stringResource(R.string.experimental_move_web_search),
            subtitle = stringResource(R.string.experimental_move_web_search_description),
            checked = moveWebSearch,
            onCheckedChange = { moveWebSearch = it },
            icon = { Icon(Icons.Rounded.TravelExplore, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor
        )

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Dock Tweaks ---
        Column {
            SettingsSwitchTile(
                title = stringResource(R.string.hide_dock_scrolling),
                subtitle = stringResource(R.string.hide_dock_scrolling_description),
                checked = hideDockScrolling,
                onCheckedChange = { hideDockScrolling = it },
                icon = {
                    Icon(
                        Icons.Rounded.KeyboardDoubleArrowDown,
                        null,
                        tint = tileSubtitleColor
                    )
                },
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(R.string.hide_dock_widgets),
                subtitle = stringResource(R.string.hide_dock_widgets_description),
                checked = hideDockWidgets,
                onCheckedChange = { hideDockWidgets = it },
                icon = { Icon(Icons.Rounded.Widgets, null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(R.string.hide_action_button),
                subtitle = stringResource(R.string.hide_action_button_description),
                checked = hideActionButton,
                onCheckedChange = { hideActionButton = it },
                icon = { Icon(Icons.Rounded.AdsClick, null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Notification Tweaks ---
        Column {
            SettingsSwitchTile(
                title = stringResource(R.string.show_mute_notifications),
                subtitle = stringResource(R.string.show_mute_notifications_description),
                checked = showMuteNotifications,
                onCheckedChange = { showMuteNotifications = it },
                icon = {
                    Icon(
                        Icons.Rounded.NotificationsPaused,
                        null,
                        tint = tileSubtitleColor
                    )
                },
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(R.string.show_permanent_notifications),
                subtitle = stringResource(R.string.show_permanent_notifications_description),
                checked = showPermanentNotifications,
                onCheckedChange = { showPermanentNotifications = it },
                icon = { Icon(Icons.Rounded.PushPin, null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(R.string.experimental_disable_grouping),
                subtitle = stringResource(R.string.experimental_disable_grouping_description),
                checked = disableGrouping,
                onCheckedChange = { disableGrouping = it },
                icon = { Icon(Icons.Rounded.TableRows, null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor
            )
        }
    }
}
