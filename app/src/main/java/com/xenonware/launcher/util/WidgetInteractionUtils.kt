package com.xenonware.launcher.util

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

@Suppress("RedundantOverride")
class InteractiveAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onWidgetLongPress: (() -> Unit)? = null

    var longPressTimeoutMs: Long = ViewConfiguration.getLongPressTimeout().toLong()

    private val holdSlop = (2f * context.resources.displayMetrics.density).coerceAtMost(6f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

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

            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> clearLongPressCheck()
        }

        return hasPerformedLongPress
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> checkAndCancelIfMoved(event.rawX, event.rawY)
            MotionEvent.ACTION_UP -> {
                clearLongPressCheck()
                val movedPastSlop = abs(event.rawX - downRawX) > touchSlop ||
                        abs(event.rawY - downRawY) > touchSlop
                if (!hasPerformedLongPress && !movedPastSlop) {
                    performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> clearLongPressCheck()
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
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

class InteractiveAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = InteractiveAppWidgetHostView(context)
}