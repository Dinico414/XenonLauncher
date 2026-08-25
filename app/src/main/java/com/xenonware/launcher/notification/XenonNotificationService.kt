package com.xenonware.launcher.notification

import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xenonware.launcher.data.SharedPreferenceManager

class XenonNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "XenonNotificationService"
        private var instance: XenonNotificationService? = null

        fun dismissNotification(key: String) {
            try {
                instance?.cancelNotification(key)
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException canceling notification: $key", e)
            } catch (e: Throwable) {
                Log.e(TAG, "Error canceling notification: $key", e)
            }
        }

        fun dismissAllNotifications() {
            try {
                instance?.cancelAllNotifications()
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException canceling all notifications", e)
            } catch (e: Throwable) {
                Log.e(TAG, "Error canceling all notifications", e)
            }
        }

        fun dismissNotificationsByPackage(packageName: String) {
            try {
                val active = instance?.safeActiveNotifications ?: return
                active.forEach { sbn ->
                    if (sbn.packageName == packageName) {
                        try {
                            instance?.cancelNotification(sbn.key)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error canceling notification ${sbn.key}", e)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error dismissing notifications for package: $packageName", e)
            }
        }

        fun getInstance(): XenonNotificationService? = instance

        fun getNotificationForSession(token: android.media.session.MediaSession.Token): StatusBarNotification? {
            val active = instance?.safeActiveNotifications ?: return null
            return try {
                active.find { sbn ->
                    val extras = sbn.notification.extras
                    @Suppress("DEPRECATION")
                    val session = extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION) as? android.media.session.MediaSession.Token
                        ?: extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
                    session == token
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error getting notification for session", e)
                null
            }
        }
    }

    val safeActiveNotifications: Array<StatusBarNotification>?
        get() = try {
            activeNotifications
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException accessing activeNotifications", e)
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Error accessing activeNotifications", e)
            null
        }

    val safeCurrentRanking: RankingMap?
        get() = try {
            currentRanking
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException accessing currentRanking", e)
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Error accessing currentRanking", e)
            null
        }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d(TAG, "Notification posted: ${sbn?.packageName}")
        updateNotificationCount()
        try {
            com.xenonware.launcher.media.MediaControllerManager.update()
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating MediaControllerManager on notification posted", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d(TAG, "Notification removed: ${sbn?.packageName}")
        updateNotificationCount()
        try {
            com.xenonware.launcher.media.MediaControllerManager.update()
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating MediaControllerManager on notification removed", e)
        }
    }

    private lateinit var prefManager: SharedPreferenceManager

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "visible_notification_apps") {
            NotificationManager.visibleApps = prefManager.visibleNotificationApps.toSet()
            updateNotificationCount()
        }
    }

    override fun onListenerConnected() {
        instance = this
        prefManager = SharedPreferenceManager(this)
        NotificationManager.visibleApps = prefManager.visibleNotificationApps.toSet()
        prefManager.registerListener(preferenceListener)
        updateNotificationCount()
    }

    override fun onListenerDisconnected() {
        if (::prefManager.isInitialized) {
            prefManager.unregisterListener(preferenceListener)
        }
        instance = null
        super.onListenerDisconnected()
    }

    private fun updateNotificationCount() {
        try {
            NotificationManager.updateFromNotifications(
                context = this,
                activeNotifications = safeActiveNotifications,
                rankingMap = safeCurrentRanking,
                ownPackageName = packageName
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating notification count", e)
        }
    }
}

