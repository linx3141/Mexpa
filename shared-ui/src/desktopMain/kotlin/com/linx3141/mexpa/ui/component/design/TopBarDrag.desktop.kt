package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

/**
 * 无边框窗口的顶栏拖动。
 *
 * - 收起后的固定条（高度 [collapsedHeight]）才是窗口标题栏：在这一条里按住拖动移动窗口；
 * - 大标题展开区不属于标题栏：拖动它走页面滚动（与列表拖动同一套 nested scroll 驱动），
 *   否则展开态下拖标题会把整个窗口拖走，与普通应用的行为不一致；
 * - 固定小标题栏（`largeTitleBar = false`）整条都是标题栏。
 *
 * 两个实现要点：
 * - 移动窗口用 AWT 全局指针坐标算位移，不能用 Compose 的 dragAmount（窗口内局部坐标）：
 *   窗口一移动，指针在窗口内的坐标就反向变化，局部 delta 会自反馈——抖动、拖不动或跳一下。
 * - 拖动在 [PointerEventPass.Initial] 消费：顶栏内部还有大标题自己可折叠的拖拽手势
 *   （Material/Miuix 的 appBar drag），不先消费会同时触发顶栏折叠。
 */
@Composable
fun rememberTopBarDragModifier(
    window: Window,
    largeTitleBar: Boolean,
    collapsedHeight: Dp,
): Modifier {
    val density = LocalDensity.current
    val pageDragTarget = LocalPageDragScrollTarget.current
    val stripHeightPx = with(density) { collapsedHeight.toPx() }
    return remember(window, largeTitleBar, stripHeightPx, pageDragTarget) {
        Modifier.pointerInput(window, largeTitleBar, stripHeightPx, pageDragTarget) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val touchSlop = viewConfiguration.touchSlop
                // 固定小标题栏整条都算标题栏；大标题栏只有顶部固定条算
                val dragsWindow = !largeTitleBar || down.position.y <= stripHeightPx
                var dragging = false
                var pointerOrigin: Point? = null
                var windowOrigin: Point? = null
                var lastPointer: Point? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (!dragging) {
                        if ((change.position - down.position).getDistance() <= touchSlop) continue
                        val pointer = MouseInfo.getPointerInfo()?.location ?: break
                        dragging = true
                        lastPointer = pointer
                        if (dragsWindow) {
                            pointerOrigin = pointer
                            windowOrigin = window.location
                        }
                        change.consume()
                        continue
                    }
                    change.consume()
                    val pointer = MouseInfo.getPointerInfo()?.location ?: continue
                    if (dragsWindow) {
                        val startPointer = pointerOrigin ?: break
                        val startWindow = windowOrigin ?: break
                        window.location = Point(
                            startWindow.x + pointer.x - startPointer.x,
                            startWindow.y + pointer.y - startPointer.y,
                        )
                    } else {
                        val previous = lastPointer ?: pointer
                        val fingerDelta = (pointer.y - previous.y).toFloat()
                        if (fingerDelta != 0f) pageDragTarget?.scrollByFingerDelta(fingerDelta)
                        lastPointer = pointer
                    }
                }
            }
        }
    }
}
