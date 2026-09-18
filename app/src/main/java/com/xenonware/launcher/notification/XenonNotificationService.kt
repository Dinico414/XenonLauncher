package com.xenonware.launcher.notification

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
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

        fun dismissNotifications(keys: List<String>) {
            keys.forEach { key ->
                try {
                    instance?.cancelNotification(key)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error canceling notification: $key", e)
                }
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

        /**
         * Cancels every notification that currently matches, re-derived from the live listener
         * state at cancel time. Dismissing by a key list the UI captured earlier loses anything
         * that was reposted or re-keyed in between, which is what made "clear muted" drop only
         * one item.
         */
        fun dismissWhere(
            tag: String,
            predicate: (StatusBarNotification, Ranking) -> Boolean
        ) {
            val svc = instance ?: return
            svc.cancelMatching(tag, predicate, attempt = 0)
        }

        fun dismissMuted() = dismissWhere("muted") { sbn, ranking ->
            !sbn.isOngoing && ranking.importance <= 2
        }

        fun dismissPermanent() = dismissWhere("permanent") { sbn, _ -> sbn.isOngoing }

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

    private val retryHandler = Handler(Looper.getMainLooper())

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

    /**
     * One batch cancel, then up to two verified retries. Each individual cancel triggers
     * onNotificationRemoved and a full rebuild, so cancelling in a loop over a stale key list
     * races against that rebuild; the batch API plus a survivor check does not.
     */
    private fun cancelMatching(
        tag: String,
        predicate: (StatusBarNotification, Ranking) -> Boolean,
        attempt: Int
    ) {
        val active = safeActiveNotifications ?: return
        val rankingMap = safeCurrentRanking

        val keys = active.filter { sbn ->
            val ranking = Ranking()
            val has = try {
                rankingMap?.getRanking(sbn.key, ranking) == true
            } catch (_: Throwable) {
                false
            }
            if (has) predicate(sbn, ranking) else false
        }.map { it.key }

        if (keys.isEmpty()) return

        try {
            cancelNotifications(keys.toTypedArray())
        } catch (e: Throwable) {
            Log.e(TAG, "batch cancel failed for $tag, falling back to per-key", e)
            keys.forEach { key ->
                try {
                    cancelNotification(key)
                } catch (e2: Throwable) {
                    Log.e(TAG, "Error canceling notification: $key", e2)
                }
            }
        }

        if (attempt < 2) {
            retryHandler.postDelayed({
                val survivors = safeActiveNotifications?.count { it.key in keys } ?: 0
                if (survivors > 0) {
                    Log.w(TAG, "$tag: $survivors/${keys.size} survived cancel, retrying")
                    cancelMatching(tag, predicate, attempt + 1)
                }
            }, 350L)
        }
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
        when (key) {
            "visible_notification_apps" -> {
                NotificationManager.visibleApps = prefManager.visibleNotificationApps.toSet()
                updateNotificationCount()
            }
            "show_permanent_notifications" -> {
                NotificationManager.showPermanentNotifications = prefManager.showPermanentNotifications
                updateNotificationCount()
            }
            "show_mute_notifications" -> {
                NotificationManager.showMuteNotifications = prefManager.showMuteNotifications
                updateNotificationCount()
            }
            "disable_grouping" -> {
                NotificationManager.disableGrouping = prefManager.disableGrouping
                updateNotificationCount()
            }
        }
    }

    override fun onListenerConnected() {
        instance = this
        prefManager = SharedPreferenceManager(this)
        NotificationManager.visibleApps = prefManager.visibleNotificationApps.toSet()
        NotificationManager.showPermanentNotifications = prefManager.showPermanentNotifications
        NotificationManager.showMuteNotifications = prefManager.showMuteNotifications
        NotificationManager.disableGrouping = prefManager.disableGrouping
        prefManager.registerListener(preferenceListener)
        updateNotificationCount()
    }

    override fun onListenerDisconnected() {
        if (::prefManager.isInitialized) {
            prefManager.unregisterListener(preferenceListener)
        }
        retryHandler.removeCallbacksAndMessages(null)
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