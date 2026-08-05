package com.xenonware.launcher.ui.res

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.values.ExtraLargePadding
import com.xenon.mylibrary.values.LargeCornerRadius
import com.xenon.mylibrary.values.LargerPadding
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
@Suppress("unused")
@Composable
fun SettingsSwitchMenuTile(
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
    arrowColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    dividerColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    shape: Shape = RoundedCornerShape(LargeCornerRadius),
    horizontalPadding: Dp = LargestPadding,
    verticalPadding: Dp = ExtraLargePadding,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    iconSpacing: Dp = ExtraLargePadding,
    tileSpacing: Dp = LargerPadding,
    enableRipple: Boolean = true,
) {
    val leftInteractionSource = remember { MutableInteractionSource() }
    val rightInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left clickable area (icon + text + chevron)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = leftInteractionSource,
                            indication = if (enableRipple) LocalIndication.current else null,
                            onClick = onClick,
                            role = Role.Button
                        )
                    } else Modifier
                )
                .padding(start = horizontalPadding, top = verticalPadding, bottom = verticalPadding),
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

            onClick?.let {
                Spacer(modifier = Modifier.width(MediumPadding))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Navigate",
                    tint = arrowColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MediumPadding))
            }
        }

        // Switch with divider
        onCheckedChange?.let { onCheckedChangeFunc ->
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = verticalPadding + 8.dp),
                thickness = 1.dp,
                color = dividerColor
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = rightInteractionSource,
                        indication = if (enableRipple) LocalIndication.current else null,
                        onClick = { onCheckedChangeFunc(!checked) },
                        role = Role.Button
                    )
                    .padding(start = tileSpacing, end = horizontalPadding, top = verticalPadding, bottom = verticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChangeFunc,
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
}
