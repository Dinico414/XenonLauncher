package com.xenonware.launcher.ui.layouts.main

import androidx.compose.ui.input.key.KeyEvent

/**
 * Bridges hardware key events from the Activity to the open app drawer.
 *
 * The Activity's `dispatchKeyEvent` is the standard, first hook every key passes through, so
 * handling navigation there is still "default Android behaviour" - it is exactly how a TV app reads
 * a D-pad. The drawer registers a handler while it is visible and the Activity forwards keys to it;
 * the handler returns true only for keys it actually acts on, so everything else keeps its normal
 * behaviour.
 */
class DrawerKeyDispatcher {
    var handler: ((KeyEvent) -> Boolean)? = null

    val isActive: Boolean get() = handler != null

    fun dispatch(event: KeyEvent): Boolean = handler?.invoke(event) ?: false
}
