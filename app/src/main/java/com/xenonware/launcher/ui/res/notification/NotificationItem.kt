package com.xenonware.launcher.ui.res.notification

 import android.app.ActivityOptions
import android.app.RemoteInput
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.notification.LauncherNotificationAction
import com.xenonware.launcher.util.ColorUtils
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign


@Composable
fun NotificationItem(
    modifier: Modifier = Modifier,
    notification: LauncherNotification,
    appColor: Color,
    isFirst: Boolean,
    isLast: Boolean,
    offsetAbove: Float = 0f,
    offsetBelow: Float = 0f,
    onOffsetChanged: (Float) -> Unit = {},
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current

    val offsetX = remember { Animatable(0f) }
    var rawDragOffset by remember { mutableFloatStateOf(0f) }
    var isStuck by remember { mutableStateOf(true) }
    var isDismissing by remember { mutableStateOf(false) }

    val dismissThreshold = remember(density) { 
        val threshold = with(density) { 100.dp.toPx() }
        if (threshold <= 0f) 1f else threshold
    }
    val stretchLimit = with(density) { 120.dp.toPx() }

    val finalAppColor = if (appColor == Color.Unspecified) colorScheme.primary else appColor
    val finalContrastColor = remember(finalAppColor) { ColorUtils.getContrastColor(finalAppColor) }
    var expanded by remember { mutableStateOf(false) }

    val swipeProgress by remember {
        derivedStateOf { (abs(offsetX.value) / dismissThreshold).coerceIn(0f, 1f) }
    }

    val largeRadius = 24.dp
    val smallRadius = 6.dp

    val currentOffsetAbove by rememberUpdatedState(offsetAbove)
    val currentOffsetBelow by rememberUpdatedState(offsetBelow)

    val topStartRadius by animateDpAsState(
        targetValue = if (isFirst) largeRadius else lerp(smallRadius, largeRadius, max(swipeProgress, (currentOffsetAbove / dismissThreshold).coerceIn(0f, 1f))),
        label = "topStartRadius"
    )
    val topEndRadius by animateDpAsState(
        targetValue = if (isFirst) largeRadius else lerp(smallRadius, largeRadius, max(swipeProgress, (-currentOffsetAbove / dismissThreshold).coerceIn(0f, 1f))),
        label = "topEndRadius"
    )
    val bottomStartRadius by animateDpAsState(
        targetValue = if (isLast) largeRadius else lerp(smallRadius, largeRadius, max(swipeProgress, (currentOffsetBelow / dismissThreshold).coerceIn(0f, 1f))),
        label = "bottomStartRadius"
    )
    val bottomEndRadius by animateDpAsState(
        targetValue = if (isLast) largeRadius else lerp(smallRadius, largeRadius, max(swipeProgress, (-currentOffsetBelow / dismissThreshold).coerceIn(0f, 1f))),
        label = "bottomEndRadius"
    )
    val mainShape = RoundedCornerShape(
        topStart = topStartRadius, topEnd = topEndRadius,
        bottomStart = bottomStartRadius, bottomEnd = bottomEndRadius
    )

    DisposableEffect(notification.key) {
        onDispose {
            onOffsetChanged(0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                // Stay visible longer during dismissal to show the flight animation
                val fadeThreshold = if (isDismissing) dismissThreshold * 12f else dismissThreshold * 4f
                alpha = (1f - (abs(offsetX.value) / fadeThreshold)).coerceIn(0f, 1f)
            }
    ) {
        Surface(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        rawDragOffset = offsetX.value
                        isStuck = abs(rawDragOffset) < dismissThreshold
                    },
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val restickDist = dismissThreshold * 0.8f

                            val newRawDrag = rawDragOffset + delta
                            val newStuck = if (isStuck) {
                                abs(newRawDrag) < dismissThreshold
                            } else {
                                abs(newRawDrag) < restickDist
                            }

                            if (newStuck != isStuck) {
                                haptic.performHapticFeedback(if (newStuck) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.Confirm)
                                isStuck = newStuck
                            }

                            rawDragOffset = newRawDrag
                            val friction = 1.8f
                            val intendedOffset = rawDragOffset / if (isStuck) friction else 1f

                            val targetDrag = applyStretch(intendedOffset, dismissThreshold, 1f)
                                .coerceIn(-stretchLimit, stretchLimit)

                            offsetX.animateTo(
                                targetValue = targetDrag,
                                animationSpec = spring(
                                    dampingRatio = 0.65f,
                                    stiffness = 1500f
                                )
                            ) {
                                onOffsetChanged(value)
                            }
                        }
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val isDismiss = !isStuck || abs(velocity) > 4000f
                            if (isDismiss) {
                                isDismissing = true
                                // Moderate target to clear screen without "jumping"
                                val target = if (offsetX.value > 0) stretchLimit * 4 else -stretchLimit * 4
                                
                                // Sequential: Wait for flight (200ms) then dismiss
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = 200,
                                        easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
                                    )
                                ) {
                                    onOffsetChanged(value)
                                }

                                onDismiss()
                            } else {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    onOffsetChanged(value)
                                }
                                onOffsetChanged(0f)
                            }
                            isStuck = true
                        }
                    }
                ),
            shape = mainShape,
            color = colorScheme.surfaceBright.copy(alpha = 0.8f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val iconToDraw = notification.icon
                    if (iconToDraw != null) {
                        val iconBitmap = remember(iconToDraw) {
                            try {
                                iconToDraw.toBitmap(
                                    width = (40 * density.density).toInt().coerceAtLeast(1),
                                    height = (40 * density.density).toInt().coerceAtLeast(1)
                                ).asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(finalAppColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(finalContrastColor)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val hasTitle = !notification.title.isNullOrBlank()
                        val displayTitle = if (hasTitle) notification.title else notification.text
                        val displayText = if (hasTitle) notification.text else null

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            displayTitle?.let {
                                Text(
                                    text = it,
                                    color = colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = if (expanded) 3 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(top = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (notification.actions.isNotEmpty() || (displayText != null && displayText.length > 50) || (displayTitle != null && displayTitle.length > 40)) {
                                Surface(
                                    onClick = { expanded = !expanded },
                                    shape = RoundedCornerShape(8.dp),
                                    color = colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = formatNotificationTime(notification.postTime),
                                            color = colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = if (expanded) "Collapse" else "Expand",
                                            tint = colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = formatNotificationTime(notification.postTime),
                                    color = colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        displayText?.let {
                            Text(
                                text = it,
                                color = colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                maxLines = if (expanded) 10 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = expanded && notification.actions.isNotEmpty(),
                    enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = 800f)),
                    exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = 800f))
                ) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        var selectedActionForReply by remember { mutableStateOf<LauncherNotificationAction?>(null) }
                        var replyTextValue by remember { mutableStateOf("") }
                        val context = LocalContext.current

                        AnimatedVisibility(
                            visible = selectedActionForReply != null,
                            enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = 800f)),
                            exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = 800f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextField(
                                    value = replyTextValue,
                                    onValueChange = { replyTextValue = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Type a message...", fontSize = 13.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = colorScheme.surfaceContainerLowest.copy(alpha = 0.2f),
                                        unfocusedContainerColor = colorScheme.surfaceContainerLowest.copy(alpha = 0.2f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                                )

                                Surface(
                                    onClick = {
                                        selectedActionForReply?.let { action ->
                                            Log.d("XenonNotification", "Replying to action: ${action.title}, text: $replyTextValue")
                                            if (replyTextValue.isNotBlank() && action.remoteInput != null) {
                                                val results = Bundle().apply {
                                                    putString(action.remoteInput.resultKey, replyTextValue)
                                                }
                                                val fillInIntent = Intent().apply {
                                                    RemoteInput.addResultsToIntent(arrayOf(action.remoteInput), this, results)
                                                }
                                                try {
                                                    Log.d("XenonNotification", "Sending reply intent: ${action.actionIntent}")

                                                    val options = ActivityOptions.makeBasic()
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                                                    }

                                                    action.actionIntent?.send(context, 0, fillInIntent, null, null, null, options.toBundle())
                                                } catch (e: Exception) {
                                                    Log.e("XenonNotification", "Failed to send reply intent", e)
                                                }
                                                selectedActionForReply = null
                                                replyTextValue = ""
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    color = finalAppColor,
                                    modifier = Modifier.size(40.dp),
                                    enabled = replyTextValue.isNotBlank()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                            contentDescription = "Send",
                                            tint = finalContrastColor,
                                            modifier = Modifier.size(24.dp).padding(start = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val blockPagerScrollActions = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (source == NestedScrollSource.UserInput && abs(available.x) > abs(available.y)) {
                                        view.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    return Offset.Zero
                                }

                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    return if (source == NestedScrollSource.UserInput) {
                                        Offset(x = available.x, y = 0f)
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(blockPagerScrollActions)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            notification.actions.forEach { action ->
                                Surface(
                                    onClick = {
                                        if (action.remoteInput != null) {
                                            Log.d("XenonNotification", "Action clicked (Reply): ${action.title}")
                                            if (selectedActionForReply == action) {
                                                selectedActionForReply = null
                                            } else {
                                                selectedActionForReply = action
                                                replyTextValue = ""
                                            }
                                        } else {
                                            Log.d("XenonNotification", "Action clicked: ${action.title}")
                                            try {
                                                val options = ActivityOptions.makeBasic()
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                    options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                                                }

                                                action.actionIntent?.let { intent ->
                                                    Log.d("XenonNotification", "Sending actionIntent with context: $intent")
                                                    intent.send(context, 0, null, null, null, null, options.toBundle())
                                                } ?: Log.w("XenonNotification", "No actionIntent found for action")
                                            } catch (e: Exception) {
                                                Log.e("XenonNotification", "Failed to send actionIntent with context", e)
                                                try {
                                                    Log.d("XenonNotification", "Retrying actionIntent without context")
                                                    action.actionIntent?.send()
                                                } catch (e2: Exception) {
                                                    Log.e("XenonNotification", "Failed to send actionIntent without context", e2)
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedActionForReply == action) finalAppColor else Color.Transparent,
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = action.title,
                                            color = if (selectedActionForReply == action) finalContrastColor else colorScheme.onSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatNotificationTime(postTime: Long): String {
    val diffMinutes = (System.currentTimeMillis() - postTime) / 60000
    return when {
        diffMinutes < 1 -> "now"
        diffMinutes < 60 -> "${diffMinutes}m"
        else -> "${diffMinutes / 60}h"
    }
}

private fun applyStretch(offset: Float, threshold: Float, stretchFactor: Float = 0.5f): Float {
    val s = sign(offset)
    val a = abs(offset)

    if (a <= threshold) {
        return offset
    }

    val overscroll = a - threshold
    val stretchedOverscroll = overscroll.pow(1f - stretchFactor)
    return s * (threshold + stretchedOverscroll)
}