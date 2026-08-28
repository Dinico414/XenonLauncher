package com.xenonware.launcher

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.xenonware.launcher.data.SharedPreferenceManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XenonApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedLocale()
        setupCrashHandler()
    }

    private fun applySavedLocale() {
        val prefs = SharedPreferenceManager(this)
        val tag = prefs.languageTag
        if (tag.isNotEmpty()) {
            val appLocale = LocaleListCompat.forLanguageTags(tag)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = throwable.stackTraceToString()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val logFile = File(getExternalFilesDir(null), "crash_log.txt")
                
                val deviceInfo = "Device: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\n" +
                        "App Version: ${packageManager.getPackageInfo(packageName, 0).versionName}\n"
                
                val logContent = "--- CRASH LOG ---\n" +
                        "Time: $timestamp\n" +
                        deviceInfo +
                        "Thread: ${thread.name}\n" +
                        "Message: ${throwable.message}\n" +
                        "Stacktrace:\n$trace\n" +
                        "-----------------\n\n"
                // Append to file so we can store multiple crashes if they happen before a manual clear
                logFile.appendText(logContent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Let the system handle the crash as well (shows the "App has stopped" dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
