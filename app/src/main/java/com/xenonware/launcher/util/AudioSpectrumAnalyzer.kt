package com.xenonware.launcher.media

import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Liest das FFT-Spektrum des globalen Audio-Ausgangs (Session 0) und verdichtet es zu
 * logarithmischen Bändern plus drei Gruppenwerten (Bass / Mitten / Höhen), jeweils 0..1.
 *
 * Braucht RECORD_AUDIO (Runtime) + MODIFY_AUDIO_SETTINGS (Manifest). Es wird kein Mikrofon
 * genutzt – Android hängt den Visualizer nur an diese Berechtigung.
 *
 * Geschrieben wird vom Visualizer-Thread, gelesen im UI-Frame-Loop. Ein gelegentlich
 * "gemischter" Frame zwischen zwei Floats ist für eine Animation egal, daher kein Locking.
 */
class AudioSpectrumAnalyzer(val bandCount: Int = 24) {

    /** Normalisierte log-Bänder 0..1, tief → hoch. */
    val bands = FloatArray(bandCount)

    @Volatile var bass = 0f; private set
    @Volatile var mid = 0f; private set
    @Volatile var high = 0f; private set

    @Volatile private var lastSignalAt = 0L
    private var visualizer: Visualizer? = null

    /** start() is called from a 1 s poll loop: don't hammer a device that can't create a Visualizer. */
    private var lastStartFailureAt = 0L

    private val peaks = FloatArray(bandCount) { MIN_PEAK }
    private val bandCenters = FloatArray(bandCount)
    private val binStart = IntArray(bandCount)
    private val binEnd = IntArray(bandCount)

    val isRunning: Boolean get() = visualizer != null

    /** true, solange in den letzten ~0,6 s hörbares Signal ankam. */
    fun hasFreshSignal(): Boolean =
        SystemClock.uptimeMillis() - lastSignalAt < SIGNAL_TIMEOUT_MS

    fun start(): Boolean {
        if (visualizer != null) return true
        if (lastStartFailureAt != 0L &&
            SystemClock.uptimeMillis() - lastStartFailureAt < RETRY_BACKOFF_MS
        ) return false
        var v: Visualizer? = null
        return try {
            v = Visualizer(0)
            v.setEnabled(false)
            v.setCaptureSize(Visualizer.getCaptureSizeRange()[1])
            v.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED)
            computeBins(v.captureSize, v.samplingRate / 1000f) // samplingRate ist in mHz
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) process(fft)
                    }
                },
                Visualizer.getMaxCaptureRate(),
                false,
                true
            )
            v.setEnabled(true)
            visualizer = v
            lastStartFailureAt = 0L
            true
        } catch (_: Throwable) {
            lastStartFailureAt = SystemClock.uptimeMillis()
            // Keine Berechtigung, Gerät unterstützt Session 0 nicht, o. Ä.
            runCatching { v?.release() }
            false
        }
    }

    fun stop() {
        visualizer?.let {
            runCatching { it.setEnabled(false) }
            runCatching { it.release() }
        }
        visualizer = null
        bands.fill(0f)
        bass = 0f; mid = 0f; high = 0f
    }

    private fun computeBins(captureSize: Int, sampleRateHz: Float) {
        val lastBin = captureSize / 2 - 1
        val hzPerBin = sampleRateHz / captureSize
        val fMax = min(MAX_FREQ, sampleRateHz / 2f)
        val ratio = fMax / MIN_FREQ
        for (i in 0 until bandCount) {
            val lo = MIN_FREQ * ratio.pow(i / bandCount.toFloat())
            val hi = MIN_FREQ * ratio.pow((i + 1) / bandCount.toFloat())
            bandCenters[i] = sqrt(lo * hi)
            // Tiefe Bänder sind schmaler als ein FFT-Bin → mindestens ein Bin pro Band.
            val s = (lo / hzPerBin).toInt().coerceIn(1, lastBin)
            binStart[i] = s
            binEnd[i] = (hi / hzPerBin).toInt().coerceIn(s, lastBin)
        }
    }

    private fun process(fft: ByteArray) {
        var bassSum = 0f; var bassMax = 0f; var bassN = 0
        var midSum = 0f; var midN = 0
        var highSum = 0f; var highN = 0
        var loudness = 0f

        for (i in 0 until bandCount) {
            var sum = 0f
            for (k in binStart[i]..binEnd[i]) {
                // Bin k: Realteil fft[2k], Imaginärteil fft[2k+1] (k = 0 enthält DC/Nyquist)
                sum += hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
            }
            val mag = sum / (binEnd[i] - binStart[i] + 1)
            val level = (ln(1f + mag) / LN_MAX_MAG).coerceIn(0f, 1f)
            loudness += level

            // Auto-Gain: langsam abfallender Peak pro Band → reagiert bei jeder Lautstärke ähnlich.
            peaks[i] = max(level, peaks[i] * PEAK_DECAY).coerceAtLeast(MIN_PEAK)
            val gate = ((level - NOISE_FLOOR) / NOISE_FLOOR).coerceIn(0f, 1f)
            val value = (level / peaks[i]).coerceIn(0f, 1f) * gate
            bands[i] = value

            when {
                bandCenters[i] < BASS_MAX_HZ -> { bassSum += value; bassMax = max(bassMax, value); bassN++ }
                bandCenters[i] < MID_MAX_HZ -> { midSum += value; midN++ }
                else -> { highSum += value; highN++ }
            }
        }

        // Bass aus Max + Mittel: Kick-Drums sollen "punchen", nicht verwaschen.
        bass = if (bassN > 0) 0.6f * bassMax + 0.4f * bassSum / bassN else 0f
        mid = if (midN > 0) midSum / midN else 0f
        high = if (highN > 0) highSum / highN else 0f

        if (loudness / bandCount > NOISE_FLOOR) lastSignalAt = SystemClock.uptimeMillis()
    }

    private companion object {
        const val MIN_FREQ = 30f
        const val MAX_FREQ = 14_000f
        const val BASS_MAX_HZ = 160f
        const val MID_MAX_HZ = 2_500f
        const val NOISE_FLOOR = 0.15f
        const val MIN_PEAK = 0.35f
        const val PEAK_DECAY = 0.996f // bei ~20 Captures/s ≈ −8 % pro Sekunde
        const val SIGNAL_TIMEOUT_MS = 600L
        const val RETRY_BACKOFF_MS = 10_000L
        val LN_MAX_MAG = ln(129f)
    }
}

/**
 * Standalone helper for use outside the launcher. NOTE: only stops when leaving the composition,
 * not on Activity.onStop — in the launcher, LauncherViewModel.audioAnalyzer is used instead.
 *
 * Liefert einen laufenden Analyzer, solange [active] true ist, oder null ohne RECORD_AUDIO.
 * Der Visualizer wird bei [active] = false bzw. beim Verlassen der Composition freigegeben.
 */
@Composable
fun rememberAudioSpectrumAnalyzer(active: Boolean): AudioSpectrumAnalyzer? {
    val context = LocalContext.current
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return null

    val analyzer = remember { AudioSpectrumAnalyzer() }
    DisposableEffect(analyzer, active) {
        if (active) analyzer.start()
        onDispose { analyzer.stop() }
    }
    return analyzer
}