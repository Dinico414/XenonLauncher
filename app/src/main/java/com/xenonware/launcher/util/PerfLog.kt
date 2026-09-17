package com.xenonware.launcher.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import com.xenonware.launcher.util.JankWatchdog.SAMPLE_MS
import com.xenonware.launcher.util.JankWatchdog.STALL_THRESHOLD_MS
import com.xenonware.launcher.util.PerfLog.init
import kotlin.concurrent.thread

/**
 * Debug-only performance logging. Everything here is a no-op unless the app is debuggable
 * (or forced on) and [init] has been called.
 *
 * Logcat filter:  tag~:Xenon|Choreographer
 */
object PerfLog {
    const val TAG = "XenonPerf"

    @Volatile
    var enabled = false
        private set

    /**
     * Call once, e.g. at the top of MainActivity.onCreate.
     * Pass force = true to enable it in a release build as well (remove before shipping).
     */
    fun init(context: Context, watchdog: Boolean = true, force: Boolean = false) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        enabled = debuggable || force
        // Logged even when disabled, so you can tell whether init ran at all.
        Log.w(TAG, "PerfLog.init: debuggable=$debuggable force=$force -> enabled=$enabled, watchdog=$watchdog")
        if (enabled && watchdog) JankWatchdog.start()
    }

    /**
     * Times [block]. Logs when it takes at least [thresholdMs], and also emits a trace
     * section so the block shows up by name in an Android Studio System Trace.
     */
    inline fun <T> measure(tag: String, thresholdMs: Long = 4L, block: () -> T): T {
        if (!enabled) return block()
        // Everything that could throw happens before beginSection, so the section is
        // always closed by the finally block below.
        val sectionName = tag.take(127)
        val start = SystemClock.elapsedRealtimeNanos()
        Trace.beginSection(sectionName)
        try {
            return block()
        } finally {
            Trace.endSection()
            val ms = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
            if (ms >= thresholdMs) {
                Log.w(TAG, "$tag took ${ms}ms on ${Thread.currentThread().name}")
            }
        }
    }
}

/**
 * A sampling profiler for main-thread stalls.
 *
 * The main thread bumps a timestamp every frame. A background thread checks it every
 * [SAMPLE_MS]; while the timestamp is older than [STALL_THRESHOLD_MS], it captures the main
 * thread's stack. When the stall ends, it logs which of your methods were on the stack most
 * often, which framework calls were innermost, and the most common full stack.
 */
object JankWatchdog {
    private const val TAG = "XenonJank"
    private const val TICK_MS = 16L
    private const val SAMPLE_MS = 20L
    private const val STALL_THRESHOLD_MS = 100L
    private const val STACK_DEPTH = 45
    private val APP_PREFIXES = listOf("com.xenonware.", "com.xenon.")

    @Volatile private var started = false
    @Volatile private var lastTick = 0L

    fun start() {
        if (started) return
        started = true

        val main = Handler(Looper.getMainLooper())
        lastTick = SystemClock.uptimeMillis()
        main.post(object : Runnable {
            override fun run() {
                lastTick = SystemClock.uptimeMillis()
                main.postDelayed(this, TICK_MS)
            }
        })

        thread(name = "XenonJankWatchdog", isDaemon = true, priority = Thread.MAX_PRIORITY) {
            runCatching { loop() }.onFailure { Log.e(TAG, "watchdog died", it) }
        }
    }

    private class Stall(val startTick: Long) {
        var samples = 0
        val appMethods = HashMap<String, Int>()
        val appMethodLine = HashMap<String, String>()
        val leaves = HashMap<String, Int>()
        val stacks = HashMap<String, Int>()
        val stackText = HashMap<String, String>()
    }

    private fun isApp(frame: StackTraceElement) = APP_PREFIXES.any { frame.className.startsWith(it) }

    private fun loop() {
        val mainThread = Looper.getMainLooper().thread
        var stall: Stall? = null

        while (true) {
            Thread.sleep(SAMPLE_MS)
            val tick = lastTick
            val blocked = SystemClock.uptimeMillis() - tick >= STALL_THRESHOLD_MS

            val current = stall
            if (current != null && (!blocked || current.startTick != tick)) {
                // The main thread moved on: the tick advanced since this stall began.
                report(current, endTick = if (blocked) tick else lastTick)
                stall = null
            }
            if (!blocked) continue

            val s = stall ?: Stall(tick).also { stall = it }
            sample(s, mainThread.stackTrace)
        }
    }

    private fun sample(s: Stall, trace: Array<StackTraceElement>) {
        if (trace.isEmpty()) return
        s.samples++

        val seen = HashSet<String>()
        for (frame in trace) {
            if (!isApp(frame)) continue
            val key = "${frame.className}.${frame.methodName}"
            if (seen.add(key)) {
                s.appMethods[key] = (s.appMethods[key] ?: 0) + 1
                s.appMethodLine.putIfAbsent(key, "${frame.fileName}:${frame.lineNumber}")
            }
        }

        val leaf = trace[0].let { "${it.className}.${it.methodName}" }
        s.leaves[leaf] = (s.leaves[leaf] ?: 0) + 1

        val deepestApp = trace.firstOrNull(::isApp)?.let { "${it.className}.${it.methodName}" } ?: "-"
        val stackKey = "$leaf <- $deepestApp"
        s.stacks[stackKey] = (s.stacks[stackKey] ?: 0) + 1
        s.stackText.putIfAbsent(
            stackKey,
            trace.take(STACK_DEPTH).joinToString("\n") { "    at $it" }
        )
    }

    private fun report(s: Stall, endTick: Long) {
        if (s.samples == 0) return
        val duration = (endTick - s.startTick).coerceAtLeast(STALL_THRESHOLD_MS)
        fun pct(n: Int) = "%3d%%".format(n * 100 / s.samples)

        val sb = StringBuilder()
        sb.append("MAIN THREAD BLOCKED ~${duration}ms (${s.samples} samples)\n")
        sb.append("Your code on the stack (share of samples):\n")
        s.appMethods.entries.sortedByDescending { it.value }.take(15).forEach { (m, n) ->
            sb.append("  ${pct(n)}  $m (${s.appMethodLine[m]})\n")
        }
        sb.append("Innermost calls:\n")
        s.leaves.entries.sortedByDescending { it.value }.take(8).forEach { (m, n) ->
            sb.append("  ${pct(n)}  $m\n")
        }
        logLong(sb.toString())

        s.stacks.entries.sortedByDescending { it.value }.take(2).forEachIndexed { i, (key, n) ->
            logLong("Stack #${i + 1} (${pct(n).trim()} of samples):\n${s.stackText[key]}")
        }
    }

    // Logcat truncates entries at roughly 4 KB.
    private fun logLong(text: String) {
        var chunk = StringBuilder()
        for (line in text.lineSequence()) {
            if (chunk.length + line.length > 3500) {
                Log.w(TAG, chunk.toString())
                chunk = StringBuilder()
            }
            chunk.append(line).append('\n')
        }
        if (chunk.isNotEmpty()) Log.w(TAG, chunk.toString())
    }
}