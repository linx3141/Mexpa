package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * 顶栏尾部的平台动作插槽。桌面端注入窗口三键，移动端保持 null。
 */
val LocalTopBarActions = compositionLocalOf<(@Composable RowScope.() -> Unit)?> { null }

/**
 * 顶栏拖动行为的平台插槽：桌面端注入「按住移动窗口 / 在展开的大标题区拖动页面」的实现，
 * 移动端不提供（null → 不加任何手势）。
 *
 * 参数 [largeTitleBar] 表示顶栏是否为可展开的大标题栏；[collapsedHeight] 是收起后固定条的
 * 高度——只有这条固定条算窗口标题栏，其下大标题区域的拖动不应该移动窗口。
 */
val LocalTopBarDragModifier =
    staticCompositionLocalOf<(@Composable (largeTitleBar: Boolean, collapsedHeight: Dp) -> Modifier)?> { null }
