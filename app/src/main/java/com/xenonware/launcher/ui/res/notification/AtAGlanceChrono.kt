package com.xenonware.launcher.ui.res.notification

import android.app.AlarmManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.xenonware.launcher.notification.LauncherNotification
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Ticks once per second, phase-locked to [phase].
 *
 * A chronometer based at, say, x.300s rolls over at .300 — not at .000. Ticking on
 * wall-clock second boundaries would repaint up to 999ms after the value actually
 * changed, which reads as the display lagging a second behind the clock app.
 */
@Composable
fun rememberChronoTick(enabled: Boolean, phase: Long): State<Long> {
    val state = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(enabled, phase) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            state.longValue = now
            // Land just past the next rollover, so jitter can't read the stale value.
            val rem = ((now - phase) % 1000L + 1000L) % 1000L
            delay(1000L - rem + 10L)
        }
    }
    return state
}

/**
 * The right-hand cluster of the At a Glance header.
 * Falls back to the next alarm only when nothing is running or paused.
 */
@Composable
fun ChronoCluster(
    timers: List<LauncherNotification>,
    stopwatches: List<LauncherNotification>,
    nextAlarm: AlarmManager.AlarmClockInfo?,
    fontSize: TextUnit,
    isWallpaperDark: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val items = remember(timers, stopwatches) {
        (timers + stopwatches)
            .map { it.chrono }
            .filter { it.isDisplayable }
            // Clock can post the same chrono under several keys during updates.
            .distinctBy { Triple(it.kind, it.baseWallTime, it.frozenMs) }
            .sortedBy { it.kind.ordinal }
            .take(3)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Each item owns a ticker locked to its own base.
        items.forEach { chrono -> ChronoPreviewItem(chrono, fontSize, isWallpaperDark) }

        if (items.isEmpty() && nextAlarm != null) {
            val context = LocalContext.current
            val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            val alarmText = remember(nextAlarm.triggerTime, pattern) {
                val cal = Calendar.getInstance().apply { timeInMillis = nextAlarm.triggerTime }
                SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)
            }
            TimePreviewItem(Icons.Rounded.Alarm, alarmText, fontSize, alpha = 0.7f, isWallpaperDark = isWallpaperDark)
        }
    }
}

@Composable
private fun ChronoPreviewItem(chrono: ChronoState, fontSize: TextUnit, isWallpaperDark: Boolean) {
    // Both kinds roll over on the same phase: TIMER shows ceil(base - now),
    // STOPWATCH shows floor(now - base), and both change when (now - base) % 1000 == 0.
    val now by rememberChronoTick(enabled = chrono.isRunning, phase = chrono.baseWallTime)

    when (chrono.kind) {
        ChronoKind.TIMER -> {
            val remainingMs =
                if (chrono.isRunning) chrono.baseWallTime - now
                else chrono.frozenMs.coerceAtLeast(0L)
            val expired = chrono.isRunning && remainingMs <= 0L
            // Round UP: a freshly started 5:00 timer must read 5:00, not 4:59.
            val seconds = ceilSeconds(abs(remainingMs))

            TimePreviewItem(
                icon = when {
                    !chrono.isRunning -> Icons.Rounded.PauseCircle
                    expired -> Icons.Rounded.HourglassEmpty
                    seconds % 60 >= 30 -> Icons.Rounded.HourglassTop
                    else -> Icons.Rounded.HourglassBottom
                },
                text = if (expired) "-${formatClock(seconds)}" else formatClock(seconds),
                fontSize = fontSize,
                isWallpaperDark = isWallpaperDark
            )
        }

        ChronoKind.STOPWATCH -> {
            val elapsedMs =
                if (chrono.isRunning) now - chrono.baseWallTime
                else chrono.frozenMs.coerceAtLeast(0L)

            TimePreviewItem(
                icon = if (chrono.isRunning) Icons.Rounded.Timer else Icons.Rounded.PauseCircle,
                // Floor, matching how a counting-up Chronometer renders.
                text = formatClock(elapsedMs.coerceAtLeast(0L) / 1000L),
                fontSize = fontSize,
                isWallpaperDark = isWallpaperDark
            )
        }

        ChronoKind.NONE -> Unit
    }
}

@Composable
fun TimePreviewItem(
    icon: ImageVector,
    text: String,
    fontSize: TextUnit,
    alpha: Float = 1f,
    isWallpaperDark: Boolean = false
) {
    val baseColor = if (isWallpaperDark) Color.Black else Color.White
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = baseColor.copy(alpha = alpha),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = baseColor.copy(alpha = alpha)
        )
    }
}

private fun ceilSeconds(ms: Long): Long = (ms + 999L) / 1000L

private fun formatClock(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val locale = Locale.getDefault()
    return if (h > 0) String.format(locale, "%d:%02d:%02d", h, m, s)
    else String.format(locale, "%02d:%02d", m, s)
}