package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 鼠标左键拖动滚动的平台入口。
 *
 * 移动端的触摸拖动本来就由 `LazyColumn` 内部的 `scrollable` 处理（含 nested scroll），
 * Android 的 actual 直接返回原 Modifier；桌面端见 desktopMain 的 actual 实现。
 */
@Composable
expect fun Modifier.mouseDragScroll(state: ScrollableState, enabled: Boolean): Modifier
