package com.xenonware.launcher.ui.res.dock

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.launcher.ui.res.AtAGlance
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun StatusSection(
    isExpanded: Boolean,
    notificationCount: Int,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    progress: Float,
    isCharging: Boolean,
    buttonAlpha: Float,
    onExpand: () -> Unit,
    onClickExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBell by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var prevNotificationCount by remember { mutableIntStateOf(notificationCount) }

    LaunchedEffect(notificationCount) {
        if (notificationCount > prevNotificationCount) {
            showBell = true
            delay(2000)
            showBell = false
        }
        prevNotificationCount = notificationCount
    }

    LaunchedEffect(isCharging) {
        if (isCharging) {
            showFlash = true
            delay(1000)
            showFlash = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "statusAnimations")
    val bellRotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bellRotation"
    )

    val flashOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashOffset"
    )

    val verticalPadding by animateDpAsState(
        targetValue = if (isExpanded) 4.dp else 12.dp, label = "statusPadding"
    )

    val strokeColor = remember(progress) {
        when {
            progress <= 0.15f -> Color.Red
            progress <= 0.20f -> {
                val fraction = (progress - 0.15f) / 0.05f
                lerp(Color.Red, Color.Yellow, fraction)
            }

            progress <= 0.25f -> {
                val fraction = (progress - 0.20f) / 0.05f
                lerp(Color.Yellow, Color.Green, fraction)
            }

            else -> Color.Green
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isExpanded && isCharging -> Color.Transparent // Use brush background
            !isExpanded && notificationCount > 0 && !isCharging -> colorScheme.primaryContainer
            else -> colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
        }, label = "statusBg"
    )
    val contentColor by animateColorAsState(
        targetValue = colorScheme.onSurface,
        label = "statusContent"
    )

    val strokeRotationProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(500),
        label = "strokeRotationProgress"
    )

    val strokeWidth by animateDpAsState(
        targetValue = if (isExpanded) 2.dp else 1.dp, label = "strokeWidth"
    )

    val borderBrush = remember(strokeRotationProgress, progress, strokeColor) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val start = Offset(
                    x = 0f, y = size.height * (1f - strokeRotationProgress)
                )
                val end = Offset(
                    x = size.width * strokeRotationProgress, y = 0f
                )
                return LinearGradientShader(
                    from = start, to = end, colors = listOf(
                        strokeColor,
                        strokeColor,
                        Color.Transparent,
                        Color.Transparent
                    ), colorStops = listOf(0.0f, progress, progress, 1.0f)
                )
            }
        }
    }

    val baseBgColor = colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
    val backgroundBrush = remember(progress, strokeColor, isCharging, baseBgColor) {
        if (!isCharging) null else object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val solidColor = strokeColor.copy(alpha = 0.5f)
                return LinearGradientShader(
                    from = Offset(0f, size.height),
                    to = Offset(0f, 0f),
                    colors = listOf(
                        solidColor,
                        solidColor,
                        baseBgColor,
                        baseBgColor
                    ),
                    colorStops = listOf(0.0f, progress, progress, 1.0f)
                )
            }
        }
    }

    Surface(
        onClick = {
            if (isExpanded) {
                onClickExpanded()
            } else {
                onExpand()
            }
        },
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = verticalPadding)
            .then(if (isExpanded) Modifier.fillMaxWidth() else Modifier.width(32.dp)),
        shape = CircleShape,
        color = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(strokeWidth, borderBrush)
    ) {
        Box(
            modifier = if (!isExpanded && isCharging && backgroundBrush != null) {
                Modifier.background(backgroundBrush)
            } else Modifier,
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isExpanded, transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                        animationSpec = tween(300)
                    )
                }, label = "statusTransition"
            ) { targetExpanded ->
                if (targetExpanded) {
                    AtAGlance(
                        currentTime,
                        currentDate,
                        weatherTemp,
                        weatherCondition,
                        notificationCount
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = when {
                                showBell -> StatusViewState.Bell
                                showFlash -> StatusViewState.Flash
                                isCharging -> StatusViewState.Battery
                                else -> StatusViewState.Default
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(
                                    animationSpec = tween(250)
                                )
                            },
                            label = "collapsedContent"
                        ) { state ->
                            when (state) {
                                StatusViewState.Bell -> {
                                    Icon(
                                        Icons.Rounded.Notifications,
                                        null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer {
                                                rotationZ = bellRotation
                                                transformOrigin = TransformOrigin(0.5f, 0.25f)
                                            }
                                    )
                                }

                                StatusViewState.Flash -> {
                                    Icon(
                                        Icons.Rounded.ElectricBolt,
                                        null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .offset { IntOffset(flashOffset.dp.toPx().roundToInt(), 0) }
                                    )
                                }

                                StatusViewState.Battery -> {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                StatusViewState.Default -> {
                                    Icon(
                                        Icons.Rounded.Info,
                                        null,
                                        modifier = Modifier.size(24.dp)
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

private enum class StatusViewState {
    Default, Bell, Flash, Battery
}

fun openNotifications(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
        expandMethod.isAccessible = true
        expandMethod.invoke(statusBarService)
    } catch (_: Exception) {
        try {
            val intent = Intent("android.intent.action.SHOW_NOTIFICATIONS_PANEL")
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }
}
