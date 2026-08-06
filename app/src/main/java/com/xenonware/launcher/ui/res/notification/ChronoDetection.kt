package com.xenonware.launcher.ui.res.notification

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import kotlin.math.abs

enum class ChronoKind { NONE, TIMER, STOPWATCH }

/**
 * Everything the UI needs to render a live timer/stopwatch, in one place.
 *
 * All times are WALL CLOCK (System.currentTimeMillis). elapsedRealtime bases are
 * normalised on the way in, so the UI never has to care about time domains.
 */
data class ChronoState(
    val kind: ChronoKind = ChronoKind.NONE,
    val isRunning: Boolean = false,
    /** TIMER: wall-clock instant it expires. STOPWATCH: wall-clock instant it started. */
    val baseWallTime: Long = 0L,
    /** Only when !isRunning. TIMER: remaining ms. STOPWATCH: elapsed ms. -1 = unknown. */
    val frozenMs: Long = -1L,
) {
    val isTimeRelated: Boolean get() = kind != ChronoKind.NONE

    val isDisplayable: Boolean
        get() = kind != ChronoKind.NONE && (isRunning || frozenMs >= 0L)

    companion object {
        val NONE = ChronoState()
    }
}

object ChronoDetector {

    private const val TAG = "XenonChrono"
    private const val DEBUG = true

    // Android 16 promoted-ongoing / MetricStyle payload. No public constants yet.
    private const val KEY_METRICS = "android.metrics"
    private const val KEY_LABEL = "label"
    private const val KEY_VALUE = "value"
    private const val KEY_ZERO_ELAPSED = "zeroElapsedRealtime"
    private const val KEY_PAUSED_DURATION = "pausedDuration"
    private const val KEY_COUNT_DOWN = "countDown"

    /**
     * A Chronometer base is only credible as a live base if it sits near the current
     * elapsedRealtime. Paused notifications reuse the same field for a raw duration
     * (e.g. -25761), which would otherwise be read as a base ~5 days in the past.
     * Cost: a chronometer running longer than this window is treated as paused.
     */
    private const val LIVE_BASE_WINDOW_MS = 24L * 60 * 60 * 1000

    private val CLOCK_PACKAGE_HINTS = listOf("deskclock", "clock", "alarm", "timer", "stopwatch")

    private val TIMER_HINTS = listOf("timer", "countdown", "count_down")
    private val STOPWATCH_HINTS = listOf("stopwatch", "stop_watch", "chronometer", "lap")

    private val TIMER_WORDS = listOf(
        "timer", "minuteur", "temporizador", "таймер", "计时器", "タイマー", "타이머",
    )
    private val STOPWATCH_WORDS = listOf(
        "stopwatch", "stoppuhr", "chronomètre", "chronometre", "cronómetro", "cronometro",
        "chronometr", "секундомер", "秒表", "ストップウォッチ", "스톱워치",
    )

    private val CLOCK_PATTERN = Regex("""(?<!\d)(?:(\d{1,3}):)?(\d{1,2}):([0-5]\d)(?!\d)""")
    private val UNIT_PATTERN = Regex("""(\d+)\s*(h|hr|hrs|std|m|min|mins|s|sec|secs)\b""")

    fun looksLikeClockApp(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return CLOCK_PACKAGE_HINTS.any { pkg.contains(it) }
    }

    /** Main thread only — the RemoteViews fallback inflates views. */
    fun detect(context: Context, sbn: StatusBarNotification): ChronoState {
        val n = sbn.notification ?: return ChronoState.NONE
        val extras = n.extras ?: return ChronoState.NONE

        val now = System.currentTimeMillis()

        val idHint = buildString {
            append(n.channelId?.lowercase().orEmpty()).append(' ')
            append(n.group?.lowercase().orEmpty()).append(' ')
            append(n.sortKey?.lowercase().orEmpty())
        }
        val textHint = textOf(extras)
        val kindFromId = hintKind(idHint)
        val kindFromText = hintKind(textHint, localised = true)

        // -----------------------------------------------------------------
        // Path 0 — android.metrics (Android 16 promoted ongoing / MetricStyle).
        // Structured, unlocalised, and exactly what the shelf renders from.
        // -----------------------------------------------------------------
        fromMetrics(extras, now, kindFromId, kindFromText)?.let {
            if (DEBUG) Log.d(TAG, "PATH0 (metrics): $it")
            return it
        }

        // -----------------------------------------------------------------
        // Path 1 — standard header chronometer (EXTRA_SHOW_CHRONOMETER).
        // -----------------------------------------------------------------
        val showChrono = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
        val countDownFlag = extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN, false)
        val hasCountDownKey = extras.containsKey(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val whenBase = normaliseToWallClock(n.`when`, now)

        if (showChrono && whenBase > 0L) {
            val kind = when {
                whenBase > now + 1_500L -> ChronoKind.TIMER
                countDownFlag -> ChronoKind.TIMER
                hasCountDownKey -> ChronoKind.STOPWATCH
                kindFromId != ChronoKind.NONE -> kindFromId
                else -> ChronoKind.STOPWATCH
            }
            if (DEBUG) Log.d(TAG, "PATH1 (chronometer extras): kind=$kind base=${whenBase - now}ms")
            return ChronoState(kind, isRunning = true, baseWallTime = whenBase)
        }

        val isCandidate = looksLikeClockApp(sbn.packageName) ||
                kindFromId != ChronoKind.NONE ||
                kindFromText != ChronoKind.NONE
        if (!isCandidate) return ChronoState.NONE

        if (n.category == Notification.CATEGORY_ALARM && kindFromId == ChronoKind.NONE) {
            return ChronoState.NONE
        }

        // -----------------------------------------------------------------
        // Path 2 — inflate the RemoteViews and read the Chronometer widget.
        // Fallback for clock apps that predate MetricStyle.
        // -----------------------------------------------------------------
        val probe = probeRemoteViews(context, n)
        val kindHint = when {
            kindFromId != ChronoKind.NONE -> kindFromId
            else -> kindFromText
        }

        if (probe?.chronoBase != null) {
            val elapsedNow = SystemClock.elapsedRealtime()
            val raw = probe.chronoBase
            val isLiveBase = abs(raw - elapsedNow) < LIVE_BASE_WINDOW_MS

            if (isLiveBase) {
                val base = now - elapsedNow + raw
                val kind = when {
                    base > now + 1_500L -> ChronoKind.TIMER
                    probe.countDown == true -> ChronoKind.TIMER
                    probe.countDown == false -> ChronoKind.STOPWATCH
                    kindHint != ChronoKind.NONE -> kindHint
                    else -> ChronoKind.STOPWATCH
                }
                if (DEBUG) Log.d(TAG, "PATH2 (remoteviews): kind=$kind base=${base - now}ms")
                return ChronoState(kind, isRunning = true, baseWallTime = base)
            }

            // Not a base — a paused duration reusing the same field.
            val kind = when {
                probe.countDown == true -> ChronoKind.TIMER
                probe.countDown == false -> ChronoKind.STOPWATCH
                kindHint != ChronoKind.NONE -> kindHint
                else -> ChronoKind.NONE
            }
            if (kind != ChronoKind.NONE) {
                if (DEBUG) Log.d(TAG, "PATH2 (paused, raw=$raw): kind=$kind frozen=${abs(raw)}")
                return ChronoState(kind, isRunning = false, frozenMs = abs(raw))
            }
        }

        // -----------------------------------------------------------------
        // Path 3 — last resort: scrape a duration out of the text.
        // -----------------------------------------------------------------
        if (kindHint == ChronoKind.NONE) return ChronoState.NONE

        val frozen = parseDuration(textHint).takeIf { it >= 0L }
            ?: probe?.texts?.firstNotNullOfOrNull { t -> parseDuration(t).takeIf { it >= 0L } }
            ?: -1L

        if (DEBUG) Log.d(TAG, "PATH3 (text): kind=$kindHint frozen=$frozen")
        return ChronoState(kindHint, isRunning = false, frozenMs = frozen)
    }

    // ------------------------------------------------------------------
    // Path 0: android.metrics
    // ------------------------------------------------------------------

    private fun fromMetrics(
        extras: Bundle,
        now: Long,
        kindFromId: ChronoKind,
        kindFromText: ChronoKind,
    ): ChronoState? {
        val raw = try { extras.get(KEY_METRICS) } catch (_: Throwable) { null } ?: return null

        val entries: List<Bundle> = when (raw) {
            is Bundle -> listOf(raw)
            is Array<*> -> raw.filterIsInstance<Bundle>()
            is Collection<*> -> raw.filterIsInstance<Bundle>()
            else -> emptyList()
        }
        if (entries.isEmpty()) return null

        for (entry in entries) {
            val value = entry.getBundle(KEY_VALUE) ?: continue
            val label = entry.get(KEY_LABEL)?.toString()?.lowercase().orEmpty()

            val countDown = if (value.containsKey(KEY_COUNT_DOWN)) {
                value.getBoolean(KEY_COUNT_DOWN)
            } else null

            val kind = when {
                countDown == true -> ChronoKind.TIMER
                countDown == false -> ChronoKind.STOPWATCH
                hintKind(label, localised = true) != ChronoKind.NONE ->
                    hintKind(label, localised = true)
                kindFromId != ChronoKind.NONE -> kindFromId
                kindFromText != ChronoKind.NONE -> kindFromText
                else -> continue
            }

            // Running: base is an elapsedRealtime instant.
            if (value.containsKey(KEY_ZERO_ELAPSED)) {
                val zero = (value.get(KEY_ZERO_ELAPSED) as? Number)?.toLong() ?: continue
                val baseWall = now - SystemClock.elapsedRealtime() + zero
                return ChronoState(kind, isRunning = true, baseWallTime = baseWall)
            }

            // Paused: a raw duration. TIMER -> remaining, STOPWATCH -> elapsed.
            if (value.containsKey(KEY_PAUSED_DURATION)) {
                val paused = (value.get(KEY_PAUSED_DURATION) as? Number)?.toLong() ?: continue
                return ChronoState(kind, isRunning = false, frozenMs = abs(paused))
            }
        }
        return null
    }

    // ------------------------------------------------------------------
    // Path 2: RemoteViews probing
    // ------------------------------------------------------------------

    private class Probe(
        /** RAW Chronometer.getBase() — caller decides if it's a base or a duration. */
        val chronoBase: Long?,
        val countDown: Boolean?,
        val texts: List<String>,
    )

    private fun probeRemoteViews(context: Context, n: Notification): Probe? {
        var source = "bigContentView"
        var views: RemoteViews? = n.bigContentView
        if (views == null) { source = "contentView"; views = n.contentView }
        if (views == null) { source = "headsUpContentView"; views = n.headsUpContentView }

        if (views == null) {
            source = "recoverBuilder"
            views = try {
                val b = Notification.Builder.recoverBuilder(context, n)
                b.createBigContentView() ?: b.createContentView()
            } catch (e: Throwable) {
                if (DEBUG) Log.w(TAG, "probe: recoverBuilder failed: ${e.javaClass.simpleName}")
                null
            }
        }
        if (views == null) return null

        val root: View = try {
            views.apply(context, FrameLayout(context))
        } catch (e: Throwable) {
            if (DEBUG) Log.w(TAG, "probe: apply($source) failed: ${e.javaClass.simpleName}: ${e.message}")
            return null
        }

        val chronometers = ArrayList<Chronometer>(2)
        val texts = ArrayList<String>(8)
        walk(root, chronometers, texts)

        if (DEBUG) {
            Log.d(TAG, "probe($source): bases=${chronometers.map { it.base }} texts=$texts")
        }

        val chrono = chronometers.firstOrNull { it.base != 0L }
        val countDown = if (chrono != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            chrono.isCountDown
        } else null

        return Probe(chrono?.base, countDown, texts)
    }

    private fun walk(view: View, chronos: MutableList<Chronometer>, texts: MutableList<String>) {
        if (view.visibility != View.VISIBLE) return

        when (view) {
            // Chronometer extends TextView, so it must be matched first.
            is Chronometer -> chronos.add(view)
            is TextView -> view.text?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { texts.add(it) }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), chronos, texts)
        }
    }

    // ------------------------------------------------------------------

    private fun normaliseToWallClock(raw: Long, now: Long): Long = when {
        raw <= 0L -> 0L
        raw < 1_000_000_000_000L -> now - SystemClock.elapsedRealtime() + raw
        else -> raw
    }

    private fun hintKind(haystack: String, localised: Boolean = false): ChronoKind {
        val timer = if (localised) TIMER_WORDS else TIMER_HINTS
        val stopwatch = if (localised) STOPWATCH_WORDS else STOPWATCH_HINTS
        return when {
            stopwatch.any { haystack.contains(it) } -> ChronoKind.STOPWATCH
            timer.any { haystack.contains(it) } -> ChronoKind.TIMER
            else -> ChronoKind.NONE
        }
    }

    private fun textOf(extras: Bundle): String = buildString {
        listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
        ).forEach { k -> append(extras.getCharSequence(k)?.toString().orEmpty()).append(' ') }
    }.lowercase()

    private fun parseDuration(text: String): Long {
        CLOCK_PATTERN.find(text)?.let { m ->
            val a = m.groupValues[1].toLongOrNull()
            val b = m.groupValues[2].toLongOrNull() ?: 0L
            val c = m.groupValues[3].toLongOrNull() ?: 0L
            return if (a != null) (a * 3600 + b * 60 + c) * 1000L else (b * 60 + c) * 1000L
        }

        var total = 0L
        var matched = false
        UNIT_PATTERN.findAll(text).forEach { m ->
            val value = m.groupValues[1].toLongOrNull() ?: return@forEach
            matched = true
            total += when (m.groupValues[2]) {
                "h", "hr", "hrs", "std" -> value * 3_600_000L
                "m", "min", "mins" -> value * 60_000L
                else -> value * 1_000L
            }
        }
        return if (matched) total else -1L
    }

    // ------------------------------------------------------------------

    /** One Log.d per line — Android Studio collapses multi-line entries. */
    fun dump(context: Context, sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        val e = n.extras
        val now = System.currentTimeMillis()

        Log.d(TAG, "--- DUMP ${sbn.packageName} id=${sbn.id}")
        Log.d(TAG, "    channel=${n.channelId} category=${n.category} ongoing=${sbn.isOngoing}")
        Log.d(TAG, "    when=${n.`when`} delta=${n.`when` - now}ms elapsedNow=${SystemClock.elapsedRealtime()}")
        Log.d(TAG, "    showChrono=${e.get(Notification.EXTRA_SHOW_CHRONOMETER)} countDown=${e.get(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)}")
        Log.d(TAG, "    template=${e.getString(Notification.EXTRA_TEMPLATE)}")
        Log.d(TAG, "    metrics=${e.get(KEY_METRICS)}")
        Log.d(TAG, "    actions=${n.actions?.map { it.title }}")
    }
}