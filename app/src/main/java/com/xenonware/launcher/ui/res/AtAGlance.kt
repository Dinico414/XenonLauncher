package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.launcher.R
import com.xenonware.launcher.ui.res.dock.StatusCounters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AtAGlance(
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
            c.contains("partly") || c.contains("teilweise") -> R.drawable.pcloudy1 to R.drawable.pcloudy0
            c.contains("overcast") || c.contains("cloud") || c.contains("bedeckt") || c.contains("wolken") -> R.drawable.mcloudy1 to R.drawable.mcloudy0
            c.contains("clear") || c.contains("sunny") || c.contains("klar") || c.contains("sonnig") -> R.drawable.clear1 to R.drawable.clear0
            else -> R.drawable.unknown1 to R.drawable.unknown0
        }
        if (isDay) day else night
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxHeight()
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((-8).dp, Alignment.CenterVertically)
        ) {
            Text(
                time,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 16.sp,
                color = contentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            triggerPillRipple()
                            onTimeClick()
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Text(
                date,
                maxLines = 1,
                fontSize = 10.sp,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            triggerPillRipple()
                            onDateClick()
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (notificationCount > 0 || calendarEventCount > 0) {
            StatusCounters(
                notificationCount = notificationCount,
                calendarEventCount = calendarEventCount
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        triggerPillRipple()
                        onWeatherClick()
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Shadow
                Image(
                    painter = painterResource(id = weatherRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.1f)),
                    modifier = Modifier.size(26.dp)
                )
                // Real icon
                Image(
                    painter = painterResource(id = weatherRes),
                    contentDescription = condition,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(temperature.replace("+", ""), color = contentColor, maxLines = 1, fontSize = 14.sp)
        }
    }
}
