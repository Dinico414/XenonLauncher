package com.xenonware.launcher.notification

import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.xenonware.launcher.data.SharedPreferenceManager

class XenonNotificationService : NotificationListenerService() {

    companion object {
        private var instance: XenonNotificationService? = null

        fun dismissNotification(key: String) {
            instance?.cancelNotification(key)
        }

        fun dismissAllNotifications() {
            instance?.cancelAllNotifications()
        }

        fun dismissNotificationsByPackage(packageName: String) {
            instance?.activeNotifications?.forEach { sbn ->
                if (sbn.packageName == packageName) {
                    instance?.cancelNotification(sbn.key)
                }
            }
        }

        fun getInstance(): XenonNotificationService? = instance

        fun getNotificationForSession(token: android.media.session.MediaSession.Token): StatusBarNotification? {
            val active = instance?.activeNotifications ?: return null
            return active.find { sbn ->
                val extras = sbn.notification.extras
                @Suppress("DEPRECATION")
                val session = extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION) as? android.media.session.MediaSession.Token
                    ?: extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
                session == token
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d("XenonNotificationService", "Notification posted: ${sbn?.packageName}")
        updateNotificationCount()
        com.xenonware.launcher.media.MediaControllerManager.update()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d("XenonNotificationService", "Notification removed: ${sbn?.packageName}")
        updateNotificationCount()
        com.xenonware.launcher.media.MediaControllerManager.update()
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
        NotificationManager.updateFromNotifications(
            context = this,
            activeNotifications = activeNotifications,
            rankingMap = currentRanking,
            ownPackageName = packageName
        )
    }
}
