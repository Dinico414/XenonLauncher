package com.xenonware.launcher

import android.app.Application
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XenonApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val trace = throwable.stackTraceToString()
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val logFile = File(getExternalFilesDir(null), "crash_log.txt")
                
                val deviceInfo = "Device: ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n" +
                        "App Version: ${packageManager.getPackageInfo(packageName, 0).versionName}\n"
                
                val logContent = "--- CRASH LOG ---\n" +
                        "Time: $timestamp\n" +
                        deviceInfo +
                        "Thread: ${thread.name}\n" +
                        "Message: ${throwable.message}\n" +
                        "Stacktrace:\n$trace\n" +
                        "-----------------\n\n"
                logFile.appendText(logContent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
