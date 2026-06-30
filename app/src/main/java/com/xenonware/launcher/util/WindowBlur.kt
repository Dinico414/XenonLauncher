package com.xenonware.launcher.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.util.function.Consumer

fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun rememberBlurAvailable(): Boolean {
    val context = LocalContext.current
    var enabled by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        DisposableEffect(Unit) {
            val wm = context.getSystemService(WindowManager::class.java)
            // Callback feuert sofort mit dem aktuellen Zustand und danach bei jeder Änderung.
            val listener = Consumer<Boolean> { enabled = it }
            wm?.addCrossWindowBlurEnabledListener(listener)
            onDispose { wm?.removeCrossWindowBlurEnabledListener(listener) }
        }
    }
    return enabled
}

@Composable
fun WindowBlurBehind(
    targetRadiusPx: Int,
    durationMillis: Int = 250,
    steps: Int = 8,
) {
    val context = LocalContext.current
    val applied = remember { mutableIntStateOf(0) }

    LaunchedEffect(targetRadiusPx) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        val from = applied.intValue
        val to = targetRadiusPx
        if (from == to) return@LaunchedEffect

        val stepDelay = (durationMillis / steps).toLong().coerceAtLeast(1)
        for (i in 1..steps) {
            val r = from + (to - from) * i / steps
            applyBackgroundBlur(window, r)
            applied.intValue = r
            delay(stepDelay)
        }
        applyBackgroundBlur(window, to)
        applied.intValue = to
    }

    DisposableEffect(Unit) {
        onDispose { context.findActivity()?.window?.let { applyBackgroundBlur(it, 0) } }
    }
}

private fun applyBackgroundBlur(window: Window, radiusPx: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    setBlurApi31(window, radiusPx)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun setBlurApi31(window: Window, radiusPx: Int) {
    val wm = window.context.getSystemService(WindowManager::class.java)
    val enabled = wm?.isCrossWindowBlurEnabled == true
    window.setBackgroundBlurRadius(if (enabled && radiusPx > 0) radiusPx else 0)
}