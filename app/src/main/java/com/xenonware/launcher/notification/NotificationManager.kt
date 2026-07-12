package com.xenonware.launcher.notification

import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LauncherNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val icon: Drawable? = null,
    val contentIntent: android.app.PendingIntent? = null
)

object NotificationManager {
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    private val _notifications = MutableStateFlow<List<LauncherNotification>>(emptyList())
    val notifications: StateFlow<List<LauncherNotification>> = _notifications

    fun updateCount(count: Int) {
        _notificationCount.value = count
    }

    fun updateFromNotifications(
        context: android.content.Context,
        activeNotifications: Array<StatusBarNotification>?,
        rankingMap: RankingMap?,
        ownPackageName: String
    ) {
        try {
            if (activeNotifications == null) {
                _notificationCount.value = 0
                _notifications.value = emptyList()
                return
            }

            val filtered = activeNotifications.filter { sbn ->
                val ranking = Ranking()
                val hasRanking = rankingMap?.getRanking(sbn.key, ranking) ?: false

                val isOngoing = sbn.isOngoing
                val isMuted = if (hasRanking) {
                    ranking.importance < 3 // IMPORTANCE_DEFAULT is 3
                } else {
                    @Suppress("DEPRECATION")
                    sbn.notification.defaults == 0 && sbn.notification.sound == null && sbn.notification.vibrate == null
                }

                !isOngoing && !isMuted && sbn.packageName != ownPackageName
            }

            _notificationCount.value = filtered.size
            _notifications.value = filtered.map { sbn ->
                val extras = sbn.notification.extras
                
                LauncherNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = extras.getString("android.title"),
                    text = extras.getCharSequence("android.text")?.toString(),
                    postTime = sbn.postTime,
                    icon = sbn.notification.smallIcon?.loadDrawable(context),
                    contentIntent = sbn.notification.contentIntent
                )
            }.sortedByDescending { it.postTime }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
