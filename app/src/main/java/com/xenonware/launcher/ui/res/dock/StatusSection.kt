package com.xenonware.launcher.ui.res.dock

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.launcher.ui.res.AtAGlance

@Composable
fun StatusSection(
    isExpanded: Boolean,
    notificationCount: Int,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    progress: Float,
    buttonAlpha: Float,
    onExpand: () -> Unit,
    onClickExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalPadding by animateDpAsState(
        targetValue = if (isExpanded) 4.dp else 12.dp, label = "statusPadding"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isExpanded && notificationCount > 0 -> colorScheme.primaryContainer
            else -> colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
        }, label = "statusBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !isExpanded && notificationCount > 0 -> colorScheme.onPrimaryContainer
            else -> colorScheme.onSurface
        }, label = "statusContent"
    )

    val strokeRotationProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(500),
        label = "strokeRotationProgress"
    )

    val strokeWidth by animateDpAsState(
        targetValue = if (isExpanded) 2.dp else 1.dp, label = "strokeWidth"
    )

    val borderBrush = remember(strokeRotationProgress, progress) {
        val strokeColor = when {
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
                    if (notificationCount > 0) {
                        Text(
                            text = notificationCount.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
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
