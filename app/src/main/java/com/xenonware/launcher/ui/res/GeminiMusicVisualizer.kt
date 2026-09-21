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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

val GeminiColors = listOf(
    Color(0xFF2E96FF), // blue
    Color(0xFF8A6CFF), // violet
    Color(0xFFFF6F91), // pink
    Color(0xFFFFC24B), // warm yellow
)

/**
 * Gemini-style music visualizer, two layers:
 *
 * 1. GLOW (fully blurred): four colored light layers at the bottom edge, mixed additively
 *    (Screen). Each has a slowly drifting bright "hump" so the colors wander and blend.
 *    Mids drive broad undulations, highs make the layers shimmer, bass lifts soft light orbs
 *    that shoot up elastically (spring physics) and sink back.
 *
 * 2. SHAPES (crisp, on top): Material-3-Expressive-style geometric shapes (mostly rounded
 *    squares, plus cookie / clover / soft triangle). Strong bass kicks launch them out of the
 *    glow; they pop in with overshoot, float up, rotate, morph into another shape and fade.
 *    A few small ambient shapes drift up during playback even without strong bass.
 *
 * Blur needs API 31+. Below that, the glow is still soft (gradients) but not blurred.
 */
@Composable
fun GeminiMusicVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    colors: List<Color> = GeminiColors,
    bandThickness: Dp = 64.dp,
    blurRadius: Dp = 40.dp,
    intensity: Float = 1f,
    showShapes: Boolean = true,
    /**
     * Owned by LauncherViewModel (started/stopped with foreground, page visibility and playback).
     * null → synthetic pulse while [isPlaying].
     */
    analyzer: AudioSpectrumAnalyzer? = null,
) {
    require(colors.isNotEmpty()) { "colors must not be empty" }

    val engine = remember { GlowEngine() }
    val currentPlaying by rememberUpdatedState(isPlaying)
    val currentAnalyzer by rememberUpdatedState(analyzer)
    val currentIntensity by rememberUpdatedState(intensity)
    val currentShowShapes by rememberUpdatedState(showShapes)

    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 1f / 60f
                else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = now
                engine.step(dt, currentAnalyzer, currentPlaying, currentIntensity, currentShowShapes)
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

        // 2) Geometric shapes — crisp, floating over the glow
        if (showShapes) {
            Canvas(Modifier.matchParentSize()) {
                drawShapes(engine, colors, bandThickness.toPx())
            }
        }
    }
}

private fun DrawScope.drawGlow(engine: GlowEngine, colors: List<Color>, bandPx: Float) {
    val h = size.height
    val energy = engine.energy.coerceIn(0f, 1f)

    // Ambient bloom rising from the bottom edge
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to colors[0].copy(alpha = 0.2f + 0.35f * energy),
            startY = h * 0.25f,
            endY = h
        )
    )

    // Colored light layers, mixed additively
    val fadeTop = h - (bandPx * 1.4f + engine.maxRise(size, bandPx) * 0.55f)
    for (i in 0 until LAYERS) {
        val c = colors[i % colors.size]
        val a = engine.layerAlpha(i)
        drawPath(
            path = engine.layerPath(i, size, bandPx),
            brush = Brush.verticalGradient(
                0f to c.copy(alpha = 0f),
                0.55f to c.copy(alpha = 0.55f * a),
                1f to c.copy(alpha = a),
                startY = fadeTop,
                endY = h
            ),
            blendMode = BlendMode.Screen
        )
    }

    // Bass orbs: soft balls of light that shoot up and sink back
    engine.forEachOrb(size, bandPx) { cx, cy, r, sx, sy, colorIdx ->
        val c = colors[colorIdx % colors.size]
        val center = Offset(cx, cy)
        withTransform({ scale(sx, sy, pivot = center) }) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to c.copy(alpha = 0.95f),
                    0.55f to c.copy(alpha = 0.55f),
                    1f to c.copy(alpha = 0f),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center,
                blendMode = BlendMode.Screen
            )
        }
    }

    // Hot white core along the very bottom
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            1f to Color.White.copy(alpha = 0.18f + 0.3f * energy),
            startY = h - bandPx * 0.7f,
            endY = h
        ),
        blendMode = BlendMode.Screen
    )
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

private const val LAYERS = 4
private const val SEGMENTS = 48          // enough: the glow is blurred anyway
private const val SHAPE_POINTS = 72
private const val MAX_SHAPES = 16
private const val TWO_PI = (2 * PI).toFloat()
private const val DEG = (PI / 180).toFloat()
private const val KICK_IMPULSE = 9f
private const val LIVE_GRACE_SECONDS = 3f
private const val AMBIENT_INTERVAL = 1.4f

private val LAYER_BASE = floatArrayOf(0.55f, 0.75f, 0.6f, 0.7f)   // × band thickness
private val LAYER_FREQ = floatArrayOf(1.2f, 1.9f, 1.5f, 2.6f)     // waves per width
private val LAYER_SPEED = floatArrayOf(0.35f, -0.45f, 0.4f, -0.3f)
private val HUMP_BASE = floatArrayOf(0.15f, 0.42f, 0.68f, 0.9f)   // where each color is brightest
private val HUMP_SPEED = floatArrayOf(0.11f, 0.08f, 0.13f, 0.09f)

private const val KIND_SQUIRCLE = 0
private const val KIND_COOKIE = 1
private const val KIND_CLOVER = 2
private const val KIND_TRIANGLE = 3

private class Orb(
    val anchor: Float, val gain: Float, val size: Float,
    val stiffness: Float, val damping: Float, val seed: Float, val colorIdx: Int,
) {
    var x = anchor
    var h = 0f
    var v = 0f
}

private class FloatingShape {
    var alive = false
    var x = 0f          // 0..1 of width
    var y = 0f          // height above the band, in fractions of the max rise
    var vx = 0f
    var vy = 0f
    var rotation = 0f   // degrees
    var spin = 0f       // degrees / s
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

    val orbs = arrayOf(
        Orb(0.12f, 0.75f, 0.85f, 150f, 7.5f, 0.13f, 0),
        Orb(0.31f, 1.00f, 1.05f, 185f, 8.5f, 0.57f, 1),
        Orb(0.50f, 0.90f, 1.20f, 130f, 6.5f, 0.91f, 2),
        Orb(0.69f, 1.00f, 1.00f, 170f, 8.0f, 0.34f, 3),
        Orb(0.88f, 0.70f, 0.80f, 200f, 9.0f, 0.72f, 0),
    )
    val shapes = Array(MAX_SHAPES) { FloatingShape() }

    var bass = 0f; private set
    var energy = 0f; private set
    private var bassAvg = 0f
    private var mid = 0f
    private var high = 0f
    private var time = 0f
    private var lastLiveAt = 0f
    private var kickArmed = true
    private var kickCount = 0
    private var ambientTimer = 0f
    private val phase = FloatArray(LAYERS)
    private val hump = FloatArray(LAYERS) { HUMP_BASE[it] }
    private val rng = Random(7)

    private val paths = Array(LAYERS) { Path() }
    private val pathFrame = LongArray(LAYERS) { -1L }
    private var pathW = 0f
    private var pathH = 0f
    private var pathBand = 0f

    /** Read in the draw phase → the Canvas redraws every frame without recomposition. */
    fun observeFrame(): Long = frame.longValue

    fun step(dt: Float, analyzer: AudioSpectrumAnalyzer?, isPlaying: Boolean, intensity: Float, shapesEnabled: Boolean) {
        time += dt

        if (analyzer?.hasFreshSignal() == true) lastLiveAt = time
        val live = analyzer?.takeIf {
            it.isRunning && (it.hasFreshSignal() || !isPlaying || time - lastLiveAt < LIVE_GRACE_SECONDS)
        }

        val rawBass: Float
        val rawMid: Float
        val rawHigh: Float
        if (live != null) {
            rawBass = live.bass
            rawMid = live.mid
            rawHigh = live.high
        } else if (isPlaying) {
            val beat = (time * 2f) % 1f // synthetic 120 BPM
            rawBass = 0.2f + 0.7f * exp(-beat * 6f)
            rawMid = 0.45f + 0.2f * sin(time * 1.3f)
            rawHigh = 0.35f + 0.2f * sin(time * 2.7f + 1f)
        } else {
            rawBass = 0f
            rawMid = 0.08f
            rawHigh = 0.05f
        }

        val b = (rawBass * intensity).coerceIn(0f, 1.2f)
        bass = follow(bass, b, 0.025f, 0.2f, dt)
        bassAvg = follow(bassAvg, b, 0.9f, 0.9f, dt)
        mid = follow(mid, (rawMid * intensity).coerceIn(0f, 1.2f), 0.05f, 0.25f, dt)
        high = follow(high, (rawHigh * intensity).coerceIn(0f, 1.2f), 0.03f, 0.15f, dt)
        energy = follow(energy, (bass + mid + high) / 3f, 0.1f, 0.6f, dt)

        // Onset with hysteresis: one kick = one impulse
        val onset = b - bassAvg * 1.1f
        if (kickArmed && onset > 0.14f) {
            kick(onset * (0.5f + bass), shapesEnabled)
            kickArmed = false
        } else if (onset < 0.05f) {
            kickArmed = true
        }

        // Springs, sub-stepped for stability
        val target = 0.6f * bass.pow(1.5f)
        var remaining = dt
        while (remaining > 0f) {
            val h = min(remaining, 1f / 240f)
            remaining -= h
            for (orb in orbs) {
                val a = orb.stiffness * (target * orb.gain - orb.h) - orb.damping * orb.v
                orb.v += a * h
                orb.h += orb.v * h
                if (orb.h < -0.08f) {
                    orb.h = -0.08f
                    if (orb.v < 0f) orb.v *= -0.3f
                }
            }
        }
        for (orb in orbs) {
            orb.x = orb.anchor +
                    0.035f * sin(time * 0.21f + orb.seed * TWO_PI) +
                    0.012f * mid * sin(time * 1.7f + orb.seed * 10f)
        }

        for (i in 0 until LAYERS) {
            phase[i] += dt * LAYER_SPEED[i] * (0.6f + 1.8f * mid)
            hump[i] = HUMP_BASE[i] + 0.15f * sin(time * HUMP_SPEED[i] * TWO_PI + i * 1.7f)
        }

        if (shapesEnabled) updateShapes(dt, isPlaying)

        frame.longValue++
    }

    private fun kick(strength: Float, shapesEnabled: Boolean) {
        kickCount++
        var launched = 0
        orbs.forEachIndexed { i, orb ->
            val full = hash(kickCount * 7 + i * 13) > 0.45f
            orb.v += strength * KICK_IMPULSE * orb.gain * (if (full) 1f else 0.35f)
            if (shapesEnabled && full && launched < 2) {
                launched++
                spawnShape(
                    x = orb.x + (rng.nextFloat() - 0.5f) * 0.06f,
                    vy = 0.8f + strength * 2f,
                    size = 0.6f + rng.nextFloat() * 0.45f,
                    life = 2.4f + rng.nextFloat() * 1.2f,
                    colorIdx = orb.colorIdx,
                )
            }
        }
    }

    private fun updateShapes(dt: Float, isPlaying: Boolean) {
        for (s in shapes) {
            if (!s.alive) continue
            s.age += dt
            if (s.age >= s.life) {
                s.alive = false
                continue
            }
            s.vy = s.vy * exp(-dt * 1.4f) + 0.06f * dt // drag + slight buoyancy
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

    /** Squares dominate; the other Material shapes add variety. */
    private fun pickKind(): Int {
        val r = rng.nextFloat()
        return when {
            r < 0.45f -> KIND_SQUIRCLE
            r < 0.65f -> KIND_COOKIE
            r < 0.85f -> KIND_CLOVER
            else -> KIND_TRIANGLE
        }
    }

    fun maxRise(size: Size, bandPx: Float) = (size.height - bandPx) * 0.6f

    fun layerAlpha(i: Int): Float {
        val shimmer = 0.2f * high * sin(time * (5f + 1.3f * i) + i)
        val level = 0.65f + 0.35f * (energy * 1.6f).coerceIn(0f, 1f)
        return ((0.8f + shimmer) * level).coerceIn(0f, 1f)
    }

    fun layerPath(i: Int, size: Size, bandPx: Float): Path {
        val f = frame.longValue
        if (size.width != pathW || size.height != pathH || bandPx != pathBand) {
            pathW = size.width; pathH = size.height; pathBand = bandPx
            pathFrame.fill(-1L)
        }
        val path = paths[i]
        if (pathFrame[i] == f) return path
        pathFrame[i] = f

        val w = size.width
        val h = size.height
        val rise = maxRise(size, bandPx)
        path.reset()
        path.moveTo(0f, h)
        for (s in 0..SEGMENTS) {
            val xn = s / SEGMENTS.toFloat()
            val dh = (xn - hump[i]) / 0.3f
            var lift = bandPx * LAYER_BASE[i]
            lift += bandPx * (0.3f + 0.7f * mid) * exp(-dh * dh)
            lift += bandPx * (0.12f + 0.4f * mid) * sin(TWO_PI * LAYER_FREQ[i] * xn + phase[i])
            lift += bandPx * 0.3f * high * sin(TWO_PI * LAYER_FREQ[i] * 2.3f * xn - phase[i] * 1.7f)
            lift += bandPx * 0.06f * sin(time * 0.9f + xn * 4f + i)
            for (orb in orbs) {
                val weight = if (orb.colorIdx % LAYERS == i) 1f else 0.3f
                val d = (xn - orb.x) / 0.1f
                lift += weight * orb.h.coerceIn(0f, 0.5f) * rise * 0.3f * exp(-d * d)
            }
            path.lineTo(xn * w, h - lift)
        }
        path.lineTo(w, h)
        path.close()
        return path
    }

    inline fun forEachOrb(
        size: Size,
        bandPx: Float,
        draw: (cx: Float, cy: Float, r: Float, sx: Float, sy: Float, colorIdx: Int) -> Unit,
    ) {
        val baseY = size.height - bandPx
        val rise = maxRise(size, bandPx)
        val baseR = min(size.width * 0.1f, (size.height - bandPx) * 0.28f)
        val swell = 0.6f + 0.6f * bass.coerceIn(0f, 1.2f)
        for (orb in orbs) {
            val r = baseR * orb.size * swell
            // Squash & stretch: fast upward = tall and narrow, landing = wide
            val stretch = 1f + (orb.v * 0.06f).coerceIn(-0.3f, 0.55f)
            val cy = (baseY + r * 0.35f - orb.h * rise).coerceAtLeast(r * 0.5f)
            draw(orb.x * size.width, cy, r, 1f / sqrt(stretch), stretch, orb.colorIdx)
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

/**
 * Every shape is a polar radius function r(θ), so any two shapes morph into each other
 * simply by blending their radii per angle.
 */
private fun shapeRadius(kind: Int, theta: Float): Float = when (kind) {
    KIND_SQUIRCLE -> {
        val c = abs(cos(theta))
        val s = abs(sin(theta))
        0.92f / (c.pow(4f) + s.pow(4f)).pow(0.25f) // rounded square
    }
    KIND_COOKIE -> 0.95f * (1f + 0.07f * cos(9f * theta))
    KIND_CLOVER -> 0.9f * (1f + 0.18f * cos(4f * theta))
    else -> 0.95f * (1f + 0.13f * cos(3f * theta)) // soft triangle
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