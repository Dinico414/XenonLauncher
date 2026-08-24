package com.xenonware.launcher.ui.res.notification

import android.app.ActivityOptions
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.xenonware.launcher.util.blockHorizontalPagerSwipe
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItem(
    modifier: Modifier = Modifier,
    notification: LauncherNotification,
    appColor: Color,
    isFirst: Boolean,
    isLast: Boolean,
    offsetAbove: Float = 0f,
    offsetBelow: Float = 0f,
    replyingNotificationKey: String? = null,
    onReplyOpen: (String?) -> Unit = {},
    onReplyBoundsChanged: (Rect) -> Unit = {},
    onOffsetChanged: (Float) -> Unit = {},
    onSwipeActiveChange: (Boolean) -> Unit = {},
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    val context = LocalContext.current

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

    // The gesture loop below is keyed on notification.key and therefore captures the
    // callbacks from the composition in which it started. Keep live references so a
    // list reorder can never dismiss a stale notification.
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnOffsetChanged by rememberUpdatedState(onOffsetChanged)
    val currentOnSwipeActiveChange by rememberUpdatedState(onSwipeActiveChange)
    val currentOnReplyBoundsChanged by rememberUpdatedState(onReplyBoundsChanged)

    // Only the notification currently being replied to reports its position upward;
    // the page uses it to lift this item clear of the keyboard.
    val isReplyTarget = replyingNotificationKey == notification.key
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = expanded || isReplyTarget) {
        if (isReplyTarget) {
            onReplyOpen(null)
        } else {
            expanded = false
        }
    }

    val swipeProgress by remember {
        derivedStateOf { (abs(offsetX.value) / dismissThreshold).coerceIn(0f, 1f) }
    }

    val largeRadius = 24.dp
    val smallRadius = 6.dp

    val currentOffsetAbove by rememberUpdatedState(offsetAbove)
    val currentOffsetBelow by rememberUpdatedState(offsetBelow)

    var selectedActionForReply by remember { mutableStateOf<LauncherNotificationAction?>(null) }
    val replyState = rememberTextFieldState()

    LaunchedEffect(replyingNotificationKey) {
        if (replyingNotificationKey != notification.key) {
            selectedActionForReply = null
            replyState.setTextAndPlaceCursorAtEnd("")
        }
    }

    fun sendReply() {
        val action = selectedActionForReply ?: return
        val remoteInput = action.remoteInput ?: return
        val text = replyState.text.toString()
        if (text.isBlank()) return

        val results = Bundle().apply {
            putString(remoteInput.resultKey, text)
        }
        val fillInIntent = Intent().apply {
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), this, results)
        }
        try {
            val options = ActivityOptions.makeBasic()
            options.pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED

            action.actionIntent?.send(context, 0, fillInIntent, null, null, null, options.toBundle())
        } catch (_: Exception) {
        }
        selectedActionForReply = null
        replyState.clearText()
        onReplyOpen(null)
    }

    // Without focus the IME never opens, so the lift would never trigger. Wait a
    // couple of frames so the TextField is actually attached before requesting it.
    LaunchedEffect(selectedActionForReply) {
        if (selectedActionForReply != null) {
            repeat(3) { withFrameNanos { } }
            runCatching { focusRequester.requestFocus() }
        }
    }

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
            onSwipeActiveChange(false)
        }
    }

    // --- Swipe logic, extracted so the raw pointer loop can drive it ---

    fun handleDragStart() {
        rawDragOffset = offsetX.value
        isStuck = abs(rawDragOffset) < dismissThreshold
        currentOnSwipeActiveChange(true)
    }

    fun handleDelta(delta: Float) {
        coroutineScope.launch {
            val restickDist = dismissThreshold * 0.8f

            val newRawDrag = rawDragOffset + delta
            val newStuck = if (isStuck) {
                abs(newRawDrag) < dismissThreshold
            } else {
                abs(newRawDrag) < restickDist
            }

            if (newStuck != isStuck) {
                haptic.performHapticFeedback(
                    if (newStuck) HapticFeedbackType.GestureThresholdActivate
                    else HapticFeedbackType.Confirm
                )
                isStuck = newStuck
            }

            rawDragOffset = newRawDrag
            val friction = 1.8f
            val intendedOffset = rawDragOffset / if (isStuck) friction else 1f

            val targetDrag = 1f.applyStretch(intendedOffset, dismissThreshold)
                .coerceIn(-stretchLimit, stretchLimit)

            offsetX.animateTo(
                targetValue = targetDrag,
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = 1500f
                )
            ) {
                currentOnOffsetChanged(value)
            }
        }
    }

    fun handleDragStop(velocity: Float) {
        currentOnSwipeActiveChange(false)
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
                    currentOnOffsetChanged(value)
                }

                currentOnDismiss()
            } else {
                offsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) {
                    currentOnOffsetChanged(value)
                }
                currentOnOffsetChanged(0f)
            }
            isStuck = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                if (isReplyTarget) currentOnReplyBoundsChanged(it.boundsInRoot())
            }
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
                .blockHorizontalPagerSwipe()
                // Raw pointer handling instead of Modifier.draggable: the horizontal
                // pointer changes are consumed, so the parent HorizontalPager never
                // sees enough movement to start its own page swipe. Nested scroll
                // cannot be used here because draggable/this gesture does not
                // dispatch it, and requestDisallowInterceptTouchEvent only affects
                // the Android View hierarchy, not Compose's pager.
                .pointerInput(notification.key) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Do not consume anything yet: if the user moves vertically
                        // first, the LazyColumn wins and this returns null.
                        var overSlop = 0f
                        val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            overSlop = over
                        } ?: return@awaitEachGesture

                        handleDragStart()

                        val tracker = VelocityTracker()
                        tracker.addPointerInputChange(drag)
                        handleDelta(overSlop)

                        val completed = horizontalDrag(drag.id) { change ->
                            tracker.addPointerInputChange(change)
                            handleDelta(change.positionChange().x)
                            change.consume()
                        }

                        handleDragStop(
                            if (completed) tracker.calculateVelocity().x else 0f
                        )
                    }
                },
            shape = mainShape,
            color = colorScheme.surfaceBright.copy(alpha = 0.8f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val iconToDraw = notification.icon
                    val stableKey = remember(notification.iconKey, notification.key, iconToDraw) {
                        notification.iconKey ?: notification.key
                    }

                    Crossfade(targetState = stableKey to iconToDraw, label = "notification_icon_fade") { (_, targetIcon) ->
                        if (targetIcon != null) {
                            val iconBitmap = remember(stableKey) {
                                try {
                                    targetIcon.toBitmap(
                                        width = (40 * density.density).toInt().coerceAtLeast(1),
                                        height = (40 * density.density).toInt().coerceAtLeast(1)
                                    ).asImageBitmap()
                                } catch (_: Exception) {
                                    null
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(finalAppColor, RoundedCornerShape(12.dp)),
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
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val hasTitle = !notification.title.isNullOrBlank()
                        val displayTitle = if (hasTitle) notification.title else notification.text

                        displayTitle?.let {
                            Text(
                                text = it,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = if (expanded) 3 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Sender Icon / Profile Picture (Same size as package icon, stays in row)
                    val showSenderIcon = if (expanded) {
                        notification.senderIcon != null
                    } else {
                        notification.senderIcon != null && notification.mediaImage == null
                    }

                    if (showSenderIcon) {
                        val senderBitmap = remember(notification.senderIcon) {
                            try {
                                notification.senderIcon?.toBitmap()?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (senderBitmap != null) {
                            Image(
                                bitmap = senderBitmap,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Collapsed Media Thumbnail
                    if (!expanded && notification.mediaImage != null) {
                        val thumbnailBitmap = remember(notification.mediaImage) {
                            try {
                                notification.mediaImage.toBitmap().asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (thumbnailBitmap != null) {
                            Image(
                                bitmap = thumbnailBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.surfaceContainer),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Expand Button Area
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.height(40.dp)
                    ) {
                        val hasTitle = !notification.title.isNullOrBlank()
                        val displayText = if (hasTitle) notification.text else null
                        val displayTitle = if (hasTitle) notification.title else notification.text

                        val canExpand = notification.actions.isNotEmpty() ||
                                (displayText != null && displayText.length > 50) ||
                                (displayTitle != null && displayTitle.length > 40) ||
                                notification.mediaImage != null ||
                                notification.senderIcon != null

                        if (canExpand) {
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
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Message content (Below the top row)
                val hasTitle = !notification.title.isNullOrBlank()
                val bodyText = if (hasTitle) notification.text else null

                if (!bodyText.isNullOrBlank()) {
                    Text(
                        text = bodyText,
                        color = colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = if (expanded) 10 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp) // Starts at the very beginning
                    )
                }

                // Expanded Media (Big) with Aspect Ratio
                if (expanded && notification.mediaImage != null) {
                    var aspectRatio by remember { mutableFloatStateOf(16f / 9f) }
                    val mediaBitmap = remember(notification.mediaImage) {
                        try {
                            val bmp = notification.mediaImage.toBitmap()
                            aspectRatio = (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.2f, 2.5f)
                            bmp.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (mediaBitmap != null) {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Image(
                                bitmap = mediaBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainer)
                                    .then(
                                        if (aspectRatio < 1f) {
                                            // Vertical: Cap height at 300dp (approx square size on most screens)
                                            // and let the width adjust to maintain aspect ratio
                                            Modifier.height(300.dp).aspectRatio(aspectRatio)
                                        } else {
                                            // Horizontal/Square: Fill width
                                            Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                                        }
                                    ),
                                contentScale = ContentScale.Fit
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

                        AnimatedVisibility(
                            visible = selectedActionForReply != null,
                            enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = 800f)),
                            exit = fadeOut() + shrinkVertically(animationSpec = spring(stiffness = 800f))
                        ) {
                            val keyboardScrollBlocker = remember {
                                object : NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: NestedScrollSource
                                    ): Offset {
                                        // Consume ALL scroll that originates inside this row to ensure
                                        // it never reaches the parent LazyColumn.
                                        return if (source == NestedScrollSource.UserInput) available else Offset.Zero
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .nestedScroll(keyboardScrollBlocker),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val replyScrollState = rememberScrollState()
                                val replyInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                val lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5)

                                BasicTextField(
                                    state = replyState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .padding(vertical = 4.dp) // Small buffer so clip doesn't cut text
                                        .clip(RoundedCornerShape(16.dp))
                                        .drawTextFieldScrollbar(replyScrollState, colorScheme.primary),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    onKeyboardAction = { sendReply() },
                                    cursorBrush = SolidColor(colorScheme.primary),
                                    lineLimits = lineLimits,
                                    scrollState = replyScrollState,
                                    interactionSource = replyInteractionSource,
                                    decorator = TextFieldDefaults.decorator(
                                        state = replyState,
                                        enabled = true,
                                        lineLimits = lineLimits,
                                        outputTransformation = null,
                                        interactionSource = replyInteractionSource,
                                        placeholder = { Text("Type a message...", fontSize = 13.sp) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = colorScheme.surfaceContainerLowest.copy(alpha = 0.2f),
                                            unfocusedContainerColor = colorScheme.surfaceContainerLowest.copy(alpha = 0.2f),
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        container = {
                                            TextFieldDefaults.Container(
                                                enabled = true,
                                                isError = false,
                                                interactionSource = replyInteractionSource,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = colorScheme.surfaceContainerLowest.copy(alpha = 0.2f),
                                                    unfocusedContainerColor = colorScheme.surfaceContainerLowest.copy(alpha = 0.2f),
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    )
                                )

                                Surface(
                                    onClick = { sendReply() },
                                    shape = CircleShape,
                                    color = finalAppColor,
                                    modifier = Modifier.size(40.dp),
                                    enabled = replyState.text.isNotEmpty()
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
                                            if (selectedActionForReply == action) {
                                                selectedActionForReply = null
                                                onReplyOpen(null)
                                            } else {
                                                selectedActionForReply = action
                                                replyState.setTextAndPlaceCursorAtEnd("")
                                                onReplyOpen(notification.key)
                                            }
                                        } else {
                                            try {
                                                val options = ActivityOptions.makeBasic()
                                                options.pendingIntentBackgroundActivityStartMode =
                                                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED

                                                action.actionIntent?.send(context, 0, null, null, null, null, options.toBundle())
                                            } catch (_: Exception) {
                                                try {
                                                    action.actionIntent?.send()
                                                } catch (_: Exception) {
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

fun Modifier.drawTextFieldScrollbar(
    state: androidx.compose.foundation.ScrollState,
    color: Color
): Modifier = drawWithContent {
    drawContent()
    if (state.maxValue > 0) {
        val viewportHeight = size.height
        val totalHeight = state.maxValue + viewportHeight
        val scrollbarHeight = (viewportHeight / totalHeight) * viewportHeight
        val scrollbarOffset = (state.value.toFloat() / totalHeight) * viewportHeight

        drawRoundRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(size.width - 10.dp.toPx(), scrollbarOffset + 4.dp.toPx()),
            size = Size(4.dp.toPx(), (scrollbarHeight - 8.dp.toPx()).coerceAtLeast(16.dp.toPx())),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
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

private fun Float.applyStretch(offset: Float, threshold: Float): Float {
    val s = sign(offset)
    val a = abs(offset)

    if (a <= threshold) {
        return offset
    }

    val overscroll = a - threshold
    val stretchedOverscroll = overscroll.pow(1f - this)
    return s * (threshold + stretchedOverscroll)
}