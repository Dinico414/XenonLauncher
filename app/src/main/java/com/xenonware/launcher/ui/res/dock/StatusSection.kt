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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Usb
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargeSpacing
import com.xenon.mylibrary.values.MediumIconSize
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.SmallIconSize
import com.xenon.mylibrary.values.SmallerStroke
import com.xenon.mylibrary.values.SmallestStroke
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.res.Glancly
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.fitScale
import com.xenonware.launcher.util.rememberUsbDataTransfer
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private val BatteryFontSize = 11.sp
private val CounterFontSize = 10.sp
private val CalendarFontSize = 10.5.sp

private const val CIRCLE_TEXT_FRACTION = 0.7f

private val DataTransferColor = Color(0xFF3B82F6)

private val ChargeSettleDelay = 1500.milliseconds

private val AnnouncementDuration = 1000.milliseconds

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
    isDataTransfer: Boolean = rememberUsbDataTransfer(),
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val buttonAlpha = dockButtonAlpha()

    // The pill animates for either kind of activity; the flags only differ in color and direction
    val isActive = isCharging || isDataTransfer

    val pillInteractionSource = remember { MutableInteractionSource() }
    var showBell by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var showUsb by remember { mutableStateOf(false) }
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

    // The charge state as the icons see it: flapping is ignored until it holds for a moment.
    // The ripples still read isCharging directly, so they start the instant a cable goes in.
    var settledCharging by remember { mutableStateOf(isCharging) }
    LaunchedEffect(isCharging) {
        if (isCharging != settledCharging) {
            delay(ChargeSettleDelay)
            settledCharging = isCharging
        }
    }

    var prevSettledCharging by remember { mutableStateOf(settledCharging) }
    LaunchedEffect(settledCharging) {
        val startedCharging = settledCharging && !prevSettledCharging
        prevSettledCharging = settledCharging
        if (startedCharging) {
            showFlash = true
            delay(AnnouncementDuration)
            showFlash = false
        }
    }

    var prevDataTransfer by remember { mutableStateOf(isDataTransfer) }
    LaunchedEffect(isDataTransfer) {
        val linkCameUp = isDataTransfer && !prevDataTransfer
        prevDataTransfer = isDataTransfer
        if (linkCameUp) {
            showUsb = true
            delay(AnnouncementDuration)
            showUsb = false
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

    // Data runs the ripples the other way: top to bottom collapsed, end to start expanded
    val flowProgress = if (isDataTransfer) 1f - rippleProgress else rippleProgress
    // ...and curves them the other way, with the circle's center past the pill instead of before it
    val arcSide = if (isDataTransfer) -1f else 1f

    val strokeColor = remember(progress, isDataTransfer) {
        if (isDataTransfer) DataTransferColor else when {
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
            !isExpanded && isActive -> Color.Transparent // Use brush background
            !isExpanded && notificationCount > 0 && !isActive -> colorScheme.primaryContainer
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
        targetValue = if (isExpanded) SmallerStroke else SmallestStroke, label = "strokeWidth"
    )

    val chargingAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(500),
        label = "chargingAlpha"
    )

    val bgChargingAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (isActive) tween(500) else snap(),
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
        border = BorderStroke(SmallestStroke, colorScheme.onSurface.copy(alpha = 0.15f))
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
                            val p2 = (flowProgress + 0.5f) % 1f

                            listOf(flowProgress, p2).forEach { p ->
                                val dimension = if (isExpanded) size.width else size.height
                                // Extend travel range to ensure the wave enters and exits fully
                                val travelRange = dimension + radius * 0.5f
                                val currentPos = p * travelRange - (radius * 0.25f)
                                // Which side of the pill the circle's center sits on, and so
                                // which way the visible arc bows
                                val arcShift = arcSide * radius * 0.95f

                                val center = if (isExpanded) {
                                    // Moving from start to end
                                    Offset(currentPos - arcShift, size.height / 2)
                                } else {
                                    // Moving from bottom to top
                                    Offset(
                                        size.width / 2,
                                        (size.height - currentPos) + arcShift
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
                    Glancly(
                        currentTime,
                        currentDate,
                        weatherTemp,
                        weatherCondition,
                        notificationCount,
                        calendarEventCount = calendarEventCount,
                        onTimeClick = onTimeClick,
                        onDateClick = onDateClick,
                        onWeatherClick = onWeatherClick,
                        pillInteractionSource = pillInteractionSource,
                        // Bound the row to the pill: animateContentSize measures its children
                        // with unbounded width mid-animation, which let the weather row and
                        // counters define the width and pushed the clock/date off-screen.
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = when {
                                showBell -> StatusViewState.Bell
                                // A PC connection is a data link first; the bolt would only
                                // repeat what the blue ripples already say
                                showUsb -> StatusViewState.Usb
                                showFlash && !isDataTransfer -> StatusViewState.Flash
                                settledCharging -> StatusViewState.Battery
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
                                            .size(SmallIconSize)
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
                                            .size(SmallIconSize)
                                            .offset {
                                                IntOffset(
                                                    flashOffset.dp.toPx().roundToInt(),
                                                    0
                                                )
                                            }
                                    )
                                }

                                StatusViewState.Usb -> {
                                    Icon(
                                        Icons.Rounded.Usb,
                                        null,
                                        modifier = Modifier
                                            .size(SmallIconSize)
                                            .offset {
                                                IntOffset(
                                                    0,
                                                    flashOffset.dp.toPx().roundToInt()
                                                )
                                            }
                                    )
                                }

                                StatusViewState.Battery -> {
                                    BatteryLabel(percent = (progress * 100).toInt())
                                }

                                StatusViewState.Default -> {
                                    Icon(
                                        Icons.Rounded.Info,
                                        null,
                                        modifier = Modifier.size(MediumIconSize)
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
    Default, Bell, Flash, Usb, Battery
}

/**
 * The battery percentage in the collapsed pill. It gives up the percent sign before it gives up
 * size, so "87%" becomes "87" and only then starts shrinking.
 */
@Composable
private fun BatteryLabel(
    percent: Int,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontSize = BatteryFontSize,
        fontWeight = FontWeight.Bold,
        fontFamily = mainFontFamily
    )

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // A little off the edges so the digits never touch the pill's border
        val availablePx = constraints.maxWidth * 0.9f
        val (text, fontSize) = remember(percent, style, availablePx) {
            val withSign = "$percent%"
            val bare = percent.toString()
            val signedWidth = measurer
                .measure(withSign, style, maxLines = 1, softWrap = false).size.width
            if (signedWidth <= availablePx) {
                withSign to BatteryFontSize
            } else {
                val bareWidth = measurer
                    .measure(bare, style, maxLines = 1, softWrap = false).size.width
                bare to BatteryFontSize * fitScale(bareWidth.toFloat(), availablePx)
            }
        }

        Text(
            text = text,
            style = style,
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun StatusCounters(
    notificationCount: Int,
    calendarEventCount: Int,
    weatherIcon: Int? = null,
    modifier: Modifier = Modifier,
    calendarColor: Color = colorScheme.tertiary,
    calendarTextColor: Color = Color.Black
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MediumSpacer, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (weatherIcon != null) {
            WeatherCounterIcon(iconRes = weatherIcon)
        }
        if (notificationCount > 0) {
            NotificationCounterBadge(count = notificationCount)
        }
        if (calendarEventCount > 0) {
            CalendarCounterIcon(
                count = calendarEventCount,
                color = calendarColor,
                textColor = calendarTextColor
            )
        }
    }
}

@Composable
fun WeatherCounterIcon(
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = colorScheme.secondaryContainer,
        shape = CircleShape,
        modifier = modifier.requiredSize(ExtraLargeSpacing)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(LargeSpacing)
            )
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
    val plusLabel = stringResource(R.string.notification_count_plus)
    val text = if (count > 99) plusLabel else count.toString()
    val measurer = rememberTextMeasurer()
    val style = TextStyle(
        fontSize = CounterFontSize,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        fontFamily = mainFontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Surface(
        color = color,
        shape = CircleShape,
        modifier = modifier.requiredSize(ExtraLargeSpacing)
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val availablePx = constraints.maxWidth * CIRCLE_TEXT_FRACTION
            val fontSize = remember(text, style, availablePx) {
                val width = measurer
                    .measure(text, style, maxLines = 1, softWrap = false).size.width
                CounterFontSize * fitScale(width.toFloat(), availablePx)
            }
            Text(
                text = text,
                style = style,
                fontSize = fontSize,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CalendarCounterIcon(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = colorScheme.tertiary,
    textColor: Color = Color.Black
) {
    val plusLabel = stringResource(R.string.notification_count_plus)
    val text = if (count > 99) plusLabel else count.toString()
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = CalendarFontSize,
        fontWeight = FontWeight.Bold,
        color = textColor,
        fontFamily = mainFontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Canvas(
        modifier = modifier
            .requiredSize(ExtraLargeSpacing)
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

        // Measure at the ideal size, then shrink to whatever the date area can hold
        val idealLayout = textMeasurer.measure(text, textStyle, maxLines = 1, softWrap = false)
        val dateAreaWidth = (w * 0.92f) - (w * 0.08f)
        val dateAreaHeight = (h - headerY) * 0.9f
        val scale = minOf(
            fitScale(idealLayout.size.width.toFloat(), dateAreaWidth),
            fitScale(idealLayout.size.height.toFloat(), dateAreaHeight)
        )
        val textLayoutResult = if (scale >= 1f) idealLayout else textMeasurer.measure(
            text,
            textStyle.copy(fontSize = CalendarFontSize * scale),
            maxLines = 1,
            softWrap = false
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