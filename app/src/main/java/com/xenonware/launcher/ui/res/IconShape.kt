package com.xenonware.launcher.ui.res

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
