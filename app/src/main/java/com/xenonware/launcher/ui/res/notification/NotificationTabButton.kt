package com.xenonware.launcher.ui.res.notification

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.util.ColorUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NotificationTabButton(
    app: AppInfo?,
    notificationIcon: Drawable?,
    notificationCount: Int,
    isSelected: Boolean,
    appColor: Color,
    contrastColor: Color,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    isOverDelete: (androidx.compose.ui.geometry.Rect) -> Boolean = { false },
    iconKey: String? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val view = androidx.compose.ui.platform.LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var itemPos by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }

    val iconScale = remember { Animatable(1f) }
    var prevCount by remember { mutableIntStateOf(notificationCount) }

    LaunchedEffect(notificationCount) {
        if (notificationCount > prevCount) {
            iconScale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
        prevCount = notificationCount
    }

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isDragging || isPressed -> 4.dp
            isSelected -> 12.dp
            else -> 20.dp
        },
        label = "corner_radius"
    )

    val finalAppColor = if (appColor == Color.Unspecified) colorScheme.primary else appColor
    val finalContrastColor = if (appColor == Color.Unspecified) {
        ColorUtils.getContrastColor(finalAppColor)
    } else {
        contrastColor
    }

    val backgroundColor = if (isSelected) finalAppColor else colorScheme.surfaceDim.copy(alpha = 0.8f)
    val iconColor = if (isSelected) finalContrastColor else colorScheme.onSurface

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        color = if (isDragging) backgroundColor.copy(alpha = 0.9f) else backgroundColor,
        modifier = modifier
            .height(40.dp)
            .onGloballyPositioned { 
                itemPos = it.positionInRoot()
                itemSize = it.size
            }
            .offset {
                IntOffset(
                    dragOffset.value.x.roundToInt(),
                    dragOffset.value.y.roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { 
                        isDragging = true
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            // Only update X for horizontal movement
                            dragOffset.snapTo(dragOffset.value.copy(x = dragOffset.value.x + dragAmount.x))
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val currentRect = androidx.compose.ui.geometry.Rect(
                            itemPos + dragOffset.value,
                            androidx.compose.ui.geometry.Size(itemSize.width.toFloat(), itemSize.height.toFloat())
                        )
                        if (isOverDelete(currentRect)) {
                            onDismiss()
                            scope.launch {
                                dragOffset.snapTo(Offset.Zero)
                            }
                        } else {
                            scope.launch {
                                dragOffset.animateTo(
                                    Offset.Zero,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            dragOffset.animateTo(Offset.Zero)
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxHeight()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val iconToDraw = notificationIcon ?: app?.icon
            val stableKey = remember(iconKey, app?.packageName, iconToDraw) {
                iconKey ?: app?.packageName ?: iconToDraw?.hashCode()?.toString() ?: "no_icon"
            }

            Crossfade(targetState = stableKey to iconToDraw, label = "tab_icon_fade") { (_, targetIcon) ->
                if (targetIcon != null) {
                    val iconBitmap = remember(stableKey) {
                        try {
                            targetIcon.toBitmap(width = 40, height = 40).asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(iconScale.value),
                            colorFilter = ColorFilter.tint(iconColor)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(iconScale.value),
                            tint = iconColor
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .scale(iconScale.value),
                        tint = iconColor
                    )
                }
            }

            AnimatedVisibility(
                visible = notificationCount > 1,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                        color = iconColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = QuicksandTitleVariable
                    )
                }
            }
        }
    }
}

