package com.xenonware.launcher.ui.layouts.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.values.ExtraBiggestBigSpacing
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.HugeSpacing
import com.xenon.mylibrary.values.HugestSpacing
import com.xenon.mylibrary.values.SmallElevation
import com.xenon.mylibrary.values.SmallSpacer
import com.xenon.mylibrary.values.SmallerSpacing
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.util.DragDropState

/** Thickness of the focus outline drawn around the highlighted icon. */
private val AppFocusRingWidth = 2.dp

/** Gap between the icon and its focus outline, on every side. */
private val AppFocusRingPadding = 8.dp

@Composable
fun AppDrawerGridLayout(
    modifier: Modifier = Modifier,
    app: AppInfo,
    notificationCount: Int,
    badgeType: Int,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onPinApp: (String, Int) -> Unit,
    dragDropState: DragDropState,
    onLongPress: ((Offset) -> Unit)? = null,
    iconShape: IconShape = IconShape.Circle,
    showShadow: Boolean = false,
    showLabels: Boolean = true,
    isFocused: Boolean = false,
    // When supplied the cell becomes a Compose/Android focus target. Android's focus system then
    // moves the highlight between neighbouring cells on its own for arrow keys, D-pad, TV remotes
    // and MindControl's virtual-gamepad / ACTION_FOCUS navigation - so we never intercept arrows.
    focusRequester: FocusRequester? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
) {
    var itemPos by remember { mutableStateOf(Offset.Zero) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var isActualDrag by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Single launch path shared by every input type (tap, Enter, gamepad A, accessibility click)
    // so the focused and touched behaviours can never drift apart.
    val launch: () -> Unit = {
        onAppClick(
            if (app.className.isNotEmpty()) "${app.packageName}/${app.className}" else app.packageName
        )
        onDismiss()
    }

    val viewConfiguration = LocalViewConfiguration.current
    val customViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val touchSlop: Float
                get() = viewConfiguration.touchSlop * 3f
        }
    }

    // Animated highlight opacity. The outline wraps the whole cell (icon + label).
    val focusRingAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(150),
        label = "appFocusRing"
    )
    // Describe the "selected" state to accessibility services (TalkBack and geometric D-pad focus
    // search) when this cell is the one the highlight sits on.
    val focusDescription = stringResource(R.string.app_focused_description, app.label)
    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .then(if (!showLabels) Modifier.aspectRatio(1f) else Modifier)
                .onGloballyPositioned { itemPos = it.positionInRoot() }
                .then(
                    if (focusRequester != null) {
                        Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .onFocusChanged { onFocusChanged?.invoke(it.isFocused) }
                            .onKeyEvent { event -> onKeyEvent?.invoke(event) ?: false }
                    } else Modifier
                )
                .semantics(mergeDescendants = true) {
                    if (isFocused) contentDescription = focusDescription
                    onClick(label = app.label) { launch(); true }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { launch() }
                    )
                }
                .pointerInput(Unit) {
                    var totalDragDistance = 0f
                    detectDragGesturesAfterLongPress(onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isActualDrag = false
                        pressOffset = offset
                        totalDragDistance = 0f
                    }, onDrag = { change, dragAmount ->
                        totalDragDistance += dragAmount.getDistance()
                        val threshold = with(density) { ExtraLargerSpacing.toPx() }

                        if (totalDragDistance > threshold && !isActualDrag) {
                            isActualDrag = true
                            dragDropState.startDrag(app, itemPos + pressOffset)
                        }

                        if (isActualDrag) {
                            change.consume()
                            dragDropState.dragOffset += dragAmount

                            if (dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
                                val relativeX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
                                val itemWidth = with(density) { ExtraBiggestBigSpacing.toPx() }
                                dragDropState.targetIndex = (relativeX / itemWidth).toInt().coerceIn(0, 100)
                            } else {
                                dragDropState.targetIndex = -1
                            }
                        }
                    }, onDragEnd = {
                        if (isActualDrag) {
                            val finalPos = dragDropState.dragOffset
                            val verticalDist = if (finalPos.y < dragDropState.dockBounds.top) {
                                dragDropState.dockBounds.top - finalPos.y
                            } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                                finalPos.y - dragDropState.dockBounds.bottom
                            } else 0f

                            val hitThreshold = with(density) { HugestSpacing.toPx() }

                            if (dragDropState.dockBounds.contains(finalPos) || verticalDist < hitThreshold) {
                                onPinApp(if (app.className.isNotEmpty()) "${app.packageName}/${app.className}" else app.packageName, dragDropState.targetIndex)
                            }
                        } else {
                            // User let go after long press without dragging -> Show menu
                            onLongPress?.invoke(itemPos + pressOffset)
                        }
                        dragDropState.stopDrag()
                    }, onDragCancel = { dragDropState.stopDrag() })
                }) {
            Box(contentAlignment = Alignment.TopEnd) {
                app.icon?.let { icon ->
                    val shape = iconShape.getShape()
                    Box(
                        modifier = Modifier
                            .size(HugeSpacing + AppFocusRingPadding * 2)
                            .then(
                                if (focusRingAlpha > 0f) {
                                    Modifier.border(
                                        width = AppFocusRingWidth,
                                        color = colorScheme.primary.copy(alpha = focusRingAlpha),
                                        // Follows the icon shape so the outline hugs the icon.
                                        shape = shape
                                    )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = icon.toBitmap().asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier
                                .size(HugeSpacing)
                                .then(if (showShadow) Modifier.shadow(SmallElevation, shape) else Modifier)
                                .clip(shape)
                        )
                    }
                }
                NotificationBadge(
                    count = notificationCount,
                    badgeType = badgeType,
                    appIcon = app.icon,
                    modifier = Modifier.offset(x = SmallerSpacing, y = SmallerSpacing * (-1))
                )
            }
            if (showLabels) {
                Spacer(Modifier.height(SmallSpacer))
                Text(
                    app.label,
                    color = colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
