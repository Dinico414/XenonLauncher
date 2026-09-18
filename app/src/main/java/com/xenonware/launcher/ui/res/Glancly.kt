package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.ExtraLargestSpacing
import com.xenon.mylibrary.values.MassiveCornerRadius
import com.xenon.mylibrary.values.MediumLargePadding
import com.xenon.mylibrary.values.MediumSmallPadding
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.SmallPadding
import com.xenon.mylibrary.values.SmallSpacer
import com.xenon.mylibrary.values.SmallerPadding
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.res.dock.StatusCounters
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.fitScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

/** The ideal sizes: nothing is ever drawn larger than these. */
private val TimeFontSize = 16.sp
private val DateFontSize = 10.sp
private val TemperatureFontSize = 14.sp

/**
 * Line heights are pinned to the ideal font sizes rather than derived from them, so the clock
 * and the date keep their vertical spacing no matter how far the text shrinks.
 */
private val TimeLineHeight = 20.sp
private val DateLineHeight = 13.sp

@Composable
fun Glancly(
    time: String,
    date: String,
    temperature: String,
    condition: String,
    notificationCount: Int,
    calendarEventCount: Int = 0,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    pillInteractionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalContentColor.current
    val scope = rememberCoroutineScope()

    fun triggerPillRipple() {
        scope.launch {
            val press = PressInteraction.Press(Offset.Zero)
            pillInteractionSource.emit(press)
            delay(80.milliseconds)
            pillInteractionSource.emit(PressInteraction.Release(press))
        }
    }

    // Re-evaluated whenever the clock string changes so it flips at dusk/dawn.
    val isDay = remember(time) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour in 6..18 // 6:00 AM – 6:59 PM counts as day
    }

    val weatherRes = remember(condition, isDay) {
        val c = condition.lowercase()
        val (day, night) = when {
            c.contains("thunder shower") || c.contains("t-shower") || c.contains("gewitterregen") -> R.drawable.tshower1 to R.drawable.tshower0
            c.contains("thunder") || c.contains("storm") || c.contains("gewitter") || c.contains("sturm") -> R.drawable.tstorm1 to R.drawable.tstorm0
            c.contains("tornado") -> R.drawable.tornado1 to R.drawable.tornado0
            c.contains("hail") || c.contains("hagel") -> R.drawable.hail1 to R.drawable.hail0
            c.contains("sleet") || c.contains("schneeregen") -> R.drawable.sleet1 to R.drawable.sleet0
            c.contains("light snow") || c.contains("flurr") || c.contains("leichter schnee") -> R.drawable.lsnow1 to R.drawable.lsnow0
            c.contains("snow") || c.contains("ice") || c.contains("schnee") || c.contains("eis") -> R.drawable.snow1 to R.drawable.snow0
            c.contains("shower") || c.contains("drizzle") || c.contains("schauer") || c.contains("niesel") -> R.drawable.shower1 to R.drawable.shower0
            c.contains("rain") || c.contains("regen") -> R.drawable.rain1 to R.drawable.rain0
            c.contains("fog") || c.contains("mist") || c.contains("haze") || c.contains("nebel") || c.contains("dunst") -> R.drawable.fog1 to R.drawable.fog0
            c.contains("wind") -> R.drawable.windy1 to R.drawable.windy0
            c.contains("partly") || c.contains("teilweise") || c.contains("leicht bewölkt") -> R.drawable.pcloudy1 to R.drawable.pcloudy0
            c.contains("overcast") || c.contains("cloud") || c.contains("bedeckt") || c.contains("wolken") || c.contains("wolkig") || c.contains("bewölkt") -> R.drawable.mcloudy1 to R.drawable.mcloudy0
            c.contains("clear") || c.contains("sunny") || c.contains("klar") || c.contains("sonnig") -> R.drawable.clear1 to R.drawable.clear0
            else -> R.drawable.unknown1 to R.drawable.unknown0
        }
        if (isDay) day else night
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(MediumSpacer),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxHeight()
            .padding(MediumLargePadding)
    ) {
        // Left half: clock and date
        ClockColumn(
            time = time,
            date = date,
            contentColor = contentColor,
            onTimeClick = {
                triggerPillRipple()
                onTimeClick()
            },
            onDateClick = {
                triggerPillRipple()
                onDateClick()
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Right half: counters and weather, laid out from the end
        Row(
            horizontalArrangement = Arrangement.spacedBy(MediumSpacer, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (notificationCount > 0 || calendarEventCount > 0) {
                StatusCounters(
                    notificationCount = notificationCount,
                    calendarEventCount = calendarEventCount
                )
            }

            WeatherRow(
                temperature = temperature.replace("+", ""),
                condition = condition,
                weatherRes = weatherRes,
                contentColor = contentColor,
                onClick = {
                    triggerPillRipple()
                    onWeatherClick()
                },
                // fill = false so the weather keeps its natural width until space runs out
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

/**
 * The clock and the date, shrunk by a single shared factor so the two lines stay visually
 * matched. Whichever line is wider decides the factor; while both fit, nothing changes.
 */
@Composable
private fun ClockColumn(
    time: String,
    date: String,
    contentColor: Color,
    onTimeClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    // A fixed line height plus centred alignment keeps each line's box the same height at every
    // font size, so shrinking the text never moves the two lines closer together.
    val lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
    val timeStyle = TextStyle(
        fontSize = TimeFontSize,
        lineHeight = TimeLineHeight,
        fontWeight = FontWeight.Bold,
        fontFamily = mainFontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = lineHeightStyle
    )
    val dateStyle = TextStyle(
        fontSize = DateFontSize,
        lineHeight = DateLineHeight,
        fontFamily = mainFontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = lineHeightStyle
    )

    BoxWithConstraints(modifier) {
        // Both lines carry the same horizontal padding, so it comes off the budget once
        val availablePx = with(LocalDensity.current) {
            (maxWidth - MediumSmallPadding * 2).coerceAtLeast(0.dp).toPx()
        }
        val scale = remember(time, date, availablePx, timeStyle, dateStyle) {
            val widest = maxOf(
                measurer.measure(time, timeStyle, maxLines = 1, softWrap = false).size.width,
                measurer.measure(date, dateStyle, maxLines = 1, softWrap = false).size.width
            )
            // Text width scales with the font size, so one ratio is enough
            fitScale(widest.toFloat(), availablePx)
        }

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((-1 * SmallPadding), Alignment.CenterVertically)
        ) {
            Text(
                time,
                style = timeStyle,
                fontSize = TimeFontSize * scale,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(RoundedCornerShape(MassiveCornerRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTimeClick
                    )
                    .padding(horizontal = MediumSmallPadding, vertical = SmallerPadding)
            )
            Text(
                date,
                style = dateStyle,
                fontSize = DateFontSize * scale,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(RoundedCornerShape(MassiveCornerRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDateClick
                    )
                    .padding(horizontal = MediumSmallPadding, vertical = SmallerPadding)
            )
        }
    }
}

/**
 * The weather icon, the gap and the temperature are measured as one group and shrunk by one
 * factor, so the icon gives up width too and the text has less to give up on its own.
 */
@Composable
private fun WeatherRow(
    temperature: String,
    condition: String,
    weatherRes: Int,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val measurer = rememberTextMeasurer()
    val temperatureStyle = TextStyle(
        fontSize = TemperatureFontSize,
        fontFamily = mainFontFamily
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(MassiveCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = MediumSmallPadding, vertical = SmallerPadding)
    ) {
        // The padding above is already off these constraints
        val availablePx = constraints.maxWidth.toFloat()
        // The shadow is the larger of the two images, so it sets the icon's width
        val iconAndGapPx = with(LocalDensity.current) { (ExtraLargestSpacing + SmallSpacer).toPx() }
        val scale = remember(temperature, temperatureStyle, iconAndGapPx, availablePx) {
            val textPx = measurer
                .measure(temperature, temperatureStyle, maxLines = 1, softWrap = false)
                .size.width
            fitScale(iconAndGapPx + textPx, availablePx)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                // Shadow
                Image(
                    painter = painterResource(id = weatherRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.1f)),
                    modifier = Modifier.size(ExtraLargestSpacing * scale)
                )
                // Real icon
                Image(
                    painter = painterResource(id = weatherRes),
                    contentDescription = condition,
                    modifier = Modifier.size(ExtraLargerSpacing * scale)
                )
            }
            Spacer(Modifier.width(SmallSpacer * scale))
            Text(
                temperature,
                style = temperatureStyle,
                fontSize = TemperatureFontSize * scale,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}