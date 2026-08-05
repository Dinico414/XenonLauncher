package com.xenonware.launcher.ui.res

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
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
import com.xenon.mylibrary.values.LargerPadding
import com.xenon.mylibrary.values.LargestPadding

@Suppress("unused")
@Composable
fun SettingsSwitchTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    iconSpacing: Dp = ExtraLargePadding,
    tileSpacing: Dp = LargerPadding,
    enableRipple: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = if (enableRipple) LocalIndication.current else null,
                        onClick = onClick,
                        role = Role.Button
                    )
                } else Modifier
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

        onCheckedChange?.let {
            Spacer(modifier = Modifier.width(tileSpacing))
            Switch(
                checked = checked,
                onCheckedChange = it,
                colors = switchColors,
                thumbContent = {
                    if (checked) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Checked",
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Not Checked",
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = MaterialTheme.colorScheme.surfaceDim
                        )
                    }
                }
            )
        }
    }
}

@Suppress("unused")
@Composable
fun SettingsSwitchTileContext(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    iconSpacing: Dp = ExtraLargePadding,
    tileSpacing: Dp = LargerPadding,
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
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = if (enableRipple) LocalIndication.current else null,
                        onClick = onClick,
                        role = Role.Button
                    )
                } else Modifier
            )
    ) {
        SettingsSwitchTile(
            title = title,
            subtitle = subtitle,
            checked = checked,
            onCheckedChange = onCheckedChange,
            onClick = null,
            icon = icon,
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            subtitleColor = subtitleColor,
            shape = androidx.compose.ui.graphics.RectangleShape,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            switchColors = switchColors,
            iconSpacing = iconSpacing,
            tileSpacing = tileSpacing,
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
