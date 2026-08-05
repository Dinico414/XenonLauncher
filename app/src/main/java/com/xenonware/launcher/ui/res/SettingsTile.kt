package com.xenonware.launcher.ui.res

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.LargeCornerRadius
import com.xenon.mylibrary.values.LargestPadding

@Suppress("unused")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    iconSpacing: Dp = ExtraLargePadding,
    enableRipple: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = if (enableRipple) LocalIndication.current else null,
                enabled = onClick != null || onLongClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() },
                role = Role.Button
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke()

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = iconSpacing)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
            }
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsTileContext(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    iconSpacing: Dp = ExtraLargePadding,
    enableRipple: Boolean = true,
    showContext: Boolean = true,
    contextContent: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = if (enableRipple) LocalIndication.current else null,
                enabled = onClick != null || onLongClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() },
                role = Role.Button
            )
    ) {
        SettingsTile(
            title = title,
            subtitle = subtitle,
            onClick = null,
            onLongClick = null,
            icon = icon,
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            subtitleColor = subtitleColor,
            shape = androidx.compose.ui.graphics.RectangleShape,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            iconSpacing = iconSpacing,
            enableRipple = false
        )
        AnimatedVisibility(
            visible = showContext,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            contextContent()
        }
    }
}
