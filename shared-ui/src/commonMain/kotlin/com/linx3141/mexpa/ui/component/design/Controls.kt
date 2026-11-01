@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults as MaterialButtonDefaults
import androidx.compose.material3.CircularProgressIndicator as MaterialCircularProgressIndicator
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton as MaterialFilledTonalButton
import androidx.compose.material3.LoadingIndicator as MaterialLoadingIndicator
import androidx.compose.material3.PrimaryTabRow as MaterialPrimaryTabRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox as MaterialPullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults as MaterialPullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.HorizontalDivider as MaterialHorizontalDivider
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.IconButtonDefaults as MaterialIconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider as MaterialSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.TextField as MaterialTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixBasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardColors as MiuixCardColors
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider as MiuixHorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator as MiuixInfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh as MiuixPullToRefresh
import top.yukonga.miuix.kmp.basic.TabRow as MiuixTabRow
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults as MiuixProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.SliderDefaults as MiuixSliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle as MiuixSmallTitle
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors as MiuixTextButtonColors
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * Material 模式下对话框内的动作按钮：KSU 的 `ExpressiveDialog` 用 M3 TextButton，
 * 这里用 CompositionLocal 把对话框内容里的 TextButton 切成文字按钮形态。
 */
val LocalDialogActions = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * 主题无关的基础控件。Miuix 模式逐参数委托 miuix（默认外观不变），Material 模式用
 * M3 Expressive 的 Surface / TextField / Slider 等重绘。页面只依赖这里的签名，
 * 对应 KernelSU 的 `component/material` 与 `component/miuix` 两层组件的收口。
 */

// === 按钮 ===

@Immutable
class TextButtonColors(
    val color: Color,
    val disabledColor: Color,
    val textColor: Color,
    val disabledTextColor: Color,
)

object ButtonDefaults {

    @Composable
    fun textButtonColors(): TextButtonColors = if (LocalUiMode.current == UiMode.Miuix) {
        val colors = MiuixButtonDefaults.textButtonColors()
        TextButtonColors(
            color = colors.color,
            disabledColor = colors.disabledColor,
            textColor = colors.textColor,
            disabledTextColor = colors.disabledTextColor,
        )
    } else {
        TextButtonColors(
            color = MaterialTheme.colorScheme.secondaryContainer,
            disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }

    @Composable
    fun textButtonColorsPrimary(): TextButtonColors = if (LocalUiMode.current == UiMode.Miuix) {
        val colors = MiuixButtonDefaults.textButtonColorsPrimary()
        TextButtonColors(
            color = colors.color,
            disabledColor = colors.disabledColor,
            textColor = colors.textColor,
            disabledTextColor = colors.disabledTextColor,
        )
    } else {
        TextButtonColors(
            color = MaterialTheme.colorScheme.primary,
            disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            textColor = MaterialTheme.colorScheme.onPrimary,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = 16.dp,
    minWidth: Dp = 58.dp,
    minHeight: Dp = 40.dp,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTextButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            cornerRadius = cornerRadius,
            minWidth = minWidth,
            minHeight = minHeight,
            colors = MiuixTextButtonColors(
                color = colors.color,
                disabledColor = colors.disabledColor,
                textColor = colors.textColor,
                disabledTextColor = colors.disabledTextColor,
            ),
            insideMargin = insideMargin,
        )
        return
    }
    val buttonModifier = modifier.defaultMinSize(minWidth = minWidth, minHeight = minHeight)
    val content: @Composable RowScope.() -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    when {
        LocalDialogActions.current -> MaterialTextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            colors = MaterialButtonDefaults.textButtonColors(
                contentColor = when (colors.color) {
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.primary,
                    -> MaterialTheme.colorScheme.primary

                    else -> colors.textColor
                },
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            content = content,
        )

        colors.color == MaterialTheme.colorScheme.secondaryContainer -> MaterialFilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            contentPadding = MaterialButtonDefaults.ContentPadding,
            content = content,
        )

        else -> MaterialButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            colors = MaterialButtonDefaults.buttonColors(
                containerColor = colors.color,
                contentColor = colors.textColor,
                disabledContainerColor = colors.disabledColor,
                disabledContentColor = colors.disabledTextColor,
            ),
            contentPadding = MaterialButtonDefaults.ContentPadding,
            content = content,
        )
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backgroundColor: Color = Color.Unspecified,
    minWidth: Dp = 40.dp,
    minHeight: Dp = 40.dp,
    cornerRadius: Dp = 40.dp,
    capsule: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            holdDownState = holdDownState,
            backgroundColor = backgroundColor,
            minWidth = minWidth,
            minHeight = minHeight,
            cornerRadius = if (capsule) minHeight / 2 else cornerRadius,
        ) {
            content()
        }
        return
    }
    val colors = if (backgroundColor == Color.Unspecified) {
        MaterialIconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        MaterialIconButtonDefaults.filledTonalIconButtonColors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
    if (capsule) {
        MaterialIconButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minWidth = minWidth, minHeight = minHeight),
            enabled = enabled,
            colors = colors,
            shape = RoundedCornerShape(percent = 50),
        ) {
            content()
        }
    } else {
        MaterialIconButton(
            onClick = onClick,
            modifier = modifier.defaultMinSize(minWidth = minWidth, minHeight = minHeight),
            enabled = enabled,
            colors = colors,
        ) {
            content()
        }
    }
}

// === 开关 / 复选 ===

@Composable
fun MishkaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
        colors = SwitchDefaults.colors(
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledCheckedThumbColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledCheckedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.12f),
            disabledUncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledUncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    )
}

@Composable
fun Checkbox(
    state: androidx.compose.ui.state.ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixCheckbox(state = state, onClick = onClick ?: {}, modifier = modifier, enabled = enabled)
        return
    }
    TriStateCheckbox(state = state, onClick = onClick ?: {}, modifier = modifier, enabled = enabled)
}

// === 滑块 / 进度 ===

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    showKeyPoints: Boolean = false,
    keyPoints: List<Float>? = null,
    magnetThreshold: Float = 0.02f,
    hapticEffect: MiuixSliderDefaults.SliderHapticEffect = MiuixSliderDefaults.DefaultHapticEffect,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            showKeyPoints = showKeyPoints,
            keyPoints = keyPoints,
            magnetThreshold = magnetThreshold,
            hapticEffect = hapticEffect,
        )
        return
    }
    MaterialSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeWidth: Dp = 3.dp,
    size: Dp = MiuixProgressIndicatorDefaults.DefaultCircularProgressIndicatorSize,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixCircularProgressIndicator(
            modifier = modifier,
            progress = progress,
            strokeWidth = strokeWidth,
            size = size,
        )
        return
    }
    if (progress == null) {
        MaterialCircularProgressIndicator(modifier = modifier.size(size), strokeWidth = strokeWidth)
    } else {
        MaterialCircularProgressIndicator(progress = { progress }, modifier = modifier.size(size), strokeWidth = strokeWidth)
    }
}

// === 文本 / 容器 ===
/** Material 页面外边距 16dp；Miuix 保持原生 12dp */
val PageHorizontalPadding: Dp
    @Composable get() = if (LocalUiMode.current == UiMode.Material) 16.dp else 12.dp

/** 页面提示文字单独使用 24dp，与 16dp 的卡片列表形成 AOSP Settings 式层级 */
val HintHorizontalPadding: Dp
    @Composable get() = if (LocalUiMode.current == UiMode.Material) 24.dp else 12.dp


@Composable
fun SmallTitle(
    text: String,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixSmallTitle(text = text, modifier = modifier, insideMargin = insideMargin)
        return
    }
    // AOSP SettingsLib Expressive 的分组标题 = listPreferredItemPaddingStart(16) + category 内缩 8dp
    Text(
        text = text,
        modifier = modifier.padding(
            start = PageHorizontalPadding + 8.dp,
            top = 12.dp,
            end = PageHorizontalPadding + 8.dp,
            bottom = 8.dp,
        ),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Immutable
class CardColors internal constructor(
    internal val miuix: MiuixCardColors? = null,
    val color: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
)

object CardDefaults {
    @Composable
    fun defaultColors(
        color: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
    ): CardColors = if (LocalUiMode.current == UiMode.Miuix) {
        val miuix = when {
            color == Color.Unspecified && contentColor == Color.Unspecified -> MiuixCardDefaults.defaultColors()
            color == Color.Unspecified -> MiuixCardDefaults.defaultColors(contentColor = contentColor)
            contentColor == Color.Unspecified -> MiuixCardDefaults.defaultColors(color = color)
            else -> MiuixCardDefaults.defaultColors(color = color, contentColor = contentColor)
        }
        CardColors(miuix = miuix, color = miuix.color, contentColor = miuix.contentColor)
    } else {
        val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.surfaceBright else color
        val resolvedContent = if (contentColor == Color.Unspecified) {
            contentColorFor(resolvedColor).takeIf { it != Color.Unspecified }
                ?: MaterialTheme.colorScheme.onSurface
        } else {
            contentColor
        }
        CardColors(color = resolvedColor, contentColor = resolvedContent)
    }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    colors: CardColors = CardDefaults.defaultColors(),
    onClick: (() -> Unit)? = null,
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        val miuixColors = colors.miuix ?: MiuixCardDefaults.defaultColors(
            color = colors.color,
            contentColor = colors.contentColor,
        )
        if (onClick != null) {
            MiuixCard(
                modifier = modifier,
                cornerRadius = cornerRadius,
                insideMargin = insideMargin,
                colors = miuixColors,
                pressFeedbackType = pressFeedbackType,
                showIndication = showIndication,
                holdDownState = holdDownState,
                onClick = onClick,
            ) {
                content()
            }
        } else {
            MiuixCard(
                modifier = modifier,
                cornerRadius = cornerRadius,
                insideMargin = insideMargin,
                colors = miuixColors,
            ) {
                content()
            }
        }
        return
    }
    val shape = RoundedCornerShape(cornerRadius)
    if (onClick != null && showIndication) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = colors.color,
            contentColor = colors.contentColor,
        ) {
            Column(modifier = Modifier.padding(insideMargin), content = content)
        }
    } else if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            modifier = modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
            shape = shape,
            color = colors.color,
            contentColor = colors.contentColor,
        ) {
            Column(modifier = Modifier.padding(insideMargin), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = colors.color,
            contentColor = colors.contentColor,
        ) {
            Column(modifier = Modifier.padding(insideMargin), content = content)
        }
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = if (LocalUiMode.current == UiMode.Miuix) 0.75.dp else 1.dp,
    color: Color = if (LocalUiMode.current == UiMode.Miuix) {
        top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.dividerLine
    } else {
        MaterialTheme.colorScheme.outlineVariant
    },
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixHorizontalDivider(modifier = modifier, thickness = thickness, color = color)
        return
    }
    MaterialHorizontalDivider(modifier = modifier, thickness = thickness, color = color)
}

@Composable
fun BasicComponent(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixBasicComponent(
            title = title,
            modifier = modifier,
            summary = summary,
            startAction = startAction,
            endActions = endActions,
            onClick = onClick,
            enabled = enabled,
        )
        return
    }
    val supporting: (@Composable () -> Unit)? = summary?.let { { Text(it) } }
    val trailing: (@Composable () -> Unit)? = endActions?.let { actions ->
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
    ListItem(
        modifier = if (onClick != null) {
            modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
        } else {
            modifier.fillMaxWidth()
        },
        headlineContent = { Text(title) },
        supportingContent = supporting,
        leadingContent = startAction,
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

// === 输入框 ===

@Composable
fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    insideMargin: DpSize? = null,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    inputTransformation: InputTransformation? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onKeyboardAction: (() -> Unit)? = null,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTextField(
            state = state,
            modifier = modifier,
            insideMargin = insideMargin ?: DpSize(16.dp, 16.dp),
            label = label,
            useLabelAsPlaceholder = useLabelAsPlaceholder,
            enabled = enabled,
            inputTransformation = inputTransformation,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            onKeyboardAction = onKeyboardAction?.let { callback -> { callback() } },
        )
        return
    }
    MaterialTextField(
        state = state,
        modifier = modifier,
        enabled = enabled,
        shape = TextFieldDefaults.roundedShape,
        label = if (label.isNotEmpty() && !useLabelAsPlaceholder) {
            { Text(label) }
        } else {
            null
        },
        placeholder = if (label.isNotEmpty() && useLabelAsPlaceholder) {
            { Text(label) }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        inputTransformation = inputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction?.let { callback -> KeyboardActionHandler { callback() } },
    )
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: DpSize? = null,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            insideMargin = insideMargin ?: DpSize(16.dp, 16.dp),
            label = label,
            useLabelAsPlaceholder = useLabelAsPlaceholder,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
        )
        return
    }
    MaterialTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        shape = TextFieldDefaults.roundedShape,
        label = if (label.isNotEmpty() && !useLabelAsPlaceholder) {
            { Text(label) }
        } else {
            null
        },
        placeholder = if (label.isNotEmpty() && useLabelAsPlaceholder) {
            { Text(label) }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions.Default,
    )
}

// === 下拉刷新 / Tab / 加载指示（与 KernelSU Material 页一致） ===

@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    refreshTexts: List<String> = emptyList(),
    content: @Composable () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            contentPadding = contentPadding,
            refreshTexts = refreshTexts,
            content = content,
        )
        return
    }
    val state = rememberPullToRefreshState()
    MaterialPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            MaterialPullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = contentPadding.calculateTopPadding()),
            )
        },
    ) {
        content()
    }
}

@Composable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = modifier,
        )
        return
    }
    ExpressiveTabRow(
        selectedTabIndex = selectedTabIndex,
        tabs = tabs,
        onTabClick = onTabSelected,
        modifier = modifier,
    )
}

/** 与 KernelSU `ExpressiveTabRow` 相同：PrimaryTabRow + 选中药丸 */
@Composable
private fun ExpressiveTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeIndex = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    MaterialPrimaryTabRow(
        selectedTabIndex = safeIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = safeIndex == index
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .selectable(
                        selected = selected,
                        interactionSource = interactionSource,
                        indication = null,
                        role = androidx.compose.ui.semantics.Role.Tab,
                        onClick = { onTabClick(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(CircleShape)
                        .indication(interactionSource, androidx.compose.material3.ripple()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (selected) {
                            androidx.compose.material3.LocalContentColor.current
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
fun InfiniteProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixInfiniteProgressIndicator(modifier = modifier)
        return
    }
    if (color == Color.Unspecified) {
        MaterialLoadingIndicator(modifier = modifier)
    } else {
        MaterialLoadingIndicator(modifier = modifier, color = color)
    }
}
