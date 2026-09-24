package com.xenonware.launcher.util

import android.media.audiofx.Visualizer
import android.os.SystemClock
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Reads the FFT spectrum and the waveform of the global audio output (session 0).
 *
 * - [bands]: [bandCount] logarithmic bands, 0..1, low → high, auto-gained per band.
 *   Bands narrower than one FFT bin (the lowest ones) are interpolated between bins, so
 *   neighboring bass bands still differ instead of repeating the same value.
 * - [bass] / [mid] / [high]: group values 0..1.
 * - [volume]: overall loudness (RMS of the waveform), 0..1, auto-gained.
 * - [waveform]: 256 samples of the current audio waveform, -1..1 (for oscilloscope styles).
 *
 * Needs RECORD_AUDIO (runtime) + MODIFY_AUDIO_SETTINGS (manifest). No microphone is used.
 * Written from the Visualizer thread, read from the UI frame loop; an occasional torn frame
 * doesn't matter for an animation, so there is no locking.
 */
class AudioSpectrumAnalyzer(val bandCount: Int = 48) {

    val bands = FloatArray(bandCount)
    val waveform = FloatArray(WAVEFORM_SIZE)

    @Volatile var bass = 0f; private set
    @Volatile var mid = 0f; private set
    @Volatile var high = 0f; private set
    @Volatile var volume = 0f; private set
    private var volumePeak = MIN_VOLUME_PEAK

    @Volatile private var lastSignalAt = 0L
    private var visualizer: Visualizer? = null

    /** start() is called from a 1 s poll loop: don't hammer a device that can't create a Visualizer. */
    private var lastStartFailureAt = 0L

    private val peaks = FloatArray(bandCount) { MIN_PEAK }
    private val bandCenters = FloatArray(bandCount)
    private val binStart = IntArray(bandCount)
    private val binEnd = IntArray(bandCount)
    private val centerBin = FloatArray(bandCount)
    private val narrow = BooleanArray(bandCount)
    private var lastBin = 1

    val isRunning: Boolean get() = visualizer != null

    /** Center frequency of band [i] in Hz (valid once started). */
    fun bandCenterHz(i: Int): Float = bandCenters[i]

    fun hasFreshSignal(): Boolean =
        SystemClock.uptimeMillis() - lastSignalAt < SIGNAL_TIMEOUT_MS

    @Synchronized
    fun start(): Boolean {
        if (visualizer != null) return true
        if (lastStartFailureAt != 0L &&
            SystemClock.uptimeMillis() - lastStartFailureAt < RETRY_BACKOFF_MS
        ) return false
        var v: Visualizer? = null
        return try {
            v = Visualizer(0)
            v.enabled = false
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            computeBins(v.captureSize, v.samplingRate / 1000f) // samplingRate is in mHz
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        if (waveform != null) processWaveform(waveform)
                    }

                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) processFft(fft)
                    }
                },
                Visualizer.getMaxCaptureRate(),
                true,
                true
            )
            v.enabled = true
            visualizer = v
            lastStartFailureAt = 0L
            true
        } catch (_: Throwable) {
            lastStartFailureAt = SystemClock.uptimeMillis()
            runCatching { v?.release() }
            false
        }
    }

    @Synchronized
    fun stop() {
        visualizer?.let {
            runCatching { it.setEnabled(false) }
            runCatching { it.release() }
        }
        visualizer = null
        bands.fill(0f)
        waveform.fill(0f)
        bass = 0f; mid = 0f; high = 0f; volume = 0f
    }

    private fun computeBins(captureSize: Int, sampleRateHz: Float) {
        lastBin = captureSize / 2 - 1
        val hzPerBin = sampleRateHz / captureSize
        val fMax = min(MAX_FREQ, sampleRateHz / 2f)
        val ratio = fMax / MIN_FREQ
        for (i in 0 until bandCount) {
            val lo = MIN_FREQ * ratio.pow(i / bandCount.toFloat())
            val hi = MIN_FREQ * ratio.pow((i + 1) / bandCount.toFloat())
            val center = sqrt(lo * hi)
            bandCenters[i] = center
            centerBin[i] = (center / hzPerBin).coerceIn(1f, lastBin.toFloat())
            narrow[i] = (hi - lo) / hzPerBin < 1.5f
            val s = (lo / hzPerBin).toInt().coerceIn(1, lastBin)
            binStart[i] = s
            binEnd[i] = (hi / hzPerBin).toInt().coerceIn(s, lastBin)
        }
    }

    private fun magnitude(fft: ByteArray, k: Int): Float =
        hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())

    private fun processFft(fft: ByteArray) {
        var bassSum = 0f; var bassMax = 0f; var bassN = 0
        var midSum = 0f; var midN = 0
        var highSum = 0f; var highN = 0
        var loudness = 0f
        val maxBin = min(lastBin, fft.size / 2 - 1)

        for (i in 0 until bandCount) {
            val mag = if (narrow[i]) {
                // Narrower than a bin: interpolate the magnitude at the band's center frequency
                val f = centerBin[i].coerceAtMost(maxBin - 1f)
                val k = floor(f).toInt()
                val t = f - k
                magnitude(fft, k) * (1f - t) + magnitude(fft, k + 1) * t
            } else {
                var sum = 0f
                val end = min(binEnd[i], maxBin)
                for (k in binStart[i]..end) sum += magnitude(fft, k)
                sum / (end - binStart[i] + 1).coerceAtLeast(1)
            }
            val level = (ln(1f + mag) / LN_MAX_MAG).coerceIn(0f, 1f)
            loudness += level

            // Auto-gain: slowly decaying peak per band → similar reaction at any volume
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

        bass = if (bassN > 0) 0.6f * bassMax + 0.4f * bassSum / bassN else 0f
        mid = if (midN > 0) midSum / midN else 0f
        high = if (highN > 0) highSum / highN else 0f

        if (loudness / bandCount > NOISE_FLOOR) lastSignalAt = SystemClock.uptimeMillis()
    }

    /** Downsampled waveform + loudness (RMS), auto-gained like the bands. */
    private fun processWaveform(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        var sum = 0f
        for (b in bytes) {
            val x = ((b.toInt() and 0xFF) - 128) / 128f
            sum += x * x
        }
        val step = bytes.size.toFloat() / WAVEFORM_SIZE
        for (i in 0 until WAVEFORM_SIZE) {
            val from = (i * step).toInt()
            val to = max(from + 1, ((i + 1) * step).toInt()).coerceAtMost(bytes.size)
            var acc = 0f
            for (k in from until to) acc += ((bytes[k].toInt() and 0xFF) - 128) / 128f
            waveform[i] = acc / (to - from)
        }
        val rms = sqrt(sum / bytes.size)
        volumePeak = max(rms, volumePeak * PEAK_DECAY).coerceAtLeast(MIN_VOLUME_PEAK)
        val gate = ((rms - VOLUME_FLOOR) / VOLUME_FLOOR).coerceIn(0f, 1f)
        volume = (rms / volumePeak).coerceIn(0f, 1f) * gate
    }

    private companion object {
        const val WAVEFORM_SIZE = 256
        const val MIN_FREQ = 30f
        const val MAX_FREQ = 14_000f
        const val BASS_MAX_HZ = 160f
        const val MID_MAX_HZ = 2_500f
        const val NOISE_FLOOR = 0.15f
        const val MIN_PEAK = 0.35f
        const val PEAK_DECAY = 0.996f // at ~20 captures/s ≈ −8 % per second
        const val VOLUME_FLOOR = 0.01f
        const val MIN_VOLUME_PEAK = 0.08f
        const val SIGNAL_TIMEOUT_MS = 600L
        const val RETRY_BACKOFF_MS = 10_000L
        val LN_MAX_MAG = ln(129f)
    }
}