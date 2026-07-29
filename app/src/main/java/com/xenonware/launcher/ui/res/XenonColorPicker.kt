package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

@Composable
fun XenonColorPicker(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hsv by remember(color) {
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsvArray)
        mutableStateOf(Triple(hsvArray[0], hsvArray[1], hsvArray[2]))
    }
    var alpha by remember(color) { mutableFloatStateOf(color.alpha) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alpha Slider (Left, Vertical)
            AlphaSlider(
                alpha = alpha,
                onAlphaChanged = { a ->
                    alpha = a
                    onColorChanged(Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), floatArrayOf(hsv.first, hsv.second, hsv.third))))
                },
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
            )

            Spacer(Modifier.width(12.dp))

            // SV Area (Center)
            SaturationValueArea(
                hue = hsv.first,
                saturation = hsv.second,
                value = hsv.third,
                onSVChanged = { s, v ->
                    hsv = Triple(hsv.first, s, v)
                    onColorChanged(Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), floatArrayOf(hsv.first, hsv.second, hsv.third))))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(12.dp))

            // Hue Slider (Right, Vertical)
            HueSlider(
                hue = hsv.first,
                onHueChanged = { h ->
                    hsv = Triple(h, hsv.second, hsv.third)
                    onColorChanged(Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), floatArrayOf(h, hsv.second, hsv.third))))
                },
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Hex Input
        var hexText by remember(color) {
            val argb = color.toArgb()
            val hex = if (color.alpha < 1f) {
                String.format("#%08X", argb)
            } else {
                String.format("#%06X", (0xFFFFFF and argb))
            }
            mutableStateOf(hex)
        }

        OutlinedTextField(
            value = hexText,
            onValueChange = {
                hexText = it
                try {
                    val colorToParse = if (it.startsWith("#")) it else "#$it"
                    if (colorToParse.length == 7 || colorToParse.length == 9) {
                        val parsedColor = Color(colorToParse.toColorInt())
                        onColorChanged(parsedColor)
                    }
                } catch (_: Exception) {}
            },
            label = { Text("Hex Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SaturationValueArea(
    hue: Float,
    saturation: Float,
    value: Float,
    onSVChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(hue) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val newS = (down.position.x / width).coerceIn(0f, 1f)
                        val newV = (1f - (down.position.y / height)).coerceIn(0f, 1f)
                        onSVChanged(newS, newV)

                        drag(down.id) { change ->
                            change.consume()
                            val s = (change.position.x / width).coerceIn(0f, 1f)
                            val v = (1f - (change.position.y / height)).coerceIn(0f, 1f)
                            onSVChanged(s, v)
                        }
                    }
                }
        ) {
            // Background Hue
            drawRect(
                color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            )

            // Saturation Gradient (White to Transparent)
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent))
            )

            // Value Gradient (Transparent to Black)
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
            )

            // Selector
            val selectorX = saturation * width
            val selectorY = (1f - value) * height
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black,
                radius = 7.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun HueSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val height = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val newH = (down.position.y / height).coerceIn(0f, 1f) * 360f
                        onHueChanged(newH)

                        drag(down.id) { change ->
                            change.consume()
                            val h = (change.position.y / height).coerceIn(0f, 1f) * 360f
                            onHueChanged(h)
                        }
                    }
                }
        ) {
            val hueColors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            drawRect(
                brush = Brush.verticalGradient(hueColors)
            )

            // Selector
            val selectorY = (hue / 360f) * height
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, selectorY - 2.dp.toPx()),
                size = Size(size.width, 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun AlphaSlider(
    alpha: Float,
    onAlphaChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val height = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val newA = 1f - (down.position.y / height).coerceIn(0f, 1f)
                        onAlphaChanged(newA)

                        drag(down.id) { change ->
                            change.consume()
                            val a = 1f - (change.position.y / height).coerceIn(0f, 1f)
                            onAlphaChanged(a)
                        }
                    }
                }
        ) {
            // Checkerboard pattern for alpha (simplified)
            val gridSize = 8.dp.toPx()
            for (x in 0 until (size.width / gridSize).toInt() + 1) {
                for (y in 0 until (size.height / gridSize).toInt() + 1) {
                    if ((x + y) % 2 == 0) {
                        drawRect(
                            color = Color.LightGray,
                            topLeft = Offset(x * gridSize, y * gridSize),
                            size = Size(gridSize, gridSize)
                        )
                    }
                }
            }
            
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent))
            )

            // Selector
            val selectorY = (1f - alpha) * height
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, selectorY - 2.dp.toPx()),
                size = Size(size.width, 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
