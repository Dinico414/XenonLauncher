package com.xenonware.launcher.ui.res.dock

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.launcher.ui.res.AtAGlance
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun StatusSection(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    notificationCount: Int,
    calendarEventCount: Int = 0,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    progress: Float,
    isCharging: Boolean,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val buttonAlpha = dockButtonAlpha()

    val pillInteractionSource = remember { MutableInteractionSource() }
    var showBell by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var prevNotificationCount by remember { mutableIntStateOf(notificationCount) }

    LaunchedEffect(notificationCount) {
        if (notificationCount > prevNotificationCount) {
            showBell = true
            delay(2000.milliseconds)
            showBell = false
        } else {
            // Ensure bell is hidden if count drops or stays same
            showBell = false
        }
        prevNotificationCount = notificationCount
    }

    LaunchedEffect(isCharging) {
        if (isCharging) {
            showFlash = true
            delay(1000.milliseconds)
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

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
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

    val chargingAlpha by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = tween(500),
        label = "chargingAlpha"
    )

    val bgChargingAlpha by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = if (isCharging) tween(500) else snap(),
        label = "bgChargingAlpha"
    )

    val baseBgColor = colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
    val backgroundBrush = remember(progress, strokeColor, baseBgColor, bgChargingAlpha) {
        if (bgChargingAlpha == 0f) null else object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val r = if (size.width > size.height) size.height / 2 else size.width / 2
                val h = size.height
                val w = size.width

                // Calculate mapped progress to match the stroke's vertical height
                val mappedProgress = if (w < h) {
                    val lTotal = PI.toFloat() * r + (h - 2 * r)
                    val d = progress * lTotal
                    val y = when {
                        d < PI.toFloat() * r / 2f -> (h - r) + r * cos(d / r)
                        d < PI.toFloat() * r / 2f + (h - 2 * r) -> (h - r) - (d - PI.toFloat() * r / 2f)
                        else -> {
                            val dPrime = d - (PI.toFloat() * r / 2f + h - 2 * r)
                            r - r * sin(dPrime / r)
                        }
                    }
                    ((h - y) / h).coerceIn(0f, 1f)
                } else progress

                val solidColor = strokeColor.copy(alpha = 0.5f * bgChargingAlpha)
                val baseColorWithAlpha = baseBgColor.copy(alpha = baseBgColor.alpha * bgChargingAlpha)

                // Always Bottom to Top
                val start = Offset(0f, size.height)
                val end = Offset(0f, 0f)

                return LinearGradientShader(
                    from = start,
                    to = end,
                    colors = listOf(
                        solidColor,
                        solidColor,
                        baseColorWithAlpha,
                        baseColorWithAlpha
                    ),
                    colorStops = listOf(0.0f, mappedProgress, mappedProgress, 1.0f)
                )
            }
        }
    }

    Surface(
        onClick = {
            if (isExpanded) openNotifications(context) else onExpand()
        },
        interactionSource = pillInteractionSource,
        modifier = modifier.dockSectionSize(isExpanded),
        shape = DockSectionShape,
        color = backgroundColor,
        contentColor = contentColor,
        border = null // Border is drawn manually for better control over rounded ends
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isExpanded && backgroundBrush != null) {
                        Modifier.background(backgroundBrush)
                    } else Modifier
                )
                .then(
                    if (chargingAlpha > 0f) {
                        Modifier.drawWithContent {
                            drawContent()

                            val sw = strokeWidth.toPx()
                            val w = size.width
                            val h = size.height
                            val s = strokeRotationProgress
                            val r = if (w > h) h / 2 else w / 2

                            val pillPath = Path().apply {
                                moveTo(r, 0f)
                                lineTo(w - r, 0f)
                                arcTo(Rect(w - 2 * r, 0f, w, 2 * r), 270f, 90f, false)
                                lineTo(w, h - r)
                                arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
                                lineTo(r, h)
                                arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
                                lineTo(0f, r)
                                arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
                                close()
                            }

                            val pm = PathMeasure().apply { setPath(pillPath, true) }
                            val totalLen = pm.length

                            val dVertical =
                                (w - 2 * r) + (PI.toFloat() * r / 2f) + (h - 2 * r) + (PI.toFloat() * r / 2f) + (w - 2 * r) / 2f
                            val dHorizontal =
                                2 * (w - 2 * r) + (PI.toFloat() * r) + (h - 2 * r) + (PI.toFloat() * r / 2f) + (h - 2 * r) / 2f

                            val dOrigin = (dVertical * (1 - s) + dHorizontal * s) % totalLen
                            val segLen = (totalLen / 2f) * progress

                            val colorWithAlpha =
                                strokeColor.copy(alpha = strokeColor.alpha * chargingAlpha)

                            fun drawWrappedSegment(startDist: Float, endDist: Float) {
                                val d1 = startDist % totalLen
                                val d2 = endDist % totalLen

                                val actualStart = if (d1 < 0) d1 + totalLen else d1
                                val actualEnd = if (d2 < 0) d2 + totalLen else d2

                                if (actualStart > actualEnd) {
                                    val s1 = Path()
                                    pm.getSegment(actualStart, totalLen, s1)
                                    drawPath(
                                        s1,
                                        colorWithAlpha,
                                        style = Stroke(sw, cap = StrokeCap.Round)
                                    )
                                    val s2 = Path()
                                    pm.getSegment(0f, actualEnd, s2)
                                    drawPath(
                                        s2,
                                        colorWithAlpha,
                                        style = Stroke(sw, cap = StrokeCap.Round)
                                    )
                                } else {
                                    val s1 = Path()
                                    pm.getSegment(actualStart, actualEnd, s1)
                                    drawPath(
                                        s1,
                                        colorWithAlpha,
                                        style = Stroke(sw, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            drawWrappedSegment(dOrigin - segLen, dOrigin)
                            drawWrappedSegment(dOrigin, dOrigin + segLen)

                        }.drawBehind {
                            val color = strokeColor.copy(alpha = 0.4f * chargingAlpha)
                            val radius = if (isExpanded) size.width * 1.5f else size.height * 1.5f
                            val p1 = rippleProgress
                            val p2 = (rippleProgress + 0.5f) % 1f

                            listOf(p1, p2).forEach { p ->
                                val dimension = if (isExpanded) size.width else size.height
                                // Extend travel range to ensure the wave enters and exits fully
                                val travelRange = dimension + radius * 0.5f
                                val currentPos = p * travelRange - (radius * 0.25f)

                                val center = if (isExpanded) {
                                    // Moving from start to end
                                    Offset(currentPos - radius * 0.95f, size.height / 2)
                                } else {
                                    // Moving from bottom to top
                                    Offset(
                                        size.width / 2,
                                        (size.height - currentPos) + radius * 0.95f
                                    )
                                }

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0.65f to Color.Transparent,
                                        0.85f to color,
                                        1.0f to Color.Transparent,
                                        center = center,
                                        radius = radius
                                    ),
                                    center = center,
                                    radius = radius
                                )
                            }
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isExpanded, transitionSpec = {
                    if (targetState) {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                            animationSpec = tween(50)
                        )
                    } else {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(
                            animationSpec = tween(300)
                        )
                    }
                }, label = "statusTransition"
            ) { targetExpanded ->
                if (targetExpanded) {
                    AtAGlance(
                        currentTime,
                        currentDate,
                        weatherTemp,
                        weatherCondition,
                        notificationCount,
                        calendarEventCount = calendarEventCount,
                        onTimeClick = onTimeClick,
                        onDateClick = onDateClick,
                        onWeatherClick = onWeatherClick,
                        pillInteractionSource = pillInteractionSource
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
                                (fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.8f))
                                    .togetherWith(fadeOut(animationSpec = tween(250)) + scaleOut(targetScale = 0.8f))
                            },
                            contentAlignment = Alignment.Center,
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
                                            .offset {
                                                IntOffset(
                                                    flashOffset.dp.toPx().roundToInt(),
                                                    0
                                                )
                                            }
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

@Composable
fun StatusCounters(
    notificationCount: Int,
    calendarEventCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (notificationCount > 0) {
            NotificationCounterBadge(count = notificationCount)
        }
        if (calendarEventCount > 0) {
            CalendarCounterIcon(count = calendarEventCount)
        }
    }
}

@Composable
fun NotificationCounterBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = colorScheme.primary,
    contentColor: Color = colorScheme.onPrimary
) {
    val text = if (count > 99) "99+" else count.toString()
    Surface(
        color = color,
        shape = CircleShape,
        modifier = modifier.requiredSize(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = contentColor,
                fontSize = if (text.length >= 3) 8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun CalendarCounterIcon(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = colorScheme.tertiary,
) {
    val text = if (count > 99) "99+" else count.toString()
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = if (text.length >= 3) 7.5.sp else if (text.length == 2) 9.sp else 10.5.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )

    Canvas(
        modifier = modifier
            .requiredSize(20.dp)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val w = size.width
        val h = size.height

        val topOffset = h * 0.15f
        val bodyHeight = h - topOffset
        val cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)

        // Draw calendar main page
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, topOffset),
            size = Size(w, bodyHeight),
            cornerRadius = cornerRadius
        )

        // Draw top binder rings/pegs
        val ringWidth = w * 0.14f
        val ringHeight = topOffset * 1.5f
        val ring1Left = w * 0.26f - ringWidth / 2f
        val ring2Left = w * 0.74f - ringWidth / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(ring1Left, 0f),
            size = Size(ringWidth, ringHeight),
            cornerRadius = CornerRadius(ringWidth / 2f, ringWidth / 2f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(ring2Left, 0f),
            size = Size(ringWidth, ringHeight),
            cornerRadius = CornerRadius(ringWidth / 2f, ringWidth / 2f)
        )

        // Draw horizontal header line cutout
        val headerY = topOffset + bodyHeight * 0.25f
        drawLine(
            color = Color.Black,
            start = Offset(w * 0.08f, headerY),
            end = Offset(w * 0.92f, headerY),
            strokeWidth = 1.5.dp.toPx(),
            blendMode = BlendMode.Clear
        )

        // Measure text and draw text cutout inside date area
        val textLayoutResult = textMeasurer.measure(
            text = text,
            style = textStyle
        )
        val textWidth = textLayoutResult.size.width
        val textHeight = textLayoutResult.size.height

        val dateAreaCenterY = headerY + (h - headerY) / 2f
        val textX = (w - textWidth) / 2f
        val textY = dateAreaCenterY - textHeight / 2f

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(textX, textY),
            blendMode = BlendMode.Clear
        )
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