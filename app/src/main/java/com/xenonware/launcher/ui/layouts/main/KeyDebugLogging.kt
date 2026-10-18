package com.xenonware.launcher.ui.layouts.main

/**
 * Temporary diagnostic switch.
 *
 * While `ENABLED` is true, every hardware key that reaches the Activity is logged under the
 * `XenonKeys` tag (key code, scan code, device id, repeat count, source). This is how we work out
 * what a controller or the MindControl remote actually emits for each direction and trigger.
 *
 * Set to `false` (or delete this file and its call site) once the input mapping is settled.
 */
object KeyDebugLogging {
    const val ENABLED = true
}
