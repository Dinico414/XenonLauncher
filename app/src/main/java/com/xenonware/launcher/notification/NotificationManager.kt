package com.xenonware.launcher.notification

import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LauncherNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val icon: Drawable? = null,
    val contentIntent: android.app.PendingIntent? = null,
    val actions: List<LauncherNotificationAction> = emptyList()
)

data class LauncherNotificationAction(
    val title: String,
    val actionIntent: android.app.PendingIntent?,
    val remoteInput: android.app.RemoteInput? = null
)

object NotificationManager {
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    private val _notifications = MutableStateFlow<List<LauncherNotification>>(emptyList())
    val notifications: StateFlow<List<LauncherNotification>> = _notifications

    fun updateCount(count: Int) {
        _notificationCount.value = count
    }

    fun removeNotificationOptimistically(key: String) {
        val current = _notifications.value
        val updated = current.filter { it.key != key }
        if (updated.size != current.size) {
            _notifications.value = updated
            _notificationCount.value = updated.size
        }
    }

    fun removeNotificationsByPackageOptimistically(packageName: String) {
        val current = _notifications.value
        val updated = current.filter { it.packageName != packageName }
        if (updated.size != current.size) {
            _notifications.value = updated
            _notificationCount.value = updated.size
        }
    }

    fun removeAllNotificationsOptimistically() {
        _notifications.value = emptyList()
        _notificationCount.value = 0
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
                val notification = sbn.notification

                // 1. Core System Filters
                if (sbn.packageName == ownPackageName) return@filter false
                if (sbn.isOngoing) return@filter false

                // 1.5 Media Filter - Exclude media playback notifications
                val isMedia = notification.category == android.app.Notification.CATEGORY_TRANSPORT ||
                             notification.extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION) ||
                             notification.extras.getString(android.app.Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
                
                if (isMedia) return@filter false
                
                // 2. Ranking/Importance Filters
                if (hasRanking) {
                    // IMPORTANCE_MIN = 1, IMPORTANCE_LOW = 2, IMPORTANCE_DEFAULT = 3
                    // Show anything that isn't MIN importance (min is usually completely hidden/silent)
                    if (ranking.importance <= 1) return@filter false
                    if (ranking.isSuspended) return@filter false
                }

                // 3. Content Filters
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
                
                // Extracting text body
                var body = extras.getCharSequence("android.text")?.toString()
                    ?: extras.getCharSequence("android.bigText")?.toString()
                    ?: ""
                
                // 5. Advanced text extraction for MessagingStyle/InboxStyle
                // If it's a summary or the body is generic, try to get more specific content
                val isSummary = (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0
                
                if (isSummary || body.isBlank() || body.contains("new messages", ignoreCase = true) || body.contains("nachrichten", ignoreCase = true)) {
                    // Try MessagingStyle messages
                    @Suppress("DEPRECATION")
                    val messages = extras.get("android.messages") as? Array<*>
                    if (!messages.isNullOrEmpty()) {
                        val lastMessage = messages.last() as? android.os.Bundle
                        val messageText = lastMessage?.getCharSequence("text")
                        if (messageText != null) {
                            body = messageText.toString()
                        }
                    } else {
                        // Try InboxStyle lines
                        val lines = extras.getCharSequenceArray("android.textLines")
                        if (!lines.isNullOrEmpty()) {
                            body = lines.last().toString()
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
                    contentIntent = sbn.notification.contentIntent,
                    actions = sbn.notification.actions?.map { action ->
                        Log.d("NotificationManager", "Parsing action: ${action.title}, hasIntent=${action.actionIntent != null}")
                        LauncherNotificationAction(
                            title = action.title.toString(),
                            actionIntent = action.actionIntent,
                            remoteInput = action.remoteInputs?.firstOrNull()
                        )
                    } ?: emptyList()
                ).also {
                    Log.d("NotificationManager", "Parsed notification from ${it.packageName}: title=${it.title}, hasContentIntent=${it.contentIntent != null}, actionCount=${it.actions.size}")
                }
            }.sortedByDescending { it.postTime }
            
            _notificationCount.value = _notifications.value.size
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
