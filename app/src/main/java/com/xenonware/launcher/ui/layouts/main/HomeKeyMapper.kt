package com.xenonware.launcher.ui.layouts.main

import android.view.KeyEvent

/**
 * A logical home-screen action, abstracted from the physical key that produced it.
 *
 * The home screen (the 3-page pager: Media / Home / Widgets) reacts to a small, fixed set of
 * intents. Mapping raw key codes to these once, in [homeKeyToAction], keeps the Activity handler
 * simple and makes the mapping unit-testable without a device.
 */
enum class HomeKeyAction {
    /** Move to the previous page (left / bumper-L / stick-left / numpad-4 ...). */
    PageLeft,

    /** Move to the next page (right / bumper-R / stick-right / numpad-6 ...). */
    PageRight,

    /** Open the app drawer (Up / Space). */
    OpenDrawer,

    /** Toggle the app drawer open/closed (number key 0). Works from either state. */
    ToggleDrawer,

    /** Expand the notification shade (Down). */
    OpenNotifications,

    /** The system Home key: page-aware - see the handler. */
    Home,
}

/**
 * Translates a raw hardware [KeyEvent] into a [HomeKeyAction], or null when the key is not a
 * home-screen action (in which case the caller lets it fall through).
 *
 * Accepts every alias for each direction so keyboards, TV remotes, controllers and the MindControl
 * remote all work:
 *  - arrows: `DPAD_LEFT/RIGHT`, `SYSTEM_NAVIGATION_LEFT/RIGHT`
 *  - numpad arrows: `NUMPAD_4` / `NUMPAD_6`
 *  - gamepad: D-pad `DPAD_*`, bumpers `BUTTON_L1/R1`, sticks `BUTTON_THUMBL/R`
 *  - vertical: `DPAD_UP` / `SPACE` (drawer), `DPAD_DOWN` (shade)
 *
 * The iKKO Mind keyboard reports the directions as `KEYCODE_UNKNOWN` with only a scan code - the
 * same quirk the drawer handles in [scanCodeToNavAction] - so that path is mirrored here, which is
 * what makes MindControl's Up/Down work for open-drawer / open-shade.
 *
 * Only key-down events are considered.
 */
fun homeKeyToAction(event: KeyEvent): HomeKeyAction? {
    if (event.action != KeyEvent.ACTION_DOWN) return null

    // MindControl (and some keyboards) send the direction with no key code at all, carrying it only
    // in the scan code. Recover it first, exactly like the app drawer does.
    if (event.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
        return when (event.scanCode) {
            544 -> HomeKeyAction.OpenDrawer      // up
            545 -> HomeKeyAction.OpenNotifications // down
            542 -> HomeKeyAction.PageLeft        // left
            543 -> HomeKeyAction.PageRight       // right
            else -> null
        }
    }

    return when (event.keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
        KeyEvent.KEYCODE_NUMPAD_4,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        -> HomeKeyAction.PageLeft

        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
        KeyEvent.KEYCODE_NUMPAD_6,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_THUMBR,
        -> HomeKeyAction.PageRight

        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
        KeyEvent.KEYCODE_SPACE,
        -> HomeKeyAction.OpenDrawer

        // Number key 0 toggles the app drawer, from either state.
        KeyEvent.KEYCODE_0,
        KeyEvent.KEYCODE_NUMPAD_0,
        -> HomeKeyAction.ToggleDrawer

        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
        -> HomeKeyAction.OpenNotifications

        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_MOVE_HOME,
        -> HomeKeyAction.Home

        else -> null
    }
}
