package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A composable that draws an icon that can morph between a Back arrow and a Close (X) icon.
 *
 * @param progress 0.0 for Close icon, 1.0 for Back arrow.
 * @param color The color of the icon.
 * @param modifier Modifier for the icon.
 * @param size The size of the icon.
 */
@Composable
fun MorphingBackCloseIcon(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.dp.toPx()
        
        // Segments: 0.0 = Close, 1.0 = Back
        
        // Segment 1: One line of X -> Top Wing of Back
        drawMorphingLine(
            start0 = Offset(0.2f, 0.2f), end0 = Offset(0.8f, 0.8f),
            start1 = Offset(0.45f, 0.2f), end1 = Offset(0.15f, 0.5f),
            progress = progress,
            color = color,
            strokeWidth = strokeWidth
        )
        
        // Segment 2: Other line of X -> Bottom Wing of Back
        drawMorphingLine(
            start0 = Offset(0.8f, 0.2f), end0 = Offset(0.2f, 0.8f),
            start1 = Offset(0.45f, 0.8f), end1 = Offset(0.15f, 0.5f),
            progress = progress,
            color = color,
            strokeWidth = strokeWidth
        )
        
        // Segment 3: Center point -> Shaft of Back
        drawMorphingLine(
            start0 = Offset(0.5f, 0.5f), end0 = Offset(0.5f, 0.5f),
            start1 = Offset(0.15f, 0.5f), end1 = Offset(0.85f, 0.5f),
            progress = progress,
            color = color,
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawMorphingLine(
    start0: Offset, end0: Offset,
    start1: Offset, end1: Offset,
    progress: Float,
    color: Color,
    strokeWidth: Float
) {
    val startX = lerp(start0.x, start1.x, progress) * size.width
    val startY = lerp(start0.y, start1.y, progress) * size.height
    val endX = lerp(end0.x, end1.x, progress) * size.width
    val endY = lerp(end0.y, end1.y, progress) * size.height
    
    drawLine(
        color = color,
        start = Offset(startX, startY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
