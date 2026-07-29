@file:Suppress("unused")

package com.xenonware.launcher.ui.res

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.DialogCornerRadius
import com.xenon.mylibrary.values.DialogPadding
import com.xenon.mylibrary.values.LargestPadding

private const val FALLBACK_DRAWABLE_PX = 128

@Immutable
sealed interface XenonIcon {

    val contentDescription: String?

    @Composable
    fun Render(modifier: Modifier)

    @Immutable
    data class Vector(
        val imageVector: ImageVector,
        override val contentDescription: String? = null,
        val tint: Color? = null,
    ) : XenonIcon {
        @Composable
        override fun Render(modifier: Modifier) = Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: LocalContentColor.current
        )
    }

    @Immutable
    data class Resource(
        @DrawableRes val resId: Int,
        override val contentDescription: String? = null,
        val tint: Color? = null,
    ) : XenonIcon {
        @Composable
        override fun Render(modifier: Modifier) = Icon(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: LocalContentColor.current
        )
    }

    @Immutable
    data class Painted(
        val painter: Painter,
        override val contentDescription: String? = null,
        val tint: Color? = null,
    ) : XenonIcon {
        @Composable
        override fun Render(modifier: Modifier) = Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint ?: LocalContentColor.current
        )
    }

    data class OfDrawable(
        val drawable: Drawable,
        override val contentDescription: String? = null,
        val tint: Color? = Color.Unspecified,
    ) : XenonIcon {
        @Composable
        override fun Render(modifier: Modifier) {
            val painter = remember(drawable) {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: FALLBACK_DRAWABLE_PX
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: FALLBACK_DRAWABLE_PX
                BitmapPainter(drawable.toBitmap(width, height).asImageBitmap())
            }
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint ?: LocalContentColor.current
            )
        }
    }

    @Immutable
    class Custom(
        override val contentDescription: String? = null,
        val content: @Composable (Modifier) -> Unit,
    ) : XenonIcon {
        @Composable
        override fun Render(modifier: Modifier) {
            val description = contentDescription
            content(
                if (description != null) {
                    modifier.semantics { this.contentDescription = description }
                } else modifier
            )
        }
    }
}

fun XenonIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    tint: Color? = null,
): XenonIcon = XenonIcon.Vector(imageVector, contentDescription, tint)

fun XenonIcon(
    @DrawableRes resId: Int,
    contentDescription: String? = null,
    tint: Color? = null,
): XenonIcon = XenonIcon.Resource(resId, contentDescription, tint)

fun XenonIcon(
    painter: Painter,
    contentDescription: String? = null,
    tint: Color? = null,
): XenonIcon = XenonIcon.Painted(painter, contentDescription, tint)

fun XenonIcon(
    drawable: Drawable,
    contentDescription: String? = null,
    tint: Color? = Color.Unspecified,
): XenonIcon = XenonIcon.OfDrawable(drawable, contentDescription, tint)

fun XenonIcon(
    contentDescription: String? = null,
    content: @Composable (Modifier) -> Unit,
): XenonIcon = XenonIcon.Custom(contentDescription, content)

enum class DialogActionSizing {
    Normal,
    Compact,
    Wrapped,
}

@Suppress("UnnecessaryVariable")
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun XenonDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    title: String,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnClickOutside = true, dismissOnBackPress = true
    ),
    shape: Shape = RoundedCornerShape(DialogCornerRadius),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 6.dp,

    dialogPadding: PaddingValues = PaddingValues(DialogPadding / 2),
    dialogTitleRowPadding: PaddingValues = PaddingValues(
        start = DialogPadding, end = DialogPadding, top = 0.dp, bottom = LargestPadding
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = DialogPadding),
    buttonRowPadding: PaddingValues = PaddingValues(
        horizontal = DialogPadding, vertical = 0.dp
    ),

    actionButton1Text: String? = null,
    actionButton1LeadIcon: XenonIcon? = null,
    actionButton1Icon: XenonIcon? = null,
    actionButton1TrailingIcon: XenonIcon? = null,
    actionButton1Sizing: DialogActionSizing = DialogActionSizing.Normal,
    onActionButton1Click: (() -> Unit)? = null,
    actionButton1ContentColor: Color = MaterialTheme.colorScheme.primary,

    confirmButtonText: String? = null,
    confirmButtonLeadIcon: XenonIcon? = null,
    confirmButtonIcon: XenonIcon? = null,
    confirmButtonTrailingIcon: XenonIcon? = null,
    onConfirmButtonClick: (() -> Unit)? = null,
    isConfirmButtonEnabled: Boolean = true,
    confirmContainerColor: Color = MaterialTheme.colorScheme.primary,
    confirmContentColor: Color = MaterialTheme.colorScheme.onPrimary,

    actionButton2Text: String? = null,
    actionButton2LeadIcon: XenonIcon? = null,
    actionButton2Icon: XenonIcon? = null,
    actionButton2TrailingIcon: XenonIcon? = null,
    actionButton2Sizing: DialogActionSizing = DialogActionSizing.Normal,
    onActionButton2Click: (() -> Unit)? = null,
    actionButton2ContentColor: Color = MaterialTheme.colorScheme.primary,

    dismissIcon: XenonIcon = XenonIcon(Icons.Rounded.Close, "Dismiss Dialog (Close)"),
    dismissIconButtonContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    dismissIconButtonContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    contentManagesScrolling: Boolean = false,
    externalShowTopDivider: Boolean = false,
    externalShowBottomDivider: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest, properties = properties
    ) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val maxDialogHeight = screenHeight * 0.9f

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = maxDialogHeight),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = Modifier.padding(
                    top = dialogPadding.calculateTopPadding(),
                    bottom = dialogPadding.calculateBottomPadding()
                )
            ) {
                var titleLineCount by remember { mutableIntStateOf(0) }
                val titleVerticalAlignment =
                    if (titleLineCount > 1) Alignment.Top else Alignment.CenterVertically

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = dialogTitleRowPadding.calculateStartPadding(LayoutDirection.Ltr),
                            end = dialogTitleRowPadding.calculateEndPadding(LayoutDirection.Ltr),
                            top = dialogTitleRowPadding.calculateTopPadding(),
                            bottom = dialogTitleRowPadding.calculateBottomPadding()
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = titleVerticalAlignment
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = QuicksandTitleVariable
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .align(titleVerticalAlignment),
                        onTextLayout = { textLayoutResult: TextLayoutResult ->
                            titleLineCount = textLayoutResult.lineCount
                        })
                    FilledTonalIconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .align(titleVerticalAlignment),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = dismissIconButtonContainerColor,
                            contentColor = dismissIconButtonContentColor
                        )
                    ) {
                        dismissIcon.Render(Modifier)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(contentPadding)
                ) {
                    if (contentManagesScrolling) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (externalShowTopDivider) 1f else 0f)
                        )
                        content()
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (externalShowBottomDivider) 1f else 0f)
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        val topDividerAlpha by remember {
                            derivedStateOf { if (scrollState.value > 0) 1f else 0f }
                        }
                        val bottomDividerAlpha by remember {
                            derivedStateOf { if (scrollState.canScrollForward) 1f else 0f }
                        }

                        val maxHeightForScrollableInternalContent = screenHeight * 0.5f

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(topDividerAlpha)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxHeightForScrollableInternalContent)
                                .verticalScroll(scrollState), content = content
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(bottomDividerAlpha)
                        )
                    }
                }

                val action1Click = onActionButton1Click
                val confirmClick = onConfirmButtonClick
                val action2Click = onActionButton2Click

                val action1HasContent = actionButton1Text != null ||
                        actionButton1Icon != null ||
                        actionButton1LeadIcon != null ||
                        actionButton1TrailingIcon != null

                val confirmHasContent = confirmButtonText != null ||
                        confirmButtonIcon != null ||
                        confirmButtonLeadIcon != null ||
                        confirmButtonTrailingIcon != null

                val action2HasContent = actionButton2Text != null ||
                        actionButton2Icon != null ||
                        actionButton2LeadIcon != null ||
                        actionButton2TrailingIcon != null

                val action1Composable: (@Composable RowScope.() -> Unit)? =
                    if (action1Click != null && action1HasContent) {
                        {
                            DialogActionButton(
                                onClick = action1Click,
                                text = actionButton1Text,
                                leadIcon = actionButton1LeadIcon,
                                icon = actionButton1Icon,
                                trailingIcon = actionButton1TrailingIcon,
                                sizing = actionButton1Sizing,
                                contentColor = actionButton1ContentColor
                            )
                        }
                    } else null

                val confirmComposable: (@Composable RowScope.() -> Unit)? =
                    if (confirmClick != null && confirmHasContent) {
                        {
                            FilledTonalButton(
                                onClick = confirmClick,
                                enabled = isConfirmButtonEnabled,
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = confirmContainerColor,
                                    contentColor = confirmContentColor
                                )
                            ) {
                                DialogButtonContent(
                                    text = confirmButtonText,
                                    leadIcon = confirmButtonLeadIcon,
                                    icon = confirmButtonIcon,
                                    trailingIcon = confirmButtonTrailingIcon
                                )
                            }
                        }
                    } else null

                val action2Composable: (@Composable RowScope.() -> Unit)? =
                    if (action2Click != null && action2HasContent) {
                        {
                            DialogActionButton(
                                onClick = action2Click,
                                text = actionButton2Text,
                                leadIcon = actionButton2LeadIcon,
                                icon = actionButton2Icon,
                                trailingIcon = actionButton2TrailingIcon,
                                sizing = actionButton2Sizing,
                                contentColor = actionButton2ContentColor
                            )
                        }
                    } else null

                val anyButtonPresent = action1Composable != null ||
                        confirmComposable != null ||
                        action2Composable != null

                if (anyButtonPresent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = LargestPadding)
                            .padding(buttonRowPadding),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp, Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        action1Composable?.invoke(this)
                        confirmComposable?.invoke(this)
                        action2Composable?.invoke(this)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DialogActionButton(
    onClick: () -> Unit,
    text: String?,
    leadIcon: XenonIcon?,
    icon: XenonIcon?,
    trailingIcon: XenonIcon?,
    sizing: DialogActionSizing,
    contentColor: Color,
) {
    val onlyIcon = if (text == null && leadIcon == null && trailingIcon == null) icon else null

    when {
        onlyIcon != null && sizing == DialogActionSizing.Compact -> Box(
            modifier = Modifier.weight(1f), contentAlignment = Alignment.Center
        ) {
            DialogIconOnlyButton(onClick, onlyIcon, contentColor)
        }

        onlyIcon != null && sizing == DialogActionSizing.Wrapped ->
            DialogIconOnlyButton(onClick, onlyIcon, contentColor)

        else -> TextButton(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
        ) {
            DialogButtonContent(text, leadIcon, icon, trailingIcon)
        }
    }
}

@Composable
private fun DialogIconOnlyButton(
    onClick: () -> Unit,
    icon: XenonIcon,
    contentColor: Color,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(ButtonDefaults.MinHeight),
        colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
    ) {
        icon.Render(Modifier.size(ButtonDefaults.IconSize))
    }
}

@Composable
private fun DialogButtonContent(
    text: String?,
    leadIcon: XenonIcon?,
    icon: XenonIcon?,
    trailingIcon: XenonIcon?,
) {
    val iconModifier = Modifier.size(ButtonDefaults.IconSize)
    val iconOnly = text == null && leadIcon == null && trailingIcon == null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            ButtonDefaults.IconSpacing, Alignment.CenterHorizontally
        )
    ) {
        if (iconOnly) {
            icon?.Render(iconModifier)
            return@Row
        }

        (leadIcon ?: icon)?.Render(iconModifier)

        text?.let { Text(it) }

        trailingIcon?.Render(iconModifier)
    }
}