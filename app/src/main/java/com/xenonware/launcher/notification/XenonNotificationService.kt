package com.xenonware.launcher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class XenonNotificationService : NotificationListenerService() {

    companion object {
        private var instance: XenonNotificationService? = null

        fun dismissNotification(key: String) {
            instance?.cancelNotification(key)
        }

        fun dismissAllNotifications() {
            instance?.cancelAllNotifications()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotificationCount()
    }

    override fun onListenerConnected() {
        instance = this
        updateNotificationCount()
    }

    override fun onListenerDisconnected() {
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
