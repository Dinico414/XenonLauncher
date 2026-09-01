package com.xenonware.launcher.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object AccessibilityUtils {

    fun isAccessibilityRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            val mode = appOps.checkOpNoThrow(
                "android:bind_accessibility_service",
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ERRORED
        } catch (e: Exception) {
            false
        }
    }

    fun requestAccessibility(context: Context) {
        if (isAccessibilityRestricted(context)) {
            openAppInfo(context)
        } else {
            openAccessibilitySettings(context)
        }
    }

    fun openAppInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
