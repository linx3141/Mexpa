@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.DropdownColors
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.ListPopupColumn as MiuixListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.popup.WindowDropdownDialog as MiuixWindowDropdownDialog
import top.yukonga.miuix.kmp.window.WindowListPopup as MiuixWindowListPopup

/**
 * 剩余 miuix 菜单/单选弹窗的 Material 适配。Miuix 模式逐参数委托 miuix；
 * Material 模式换成 M3 的 DropdownMenu / AlertDialog + RadioButton 行。
 */

@Composable
fun ListPopupColumn(
    content: @Composable () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixListPopupColumn { content() }
        return
    }
    // 直接发射到 DropdownMenuGroup 的 ColumnScope，保持 KSU 的分组 item shape
    content()
}

@Composable
fun DropdownImpl(
    text: String,
    optionSize: Int,
    isSelected: Boolean,
    index: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        top.yukonga.miuix.kmp.basic.DropdownImpl(
            text = text,
            optionSize = optionSize,
            isSelected = isSelected,
            index = index,
            onSelectedIndexChange = onSelectedIndexChange,
        )
        return
    }
    SelectableDropdownMenuItem(
        text = { Text(text) },
        selected = isSelected,
        onClick = { onSelectedIndexChange(index) },
        shapes = MenuDefaults.itemShape(index = index, count = optionSize),
        selectedLeadingIcon = {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
            )
        },
    )
}

@Composable
fun WindowListPopup(
    show: Boolean,
    popupPositionProvider: PopupPositionProvider,
    alignment: PopupPositionProvider.Align,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixWindowListPopup(
            show = show,
            popupPositionProvider = popupPositionProvider,
            alignment = alignment,
            onDismissRequest = onDismissRequest,
        ) {
            content()
        }
        return
    }
    DropdownMenuPopup(
        expanded = show,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
            content()
        }
    }
}

@Composable
fun WindowDropdownDialog(
    entry: DropdownEntry,
    title: String,
    dialogButtonString: String,
    show: Boolean,
    onDismiss: () -> Unit,
    onDismissFinished: (() -> Unit)? = null,
    dropdownColors: DropdownColors = top.yukonga.miuix.kmp.basic.DropdownDefaults.dialogDropdownColors(),
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixWindowDropdownDialog(
            entry = entry,
            title = title,
            dialogButtonString = dialogButtonString,
            show = show,
            onDismiss = onDismiss,
            onDismissFinished = onDismissFinished ?: {},
            dropdownColors = dropdownColors,
        )
        return
    }
    if (!show) return
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                }
                entry.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = item.selected,
                                enabled = item.enabled,
                                role = Role.RadioButton,
                                onClick = {
                                    item.onClick?.invoke()
                                    onDismiss()
                                },
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        RadioButton(selected = item.selected, onClick = null, enabled = item.enabled)
                    }
                }
                if (dialogButtonString.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Text(dialogButtonString)
                        }
                    }
                }
            }
        }
    }
}
