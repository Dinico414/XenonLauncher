package com.xenonware.launcher.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import kotlin.math.abs

fun Modifier.blockHorizontalPagerSwipe(): Modifier = this.pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        var totalX = 0f
        var totalY = 0f
        var blocking = false

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.changes.none { it.pressed }) break

            event.changes.firstOrNull { it.id == down.id }?.let { change ->
                // IgnoreConsumed: the notification card consumes its own drag, and we still
                // need to see that movement to classify the gesture.
                val delta = change.positionChangeIgnoreConsumed()
                totalX += delta.x
                totalY += delta.y
                if (!blocking && abs(totalX) > slop && abs(totalX) > abs(totalY)) blocking = true
            }

            if (blocking) event.changes.forEach { if (it.pressed) it.consume() }
        }
    }
}
