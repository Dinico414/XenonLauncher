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
    }
}