package com.xenonware.launcher.ui.layouts.main

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * A single logical navigation action, abstracted away from the physical key/gamepad button that
 * produced it. Every supported input source (keyboard arrow keys, TV remote, controller D-pad,
 * gamepad face/bumper buttons, Escape) is normalised to one of these values by [keyEventToNavAction],
 * so the UI only has to reason about intent, never about hardware.
 */
enum class DrawerNavAction {
    Up,
    Down,
    Left,
    Right,

    /** Launch the focused app (Enter / Center / gamepad A). */
    Select,

    /** Open the focused app's context menu (Shift+Enter / gamepad Y / Menu). */
    ContextMenu,

    /** Move focus up by roughly one viewport (gamepad left bumper). */
    PageUp,

    /** Move focus down by roughly one viewport (gamepad right bumper). */
    PageDown,

    /** Leave the current context: exit search first, then close the drawer. */
    Back,

    /** A printable character was typed; used for type-to-search. */
    TypeChar,
}

/**
 * Translates a raw [KeyEvent] into a [DrawerNavAction], or null when the event is not a navigation
 * input we handle (in which case the caller should let it fall through to the system).
 *
 * Only key-down events produce actions, except that modifier state is read from the event itself so
 * `Shift+Enter` works without tracking held keys separately. Auto-repeat is intentionally allowed:
 * holding an arrow key should keep moving the highlight, exactly like a D-pad.
 *
 * The mapping covers every input type the MindControl reference project can emit:
 *  - `KEYCODE_DPAD_UP/DOWN/LEFT/RIGHT` (keyboard arrows, TV remote, controller D-pad/hat)
 *  - `KEYCODE_ENTER`, `KEYCODE_DPAD_CENTER`, `KEYCODE_BUTTON_A` (select)
 *  - `KEYCODE_BUTTON_Y`, `KEYCODE_MENU`, `KEYCODE_BUTTON_MODE` (context menu)
 *  - `KEYCODE_BUTTON_L1/R1` (page), `KEYCODE_BUTTON_B`, `KEYCODE_ESCAPE` (back)
 *  - printable characters (`TypeChar`)
 */
fun keyEventToNavAction(event: KeyEvent): DrawerNavAction? {
    if (event.type != KeyEventType.KeyDown) return null

    // Some keyboards/gamepads report the *vertical* directions with no key code at all: the event
    // arrives as KEYCODE_UNKNOWN with the direction carried only in the scan code. This device does
    // exactly that for Up/Down (scan 544 = up, 545 = down) while Left/Right map normally, which is
    // why only up/down appeared broken. Recover the direction from the scan code first.
    if (event.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_UNKNOWN) {
        return scanCodeToNavAction(event.nativeKeyEvent.scanCode)
    }

    // Match on the raw Android key code. This is deliberate: a physical keyboard and a gamepad can
    // report the vertical directions with different codes (e.g. a keyboard may send
    // SYSTEM_NAVIGATION_UP/DOWN, PAGE_UP/DOWN or MOVE_HOME/END), so we accept every common alias for
    // "up" and "down" instead of relying on the single Compose Key.DirectionUp/Down mapping.
    val code = event.nativeKeyEvent.keyCode
    return when (code) {
        NativeKeyEvent.KEYCODE_DPAD_UP,
        NativeKeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
        NativeKeyEvent.KEYCODE_PAGE_UP,
        NativeKeyEvent.KEYCODE_MOVE_HOME,
        -> DrawerNavAction.Up

        NativeKeyEvent.KEYCODE_DPAD_DOWN,
        NativeKeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
        NativeKeyEvent.KEYCODE_PAGE_DOWN,
        NativeKeyEvent.KEYCODE_MOVE_END,
        -> DrawerNavAction.Down

        NativeKeyEvent.KEYCODE_DPAD_LEFT,
        NativeKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
        -> DrawerNavAction.Left

        NativeKeyEvent.KEYCODE_DPAD_RIGHT,
        NativeKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
        -> DrawerNavAction.Right

        NativeKeyEvent.KEYCODE_ENTER,
        NativeKeyEvent.KEYCODE_NUMPAD_ENTER,
        NativeKeyEvent.KEYCODE_DPAD_CENTER,
        NativeKeyEvent.KEYCODE_SPACE,
        NativeKeyEvent.KEYCODE_BUTTON_A,
        -> if (event.isShiftPressed) DrawerNavAction.ContextMenu else DrawerNavAction.Select

        NativeKeyEvent.KEYCODE_BUTTON_Y,
        NativeKeyEvent.KEYCODE_MENU,
        NativeKeyEvent.KEYCODE_BUTTON_MODE,
        -> DrawerNavAction.ContextMenu

        NativeKeyEvent.KEYCODE_BUTTON_B,
        NativeKeyEvent.KEYCODE_ESCAPE,
        NativeKeyEvent.KEYCODE_BACK,
        -> DrawerNavAction.Back

        NativeKeyEvent.KEYCODE_BUTTON_L1 -> DrawerNavAction.PageUp
        NativeKeyEvent.KEYCODE_BUTTON_R1 -> DrawerNavAction.PageDown

        // Type-to-search: any genuinely printable character that carries no Ctrl/Alt/Meta modifier.
        // Shift is fine (it types uppercase/symbols). We require a non-blank code point so keys like
        // the plain Shift or F-keys, which have a zero code point, are ignored.
        else -> {
            if (!event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed) {
                val char = event.utf16CodePoint.toChar()
                if (char.isLetterOrDigit() || char.isWhitespace() || (char in PRINTABLE_SYMBOLS)) {
                    DrawerNavAction.TypeChar
                } else null
            } else null
        }
    }
}

/**
 * Recovers a direction from a scan code when the key code is KEYCODE_UNKNOWN.
 *
 * The iKKO Mind keyboard reports Up/Down with no key code but a scan code of 544/545, while
 * Left/Right map to real key codes. The standard HID "keyboard arrow" usages (0x4F..0x52) are also
 * accepted so the same fix covers other keyboards that behave this way.
 */
internal fun scanCodeToNavAction(scanCode: Int): DrawerNavAction? = when (scanCode) {
    // Observed on the iKKO Mind keyboard: pressing Up reports scan code 544, Down 545.
    544 -> DrawerNavAction.Up
    545 -> DrawerNavAction.Down
    542 -> DrawerNavAction.Left
    543 -> DrawerNavAction.Right

    // Standard HID keyboard arrow usages (some stacks surface the raw usage as the scan code).
    0x52 -> DrawerNavAction.Up
    0x51 -> DrawerNavAction.Down
    0x50 -> DrawerNavAction.Left
    0x4F -> DrawerNavAction.Right

    else -> null
}

/** Printable symbols accepted by type-to-search in addition to letters, digits and whitespace. */
private val PRINTABLE_SYMBOLS = ".,-_=[]\\;'/`@+*#!?()&%$:<>\"{}|~^".toSet()

/**
 * Computes the index to move to for a directional / paging action over a grid of [itemCount] items
 * laid out in [columns] columns.
 *
 * Rules (all clamped to a valid index, or -1 when there is nothing to focus):
 *  - Left/Right step one cell; crossing the row edge wraps to the previous/next row (so Right on
 *    the last column lands on the first item of the next row, and Left on the first column lands on
 *    the last item of the previous row). Because trailing rows may be short, wrapping preserves the
 *    column index where possible and otherwise snaps to the row's last item.
 *  - Up/Down step a full row and clamp at the top/bottom (no vertical wrap: it would feel
 *    disorienting on an endless app list).
 *  - PageUp/PageDown move by roughly a viewport; [pageSize] is supplied by the caller from the
 *    measured grid, and defaults to a sensible multiple of the column count.
 *
 * This is a pure function so the wrap/clamp edge cases can be unit tested without a device.
 */
fun focusIndexAfterMove(
    current: Int,
    columns: Int,
    itemCount: Int,
    action: DrawerNavAction,
    pageSize: Int = columns * 4,
): Int {
    if (itemCount <= 0 || columns <= 0) return -1

    // Nothing focused yet: start at the top-left item.
    if (current !in 0..<itemCount) {
        return if (action == DrawerNavAction.Back) -1 else 0
    }

    val row = current / columns
    val col = current % columns
    val target: Int = when (action) {
        DrawerNavAction.Right -> {
            val next = current + 1
            if (next < itemCount) next
            // Past the end of the final row: stay put (nowhere to the right).
            else current
        }

        DrawerNavAction.Left -> {
            if (col > 0) current - 1
            // Wrap to the last item of the previous row (preserving the column where the row is
            // full, otherwise snapping to that row's final item).
            else {
                val prevRowEnd = row * columns - 1
                if (row > 0) prevRowEnd else current
            }
        }

        DrawerNavAction.Down -> {
            val target = current + columns
            if (target < itemCount) target else current
        }

        DrawerNavAction.Up -> {
            val target = current - columns
            if (target >= 0) target else current
        }

        DrawerNavAction.PageDown -> (current + pageSize.coerceAtLeast(1)).coerceAtMost(itemCount - 1)
        DrawerNavAction.PageUp -> (current - pageSize.coerceAtLeast(1)).coerceAtLeast(0)

        // Non-movement actions never change the index.
        DrawerNavAction.Select,
        DrawerNavAction.ContextMenu,
        DrawerNavAction.Back,
        DrawerNavAction.TypeChar,
        -> current
    }
    return target.coerceIn(0, itemCount - 1)
}
