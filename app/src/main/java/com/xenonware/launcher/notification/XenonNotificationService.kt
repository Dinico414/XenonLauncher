package com.xenonware.launcher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class XenonNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotificationCount()
    }

    override fun onListenerConnected() {
        updateNotificationCount()
    }

    private fun updateNotificationCount() {
        NotificationManager.updateFromNotifications(
            activeNotifications = activeNotifications,
            rankingMap = currentRanking,
            ownPackageName = packageName
        )
    }
}
