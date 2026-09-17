package com.xenonware.launcher.ui.res.notification

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.ExtraLargeCornerRadius
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.MediumLargePadding
import com.xenon.mylibrary.values.MediumSmallSpacer
import com.xenon.mylibrary.values.SmallCornerRadius
import com.xenon.mylibrary.values.SmallIconSize
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.ColorUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NotificationTabButton(
    app: AppInfo?,
    notificationIcon: Drawable? = null,
    notificationIconBitmap: ImageBitmap? = null,
    overrideIcon: ImageVector? = null,
    notificationCount: Int,
    isSelected: Boolean,
    appColor: Color,
    contrastColor: Color,
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    isOverDelete: (Rect) -> Boolean = { false },
    onDragStateChanged: (Boolean) -> Unit = {},
    iconKey: String? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var itemPos by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var isDragging by remember { mutableStateOf(false) }

    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentIsOverDelete by rememberUpdatedState(isOverDelete)
    val currentOnDragStateChanged by rememberUpdatedState(onDragStateChanged)

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

    val iconScaleModifier = Modifier.graphicsLayer {
        scaleX = iconScale.value
        scaleY = iconScale.value
    }

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isDragging || isPressed -> SmallCornerRadius
            isSelected -> LargeMediumCornerRadius
            else -> ExtraLargeCornerRadius
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
            .height(ExtraBigSpacing)
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
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnDragStateChanged(true)
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
                        currentOnDragStateChanged(false)
                        val currentRect = Rect(
                            itemPos + dragOffset.value,
                            Size(itemSize.width.toFloat(), itemSize.height.toFloat())
                        )
                        if (currentIsOverDelete(currentRect)) {
                            currentOnDismiss()
                        }

                        scope.launch {
                            dragOffset.animateTo(
                                Offset.Zero,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        currentOnDragStateChanged(false)
                        scope.launch {
                            dragOffset.animateTo(Offset.Zero)
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = MediumLargePadding)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val iconToDraw = notificationIcon ?: app?.icon
            val stableKey = remember(iconKey, app?.packageName, overrideIcon) {
                iconKey ?: app?.packageName ?: overrideIcon?.name ?: "no_icon"
            }

            Crossfade(targetState = stableKey, label = "tab_icon_fade") { currentKey ->
                if (overrideIcon != null) {
                    Icon(
                        imageVector = overrideIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(SmallIconSize)
                            .then(iconScaleModifier),
                        tint = iconColor
                    )
                } else {
                    val iconBitmap = remember(currentKey, notificationIconBitmap, iconToDraw) {
                        notificationIconBitmap ?: try {
                            iconToDraw?.toBitmap(width = 40, height = 40)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ExtraLargeSpacing)
                                .then(iconScaleModifier),
                            colorFilter = ColorFilter.tint(iconColor)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = null,
                            modifier = Modifier
                                .size(SmallIconSize)
                                .then(iconScaleModifier),
                            tint = iconColor
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = notificationCount > 1,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(MediumSmallSpacer))
                    Text(
                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                        color = iconColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = mainFontFamily
                    )
                }
            }
        }
    }
}