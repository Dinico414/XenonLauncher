package com.xenonware.launcher.ui.res

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

private const val ExpandedScaleTarget = 1f
private const val ClosedScaleTarget = 0.8f
private const val ExpandedAlphaTarget = 1f
private const val ClosedAlphaTarget = 0f

private fun alignmentToTransformOrigin(alignment: Alignment): TransformOrigin {
    val pivotX = when (alignment) {
        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> 0f
        Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> 1f
        else -> 0.5f
    }
    val pivotY = when (alignment) {
        Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> 0f
        Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd -> 1f
        else -> 0.5f
    }
    return TransformOrigin(pivotX, pivotY)
}

data class MenuItem(
    val text: String,
    val onClick: () -> Unit,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val trailingIcon: (@Composable () -> Unit)? = null,
    val dismissOnClick: Boolean = true,
    val textColor: Color? = null,
    val containerColor: Color? = null,
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun XenonDropDown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<MenuItem>,
    hazeState: HazeState,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    anchorPos: Offset? = null,
    radius: Dp = 24.dp,
    widthMin: Dp = 150.dp,
    widthMax: Dp = 280.dp,
    shadowElevation: Dp = 4.dp,
    alignment: Alignment = Alignment.TopEnd,
    maxLines: Int = 1,
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var parentPos by remember { mutableStateOf(Offset.Zero) }

    // Detect our own anchor position automatically
    androidx.compose.foundation.layout.Box(
        Modifier.onGloballyPositioned { parentPos = it.positionInRoot() }
    )

    if (expandedState.currentState || expandedState.targetState) {
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val marginPx = with(density) { 16.dp.toPx() }

        val actualAnchor = anchorPos ?: parentPos

        Popup(
            alignment = Alignment.TopStart,
            offset = with(density) {
                val touchX = actualAnchor.x + offsetX.toPx()
                val touchY = actualAnchor.y + offsetY.toPx()

                // Use measured size if available, otherwise estimate
                val mWidth = if (menuSize.width > 0) menuSize.width.toFloat() else widthMax.toPx()
                val mHeight = if (menuSize.height > 0) menuSize.height.toFloat() else (items.size * 48).dp.toPx() + 8.dp.toPx()

                var targetAbsX = touchX - (mWidth / 2f)
                var targetAbsY = touchY - (mHeight / 2f)

                // Clamp to screen edges with 16dp margin
                targetAbsX = targetAbsX.coerceIn(marginPx, (screenWidthPx - mWidth - marginPx).coerceAtLeast(marginPx))
                targetAbsY = targetAbsY.coerceIn(marginPx, (screenHeightPx - mHeight - marginPx).coerceAtLeast(marginPx))

                // Popup offset is relative to the anchor position
                val finalRelX = targetAbsX - parentPos.x
                val finalRelY = targetAbsY - parentPos.y

                IntOffset(finalRelX.roundToInt(), finalRelY.roundToInt())
            },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            @Suppress("DEPRECATION")
            val transition = updateTransition(expandedState, label = "DropDownMenu")

            val scale by transition.animateFloat(
                label = "scale",
                transitionSpec = {
                    if (false isTransitioningTo true) {
                        tween(durationMillis = 120, easing = LinearOutSlowInEasing)
                    } else {
                        tween(durationMillis = 75, easing = FastOutLinearInEasing)
                    }
                },
            ) { if (it) ExpandedScaleTarget else ClosedScaleTarget }

            val alpha by transition.animateFloat(
                label = "alpha",
                transitionSpec = {
                    if (false isTransitioningTo true) {
                        tween(durationMillis = 30, easing = LinearOutSlowInEasing)
                    } else {
                        tween(durationMillis = 75, easing = FastOutLinearInEasing)
                    }
                },
            ) { if (it) ExpandedAlphaTarget else ClosedAlphaTarget }

            val transformOrigin = remember(alignment) { alignmentToTransformOrigin(alignment) }

            Column(
                modifier = Modifier
                    .onSizeChanged { menuSize = it }
                    .widthIn(min = widthMin, max = widthMax)
                    .width(IntrinsicSize.Max)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        this.transformOrigin = transformOrigin
                    }
                    .shadow(shadowElevation, RoundedCornerShape(radius))
                    .clip(RoundedCornerShape(radius))
                    .hazeEffect(
                        state = hazeState,
                        style = HazeMaterials.ultraThin(),
                    )
                    .background(colorScheme.surfaceContainer.copy(alpha = 0.4f))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = item.text,
                                color = item.textColor ?: Color.Unspecified,
                                maxLines = maxLines,
                            )
                        },
                        onClick = {
                            item.onClick()
                            if (item.dismissOnClick) onDismissRequest()
                        },
                        leadingIcon = item.leadingIcon,
                        trailingIcon = item.trailingIcon,
                        colors = MenuDefaults.itemColors(
                            textColor = item.textColor ?: colorScheme.onSurface,
                            leadingIconColor = item.textColor ?: colorScheme.onSurfaceVariant,
                            trailingIconColor = item.textColor ?: colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radius - 4.dp))
                            .then(
                                if (item.containerColor != null) {
                                    Modifier.background(item.containerColor)
                                } else Modifier
                            ),
                        contentPadding = if (item.trailingIcon != null) {
                            MenuDefaults.DropdownMenuItemContentPadding
                        } else {
                            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        },
                    )
                }
            }
        }
    }
}
