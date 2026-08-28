package com.xenonware.launcher.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
            context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
        )
    }
    DisposableEffect(Unit) {
        val wm = context.getSystemService(WindowManager::class.java)
        // Callback feuert sofort mit dem aktuellen Zustand und danach bei jeder Änderung.
        val listener = Consumer<Boolean> { enabled = it }
        wm?.addCrossWindowBlurEnabledListener(listener)
        onDispose { wm?.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

@Composable
fun WindowBlurBehind(
    radiusPx: Int
) {
    val context = LocalContext.current

    LaunchedEffect(radiusPx) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        applyBackgroundBlur(window, radiusPx)
    }

    DisposableEffect(Unit) {
        onDispose { context.findActivity()?.window?.let { applyBackgroundBlur(it, 0) } }
    }
}

private fun applyBackgroundBlur(window: Window, radiusPx: Int) {
    setBlurApi31(window, radiusPx)
}

private fun setBlurApi31(window: Window, radiusPx: Int) {
    val wm = window.context.getSystemService(WindowManager::class.java)
    val enabled = wm?.isCrossWindowBlurEnabled == true
    window.setBackgroundBlurRadius(if (enabled && radiusPx > 0) radiusPx else 0)
}