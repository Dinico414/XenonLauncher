package com.xenonware.launcher.ui.res

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun <T> XenonSingleChoiceButtonGroup(
    options: List<T>,
    selectedOption: T,
    onOptionSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = colorScheme.surfaceDim,
        checkedContainerColor = colorScheme.primary,
        contentColor = colorScheme.onSurface,
        checkedContentColor = colorScheme.onPrimary
    ),
    icon: @Composable (T, Boolean) -> Unit = { _, isSelected ->
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp)
            )
        }
    },
) {
    val interactionSources = remember(options) { options.map { MutableInteractionSource() } }

    val pressedStates = remember(options) {
        mutableStateListOf<Boolean>().apply { repeat(options.size) { add(false) } }
    }

    options.forEachIndexed { index, _ ->
        LaunchedEffect(interactionSources[index]) {
            var pressStartTime = 0L
            interactionSources[index].interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressedStates[index] = true
                        pressStartTime = System.currentTimeMillis()
                    }

                    is PressInteraction.Release -> {
                        val duration = System.currentTimeMillis() - pressStartTime
                        if (duration < 200) {
                            delay((200 - duration).milliseconds)
                        }
                        pressedStates[index] = false
                    }

                    is PressInteraction.Cancel -> {
                        pressedStates[index] = false
                    }
                }
            }
        }
    }

    val pressedIndex = pressedStates.indexOfFirst { it }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option

            val targetWeight = if (pressedIndex == -1) {
                1f
            } else {
                if (index == pressedIndex) {
                    1.05f
                } else if (abs(index - pressedIndex) == 1) {
                    val neighbors =
                        if (pressedIndex == 0 || pressedIndex == options.size - 1) 1 else 2
                    1f - (0.05f / neighbors)
                } else {
                    1f
                }
            }

            val weight by animateFloatAsState(
                targetValue = targetWeight,
                label = "weight",
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { if (it) onOptionSelect(option) },
                modifier = Modifier.weight(weight),
                colors = colors,
                interactionSource = interactionSources[index]
            ) {
                icon(option, isSelected)
                Text(
                    text = label(option), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}