package com.xenonware.launcher.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * An [AppWidgetHostView] that detects a long press without stealing normal interaction.
 *
 * A plain `setOnLongClickListener` only fires when no child view consumed the touch, which is why
 * it never triggers on widgets whose whole surface is clickable (Maps, Chrome) or scrollable
 * (Calendar). Instead this watches the gesture from the parent via [onInterceptTouchEvent]:
 * children keep receiving every event as normal, and the view only takes the gesture over once the
 * long-press timeout has elapsed — at which point Android automatically delivers ACTION_CANCEL to
 * the child, so the widget's own click never fires.
 */
class WidgetInteractionUtil(context: Context) : AppWidgetHostView(context) {

    /** Invoked once the long-press timeout elapses without the gesture turning into a scroll. */
    var onWidgetLongPress: (() -> Unit)? = null

    /** Slightly longer than the system default reduces accidental triggers on scrollable widgets. */
    var longPressTimeoutMs: Long = ViewConfiguration.getLongPressTimeout().toLong() + 100L

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var hasPerformedLongPress = false
    private var downX = 0f
    private var downY = 0f

    private val longPressCheck = Runnable {
        if (parent != null && hasWindowFocus() && !hasPerformedLongPress) {
            hasPerformedLongPress = true
            onWidgetLongPress?.invoke()
        }
    }

    private fun startLongPressCheck() {
        hasPerformedLongPress = false
        removeCallbacks(longPressCheck)
        postDelayed(longPressCheck, longPressTimeoutMs)
    }

    private fun clearLongPressCheck() {
        removeCallbacks(longPressCheck)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                startLongPressCheck()
            }

            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop) {
                    clearLongPressCheck()
                }
            }

            // A second finger means a pinch/zoom (Maps) — not a long press
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> clearLongPressCheck()
        }

        // Returning false keeps the child in charge. Only once the long press has actually fired
        // do we intercept, which cancels the child's in-flight touch.
        return hasPerformedLongPress
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always consume. Without this, a touch on a non-interactive part of the widget would fall
        // through to the Compose parent and open the wallpaper/settings menu on top of the widget.
        return true
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // A scrollable child (Calendar's event list, Maps' map surface) has claimed the gesture.
        clearLongPressCheck()
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        clearLongPressCheck()
    }

    override fun onDetachedFromWindow() {
        clearLongPressCheck()
        super.onDetachedFromWindow()
    }
}

/** [AppWidgetHost] that inflates [WidgetInteractionUtil] instead of the plain host view. */
class InteractiveAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = WidgetInteractionUtil(context)
}