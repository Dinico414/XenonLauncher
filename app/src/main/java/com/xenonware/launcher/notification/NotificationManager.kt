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

                // 1. Core System Filters
                if (sbn.packageName == ownPackageName) return@filter false
                if (sbn.isOngoing) return@filter false
                
                // 2. Ranking/Importance Filters
                if (hasRanking) {
                    // IMPORTANCE_MIN = 1, IMPORTANCE_LOW = 2, IMPORTANCE_DEFAULT = 3
                    // Show anything that isn't MIN importance (min is usually completely hidden/silent)
                    if (ranking.importance <= 1) return@filter false
                    if (ranking.isSuspended) return@filter false
                }

                // 3. Content Filters
                val notification = sbn.notification
                val extras = notification.extras
                
                val title = extras.getCharSequence("android.title")
                val text = extras.getCharSequence("android.text")
                val bigText = extras.getCharSequence("android.bigText")
                val messages = extras.get("android.messages")
                
                // Permissive content check
                if (title.isNullOrBlank() && text.isNullOrBlank() && bigText.isNullOrBlank() && messages == null) {
                    return@filter false
                }

                true
            }

            // 4. Improved Grouping Logic
            // Instead of grouping by package, group by the actual notification group key.
            // This ensures that if an app has multiple different groups (e.g. different email accounts),
            // we handle summaries correctly for each group.
            val groupedByGroup = filtered.groupBy { it.groupKey ?: (it.packageName + it.id) }
            
            val finalNotifications = groupedByGroup.flatMap { (_, sbnList) ->
                val summaries = sbnList.filter { (it.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0 }
                val children = sbnList.filter { (it.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) == 0 }
                
                if (children.isNotEmpty()) {
                    children // Prefer individual notifications
                } else {
                    summaries // Fallback to summary if no children (rare but possible)
                }
            }

            _notifications.value = finalNotifications.map { sbn ->
                val extras = sbn.notification.extras
                
                // Extracting title
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                
                // Extracting text body - prioritizing standard fields
                var body = extras.getCharSequence("android.text")?.toString()
                    ?: extras.getCharSequence("android.bigText")?.toString()
                    ?: ""
                
                // 5. Special handling for MessagingStyle (WhatsApp, Telegram, etc.)
                // These often have a list of messages in the extras.
                if (body.isBlank() || (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0) {
                    @Suppress("DEPRECATION")
                    val messages = extras.get("android.messages") as? Array<*>
                    if (!messages.isNullOrEmpty()) {
                        // Get the last message's text
                        val lastMessage = messages.last() as? android.os.Bundle
                        val messageText = lastMessage?.getCharSequence("text")
                        if (messageText != null) {
                            body = messageText.toString()
                        }
                    }
                }
                
                LauncherNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title,
                    text = body,
                    postTime = sbn.postTime,
                    icon = sbn.notification.smallIcon?.loadDrawable(context),
                    contentIntent = sbn.notification.contentIntent
                )
            }.sortedByDescending { it.postTime }
            
            _notificationCount.value = _notifications.value.size
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
