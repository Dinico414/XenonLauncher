package com.xenonware.launcher.ui.res

import android.graphics.Path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import android.graphics.Matrix as AndroidMatrix

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
enum class IconShape {
    Circle, Square, Arch, Pill, Cookie4, Cookie6, Cookie7, Teardrop;

    fun getShape(): Shape {
        return when (this) {
            Circle -> PolygonShape(MaterialShapes.Circle)
            Square -> PolygonShape(MaterialShapes.Square)
            Arch -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 25, bottomStartPercent = 25)
            Pill -> PolygonShape(MaterialShapes.Pill)
            Cookie4 -> PolygonShape(MaterialShapes.Cookie4Sided)
            Cookie6 -> PolygonShape(MaterialShapes.Cookie6Sided)
            Cookie7 -> PolygonShape(MaterialShapes.Cookie7Sided)
            Teardrop -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 25, bottomStartPercent = 50)
        }
    }

    fun getAndroidPath(width: Float, height: Float): Path {
        return when (this) {
            Arch -> {
                val path = Path()
                val rect = android.graphics.RectF(0f, 0f, width, height)
                val radii = floatArrayOf(
                    width * 0.5f, width * 0.5f,
                    width * 0.5f, width * 0.5f,
                    width * 0.25f, width * 0.25f,
                    width * 0.25f, width * 0.25f
                )
                path.addRoundRect(rect, radii, Path.Direction.CW)
                path
            }
            Teardrop -> {
                val path = Path()
                val rect = android.graphics.RectF(0f, 0f, width, height)
                val radii = floatArrayOf(
                    width * 0.5f, width * 0.5f,
                    width * 0.5f, width * 0.5f,
                    width * 0.25f, width * 0.25f,
                    width * 0.5f, width * 0.5f
                )
                path.addRoundRect(rect, radii, Path.Direction.CW)
                path
            }
            else -> {
                val poly = when (this) {
                    Circle -> MaterialShapes.Circle
                    Square -> MaterialShapes.Square
                    Pill -> MaterialShapes.Pill
                    Cookie4 -> MaterialShapes.Cookie4Sided
                    Cookie6 -> MaterialShapes.Cookie6Sided
                    Cookie7 -> MaterialShapes.Cookie7Sided
                }
                val path = poly.toPath()
                val matrix = AndroidMatrix()
                matrix.setScale(width, height)
                path.transform(matrix)
                path
            }
        }
    }
}

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = polygon.toPath().asComposePath()
        val matrix = Matrix()
        matrix.scale(size.width, size.height)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
