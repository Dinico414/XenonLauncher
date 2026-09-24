package com.xenonware.launcher.ui.res

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.xenonware.launcher.util.AudioSpectrumAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// =============================================================================================
// Configuration — change these values in code (no settings UI yet)
// =============================================================================================

/** Layer 1: how the audio is drawn. */
object VisualizerStyle {
    const val OFF = 0

    /** Soft light plumes rising out of the glow (Gemini-like). Lives only in the blur. */
    const val GLOW = 1

    /** Classic spectrum analyzer: rounded bars left → right with falling peak caps. */
    const val BARS = 2

    /** Symmetric bars: index 0 in the center, spreading to both edges. */
    const val MIRRORED_BARS = 3

    /** Smooth filled curve with a bright outline. */
    const val SPECTRUM = 4

    /** The real audio waveform as a glowing line (ignores [VisualizerReactivity]). */
    const val OSCILLOSCOPE = 5
}

/** What layer 1 reacts to. */
object VisualizerReactivity {
    /** Frequency spectrum: low → high (GLOW: sub-bass in the middle). */
    const val SPECTRUM = 0

    /** Overall loudness; "now" at index 0, ~1.3 s of history spreading outwards. */
    const val AMPLITUDE = 1

    /** Only 30–250 Hz, spread over the whole width. */
    const val BASS = 2

    /** Only detected hits: kick → center / low end, snare → middle, hi-hats → outer / high end. */
    const val BEAT = 3

    /** Only 250 Hz–4 kHz (vocals, leads). */
    const val MELODY = 4
}

object GeometricStyle {
    const val OFF = 0

    /** Floating shapes in different colors, launched by bass kicks. */
    const val FLOATING = 1

    /** Grid of rounded squares that waves, zooms on the beat and lights up. */
    const val GRID = 2

    /** Finer grid of small diamonds (rhomboid) in diagonal, staggered rows. Same wave/zoom/light-up. */
    const val DIAMONDS = 3
}

object ColorProfile {
    /** System Material You colors (primary / secondary / tertiary), made a bit more vivid. */
    const val MATERIAL_YOU = 0

    /** Material You style palette extracted from the current album cover. */
    const val ALBUM = 1

    /** Gradient colors: blue, violet, pink, amber, cyan. */
    const val COLORFUL = 2
}

object VisualizerConfig {
    /** See [VisualizerStyle]. */
    var visualizerStyle by mutableIntStateOf(VisualizerStyle.GLOW)

    /** See [VisualizerReactivity]. */
    var reactivity by mutableIntStateOf(VisualizerReactivity.MELODY)

    /** See [GeometricStyle]. */
    var geometricStyle by mutableIntStateOf(GeometricStyle.GRID)

    /** 0 = off, 1 = flowing blurred color waves at the bottom. */
    var waves by mutableIntStateOf(1)

    /** See [ColorProfile]. */
    var colorProfile by mutableIntStateOf(ColorProfile.COLORFUL)

    /**
     * Landscape & tablet (50/50 split UI): share of the width the visualizer covers, anchored to
     * the right edge, with a soft left fade. Phone portrait always uses the full width.
     */
    var widthFractionSplit by mutableFloatStateOf(0.52f)

    /** Width of the soft left edge (alpha + blur) when the visualizer isn't full width. */
    var leftFadeWidth: Dp by mutableStateOf(48.dp)

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        visualizerStyle = prefs.getInt("visualizer_style", VisualizerStyle.GLOW).coerceIn(0, 5)
        reactivity = prefs.getInt("visualizer_reactivity", VisualizerReactivity.SPECTRUM).coerceIn(0, 4)
        geometricStyle = prefs.getInt("visualizer_geometric_style", GeometricStyle.GRID).coerceIn(0, 3)
        waves = prefs.getInt("visualizer_waves", 1).coerceIn(0, 1)
        colorProfile = prefs.getInt("visualizer_color_profile", ColorProfile.COLORFUL).coerceIn(0, 2)
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("visualizer_style", visualizerStyle)
            putInt("visualizer_reactivity", reactivity)
            putInt("visualizer_geometric_style", geometricStyle)
            putInt("visualizer_waves", waves)
            putInt("visualizer_color_profile", colorProfile)
            apply()
        }
    }

    fun getStyleName(style: Int) = when (style) {
        VisualizerStyle.OFF -> "Off"
        VisualizerStyle.GLOW -> "Glow"
        VisualizerStyle.BARS -> "Bars"
        VisualizerStyle.MIRRORED_BARS -> "Mirrored Bars"
        VisualizerStyle.SPECTRUM -> "Spectrum"
        VisualizerStyle.OSCILLOSCOPE -> "Oscilloscope"
        else -> "Unknown"
    }

    fun getReactivityName(react: Int) = when (react) {
        VisualizerReactivity.SPECTRUM -> "Spectrum"
        VisualizerReactivity.AMPLITUDE -> "Amplitude"
        VisualizerReactivity.BASS -> "Bass"
        VisualizerReactivity.BEAT -> "Beat"
        VisualizerReactivity.MELODY -> "Melody"
        else -> "Unknown"
    }

    fun getGeometryName(geom: Int) = when (geom) {
        GeometricStyle.OFF -> "Off"
        GeometricStyle.FLOATING -> "Floating"
        GeometricStyle.GRID -> "Grid"
        GeometricStyle.DIAMONDS -> "Diamonds"
        else -> "Unknown"
    }

    fun getColorProfileName(prof: Int) = when (prof) {
        ColorProfile.MATERIAL_YOU -> "Material You"
        ColorProfile.ALBUM -> "Album"
        ColorProfile.COLORFUL -> "Colorful"
        else -> "Unknown"
    }

    fun nextStyle() {
        visualizerStyle = (visualizerStyle + 1) % 6
    }

    fun nextReactivity() {
        reactivity = (reactivity + 1) % 5
    }

    fun nextGeometry() {
        geometricStyle = (geometricStyle + 1) % 4
    }

    fun nextColorProfile() {
        colorProfile = (colorProfile + 1) % 3
    }
}

/** Sampled cyclically, so the colors keep flowing through each other. */
val GradientColor = listOf(
    Color(0xFF1E7BFF), // blue
    Color(0xFF7C4DFF), // violet
    Color(0xFFFF3D8B), // pink
    Color(0xFFFFB300), // amber
    Color(0xFF00C2FF), // cyan
)

// =============================================================================================
// Color profiles
// =============================================================================================

private const val PALETTE_SIZE = 5

/**
 * Resolves [profile] to a palette and animates between palettes (track changes, profile
 * switches). [systemScheme] must be the app's own scheme, not the album-tinted one.
 */
@Composable
fun rememberVisualizerPalette(
    profile: Int,
    systemScheme: ColorScheme,
    albumArt: Bitmap?,
    albumArtUri: Any?,
): List<Color> {
    val context = LocalContext.current
    val materialYou = remember(systemScheme) { materialPalette(systemScheme) }

    var albumPalette by remember { mutableStateOf<List<Color>?>(null) }
    if (profile == ColorProfile.ALBUM) {
        LaunchedEffect(albumArt, albumArtUri) {
            albumPalette = extractAlbumPalette(context, albumArt, albumArtUri)
        }
    }

    val target = when (profile) {
        ColorProfile.MATERIAL_YOU -> materialYou
        ColorProfile.ALBUM -> albumPalette ?: materialYou
        else -> GradientColor
    }

    return List(PALETTE_SIZE) { i ->
        animateColorAsState(target[i % target.size], tween(700), label = "vizColor$i").value
    }
}

private fun materialPalette(scheme: ColorScheme): List<Color> {
    val p = vivid(scheme.primary)
    val s = vivid(scheme.secondary)
    val t = vivid(scheme.tertiary)
    return listOf(p, t, s, shiftHue(p, 30f), shiftHue(t, -30f))
}

/**
 * Same idea as Material You: seed colors from the image (WallpaperColors = the extractor
 * Android uses for wallpapers), expanded into a vivid palette.
 */
private suspend fun extractAlbumPalette(context: Context, albumArt: Bitmap?, albumArtUri: Any?): List<Color>? {
    val source = albumArt ?: albumArtUri?.let { uri ->
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(112, 112)
            .allowHardware(false)
            .build()
        (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap(112, 112)
    } ?: return null

    return withContext(Dispatchers.Default) {
        try {
            val soft = if (source.config == Bitmap.Config.HARDWARE) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else source
            val small = if (soft.width > 112 || soft.height > 112) {
                Bitmap.createScaledBitmap(soft, 112, 112, true)
            } else soft

            val seeds = run {
                val wc = WallpaperColors.fromBitmap(small)
                listOfNotNull(wc.primaryColor, wc.secondaryColor, wc.tertiaryColor)
                    .map { Color(it.toArgb()) }
            }.filter { it != Color.Unspecified }
            if (seeds.isEmpty()) return@withContext null

            val p = vivid(seeds[0])
            val s = seeds.getOrNull(1)?.let { vivid(it) } ?: shiftHue(p, 35f)
            val t = seeds.getOrNull(2)?.let { vivid(it) } ?: shiftHue(p, -35f)
            listOf(p, t, s, shiftHue(p, 25f), shiftHue(t, -25f))
        } catch (_: Exception) {
            null
        }
    }
}

/** Boosts saturation/brightness so the color glows; grays stay gray (just brighter). */
private fun vivid(c: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    if (hsv[1] >= 0.08f) hsv[1] = max(hsv[1], 0.55f)
    hsv[2] = max(hsv[2], 0.85f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun shiftHue(c: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// =============================================================================================
// Composable
// =============================================================================================

/**
 * Layers, bottom to top:
 *  1. Glow canvas (heavily blurred): waves (if [waves] = 1), the bloom of layer 1, beat flash.
 *     Only present when waves or a visualizer style is on.
 *  2. Layer 1 canvas (crisp): [visualizerStyle] driven by [reactivity] (GLOW lives in the blur).
 *  3. Geometric canvas (crisp): [geometricStyle].
 *
 * With everything off nothing is drawn and the frame loop doesn't run.
 */
@Composable
fun MusicVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    colors: List<Color> = GradientColor,
    visualizerStyle: Int = VisualizerConfig.visualizerStyle,
    reactivity: Int = VisualizerConfig.reactivity,
    geometricStyle: Int = VisualizerConfig.geometricStyle,
    waves: Int = VisualizerConfig.waves,
    bandThickness: Dp = 64.dp,
    blurRadius: Dp = 40.dp,
    gridSpacing: Dp = 18.dp,
    /** Spacing of the diamond grid ([GeometricStyle.DIAMONDS]); smaller = finer. */
    diamondSpacing: Dp = 28.dp,
    /** Soft fade-out towards the left edge: alpha ramp + crisp layers dissolving into blur. 0 = off. */
    leftFade: Dp = 0.dp,
    /** How blurry the crisp layers get inside [leftFade]. */
    edgeBlur: Dp = 12.dp,
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
    val currentStyle by rememberUpdatedState(visualizerStyle)
    val currentReactivity by rememberUpdatedState(reactivity)
    val currentGeometric by rememberUpdatedState(geometricStyle)

    val wavesOn = waves != 0
    // The glow canvas (and the beat flash inside it) only exists when waves or a style is on
    val glowOn = wavesOn || visualizerStyle != VisualizerStyle.OFF
    val anythingOn = glowOn || geometricStyle != GeometricStyle.OFF

    LaunchedEffect(isActive, anythingOn) {
        if (!isActive || !anythingOn) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 1f / 60f
                else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.025f)
                lastNanos = now
                engine.step(
                    dt, currentAnalyzer, currentPlaying, currentIntensity,
                    style = currentStyle,
                    reactivity = currentReactivity,
                    shapesEnabled = currentGeometric == GeometricStyle.FLOATING
                )
            }
        }
    }

    Box(modifier) {
        // 1) Glow — heavily blurred. The left fade is applied before the blur, so the edge
        //    dissolves softly instead of being cut.
        if (glowOn) {
            val glowModifier = if (leftFade > 0.dp) {
                Modifier
                    .matchParentSize()
                    .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            } else {
                Modifier
                    .matchParentSize()
                    .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
            }
            Canvas(glowModifier) {
                drawGlow(engine, colors, bandThickness.toPx(), visualizerStyle, wavesOn)
                if (leftFade > 0.dp) fadeLeftEdge(leftFade.toPx(), GLOW_FADE)
            }
        }

        // 2) Layer 1 — crisp
        if (visualizerStyle != VisualizerStyle.OFF && visualizerStyle != VisualizerStyle.GLOW) {
            CrispLayer(leftFade, edgeBlur) {
                drawVisualizer(engine, colors, bandThickness.toPx(), visualizerStyle, bloom = false)
            }
        }

        // 3) Geometric overlay — crisp
        when (geometricStyle) {
            GeometricStyle.FLOATING -> CrispLayer(leftFade, edgeBlur) {
                drawShapes(engine, colors, bandThickness.toPx())
            }
            GeometricStyle.GRID -> CrispLayer(leftFade, edgeBlur) {
                drawGrid(engine, gridSpacing.toPx(), bandThickness.toPx(), 1.dp.toPx())
            }
            GeometricStyle.DIAMONDS -> CrispLayer(leftFade, edgeBlur) {
                drawDiamondGrid(engine, diamondSpacing.toPx(), bandThickness.toPx(), 0.8.dp.toPx())
            }
            else -> Unit
        }
    }
}

// =============================================================================================
// Left edge fade
// =============================================================================================

// (position within the fade 0..1, alpha)
private val GLOW_FADE = arrayOf(0f to 0f, 0.3f to 0.25f, 0.7f to 0.8f, 1f to 1f)
private val SHARP_FADE = arrayOf(0f to 0f, 0.35f to 0f, 1f to 1f)
private val BLUR_COPY_FADE = arrayOf(0f to 0f, 0.45f to 0.8f, 1f to 0f)

/**
 * A crisp layer. With a [leftFade], it's drawn twice: sharp (fading out towards the edge) and
 * blurred (taking over near the edge, then fading out too), so it dissolves into blur and
 * transparency instead of being cut.
 */
@Composable
private fun BoxScope.CrispLayer(leftFade: Dp, edgeBlur: Dp, draw: DrawScope.() -> Unit) {
    if (leftFade <= 0.dp) {
        Canvas(Modifier.matchParentSize(), onDraw = draw)
        return
    }
    Canvas(
        Modifier
            .matchParentSize()
            .blur(edgeBlur, BlurredEdgeTreatment.Unbounded)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        draw()
        fadeLeftEdge(leftFade.toPx(), BLUR_COPY_FADE)
    }
    Canvas(
        Modifier
            .matchParentSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        draw()
        fadeLeftEdge(leftFade.toPx(), SHARP_FADE)
    }
}

/** Multiplies what's been drawn by an alpha ramp over the first [fadePx] from the left. */
private fun DrawScope.fadeLeftEdge(fadePx: Float, stops: Array<Pair<Float, Float>>) {
    drawRect(
        brush = Brush.horizontalGradient(
            *Array(stops.size) { stops[it].first to Color.Black.copy(alpha = stops[it].second) },
            startX = 0f,
            endX = fadePx
        ),
        blendMode = BlendMode.DstIn
    )
}

// =============================================================================================
// Glow
// =============================================================================================

private fun DrawScope.drawGlow(engine: GlowEngine, colors: List<Color>, bandPx: Float, style: Int, wavesOn: Boolean) {
    engine.observeFrame()
    val w = size.width
    val h = size.height
    val energy = engine.energy.coerceIn(0f, 1f)
    val rise = engine.maxRise(size, bandPx)

    if (wavesOn) {
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
    }

    // Bloom of layer 1 (for GLOW this is layer 1 itself)
    drawVisualizer(engine, colors, bandPx, style, bloom = true)

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

    if (wavesOn) {
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
}

// =============================================================================================
// Layer 1 styles
// =============================================================================================

private const val BAR_COUNT = 48
private const val CURVE_POINTS = 96

/** [bloom] = true: soft copy inside the blurred glow. false: the crisp version on top. */
private fun DrawScope.drawVisualizer(
    engine: GlowEngine,
    colors: List<Color>,
    bandPx: Float,
    style: Int,
    bloom: Boolean,
) {
    engine.observeFrame()
    when (style) {
        VisualizerStyle.GLOW -> if (bloom) drawPlumes(engine, colors, bandPx)
        VisualizerStyle.BARS -> drawBars(engine, colors, bandPx, mirrored = false, bloom = bloom)
        VisualizerStyle.MIRRORED_BARS -> drawBars(engine, colors, bandPx, mirrored = true, bloom = bloom)
        VisualizerStyle.SPECTRUM -> drawSpectrumCurve(engine, colors, bandPx, bloom)
        VisualizerStyle.OSCILLOSCOPE -> drawOscilloscope(engine, colors, bandPx, bloom)
        else -> Unit // OFF
    }
}

private fun DrawScope.drawPlumes(engine: GlowEngine, colors: List<Color>, bandPx: Float) {
    val h = size.height
    for (j in 0 until PLUMES) {
        if (engine.plumeH[j] < 0.02f) continue
        val c = samplePalette(colors, engine.colorPhase + engine.plumeX[j] * 0.9f)
        drawPath(
            path = engine.plumePath(j, size, bandPx),
            brush = Brush.verticalGradient(
                0f to c.copy(alpha = 0.75f),
                1f to lerp(c, Color.White, 0.15f),
                startY = engine.plumeTop(j, size, bandPx),
                endY = h
            ),
            blendMode = BlendMode.Screen
        )
    }
}

private fun DrawScope.drawBars(
    engine: GlowEngine,
    colors: List<Color>,
    bandPx: Float,
    mirrored: Boolean,
    bloom: Boolean,
) {
    val w = size.width
    val h = size.height
    val baseY = engine.baseY(size, bandPx)
    val rise = engine.maxRise(size, bandPx)
    val slot = w / BAR_COUNT
    val bw = slot * 0.62f
    val half = (BAR_COUNT - 1) / 2f

    for (b in 0 until BAR_COUNT) {
        val t = if (mirrored) abs(b - half) / half else b / (BAR_COUNT - 1f)
        val barH = max(bw, engine.specAt(t) * rise)
        val x = b * slot + (slot - bw) / 2f
        val top = baseY - barH
        val c = samplePalette(colors, engine.colorPhase + b / (BAR_COUNT - 1f) * 0.9f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to lerp(c, Color.White, 0.3f),
                0.7f to c,
                1f to c.copy(alpha = 0.35f),
                startY = top,
                endY = h
            ),
            topLeft = Offset(x, top),
            size = Size(bw, h - top),
            cornerRadius = CornerRadius(bw / 2f),
            alpha = if (bloom) 0.9f else 1f,
            blendMode = if (bloom) BlendMode.Screen else BlendMode.SrcOver
        )

        if (!bloom) {
            // Falling peak cap
            val capY = baseY - max(bw, engine.capAt(t) * rise) - bw * 0.9f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, capY),
                size = Size(bw, bw * 0.5f),
                cornerRadius = CornerRadius(bw / 4f),
                alpha = 0.85f
            )
        }
    }
}

private fun DrawScope.drawSpectrumCurve(engine: GlowEngine, colors: List<Color>, bandPx: Float, bloom: Boolean) {
    val w = size.width
    val h = size.height
    val baseY = engine.baseY(size, bandPx)
    val rise = engine.maxRise(size, bandPx)

    val fill = engine.scratchPath(0)
    val line = engine.scratchPath(1)
    fill.reset()
    line.reset()
    fill.moveTo(0f, h)
    for (i in 0..CURVE_POINTS) {
        val t = i / CURVE_POINTS.toFloat()
        val x = t * w
        val y = baseY - engine.specAt(t) * rise
        fill.lineTo(x, y)
        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
    }
    fill.lineTo(w, h)
    fill.close()

    val brush = paletteBrush(colors, engine.colorPhase, w)
    if (bloom) {
        drawPath(fill, brush, alpha = 0.85f, blendMode = BlendMode.Screen)
    } else {
        drawPath(fill, brush, alpha = 0.45f)
        drawPath(
            line,
            Brush.horizontalGradient(
                List(6) { lerp(samplePalette(colors, engine.colorPhase + it / 5f), Color.White, 0.45f) },
                startX = 0f,
                endX = w
            ),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawOscilloscope(engine: GlowEngine, colors: List<Color>, bandPx: Float, bloom: Boolean) {
    val w = size.width
    val baseY = engine.baseY(size, bandPx)
    val rise = engine.maxRise(size, bandPx)
    val cy = baseY - rise * 0.35f
    val amp = rise * 0.4f
    val wave = engine.wave
    val brush = paletteBrush(colors, engine.colorPhase, w)

    for (pass in 0..1) {
        val sign = if (pass == 0) 1f else -1f
        val path = engine.scratchPath(pass)
        path.reset()
        for (i in wave.indices) {
            val t = i / (wave.size - 1f)
            val taper = sqrt(sin(t * PI.toFloat()).coerceAtLeast(0f)) // ends settle on the center line
            val x = t * w
            val y = cy + sign * wave[i] * amp * taper
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val alpha = if (pass == 0) 1f else 0.35f // faint mirrored ghost line for depth
        drawPath(
            path,
            brush,
            alpha = alpha * (if (bloom) 0.9f else 1f),
            style = Stroke(
                width = (if (bloom) 10.dp else 3.dp).toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            blendMode = if (bloom) BlendMode.Screen else BlendMode.SrcOver
        )
    }
}

// =============================================================================================
// Geometric overlays
// =============================================================================================

private fun DrawScope.drawGrid(engine: GlowEngine, spacingPx: Float, bandPx: Float, strokePx: Float) {
    engine.observeFrame()
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f || spacingPx <= 1f) return

    val rise = engine.maxRise(size, bandPx)
    val zoom = 1f + engine.zoom
    val pivotX = w / 2f
    val cols = (w / spacingPx).toInt() + 6   // extra columns so zooming out never shows edges
    val rows = (h / spacingPx).toInt() + 3
    val startX = pivotX - (cols - 1) / 2f * spacingPx
    val amp = spacingPx * (0.2f + 0.9f * engine.mid)
    val phase = engine.gridPhase
    val tilt = 10f * engine.mid
    val invSpacing25 = 1f / (spacingPx * 2.5f)
    val flashComponent = engine.flash * 0.3f
    val maxFieldRise = 1.3f * rise

    for (r in 0 until rows) {
        val baseY = h - r * spacingPx
        val heightAbove = h - baseY
        val vFade = 1f - smoothstep(h * 0.3f, h * 0.95f, heightAbove)
        if (vFade <= 0f) continue

        val topMax = bandPx * 0.6f + maxFieldRise
        val maxLitInRow = ((if (heightAbove <= topMax) 1f else exp(-(heightAbove - topMax) * invSpacing25)) +
                flashComponent).coerceAtMost(1f)
        if (vFade * (0.14f + 0.6f * maxLitInRow) < 0.01f) continue

        val rPhase = phase - r * 0.45f
        val yCosTerm = TWO_PI * (baseY / h * 1.1f) + phase * 0.8f
        val heightScale = (1f - heightAbove / h).coerceIn(0f, 1f)

        for (c in 0 until cols) {
            val baseX = startX + c * spacingPx
            val normX = (baseX / w).coerceIn(0f, 1f)
            val field = engine.fieldAt(normX)

            val wv = sin(TWO_PI * (normX * 1.4f) + rPhase)
            var x = baseX + amp * 0.35f * cos(yCosTerm + c * 0.3f)
            var y = baseY + amp * wv - field * rise * 0.18f * heightScale

            // Zoom around the bottom center
            x = pivotX + (x - pivotX) * zoom
            y = h + (y - h) * zoom

            val top = bandPx * 0.6f + field * rise
            val dist = (heightAbove - top) * invSpacing25
            val decay = if (dist <= 0f) 1f else if (dist > 4.5f) 0f else exp(-dist)
            val lit = (decay + flashComponent).coerceAtMost(1f)

            val alpha = vFade * (0.14f + 0.6f * lit)
            if (alpha < 0.01f) continue
            val s = spacingPx * zoom * (0.34f + 0.3f * lit + 0.12f * engine.bass)
            val half = s / 2f
            val center = Offset(x, y)
            val topLeft = Offset(x - half, y - half)
            val corner = CornerRadius(s * 0.28f)

            val rotAngle = wv * tilt
            if (abs(rotAngle) > 0.01f) {
                rotate(rotAngle, pivot = center) {
                    if (lit > 0.3f) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = topLeft,
                            size = Size(s, s),
                            cornerRadius = corner,
                            alpha = alpha * 0.25f * lit
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
            } else {
                if (lit > 0.3f) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = topLeft,
                        size = Size(s, s),
                        cornerRadius = corner,
                        alpha = alpha * 0.25f * lit
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
    }
}

/**
 * Fine diamond (rhombus) grid in diagonal rows: every row is shifted by half a cell and rows
 * are half a cell apart, so the diamonds line up along both diagonals. Same behavior as
 * [drawGrid]: the wave runs through it, it zooms on the beat, and diamonds above the visualizer
 * light up and grow.
 */
private fun DrawScope.drawDiamondGrid(engine: GlowEngine, spacingPx: Float, bandPx: Float, strokePx: Float) {
    engine.observeFrame()
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f || spacingPx <= 1f) return

    val rise = engine.maxRise(size, bandPx)
    val zoom = 1f + engine.zoom
    val pivotX = w / 2f
    val rowStep = spacingPx * 0.5f                // half a cell apart → diagonal rows
    val cols = (w / spacingPx).toInt() + 6
    val rows = (h / rowStep).toInt() + 3
    val startX = pivotX - (cols - 1) / 2f * spacingPx
    val amp = spacingPx * (0.25f + 1.0f * engine.mid)
    val phase = engine.gridPhase
    val tiltRad = 12f * engine.mid * DEG
    val pad4dp = 4.dp.toPx()

    val invSpacing25 = 1f / (spacingPx * 2.5f)
    val flashComponent = engine.flash * 0.3f
    val maxFieldRise = 1.3f * rise
    val strokeStyle = Stroke(width = strokePx, join = StrokeJoin.Round)
    val path = engine.scratchPath(0)

    for (r in 0 until rows) {
        val baseY = h - r * rowStep
        val heightAbove = h - baseY
        val vFade = 1f - smoothstep(h * 0.3f, h * 0.95f, heightAbove)
        if (vFade <= 0f) continue

        val topMax = bandPx * 0.6f + maxFieldRise
        val maxLitInRow = ((if (heightAbove <= topMax) 1f else exp(-(heightAbove - topMax) * invSpacing25)) +
                flashComponent).coerceAtMost(1f)
        if (vFade * (0.12f + 0.6f * maxLitInRow) < 0.02f) continue

        val shift = if (r % 2 == 1) spacingPx / 2f else 0f
        val rPhaseTerm = phase - r * 0.22f
        val yCosTerm = TWO_PI * (baseY / h * 1.1f) + phase * 0.8f
        val heightScale = (1f - heightAbove / h).coerceIn(0f, 1f)

        for (c in 0 until cols) {
            val baseX = startX + c * spacingPx + shift
            val normX = (baseX / w).coerceIn(0f, 1f)
            val field = engine.fieldAt(normX)

            // Wave travels along the diagonal
            val wv = sin(TWO_PI * (normX * 1.4f) + rPhaseTerm)
            var x = baseX + amp * 0.35f * cos(yCosTerm + c * 0.3f)
            var y = baseY + amp * wv - field * rise * 0.18f * heightScale

            // Zoom around the bottom center
            x = pivotX + (x - pivotX) * zoom
            y = h + (y - h) * zoom

            val top = bandPx * 0.6f + field * rise
            val dist = (heightAbove - top) * invSpacing25
            val decay = if (dist <= 0f) 1f else if (dist > 4.5f) 0f else exp(-dist)
            val lit = (decay + flashComponent).coerceAtMost(1f)

            val alpha = vFade * (0.12f + 0.6f * lit)
            if (alpha < 0.02f) continue

            // The bigger the diamond, the bigger the padding (4.dp more padding when lit/bigger)
            val extraPadding = pad4dp * lit
            val s = (spacingPx * zoom * (0.34f + 0.26f * lit + 0.1f * engine.bass) - extraPadding).coerceAtLeast(2.dp.toPx())
            val hw = s * 0.38f
            val hh = s * 0.52f

            path.reset()
            val a = wv * tiltRad
            if (a == 0f) {
                path.moveTo(x, y - hh)
                path.lineTo(x + hw, y)
                path.lineTo(x, y + hh)
                path.lineTo(x - hw, y)
                path.close()
            } else {
                val ca = cos(a)
                val sa = sin(a)
                path.moveTo(x + hh * sa, y - hh * ca)
                path.lineTo(x + hw * ca, y + hw * sa)
                path.lineTo(x - hh * sa, y + hh * ca)
                path.lineTo(x - hw * ca, y - hw * sa)
                path.close()
            }

            if (lit > 0.3f) {
                drawPath(path, Color.White, alpha = alpha * 0.25f * lit)
            }
            drawPath(path, Color.White, alpha = alpha, style = strokeStyle)
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

// =============================================================================================
// Engine
// =============================================================================================

private const val SPEC_N = 64              // springy bins behind every style
private const val SRC_BANDS = 48           // synthetic source resolution (matches the analyzer)
private const val WAVE_N = 128
private const val LAYERS = 4
private const val PLUMES = 21
private const val PLUME_STEP = 0.045f
private const val PLUME_POINTS = 24
private const val SEGMENTS = 64
private const val SHAPE_POINTS = 72
private const val MAX_SHAPES = 16
private const val TWO_PI = (2 * PI).toFloat()
private const val DEG = (PI / 180).toFloat()
private const val KICK_IMPULSE = 7f
private const val ZOOM_AMOUNT = 0.12f      // steady zoom per unit of bass
private const val ZOOM_KICK = 1.8f         // extra springy zoom on each kick
private const val LIVE_GRACE_SECONDS = 3f
private const val AMBIENT_INTERVAL = 1.4f
private const val HISTORY_STEP = 0.02f     // AMPLITUDE: one history slot every 20 ms

private const val BASS_MAX_HZ = 160f
private const val MID_MAX_HZ = 2_500f

private val LAYER_BASE = floatArrayOf(0.55f, 0.75f, 0.6f, 0.7f)
private val LAYER_FREQ = floatArrayOf(1.2f, 1.9f, 1.5f, 2.6f)
private val LAYER_SPEED = floatArrayOf(0.35f, -0.45f, 0.4f, -0.3f)
private val HUMP_BASE = floatArrayOf(0.15f, 0.42f, 0.68f, 0.9f)
private val HUMP_SPEED = floatArrayOf(0.11f, 0.08f, 0.13f, 0.09f)

private const val KIND_SQUIRCLE = 0
private const val KIND_COOKIE = 1
private const val KIND_CLOVER = 2
private const val KIND_TRIANGLE = 3

/** Plume 0 in the middle, then alternating left/right towards the edges. */
private fun plumeAnchor(j: Int): Float {
    if (j == 0) return 0.5f
    val k = (j + 1) / 2
    val side = if (j % 2 == 1) -1f else 1f
    return 0.5f + side * k * PLUME_STEP
}

/** Onset detector with hysteresis: returns the onset strength once per hit, else 0. */
private class Onset(private val threshold: Float, private val ratio: Float) {
    private var avg = 0f
    private var armed = true

    fun update(x: Float, dt: Float): Float {
        avg = follow(avg, x, 0.9f, 0.9f, dt)
        val o = x - avg * ratio
        return if (armed && o > threshold) {
            armed = false
            o
        } else {
            if (o < threshold * 0.35f) armed = true
            0f
        }
    }
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

    // Source spectrum (live analyzer bands or synthetic), log-spaced
    private var liveBands = FloatArray(SRC_BANDS)
    private var liveCenters = FloatArray(SRC_BANDS)
    private val synthBands = FloatArray(SRC_BANDS)
    private val synthCenters = FloatArray(SRC_BANDS) { synthCenterHz(it) }
    private var srcBands = synthBands
    private var srcCenters = synthCenters

    // Springy bins
    private val rawLevel = FloatArray(SPEC_N)
    private val specLevel = FloatArray(SPEC_N)
    private val specH = FloatArray(SPEC_N)
    private val specV = FloatArray(SPEC_N)
    private val capH = FloatArray(SPEC_N)
    private val capV = FloatArray(SPEC_N)
    private val specK = FloatArray(SPEC_N) { 110f + 260f * it / (SPEC_N - 1f) }
    private val specD = FloatArray(SPEC_N) { 2f * 0.32f * sqrt(specK[it]) } // underdamped → elastic
    private val specGain = FloatArray(SPEC_N) { 0.95f - 0.25f * it / (SPEC_N - 1f) }

    // AMPLITUDE history (index 0 = now)
    private val history = FloatArray(SPEC_N)
    private var historyTimer = 0f

    // BEAT envelopes
    private val kickOnset = Onset(0.14f, 1.1f)
    private val snareOnset = Onset(0.12f, 1.15f)
    private val hatOnset = Onset(0.12f, 1.2f)
    private var kickEnv = 0f
    private var snareEnv = 0f
    private var hatEnv = 0f

    // Oscilloscope
    val wave = FloatArray(WAVE_N)

    // Plumes
    val plumeX = FloatArray(PLUMES) { plumeAnchor(it) }
    val plumeH = FloatArray(PLUMES)
    private val plumeSigma = FloatArray(PLUMES) { 0.06f - 0.0014f * it }

    /** Height of layer 1 along x (0..1), used by the waves and the grid. */
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
    private var time = 0f
    private var lastLiveAt = 0f
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
    private val scratchPaths = Array(2) { Path() }

    fun step(
        dt: Float,
        analyzer: AudioSpectrumAnalyzer?,
        isPlaying: Boolean,
        intensity: Float,
        style: Int,
        reactivity: Int,
        shapesEnabled: Boolean,
    ) {
        time += dt

        if (analyzer?.hasFreshSignal() == true) lastLiveAt = time
        val live = analyzer?.takeIf {
            it.isRunning && (it.hasFreshSignal() || !isPlaying || time - lastLiveAt < LIVE_GRACE_SECONDS)
        }

        // 1) Source: live analyzer or synthetic
        val rawBass: Float
        val rawMid: Float
        val rawHigh: Float
        val rawVolume: Float
        if (live != null) {
            val n = live.bands.size
            if (liveBands.size != n) {
                liveBands = FloatArray(n)
                liveCenters = FloatArray(n)
            }
            for (i in 0 until n) {
                liveBands[i] = live.bands[i]
                liveCenters[i] = live.bandCenterHz(i)
            }
            srcBands = liveBands
            srcCenters = liveCenters
            rawBass = live.bass
            rawMid = live.mid
            rawHigh = live.high
            rawVolume = live.volume
            val src = live.waveform
            for (i in 0 until WAVE_N) {
                val target = (src[i * src.size / WAVE_N] * 1.5f).coerceIn(-1f, 1f)
                wave[i] = follow(wave[i], target, 0.04f, 0.04f, dt)
            }
        } else {
            if (isPlaying) synthesize() else synthBands.fill(0.02f)
            srcBands = synthBands
            srcCenters = synthCenters
            rawBass = groupAvg(0f, BASS_MAX_HZ)
            rawMid = groupAvg(BASS_MAX_HZ, MID_MAX_HZ)
            rawHigh = groupAvg(MID_MAX_HZ, 20_000f)
            rawVolume = if (isPlaying) (0.35f * rawBass + 0.4f * rawMid + 0.25f * rawHigh + 0.2f).coerceIn(0f, 1f) else 0f
            for (i in 0 until WAVE_N) {
                val x = i / (WAVE_N - 1f)
                val target = if (!isPlaying) 0f else (0.3f + energy) * (
                        0.55f * sin(TWO_PI * 3f * x + time * 6f) +
                                0.3f * sin(TWO_PI * 7f * x - time * 9f) * (0.4f + mid) +
                                0.15f * sin(TWO_PI * 17f * x + time * 13f) * (0.3f + high)
                        )
                wave[i] = follow(wave[i], target.coerceIn(-1f, 1f), 0.04f, 0.04f, dt)
            }
        }

        // 2) Onsets (flash / zoom / shapes in every mode, plumes in BEAT)
        val kickHit = kickOnset.update(rawBass, dt)
        val snareHit = snareOnset.update(rawMid, dt)
        val hatHit = hatOnset.update(rawHigh, dt)
        kickEnv = max(kickEnv * exp(-dt * 5f), if (kickHit > 0f) (0.55f + kickHit * 2f).coerceAtMost(1f) else 0f)
        snareEnv = max(snareEnv * exp(-dt * 6f), if (snareHit > 0f) (0.5f + snareHit * 2f).coerceAtMost(1f) else 0f)
        hatEnv = max(hatEnv * exp(-dt * 9f), if (hatHit > 0f) (0.45f + hatHit * 2f).coerceAtMost(1f) else 0f)

        // 3) What drives layer 1
        when (reactivity) {
            VisualizerReactivity.AMPLITUDE -> {
                historyTimer += dt
                while (historyTimer >= HISTORY_STEP) {
                    historyTimer -= HISTORY_STEP
                    System.arraycopy(history, 0, history, 1, SPEC_N - 1)
                    history[0] = rawVolume
                }
                history.copyInto(rawLevel)
            }
            VisualizerReactivity.BASS -> mapRange(30f, 250f)
            VisualizerReactivity.MELODY -> mapRange(250f, 4_000f)
            VisualizerReactivity.BEAT -> {
                for (k in 0 until SPEC_N) {
                    val t = k / (SPEC_N - 1f)
                    rawLevel[k] = (kickEnv * exp(-(t / 0.35f).pow(2)) +
                            snareEnv * 0.85f * exp(-((t - 0.6f) / 0.22f).pow(2)) +
                            hatEnv * 0.6f * smoothstep(0.75f, 1f, t)).coerceIn(0f, 1f)
                }
            }
            else -> mapRange(30f, 14_000f) // SPECTRUM
        }

        // 4) Smoothing & groups
        for (k in 0 until SPEC_N) {
            val target = (rawLevel[k] * intensity).coerceIn(0f, 1.2f)
            specLevel[k] = follow(specLevel[k], target, 0.03f, if (k < 16) 0.2f else 0.13f, dt)
        }
        bass = follow(bass, (rawBass * intensity).coerceIn(0f, 1.2f), 0.03f, 0.2f, dt)
        mid = follow(mid, (rawMid * intensity).coerceIn(0f, 1.2f), 0.05f, 0.25f, dt)
        high = follow(high, (rawHigh * intensity).coerceIn(0f, 1.2f), 0.03f, 0.15f, dt)
        group[0] = bass
        group[1] = mid
        group[2] = (mid + high) / 2f
        group[3] = high
        energy = follow(energy, (bass + mid + high) / 3f, 0.1f, 0.6f, dt)

        if (kickHit > 0f) kick(kickHit * (0.5f + bass), reactivity, shapesEnabled)

        // 5) Springs (sub-stepped for stability)
        val zoomTarget = ZOOM_AMOUNT * bass
        val powTarget = FloatArray(SPEC_N) { k ->
            val lvl = specLevel[k]
            if (lvl <= 0f) 0f else lvl.pow(1.2f) * specGain[k]
        }
        var remaining = dt
        while (remaining > 0f) {
            val h = min(remaining, 1f / 240f)
            remaining -= h
            for (k in 0 until SPEC_N) {
                val target = powTarget[k]
                val a = specK[k] * (target - specH[k]) - specD[k] * specV[k]
                specV[k] += a * h
                specH[k] += specV[k] * h
                if (specH[k] < -0.05f) {
                    specH[k] = -0.05f
                    if (specV[k] < 0f) specV[k] *= -0.3f
                }
            }
            val za = 160f * (zoomTarget - zoom) - 9f * zoomV
            zoomV += za * h
            zoom += zoomV * h
        }
        flash *= exp(-dt * 4f)

        // Falling peak caps (BARS / MIRRORED_BARS)
        for (k in 0 until SPEC_N) {
            if (specH[k] >= capH[k]) {
                capH[k] = specH[k]
                capV[k] = 0f
            } else {
                capV[k] += 1.8f * dt
                capH[k] = (capH[k] - capV[k] * dt).coerceAtLeast(0f)
            }
        }

        // 6) Plumes & field
        for (j in 0 until PLUMES) {
            plumeX[j] = plumeAnchor(j) + 0.015f * sin(time * (0.17f + 0.03f * j) + j * 2.1f)
            plumeH[j] = specAt(j / (PLUMES - 1f))
        }
        for (s in 0..SEGMENTS) {
            val xn = s / SEGMENTS.toFloat()
            field[s] = when (style) {
                VisualizerStyle.BARS, VisualizerStyle.SPECTRUM -> specAt(xn)
                VisualizerStyle.MIRRORED_BARS -> specAt(abs(xn - 0.5f) * 2f)
                VisualizerStyle.GLOW, VisualizerStyle.OFF -> {
                    var f = 0f
                    for (j in 0 until PLUMES) {
                        val d = (xn - plumeX[j]) / plumeSigma[j]
                        if (abs(d) < 3f) {
                            f += plumeH[j] * exp(-d * d)
                        }
                    }
                    f
                }
                else -> { // OSCILLOSCOPE
                    val d = (xn - 0.5f) / 0.25f
                    0.8f * bass * exp(-d * d) + 0.3f * mid
                }
            }
        }

        // 7) Background motion
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

    /** Spreads the source bands between [fromHz] and [toHz] (log scale) over all bins. */
    private fun mapRange(fromHz: Float, toHz: Float) {
        val a = bandIndexOf(fromHz)
        val b = max(a + 1f, bandIndexOf(toHz))
        val last = srcBands.size - 1
        for (k in 0 until SPEC_N) {
            val pos = (a + (b - a) * k / (SPEC_N - 1f)).coerceIn(0f, last.toFloat())
            val i = floor(pos).toInt().coerceAtMost(last - 1)
            val t = pos - i
            rawLevel[k] = srcBands[i] + (srcBands[i + 1] - srcBands[i]) * t
        }
    }

    /** Fractional band index for a frequency. */
    private fun bandIndexOf(hz: Float): Float {
        val c = srcCenters
        if (hz <= c[0]) return 0f
        for (i in 1 until c.size) {
            if (c[i] >= hz) {
                val lo = ln(c[i - 1])
                val hi = ln(c[i])
                return i - 1 + ((ln(hz) - lo) / (hi - lo)).coerceIn(0f, 1f)
            }
        }
        return (c.size - 1).toFloat()
    }

    private fun groupAvg(fromHz: Float, toHz: Float): Float {
        var s = 0f
        var n = 0
        for (i in srcBands.indices) {
            if (srcCenters[i] in fromHz..<toHz) {
                s += srcBands[i]
                n++
            }
        }
        return if (n > 0) s / n else 0f
    }

    /** Synthetic source (no permission / no signal): kick, offbeat snare, hi-hats, moving mids. */
    private fun synthesize() {
        val kick = exp(-((time * 2f) % 1f) * 6f)            // 120 BPM
        val snare = exp(-((time * 2f + 0.5f) % 1f) * 8f)
        val hat = exp(-((time * 4f) % 1f) * 10f)
        for (i in 0 until SRC_BANDS) {
            val t = i / (SRC_BANDS - 1f)
            val bassPart = 0.15f + 0.8f * kick * exp(-t * t * 40f)
            val midPart = (0.35f + 0.2f * sin(time * (0.9f + 2.3f * t) + i * 0.7f) +
                    0.15f * sin(time * (0.31f + 0.8f * t) + i * 1.3f)) * (1f - 0.3f * t) +
                    snare * 0.35f * exp(-((t - 0.45f) / 0.15f).pow(2))
            val highPart = hat * 0.5f * smoothstep(0.6f, 1f, t)
            synthBands[i] = (bassPart * (1f - smoothstep(0.15f, 0.3f, t)) +
                    midPart * smoothstep(0.1f, 0.3f, t) + highPart).coerceIn(0f, 1f)
        }
    }

    private fun kick(strength: Float, reactivity: Int, shapesEnabled: Boolean) {
        kickCount++
        // BEAT already fires its own envelope; everywhere else the low bins get a bounce
        if (reactivity != VisualizerReactivity.BEAT) {
            for (k in 0..15) specV[k] += strength * KICK_IMPULSE * (1f - k / 30f)
        }
        zoomV += strength * ZOOM_KICK
        flash = (flash + strength * 1.2f).coerceAtMost(1f)
        for (i in 0 until LAYERS) humpKick[i] += (hash(kickCount * 5 + i) - 0.5f) * 0.12f * strength
        if (shapesEnabled) {
            repeat(if (strength > 0.3f) 2 else 1) {
                spawnShape(
                    x = 0.5f + (rng.nextFloat() - 0.5f) * 0.5f,
                    vy = 0.8f + strength * 2f,
                    size = 0.6f + rng.nextFloat() * 0.45f,
                    life = 2.4f + rng.nextFloat() * 1.2f,
                    colorIdx = rng.nextInt(PALETTE_SIZE),
                )
            }
        }
    }

    // ---- Geometry helpers ----

    fun maxRise(size: Size, bandPx: Float) = (size.height - bandPx) * 0.72f

    fun baseY(size: Size, bandPx: Float) = size.height - bandPx * 0.5f

    /** Spring height at t (0 = start of the reactivity range, 1 = end), ≥ 0. */
    fun specAt(t: Float): Float = sample(specH, t).coerceAtLeast(0f)

    fun capAt(t: Float): Float = sample(capH, t).coerceAtLeast(0f)

    private fun sample(arr: FloatArray, t: Float): Float {
        val pos = t.coerceIn(0f, 1f) * (arr.size - 1)
        val i = floor(pos).toInt().coerceAtMost(arr.size - 2)
        val f = pos - i
        return arr[i] + (arr[i + 1] - arr[i]) * f
    }

    /** 0..~1.3: layer-1 height at xn (0..1), used by the waves and the grid. */
    fun fieldAt(xn: Float): Float = sample(field, xn).coerceIn(0f, 1.3f)

    fun scratchPath(slot: Int): Path = scratchPaths[slot]

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
        val peak = plumeH[j] * maxRise(size, bandPx) + bandPx * 0.3f
        return (size.height - bandPx * 0.5f - peak).coerceAtLeast(0f)
    }

    fun plumePath(j: Int, size: Size, bandPx: Float): Path {
        val w = size.width
        val h = size.height
        val hj = plumeH[j]
        val sig = plumeSigma[j] * w * (1f + 0.3f * hj)
        val cx = plumeX[j] * w
        val peak = hj * maxRise(size, bandPx) + bandPx * 0.3f
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

    // ---- Floating shapes (GeometricStyle.FLOATING) ----

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
                colorIdx = rng.nextInt(PALETTE_SIZE),
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

// =============================================================================================
// Helpers
// =============================================================================================

/** Same log spacing as the analyzer (30 Hz … 14 kHz). */
private fun synthCenterHz(i: Int): Float =
    30f * (14_000f / 30f).pow((i + 0.5f) / SRC_BANDS)

/** Cyclic palette lookup: t wraps around, neighbors are blended. */
private fun samplePalette(colors: List<Color>, t: Float): Color {
    val n = colors.size
    if (n == 1) return colors[0]
    val wrapped = t - floor(t)
    val pos = wrapped * n
    val i = pos.toInt() % n
    return lerp(colors[i], colors[(i + 1) % n], pos - floor(pos))
}

/** Horizontal gradient through the whole palette, shifted by [phase]. */
private fun paletteBrush(colors: List<Color>, phase: Float, width: Float): Brush =
    Brush.horizontalGradient(
        List(6) { samplePalette(colors, phase + it / 5f) },
        startX = 0f,
        endX = width
    )

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
    val diff = target - current
    if (abs(diff) < 0.0001f) return target
    val tau = if (target > current) attack else release
    return current + diff * (1f - exp(-dt / tau))
}

private fun hash(n: Int): Float {
    var x = n * 374761393 + 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFF) / 65535f
}