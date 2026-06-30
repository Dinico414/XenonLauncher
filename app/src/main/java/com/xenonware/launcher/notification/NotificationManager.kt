package com.xenonware.launcher.notification

import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NotificationManager {
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    fun updateCount(count: Int) {
        _notificationCount.value = count
    }

    fun updateFromNotifications(
        activeNotifications: Array<StatusBarNotification>?,
        rankingMap: RankingMap?,
        ownPackageName: String
    ) {
        try {
            if (activeNotifications == null) {
                _notificationCount.value = 0
                return
            }

            val count = activeNotifications.count { sbn ->
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
            _notificationCount.value = count
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
