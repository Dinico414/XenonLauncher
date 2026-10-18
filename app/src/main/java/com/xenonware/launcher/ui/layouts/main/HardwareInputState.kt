package com.xenonware.launcher.ui.layouts.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Tracks whether the user is currently navigating with a hardware / HID input device (a D-pad,
 * gamepad, TV remote, MindControl remote or hardware keyboard) rather than the touchscreen.
 *
 * The launcher uses this to decide whether to put an initial focus highlight on the first app when
 * the drawer opens: a hardware navigator needs a starting point for the arrow keys, while a purely
 * touch user should not see a focused cell they never asked for.
 *
 * The Activity feeds every event here - `dispatchKeyEvent` marks hardware navigation, `dispatchTouchEvent`
 * clears it - so the flag always reflects the input the user last actually used. It is a plain
 * Compose state holder (not a ViewModel) because it is a global, process-wide UI concern that both
 * the Activity and the drawer read.
 */
object HardwareInputState {
    /** True when the last input that reached the launcher came from a hardware / HID device. */
    var isHardwareNavigationActive by mutableStateOf(false)
        private set

    /** Called from the Activity when a hardware key event is received. */
    fun onHardwareInput() {
        isHardwareNavigationActive = true
    }

    /** Called from the Activity when a touch event is received. */
    fun onTouchInput() {
        isHardwareNavigationActive = false
    }
}
