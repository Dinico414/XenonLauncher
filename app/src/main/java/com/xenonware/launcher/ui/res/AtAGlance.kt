package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenonware.launcher.R
import java.util.Calendar

@Composable
fun AtAGlance(
    time: String,
    date: String,
    temperature: String,
    condition: String,
    notificationCount: Int,
    modifier: Modifier = Modifier
) {
    val contentColor = LocalContentColor.current

    // Re-evaluated whenever the clock string changes so it flips at dusk/dawn.
    val isDay = remember(time) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour in 6..18 // 6:00 AM – 6:59 PM counts as day
    }

    val weatherRes = remember(condition, isDay) {
        val (day, night) = when {
            condition.contains("Thunder Shower", true) || condition.contains(
                "T-Shower",
                true
            ) -> R.drawable.tshower1 to R.drawable.tshower0

            condition.contains("Thunder", true) || condition.contains(
                "Storm",
                true
            ) -> R.drawable.tstorm1 to R.drawable.tstorm0

            condition.contains("Tornado", true) -> R.drawable.tornado1 to R.drawable.tornado0

            condition.contains("Hail", true) -> R.drawable.hail1 to R.drawable.hail0

            condition.contains("Sleet", true) -> R.drawable.sleet1 to R.drawable.sleet0

            condition.contains("Light Snow", true) || condition.contains(
                "Flurr",
                true
            ) -> R.drawable.lsnow1 to R.drawable.lsnow0

            condition.contains("Snow", true) || condition.contains(
                "Ice",
                true
            ) -> R.drawable.snow1 to R.drawable.snow0

            condition.contains("Shower", true) || condition.contains(
                "Drizzle",
                true
            ) -> R.drawable.shower1 to R.drawable.shower0

            condition.contains("Rain", true) -> R.drawable.rain1 to R.drawable.rain0

            condition.contains("Fog", true) || condition.contains(
                "Mist",
                true
            ) || condition.contains("Haze", true) -> R.drawable.fog1 to R.drawable.fog0

            condition.contains("Wind", true) -> R.drawable.windy1 to R.drawable.windy0

            condition.contains("Partly", true) -> R.drawable.pcloudy1 to R.drawable.pcloudy0

            condition.contains("Overcast", true) || condition.contains(
                "Cloud",
                true
            ) -> R.drawable.mcloudy1 to R.drawable.mcloudy0

            condition.contains("Clear", true) || condition.contains(
                "Sunny",
                true
            ) -> R.drawable.clear1 to R.drawable.clear0

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
            verticalArrangement = Arrangement.spacedBy((-6).dp, Alignment.CenterVertically)
        ) {
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                time,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 16.sp,
                color = contentColor
            )
            Text(date, maxLines = 1, fontSize = 10.sp, color = contentColor.copy(alpha = 0.7f))
        }

        if (notificationCount > 0) {
            Surface(
                color = colorScheme.primary, shape = CircleShape, modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        notificationCount.toString(),
                        color = colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
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
