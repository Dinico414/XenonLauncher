package com.xenonware.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.xenonware.launcher.util.AccessibilityUtils

class XenonAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Splits the current top task and lets the user pick the second app; if split screen is
     * already active, this leaves it instead. Returns false when the system refuses.
     */
    fun splitScreen(): Boolean =
        performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

    companion object {
        var instance: XenonAccessibilityService? = null
            private set

        fun lockScreenOrRequestAccess(context: android.content.Context) {
            val service = instance
            if (service != null) {
                service.lockScreen()
            } else {
                AccessibilityUtils.requestAccessibility(context)
            }
        }

        fun openNotificationsOrRequestAccess(context: android.content.Context) {
            val service = instance
            if (service != null) {
                service.openNotifications()
            } else {
                AccessibilityUtils.requestAccessibility(context)
            }
        }

        /** False when the service isn't running or the system refused the action. */
        fun toggleSplitScreen(): Boolean = instance?.splitScreen() ?: false
    }
}