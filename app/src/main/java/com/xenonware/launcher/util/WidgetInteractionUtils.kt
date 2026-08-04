package com.xenonware.launcher.ui.res

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

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
class InteractiveAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    /** Invoked once the long-press timeout elapses without the gesture turning into a scroll. */
    var onWidgetLongPress: (() -> Unit)? = null

    /** System default long-press timeout. */
    var longPressTimeoutMs: Long = ViewConfiguration.getLongPressTimeout().toLong()

    /**
     * Strict tolerance for stationary hold position (~2dp, max 6px).
     * Finger micro-tremor while stationary is typically <3px.
     * Decreasing this threshold ensures that sliding or swiping across a widget (even very slowly)
     * immediately cancels long press, preventing accidental edit mode triggers.
     */
    private val holdSlop = (2f * context.resources.displayMetrics.density).coerceAtMost(6f)

    private var hasPerformedLongPress = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var totalMovement = 0f

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

    private fun checkAndCancelIfMoved(rawX: Float, rawY: Float) {
        val dx = abs(rawX - downRawX)
        val dy = abs(rawY - downRawY)
        val stepDist = hypot(rawX - lastRawX, rawY - lastRawY)
        totalMovement += stepDist
        lastRawX = rawX
        lastRawY = rawY

        if (dx > holdSlop || dy > holdSlop || totalMovement > holdSlop) {
            clearLongPressCheck()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = ev.rawX
                downRawY = ev.rawY
                lastRawX = ev.rawX
                lastRawY = ev.rawY
                totalMovement = 0f
                startLongPressCheck()
            }

            MotionEvent.ACTION_MOVE -> {
                checkAndCancelIfMoved(ev.rawX, ev.rawY)
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
        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            checkAndCancelIfMoved(event.rawX, event.rawY)
        } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            clearLongPressCheck()
        }
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

/** [AppWidgetHost] that inflates [InteractiveAppWidgetHostView] instead of the plain host view. */
class InteractiveAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = InteractiveAppWidgetHostView(context)
}