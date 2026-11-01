package com.linx3141.mexpa.ui.component.design

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 页面滚动驱动入口：桌面端 `LazyColumn` 在组合期把自己的 nested scroll 驱动注册进来，
 * 顶栏大标题区域的拖动（不属于窗口标题栏）通过它来滚动页面。
 *
 * 用栈而不是单值：同一页面里可能叠着带列表的 sheet，sheet 关闭后外层列表的注册仍要保留。
 */
class PageDragScrollTarget internal constructor() {
    private val appliers = mutableListOf<(fingerDelta: Float) -> Unit>()

    internal fun attach(applier: (fingerDelta: Float) -> Unit) {
        appliers.add(applier)
    }

    internal fun detach(applier: (fingerDelta: Float) -> Unit) {
        appliers.remove(applier)
    }

    /** fingerDelta 是指针位移口径（正 = 向下拖），与 nested scroll 的 delta 同号。 */
    fun scrollByFingerDelta(fingerDelta: Float) {
        appliers.lastOrNull()?.invoke(fingerDelta)
    }
}

val LocalPageDragScrollTarget = staticCompositionLocalOf<PageDragScrollTarget?> { null }
