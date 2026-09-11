package com.xenonware.launcher.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.R
import org.json.JSONObject

enum class FontType(val id: Int, val title: String) {
    SYSTEM(0, "System Default"),
    ROBOTO_FLEX(1, "Roboto Flex"),
    GOOGLE_SANS_FLEX(2, "Google Sans Flex"),
    QUICKSAND(3, "Quicksand");

    companion object {
        fun fromId(id: Int): FontType = entries.find { it.id == id } ?: SYSTEM
    }
}

data class AxisDef(
    val tag: String,
    @StringRes val nameRes: Int,
    val min: Float,
    val default: Float,
    val max: Float,
    val isInteger: Boolean = true
)

object FontAxes {
    val GOOGLE_SANS_AXES = listOf(
        AxisDef("wght", R.string.axis_weight, 1f, 500f, 1000f),
        AxisDef("wdth", R.string.axis_width, 25f, 100f, 151f),
        AxisDef("opsz", R.string.axis_optical_size, 6f, 18f, 144f),
        AxisDef("GRAD", R.string.axis_grade, 0f, 0f, 100f),
        AxisDef("ROND", R.string.axis_roundness, 0f, 0f, 100f),
        AxisDef("slnt", R.string.axis_slant, -10f, 0f, 0f)
    )

    val ROBOTO_FLEX_AXES = listOf(
        AxisDef("wght", R.string.axis_weight, 100f, 500f, 1000f),
        AxisDef("wdth", R.string.axis_width, 25f, 100f, 151f),
        AxisDef("opsz", R.string.axis_optical_size, 8f, 14f, 144f),
        AxisDef("slnt", R.string.axis_slant, -10f, 0f, 10f),
        AxisDef("GRAD", R.string.axis_grade, -200f, 0f, 150f),
        AxisDef("XOPQ", R.string.axis_thick_stroke, 27f, 96f, 175f),
        AxisDef("YOPQ", R.string.axis_thin_stroke, 25f, 79f, 135f),
        AxisDef("XTRA", R.string.axis_counter_width, 323f, 468f, 603f),
        AxisDef("YTUC", R.string.axis_uppercase_height, 528f, 712f, 760f),
        AxisDef("YTLC", R.string.axis_lowercase_height, 416f, 514f, 570f),
        AxisDef("YTAS", R.string.axis_ascender_height, 649f, 750f, 854f),
        AxisDef("YTDE", R.string.axis_descender_depth, -305f, -203f, -98f),
        AxisDef("YTFI", R.string.axis_figure_height, 560f, 738f, 788f)
    )

    fun parseSettings(jsonStr: String, axes: List<AxisDef>): Map<String, Float> {
        val map = axes.associate { it.tag to it.default }.toMutableMap()
        try {
            val json = JSONObject(jsonStr)
            axes.forEach { axis ->
                if (json.has(axis.tag)) {
                    map[axis.tag] = json.getDouble(axis.tag).toFloat()
                }
            }
        } catch (_: Exception) {}
        return map
    }

    fun serializeSettings(map: Map<String, Float>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v.toDouble()) }
        return json.toString()
    }
}

fun createCustomFontFamily(
    fontType: FontType,
    robotoSettings: Map<String, Float>,
    googleSansSettings: Map<String, Float>
): FontFamily {
    return when (fontType) {
        FontType.SYSTEM -> FontFamily.Default
        FontType.QUICKSAND -> QuicksandTitleVariable
        FontType.ROBOTO_FLEX -> {
            try {
                val variationSettings = robotoSettings.map { (tag, value) ->
                    FontVariation.Setting(tag, value)
                }.toTypedArray()
                FontFamily(
                    Font(
                        resId = R.font.roboto_flex,
                        variationSettings = FontVariation.Settings(*variationSettings)
                    )
                )
            } catch (_: Exception) {
                FontFamily.Default
            }
        }
        FontType.GOOGLE_SANS_FLEX -> {
            try {
                val variationSettings = googleSansSettings.map { (tag, value) ->
                    FontVariation.Setting(tag, value)
                }.toTypedArray()
                FontFamily(
                    Font(
                        resId = R.font.goggle_sans_flex,
                        variationSettings = FontVariation.Settings(*variationSettings)
                    )
                )
            } catch (_: Exception) {
                FontFamily.Default
            }
        }
    }
}

val LocalMainFontFamily = staticCompositionLocalOf {
    QuicksandTitleVariable
}

val mainFontFamily: FontFamily
    @Composable @ReadOnlyComposable get() = LocalMainFontFamily.current
