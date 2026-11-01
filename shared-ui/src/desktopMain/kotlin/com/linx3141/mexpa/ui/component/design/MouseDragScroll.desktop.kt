package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/** 只用来把 [NestedScrollDispatcher] 挂进 nested scroll 链；自身不消费任何滚动。 */
private val NoOpNestedScrollConnection = object : NestedScrollConnection {}

/**
 * 桌面端没有触摸拖动，"按住左键拖动内容滚动" 需要自己实现：用 [pointerInput] 收到鼠标位移，
 * 再经 [NestedScrollDispatcher] 交给祖先（顶部大标题折叠等），不向 AWT 注入任何合成事件。
 *
 * 每一步的交接与 foundation `scrollable` 内部的 performScroll 一致：
 * nested scroll 的 delta 是「指针位移」口径（正 = 向下拖），[ScrollableState.dispatchRawDelta]
 * 是「滚动位移」口径（正 = 向前滚动），两者反号。
 * 拖动结束时补一次 postFling，让顶部大标题按当前位置吸附到展开或收起。
 */
@Composable
actual fun Modifier.mouseDragScroll(state: ScrollableState, enabled: Boolean): Modifier {
    if (!enabled) return this
    val dispatcher = remember { NestedScrollDispatcher() }
    val scope = rememberCoroutineScope()
    val pageDragTarget = LocalPageDragScrollTarget.current
    val applier: (Float) -> Unit = remember(state, dispatcher) {
        { fingerDelta -> applyPageDragScroll(state, dispatcher, fingerDelta) }
    }
    DisposableEffect(pageDragTarget, applier) {
        pageDragTarget?.attach(applier)
        onDispose { pageDragTarget?.detach(applier) }
    }
    val settleTopBar: () -> Unit = {
        scope.launch { dispatcher.dispatchPostFling(Velocity.Zero, Velocity.Zero) }
    }
    return this
        .nestedScroll(NoOpNestedScrollConnection, dispatcher)
        .pointerInput(state, dispatcher) {
            detectVerticalDragGestures(
                onDragEnd = { settleTopBar() },
                onDragCancel = { settleTopBar() },
            ) { change, dragDelta ->
                // 消费掉拖动，clickable 的 tap 检测据此取消，拖动经过的控件不会被"点击"
                change.consume()
                applyPageDragScroll(state, dispatcher, dragDelta)
            }
        }
}

/**
 * 把一个「指针位移」delta 走完 standard nested scroll 三段：pre → self → post。
 * 供列表自身拖动与顶栏大标题区域拖动共用。
 */
internal fun applyPageDragScroll(
    state: ScrollableState,
    dispatcher: NestedScrollDispatcher,
    fingerDelta: Float,
) {
    val preConsumed = dispatcher.dispatchPreScroll(
        available = Offset(0f, fingerDelta),
        source = NestedScrollSource.UserInput,
    )
    val availableAfterPreScroll = fingerDelta - preConsumed.y
    val consumedScroll = state.dispatchRawDelta(-availableAfterPreScroll)
    val consumedDelta = -consumedScroll
    dispatcher.dispatchPostScroll(
        consumed = Offset(0f, consumedDelta),
        available = Offset(0f, availableAfterPreScroll - consumedDelta),
        source = NestedScrollSource.UserInput,
    )
}
