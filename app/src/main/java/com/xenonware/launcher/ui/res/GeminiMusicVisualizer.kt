package com.xenonware.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenonware.launcher.media.AudioSpectrumAnalyzer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** What is drawn crisp on top of the blurred glow. */
enum class VisualizerOverlay {
    /** Glow only. */
    None,

    /** Floating Material shapes launched by bass kicks (the previous version). */
    Shapes,

    /** Grid of rounded squares that waves, zooms on the beat and lights up above the glow. */
    GridTiles,

    /** Same grid as a warped line mesh. */
    GridLines,
}

/** Sampled cyclically, so the colors keep flowing through each other. */
val GeminiColors = listOf(
    Color(0xFF1E7BFF), // blue
    Color(0xFF7C4DFF), // violet
    Color(0xFFFF3D8B), // pink
    Color(0xFFFFB300), // amber
    Color(0xFF00C2FF), // cyan
)

/**
 * Gemini-style music visualizer.
 *
 * GLOW (fully blurred, always on):
 *  - 4 colored light layers along the bottom, mixed additively; their colors rotate through
 *    the palette and each layer's bright spot swells with its own frequency group.
 *  - 11 light plumes, one per frequency range, rising out of the glow (never detached).
 *    Bass sits in the middle, higher frequencies alternate outwards to the edges, so it
 *    doesn't read as a left-to-right equalizer. Each plume is a spring: bass plumes are wide
 *    and bouncy, treble plumes narrow and quick.
 *  - Bass kicks add a soft white flash from the bottom center.
 *
 * OVERLAY (crisp, optional): see [VisualizerOverlay].
 *
 * Blur needs API 31+; below that the glow is soft (gradients) but not blurred.
 */
@Composable
fun GeminiMusicVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    colors: List<Color> = GeminiColors,
    overlay: VisualizerOverlay = VisualizerOverlay.GridTiles,
    bandThickness: Dp = 64.dp,
    blurRadius: Dp = 40.dp,
    gridSpacing: Dp = 26.dp,
    intensity: Float = 1f,
    /**
     * Owned by LauncherViewModel (started/stopped with foreground, page visibility and playback).
     * null → synthetic beat while [isPlaying].
     */
    analyzer: AudioSpectrumAnalyzer? = null,
) {
    require(colors.isNotEmpty()) { "colors must not be empty" }

    val engine = remember { GlowEngine() }
    val currentPlaying by rememberUpdatedState(isPlaying)
    val currentAnalyzer by rememberUpdatedState(analyzer)
    val currentIntensity by rememberUpdatedState(intensity)
    val currentOverlay by rememberUpdatedState(overlay)

    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 1f / 60f
                else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = now
                engine.step(
                    dt, currentAnalyzer, currentPlaying, currentIntensity,
                    shapesEnabled = currentOverlay == VisualizerOverlay.Shapes
                )
            }
        }
    }

    Box(modifier) {
        // 1) Glow — everything in this layer is heavily blurred
        Canvas(
            Modifier
                .matchParentSize()
                .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawGlow(engine, colors, bandThickness.toPx())
        }

        // 2) Crisp overlay
        when (overlay) {
            VisualizerOverlay.None -> Unit
            VisualizerOverlay.Shapes -> Canvas(Modifier.matchParentSize()) {
                drawShapes(engine, colors, bandThickness.toPx())
            }
            VisualizerOverlay.GridTiles, VisualizerOverlay.GridLines -> Canvas(Modifier.matchParentSize()) {
                drawGrid(
                    engine = engine,
                    spacingPx = gridSpacing.toPx(),
                    bandPx = bandThickness.toPx(),
                    tiles = overlay == VisualizerOverlay.GridTiles,
                    strokePx = 1.dp.toPx()
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawGlow(engine: GlowEngine, colors: List<Color>, bandPx: Float) {
    engine.observeFrame()
    val w = size.width
    val h = size.height
    val energy = engine.energy.coerceIn(0f, 1f)
    val rise = engine.maxRise(size, bandPx)

    // Ambient bloom from the bottom edge
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to samplePalette(colors, engine.colorPhase + 0.5f).copy(alpha = 0.3f + 0.45f * energy),
            startY = h * 0.2f,
            endY = h
        )
    )

    // Colored light layers, mixed additively
    val fadeTop = h - (bandPx * 1.6f + rise * 0.5f)
    for (i in 0 until LAYERS) {
        val c = samplePalette(colors, engine.colorPhase + i / LAYERS.toFloat())
        val a = engine.layerAlpha(i)
        drawPath(
            path = engine.layerPath(i, size, bandPx),
            brush = Brush.verticalGradient(
                0f to c.copy(alpha = 0f),
                0.5f to c.copy(alpha = 0.6f * a),
                1f to c.copy(alpha = a),
                startY = fadeTop,
                endY = h
            ),
            blendMode = BlendMode.Screen
        )
    }

    // Frequency plumes, always connected to the bottom
    for (j in 0 until PLUMES) {
        if (engine.plumeH[j] < 0.02f) continue
        val c = samplePalette(colors, engine.colorPhase + engine.plumeX[j] * 0.9f)
        val top = engine.plumeTop(j, size, bandPx)
        drawPath(
            path = engine.plumePath(j, size, bandPx),
            brush = Brush.verticalGradient(
                0f to c.copy(alpha = 0.75f),
                1f to lerp(c, Color.White, 0.15f),
                startY = top,
                endY = h
            ),
            blendMode = BlendMode.Screen
        )
    }

    // Beat flash from the bottom center
    if (engine.flash > 0.01f) {
        drawRect(
            brush = Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.35f * engine.flash),
                1f to Color.Transparent,
                center = Offset(w / 2f, h),
                radius = max(w, h) * 0.7f
            ),
            blendMode = BlendMode.Screen
        )
    }

    // Hot white core along the very bottom
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.White.copy(alpha = 0.15f + 0.3f * energy),
            startY = h - bandPx * 0.7f,
            endY = h
        ),
        blendMode = BlendMode.Screen
    )
}

private fun DrawScope.drawGrid(
    engine: GlowEngine,
    spacingPx: Float,
    bandPx: Float,
    tiles: Boolean,
    strokePx: Float,
) {
    engine.observeFrame()
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f || spacingPx <= 1f) return

    val rise = engine.maxRise(size, bandPx)
    val zoom = 1f + engine.zoom
    val pivotX = w / 2f
    val cols = (w / spacingPx).toInt() + 4
    val rows = (h / spacingPx).toInt() + 2
    val startX = pivotX - (cols - 1) / 2f * spacingPx
    val amp = spacingPx * (0.2f + 0.9f * engine.mid)
    val phase = engine.gridPhase

    val gx = engine.gridBuffer(rows * cols, 0)
    val gy = engine.gridBuffer(rows * cols, 1)
    val lit = engine.gridBuffer(rows * cols, 2)
    val fade = engine.gridBuffer(rows * cols, 3)
    val wave = engine.gridBuffer(rows * cols, 4)

    for (r in 0 until rows) {
        val baseY = h - r * spacingPx
        val heightAbove = h - baseY
        val vFade = 1f - smoothstep(h * 0.3f, h * 0.95f, heightAbove)
        for (c in 0 until cols) {
            val idx = r * cols + c
            val baseX = startX + c * spacingPx
            val xn = (baseX / w).coerceIn(0f, 1f)
            val field = engine.fieldAt(xn)

            val wv = sin(TWO_PI * (baseX / w * 1.4f) + phase - r * 0.45f)
            var x = baseX + amp * 0.35f * cos(TWO_PI * (baseY / h * 1.1f) + phase * 0.8f + c * 0.3f)
            // The glow pushes the grid up where a plume is high
            var y = baseY + amp * wv - field * rise * 0.18f * (1f - heightAbove / h).coerceIn(0f, 1f)

            // Zoom around the bottom center
            x = pivotX + (x - pivotX) * zoom
            y = h + (y - h) * zoom

            val plumeTop = bandPx * 0.6f + field * rise
            val l = if (heightAbove <= plumeTop) 1f else exp(-(heightAbove - plumeTop) / (spacingPx * 2.5f))

            gx[idx] = x
            gy[idx] = y
            lit[idx] = (l + engine.flash * 0.3f).coerceAtMost(1f)
            fade[idx] = vFade
            wave[idx] = wv
        }
    }

    if (tiles) {
        val tiltScale = 10f * engine.mid
        for (idx in 0 until rows * cols) {
            val alpha = fade[idx] * (0.14f + 0.6f * lit[idx])
            if (alpha < 0.01f) continue
            val s = spacingPx * zoom * (0.34f + 0.3f * lit[idx] + 0.12f * engine.bass)
            val half = s / 2f
            val center = Offset(gx[idx], gy[idx])
            val topLeft = Offset(center.x - half, center.y - half)
            val corner = CornerRadius(s * 0.28f)
            withTransform({ rotate(wave[idx] * tiltScale, pivot = center) }) {
                if (lit[idx] > 0.3f) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = topLeft,
                        size = Size(s, s),
                        cornerRadius = corner,
                        alpha = alpha * 0.25f * lit[idx]
                    )
                }
                drawRoundRect(
                    color = Color.White,
                    topLeft = topLeft,
                    size = Size(s, s),
                    cornerRadius = corner,
                    alpha = alpha,
                    style = Stroke(width = strokePx)
                )
            }
        }
    } else {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                if (c + 1 < cols) {
                    val n = idx + 1
                    val a = (fade[idx] + fade[n]) * 0.5f * (0.1f + 0.5f * (lit[idx] + lit[n]) * 0.5f)
                    if (a > 0.01f) {
                        drawLine(Color.White, Offset(gx[idx], gy[idx]), Offset(gx[n], gy[n]), strokePx, alpha = a)
                    }
                }
                if (r + 1 < rows) {
                    val n = idx + cols
                    val a = (fade[idx] + fade[n]) * 0.5f * (0.1f + 0.5f * (lit[idx] + lit[n]) * 0.5f)
                    if (a > 0.01f) {
                        drawLine(Color.White, Offset(gx[idx], gy[idx]), Offset(gx[n], gy[n]), strokePx, alpha = a)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawShapes(engine: GlowEngine, colors: List<Color>, bandPx: Float) {
    val strokePx = 1.dp.toPx()
    engine.forEachShape(size, bandPx) { path, left, top, extent, alpha, colorIdx ->
        val c1 = colors[colorIdx % colors.size]
        val c2 = colors[(colorIdx + 1) % colors.size]
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                listOf(lerp(c1, Color.White, 0.3f), lerp(c2, Color.White, 0.1f)),
                start = Offset(left, top),
                end = Offset(left + extent, top + extent)
            ),
            alpha = alpha * 0.92f
        )
        drawPath(path, Color.White.copy(alpha = 0.4f * alpha), style = Stroke(width = strokePx))
    }
}

// ---------------------------------------------------------------------------------------------
// Engine
// ---------------------------------------------------------------------------------------------

private const val LAYERS = 4
private const val PLUMES = 11
private const val PLUME_STEP = 0.083f
private const val PLUME_POINTS = 24
private const val SEGMENTS = 48
private const val SHAPE_POINTS = 72
private const val MAX_SHAPES = 16
private const val TWO_PI = (2 * PI).toFloat()
private const val DEG = (PI / 180).toFloat()
private const val KICK_IMPULSE = 7f
private const val LIVE_GRACE_SECONDS = 3f
private const val AMBIENT_INTERVAL = 1.4f

private val LAYER_BASE = floatArrayOf(0.55f, 0.75f, 0.6f, 0.7f)
private val LAYER_FREQ = floatArrayOf(1.2f, 1.9f, 1.5f, 2.6f)
private val LAYER_SPEED = floatArrayOf(0.35f, -0.45f, 0.4f, -0.3f)
private val HUMP_BASE = floatArrayOf(0.15f, 0.42f, 0.68f, 0.9f)
private val HUMP_SPEED = floatArrayOf(0.11f, 0.08f, 0.13f, 0.09f)

private const val KIND_SQUIRCLE = 0
private const val KIND_COOKIE = 1
private const val KIND_CLOVER = 2
private const val KIND_TRIANGLE = 3

/** Plume 0 (sub-bass) in the middle, then alternating left/right towards the edges. */
private fun plumeAnchor(j: Int): Float {
    if (j == 0) return 0.5f
    val k = (j + 1) / 2
    val side = if (j % 2 == 1) -1f else 1f
    return 0.5f + side * k * PLUME_STEP
}

private class FloatingShape {
    var alive = false
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var rotation = 0f
    var spin = 0f
    var size = 1f
    var age = 0f
    var life = 1f
    var kindA = 0
    var kindB = 0
    var colorIdx = 0
    val path = Path()
}

private class GlowEngine {

    val frame = mutableLongStateOf(0L)

    /** Read in the draw phase → the Canvas redraws every frame without recomposition. */
    fun observeFrame(): Long = frame.longValue

    // Plumes (one per frequency range)
    val plumeX = FloatArray(PLUMES) { plumeAnchor(it) }
    val plumeH = FloatArray(PLUMES)
    private val plumeV = FloatArray(PLUMES)
    private val plumeLevel = FloatArray(PLUMES)
    private val rawLevel = FloatArray(PLUMES)
    private val plumeSigma = FloatArray(PLUMES) { 0.085f - 0.0035f * it }   // bass wide, treble narrow
    private val plumeGain = FloatArray(PLUMES) { 1f - 0.03f * it }
    private val plumeK = FloatArray(PLUMES) { 110f + 22f * it }              // treble reacts faster
    private val plumeD = FloatArray(PLUMES) { 2f * 0.32f * sqrt(110f + 22f * it) } // underdamped → elastic
    private val field = FloatArray(SEGMENTS + 1)

    var bass = 0f; private set
    var mid = 0f; private set
    var high = 0f; private set
    var energy = 0f; private set
    var zoom = 0f; private set
    var flash = 0f; private set
    var colorPhase = 0f; private set
    var gridPhase = 0f; private set

    private var zoomV = 0f
    private var bassAvg = 0f
    private var time = 0f
    private var lastLiveAt = 0f
    private var kickArmed = true
    private var kickCount = 0
    private var ambientTimer = 0f
    private val group = FloatArray(LAYERS)
    private val phase = FloatArray(LAYERS)
    private val hump = FloatArray(LAYERS) { HUMP_BASE[it] }
    private val humpKick = FloatArray(LAYERS)
    private val rng = Random(7)

    val shapes = Array(MAX_SHAPES) { FloatingShape() }

    private val layerPaths = Array(LAYERS) { Path() }
    private val plumePaths = Array(PLUMES) { Path() }
    private val gridBuffers = Array(5) { FloatArray(0) }

    fun step(dt: Float, analyzer: AudioSpectrumAnalyzer?, isPlaying: Boolean, intensity: Float, shapesEnabled: Boolean) {
        time += dt

        if (analyzer?.hasFreshSignal() == true) lastLiveAt = time
        val live = analyzer?.takeIf {
            it.isRunning && (it.hasFreshSignal() || !isPlaying || time - lastLiveAt < LIVE_GRACE_SECONDS)
        }

        // 1) Raw levels per plume
        val rawBass: Float
        if (live != null) {
            val bands = live.bands
            val n = bands.size
            for (j in 0 until PLUMES) {
                val lo = j * n / PLUMES
                val hi = max(lo + 1, (j + 1) * n / PLUMES)
                var sum = 0f
                var mx = 0f
                for (k in lo until hi) {
                    sum += bands[k]
                    mx = max(mx, bands[k])
                }
                rawLevel[j] = 0.6f * mx + 0.4f * sum / (hi - lo)
            }
            rawBass = live.bass
        } else if (isPlaying) {
            val kick = exp(-((time * 2f) % 1f) * 6f)   // synthetic 120 BPM
            val hat = exp(-((time * 4f) % 1f) * 10f)
            for (j in 0 until PLUMES) {
                rawLevel[j] = if (j < 3) {
                    0.15f + 0.75f * kick * (1f - 0.2f * j)
                } else {
                    val t = j / (PLUMES - 1f)
                    val base = 0.3f + 0.25f * sin(time * (1.1f + 0.31f * j) + j * 1.9f) +
                            0.2f * sin(time * (0.37f + 0.13f * j) + j)
                    base.coerceIn(0f, 1f) * (0.7f + 0.3f * t * hat)
                }
            }
            rawBass = (rawLevel[0] + rawLevel[1] + rawLevel[2]) / 3f
        } else {
            rawLevel.fill(0.03f)
            rawBass = 0f
        }

        // 2) Smoothing & groups
        for (j in 0 until PLUMES) {
            val target = (rawLevel[j] * intensity).coerceIn(0f, 1.2f)
            plumeLevel[j] = follow(plumeLevel[j], target, 0.03f, if (j < 3) 0.2f else 0.13f, dt)
        }
        bass = avg(0, 2)
        mid = avg(3, 6)
        high = avg(7, 10)
        group[0] = bass
        group[1] = avg(3, 4)
        group[2] = avg(5, 7)
        group[3] = avg(8, 10)
        energy = follow(energy, (bass + mid + high) / 3f, 0.1f, 0.6f, dt)

        // 3) Kick detection (one kick = one impulse)
        val b = (rawBass * intensity).coerceIn(0f, 1.2f)
        bassAvg = follow(bassAvg, b, 0.9f, 0.9f, dt)
        val onset = b - bassAvg * 1.1f
        if (kickArmed && onset > 0.14f) {
            kick(onset * (0.5f + bass), shapesEnabled)
            kickArmed = false
        } else if (onset < 0.05f) {
            kickArmed = true
        }

        // 4) Springs (sub-stepped)
        val zoomTarget = 0.06f * bass
        var remaining = dt
        while (remaining > 0f) {
            val h = min(remaining, 1f / 240f)
            remaining -= h
            for (j in 0 until PLUMES) {
                val target = plumeLevel[j].pow(1.25f) * plumeGain[j] * 0.9f
                val a = plumeK[j] * (target - plumeH[j]) - plumeD[j] * plumeV[j]
                plumeV[j] += a * h
                plumeH[j] += plumeV[j] * h
                if (plumeH[j] < -0.05f) {
                    plumeH[j] = -0.05f
                    if (plumeV[j] < 0f) plumeV[j] *= -0.3f
                }
            }
            val za = 160f * (zoomTarget - zoom) - 9f * zoomV
            zoomV += za * h
            zoom += zoomV * h
        }
        flash *= exp(-dt * 4f)

        // 5) Positions & field
        for (j in 0 until PLUMES) {
            plumeX[j] = plumeAnchor(j) + 0.02f * sin(time * (0.17f + 0.03f * j) + j * 2.1f)
        }
        for (s in 0..SEGMENTS) {
            val xn = s / SEGMENTS.toFloat()
            var f = 0f
            for (j in 0 until PLUMES) {
                val d = (xn - plumeX[j]) / plumeSigma[j]
                f += plumeH[j].coerceAtLeast(0f) * exp(-d * d)
            }
            field[s] = f
        }

        // 6) Background motion
        for (i in 0 until LAYERS) {
            phase[i] += dt * LAYER_SPEED[i] * (0.6f + 1.8f * mid)
            humpKick[i] *= exp(-dt * 1.5f)
            hump[i] = HUMP_BASE[i] + 0.18f * sin(time * HUMP_SPEED[i] * TWO_PI + i * 1.7f) + humpKick[i]
        }
        colorPhase = (colorPhase + dt * (0.025f + 0.12f * energy)) % 1f
        gridPhase += dt * (0.8f + 2.2f * mid)

        if (shapesEnabled) updateShapes(dt, isPlaying)

        frame.longValue++
    }

    private fun avg(from: Int, to: Int): Float {
        var s = 0f
        for (j in from..to) s += plumeLevel[j]
        return s / (to - from + 1)
    }

    private fun kick(strength: Float, shapesEnabled: Boolean) {
        kickCount++
        for (j in 0..2) plumeV[j] += strength * KICK_IMPULSE * (1f - 0.2f * j)
        zoomV += strength * 0.9f
        flash = (flash + strength * 1.2f).coerceAtMost(1f)
        for (i in 0 until LAYERS) humpKick[i] += (hash(kickCount * 5 + i) - 0.5f) * 0.12f * strength
        if (shapesEnabled) {
            val launches = if (strength > 0.3f) 2 else 1
            repeat(launches) { n ->
                val j = n % 3
                spawnShape(
                    x = plumeX[j] + (rng.nextFloat() - 0.5f) * 0.06f,
                    vy = 0.8f + strength * 2f,
                    size = 0.6f + rng.nextFloat() * 0.45f,
                    life = 2.4f + rng.nextFloat() * 1.2f,
                    colorIdx = rng.nextInt(LAYERS),
                )
            }
        }
    }

    fun maxRise(size: Size, bandPx: Float) = (size.height - bandPx) * 0.72f

    /** 0..~1.3: combined plume height at xn (0..1), used by the layers and the grid. */
    fun fieldAt(xn: Float): Float {
        val pos = xn.coerceIn(0f, 1f) * SEGMENTS
        val i = floor(pos).toInt().coerceAtMost(SEGMENTS - 1)
        val t = pos - i
        return (field[i] + (field[i + 1] - field[i]) * t).coerceAtMost(1.3f)
    }

    fun layerAlpha(i: Int): Float {
        val shimmer = 0.18f * high * sin(time * (5f + 1.3f * i) + i)
        val level = 0.75f + 0.25f * (energy * 1.6f).coerceIn(0f, 1f) + 0.15f * bass
        return ((0.9f + shimmer) * level).coerceIn(0f, 1f)
    }

    fun layerPath(i: Int, size: Size, bandPx: Float): Path {
        val w = size.width
        val h = size.height
        val rise = maxRise(size, bandPx)
        val path = layerPaths[i]
        path.reset()
        path.moveTo(0f, h)
        for (s in 0..SEGMENTS) {
            val xn = s / SEGMENTS.toFloat()
            val dh = (xn - hump[i]) / 0.28f
            var lift = bandPx * LAYER_BASE[i] * (0.85f + 0.3f * energy)
            lift += bandPx * (0.4f + 1.3f * group[i]) * exp(-dh * dh)
            lift += bandPx * (0.15f + 0.5f * mid) * sin(TWO_PI * LAYER_FREQ[i] * xn + phase[i])
            lift += bandPx * 0.35f * high * sin(TWO_PI * LAYER_FREQ[i] * 2.3f * xn - phase[i] * 1.7f)
            lift += bandPx * 0.06f * sin(time * 0.9f + xn * 4f + i)
            lift += field[s] * rise * 0.3f
            path.lineTo(xn * w, h - lift)
        }
        path.lineTo(w, h)
        path.close()
        return path
    }

    fun plumeTop(j: Int, size: Size, bandPx: Float): Float {
        val rise = maxRise(size, bandPx)
        val peak = plumeH[j].coerceAtLeast(0f) * rise + bandPx * 0.3f
        return (size.height - bandPx * 0.5f - peak).coerceAtLeast(0f)
    }

    fun plumePath(j: Int, size: Size, bandPx: Float): Path {
        val w = size.width
        val h = size.height
        val rise = maxRise(size, bandPx)
        val hj = plumeH[j].coerceAtLeast(0f)
        val sig = plumeSigma[j] * w * (1f + 0.3f * hj)
        val cx = plumeX[j] * w
        val peak = hj * rise + bandPx * 0.3f
        val base = h - bandPx * 0.5f
        val path = plumePaths[j]
        path.reset()
        path.moveTo(cx - 3f * sig, h)
        for (k in 0..PLUME_POINTS) {
            val u = k / PLUME_POINTS.toFloat() * 6f - 3f
            path.lineTo(cx + u * sig, (base - peak * exp(-u * u)).coerceAtLeast(0f))
        }
        path.lineTo(cx + 3f * sig, h)
        path.close()
        return path
    }

    /** Reusable scratch arrays for the grid (no per-frame allocation). */
    fun gridBuffer(n: Int, slot: Int): FloatArray {
        if (gridBuffers[slot].size < n) gridBuffers[slot] = FloatArray(n)
        return gridBuffers[slot]
    }

    // ---- Floating shapes (overlay = Shapes) ----

    private fun updateShapes(dt: Float, isPlaying: Boolean) {
        for (s in shapes) {
            if (!s.alive) continue
            s.age += dt
            if (s.age >= s.life) {
                s.alive = false
                continue
            }
            s.vy = s.vy * exp(-dt * 1.4f) + 0.06f * dt
            s.y += s.vy * dt
            s.x += s.vx * dt
            s.rotation += s.spin * (1f + bass) * dt
        }

        ambientTimer += dt * (if (isPlaying) 1f else 0.2f)
        if (ambientTimer > AMBIENT_INTERVAL) {
            ambientTimer = 0f
            spawnShape(
                x = 0.08f + rng.nextFloat() * 0.84f,
                vy = 0.25f + rng.nextFloat() * 0.2f,
                size = 0.35f + rng.nextFloat() * 0.25f,
                life = 3f + rng.nextFloat() * 1.5f,
                colorIdx = rng.nextInt(LAYERS),
            )
        }
    }

    private fun spawnShape(x: Float, vy: Float, size: Float, life: Float, colorIdx: Int) {
        val s = shapes.firstOrNull { !it.alive } ?: shapes.maxBy { it.age / it.life }
        s.alive = true
        s.x = x.coerceIn(0.04f, 0.96f)
        s.y = 0.05f
        s.vx = (rng.nextFloat() - 0.5f) * 0.04f
        s.vy = vy
        s.rotation = rng.nextFloat() * 360f
        s.spin = (30f + rng.nextFloat() * 90f) * (if (rng.nextBoolean()) 1f else -1f)
        s.size = size
        s.age = 0f
        s.life = life
        s.kindA = pickKind()
        s.kindB = pickKind().let { if (it == s.kindA) (it + 1) % 4 else it }
        s.colorIdx = colorIdx
    }

    private fun pickKind(): Int {
        val r = rng.nextFloat()
        return when {
            r < 0.45f -> KIND_SQUIRCLE
            r < 0.65f -> KIND_COOKIE
            r < 0.85f -> KIND_CLOVER
            else -> KIND_TRIANGLE
        }
    }

    inline fun forEachShape(
        size: Size,
        bandPx: Float,
        draw: (path: Path, left: Float, top: Float, extent: Float, alpha: Float, colorIdx: Int) -> Unit,
    ) {
        observeFrame()
        val baseY = size.height - bandPx
        val rise = maxRise(size, bandPx)
        val ref = min(size.width * 0.07f, (size.height - bandPx) * 0.2f)
        for (s in shapes) {
            if (!s.alive) continue
            val t = s.age / s.life
            val pop = easeOutBack((s.age / 0.45f).coerceIn(0f, 1f))
            val shrink = if (t > 0.75f) 1f - (t - 0.75f) / 0.25f * 0.4f else 1f
            val r = ref * s.size * pop * shrink * (1f + 0.18f * bass)
            if (r <= 0.5f) continue
            val alpha = min(
                (s.age / 0.2f).coerceIn(0f, 1f),
                ((s.life - s.age) / (s.life * 0.4f)).coerceIn(0f, 1f)
            )
            val cx = s.x * size.width
            val cy = baseY - s.y * rise
            buildShapePath(s, cx, cy, r)
            draw(s.path, cx - r * 1.25f, cy - r * 1.25f, r * 2.5f, alpha, s.colorIdx)
        }
    }

    fun buildShapePath(s: FloatingShape, cx: Float, cy: Float, r: Float) {
        val morph = smoothstep(0.25f, 0.85f, s.age / s.life)
        val rot = s.rotation * DEG
        val path = s.path
        path.reset()
        for (i in 0 until SHAPE_POINTS) {
            val theta = i * TWO_PI / SHAPE_POINTS
            val rr = r * (shapeRadius(s.kindA, theta) * (1f - morph) + shapeRadius(s.kindB, theta) * morph)
            val phi = theta + rot
            val px = cx + rr * cos(phi)
            val py = cy + rr * sin(phi)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
    }
}

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

/** Cyclic palette lookup: t wraps around, neighbours are blended. */
private fun samplePalette(colors: List<Color>, t: Float): Color {
    val n = colors.size
    if (n == 1) return colors[0]
    val wrapped = t - floor(t)
    val pos = wrapped * n
    val i = pos.toInt() % n
    return lerp(colors[i], colors[(i + 1) % n], pos - floor(pos))
}

/** Polar radius functions: any two shapes morph by blending their radii per angle. */
private fun shapeRadius(kind: Int, theta: Float): Float = when (kind) {
    KIND_SQUIRCLE -> {
        val c = abs(cos(theta))
        val s = abs(sin(theta))
        0.92f / (c.pow(4f) + s.pow(4f)).pow(0.25f)
    }
    KIND_COOKIE -> 0.95f * (1f + 0.07f * cos(9f * theta))
    KIND_CLOVER -> 0.9f * (1f + 0.18f * cos(4f * theta))
    else -> 0.95f * (1f + 0.13f * cos(3f * theta))
}

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun follow(current: Float, target: Float, attack: Float, release: Float, dt: Float): Float {
    val tau = if (target > current) attack else release
    return current + (target - current) * (1f - exp(-dt / tau))
}

private fun hash(n: Int): Float {
    var x = n * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFF) / 65535f
}