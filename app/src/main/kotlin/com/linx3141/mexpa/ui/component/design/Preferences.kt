@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.preference.ArrowPreference as MiuixArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference as MiuixOverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation as MiuixRadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference as MiuixRadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference as MiuixSwitchPreference

/**
 * 主题无关的语义化偏好行。
 *
 * 每个函数在 Miuix 模式下原样委托 miuix 的同名组件（默认外观零变化），在 Material 模式下
 * 用 M3 Expressive 的 ListItem + 对应控件重绘。页面只依赖这里的签名，因此同一份页面代码
 * 在两套风格下得到各自原生的列表行，而结构与交互保持一致——对应 KernelSU 的
 * `SwitchPreference` / `SegmentedSwitchItem` 这一层映射。
 */

@Composable
fun SwitchPreference(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    insideMargin: PaddingValues? = null,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        if (insideMargin != null) {
            MiuixSwitchPreference(
                checked = checked,
                onCheckedChange = onCheckedChange,
                title = title,
                modifier = modifier,
                summary = summary,
                startAction = startAction,
                endActions = endActions ?: {},
                enabled = enabled,
                holdDownState = holdDownState,
                insideMargin = insideMargin,
            )
        } else {
            MiuixSwitchPreference(
                checked = checked,
                onCheckedChange = onCheckedChange,
                title = title,
                modifier = modifier,
                summary = summary,
                startAction = startAction,
                endActions = endActions ?: {},
                enabled = enabled,
                holdDownState = holdDownState,
            )
        }
        return
    }
    val haptic = LocalHapticFeedback.current
    MaterialPreferenceRow(
        title = title,
        summary = summary,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onCheckedChange(it)
            },
        ),
        enabled = enabled,
        onClick = null,
        leading = startAction,
        trailing = {
            MishkaSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    onClick: () -> Unit,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable RowScope.() -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues? = null,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        if (insideMargin != null) {
            MiuixArrowPreference(
                title = title,
                modifier = modifier,
                summary = summary,
                startAction = startAction,
                endActions = endActions ?: {},
                bottomAction = bottomAction,
                onClick = onClick,
                enabled = enabled,
                holdDownState = holdDownState,
                insideMargin = insideMargin,
            )
        } else {
            MiuixArrowPreference(
                title = title,
                modifier = modifier,
                summary = summary,
                startAction = startAction,
                endActions = endActions ?: {},
                bottomAction = bottomAction,
                onClick = onClick,
                enabled = enabled,
                holdDownState = holdDownState,
            )
        }
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        MaterialPreferenceRow(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = onClick,
            leading = startAction,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    endActions?.invoke(this)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        bottomAction?.let { action ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
            ) {
                action()
            }
        }
    }
}

@Composable
fun OverlayDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixOverlayDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            enabled = enabled,
            onSelectedIndexChange = onSelectedIndexChange,
        )
        return
    }
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    var anchorOffset by remember { mutableStateOf(IntOffset.Zero) }
    val safeIndex = if (items.isEmpty()) -1 else selectedIndex.coerceIn(0, items.lastIndex)
    Box(modifier = Modifier.trackPressPosition { anchorOffset = it.round() }) {
        MaterialPreferenceRow(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                expanded = true
            },
            trailing = {
                Text(
                    text = if (items.isNotEmpty() && safeIndex >= 0) items[safeIndex] else "",
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(0.3f),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
        )
        OffsetAnchoredMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchorOffset = anchorOffset,
        ) {
            items.forEachIndexed { index, label ->
                SelectableDropdownMenuItem(
                    text = { Text(label) },
                    selected = index == safeIndex,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        if (index in items.indices) {
                            onSelectedIndexChange(index)
                        }
                        expanded = false
                    },
                    shapes = MenuDefaults.itemShape(index = index, count = items.size),
                    selectedLeadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun RadioButtonPreference(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    radioButtonLocation: RadioButtonLocation = RadioButtonLocation.Start,
    enabled: Boolean = true,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixRadioButtonPreference(
            title = title,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            summary = summary,
            radioButtonLocation = radioButtonLocation.toMiuix(),
            enabled = enabled,
        )
        return
    }
    val radio: @Composable () -> Unit = {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
    }
    MaterialPreferenceRow(
        title = title,
        summary = summary,
        modifier = modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
        enabled = enabled,
        onClick = null,
        leading = if (radioButtonLocation == RadioButtonLocation.Start) radio else null,
        trailing = if (radioButtonLocation == RadioButtonLocation.End) radio else null,
    )
}

enum class RadioButtonLocation {
    Start,
    End;

    internal fun toMiuix(): MiuixRadioButtonLocation = when (this) {
        Start -> MiuixRadioButtonLocation.Start
        End -> MiuixRadioButtonLocation.End
    }
}

/** Material 模式下的标准列表行：无背景（由 CardSegment 提供底色），行高/字号跟随 M3 ListItem。 */
@Composable
internal fun MaterialPreferenceRow(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val supporting: (@Composable () -> Unit)? = summary?.let {
        {
            Text(
                text = it,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
    if (onClick != null) {
        ListItem(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            verticalAlignment = Alignment.CenterVertically,
            content = titleContent,
        )
    } else {
        ListItem(
            headlineContent = titleContent,
            modifier = modifier.fillMaxWidth(),
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supporting,
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
        )
    }
}
