@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.linx3141.mexpa.desktop

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.component.ListPopupDefaults.MenuPositionProvider
import com.linx3141.mexpa.ui.component.design.DropdownImpl
import com.linx3141.mexpa.ui.component.design.ListPopupColumn
import com.linx3141.mexpa.ui.component.design.WindowListPopup
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

/**
 * Compose Desktop 的默认文本右键菜单会退回旧 AWT JPopupMenu。复用页面现有的
 * WindowListPopup + DropdownImpl，让 Material 和 Miuix 分别走各自的标准下拉样式。
 */
val MishkaContextMenuRepresentation: ContextMenuRepresentation = object : ContextMenuRepresentation {
    @Composable
    override fun Representation(
        state: ContextMenuState,
        items: () -> List<ContextMenuItem>,
    ) {
        val open = state.status as? ContextMenuState.Status.Open ?: return
        val entries = items()
        if (entries.isEmpty()) return

        val density = LocalDensity.current
        val x = with(density) { open.rect.left.toDp() }
        val y = with(density) { open.rect.bottom.toDp() }

        // 0 尺寸锚点只负责把标准 Dropdown 放到点击位置；菜单本体仍由 WindowListPopup 绘制。
        Box(
            modifier = Modifier
                .offset(x = x, y = y)
                .size(0.dp),
        ) {
            WindowListPopup(
                show = true,
                popupPositionProvider = MenuPositionProvider,
                alignment = PopupPositionProvider.Align.TopStart,
                onDismissRequest = { state.status = ContextMenuState.Status.Closed },
            ) {
                ListPopupColumn {
                    entries.forEachIndexed { index, item ->
                        val enabled = item.isEnabled()
                        Box(Modifier.alpha(if (enabled) 1f else 0.38f)) {
                            DropdownImpl(
                                text = item.label,
                                optionSize = entries.size,
                                isSelected = false,
                                index = index,
                                onSelectedIndexChange = {
                                    if (!enabled) return@DropdownImpl
                                    state.status = ContextMenuState.Status.Closed
                                    item.onClick()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}


/** ContextMenuItem.enabled 是 foundation 的 internal 属性，公开 API 只暴露 mangled accessor。 */
private fun ContextMenuItem.isEnabled(): Boolean =
    runCatching {
        javaClass.getMethod("contextMenuItemBaseClassEnabled").invoke(this) as Boolean
    }.getOrDefault(true)
