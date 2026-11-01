package com.linx3141.mexpa.desktop

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

/**
 * 无边框窗口的自定义缩放命中区。
 *
 * KWin 对 undecorated XWayland 窗口只留极窄的系统缩放边，实际几乎点不中；
 * 这里在内容四周叠透明条/角，用 8dp 命中区接管缩放（与常见桌面窗口的边框手感一致）。
 * 拖动使用 AWT 全局指针坐标，避免窗口移动后局部坐标反噬。
 */
@Composable
fun BoxScope.WindowResizeOverlay(window: Window, enabled: Boolean) {
    if (!enabled) return
    val edge = 8.dp
    val corner = 14.dp

    ResizeHandle(
        modifier = Modifier.align(Alignment.TopStart).width(corner).height(corner),
        cursor = Cursor.NW_RESIZE_CURSOR,
        window = window,
        resizeLeft = true,
        resizeTop = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.TopEnd).width(corner).height(corner),
        cursor = Cursor.NE_RESIZE_CURSOR,
        window = window,
        resizeRight = true,
        resizeTop = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.BottomStart).width(corner).height(corner),
        cursor = Cursor.SW_RESIZE_CURSOR,
        window = window,
        resizeLeft = true,
        resizeBottom = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.BottomEnd).width(corner).height(corner),
        cursor = Cursor.SE_RESIZE_CURSOR,
        window = window,
        resizeRight = true,
        resizeBottom = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(edge),
        cursor = Cursor.N_RESIZE_CURSOR,
        window = window,
        resizeTop = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(edge),
        cursor = Cursor.S_RESIZE_CURSOR,
        window = window,
        resizeBottom = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.CenterStart).width(edge).fillMaxHeight(),
        cursor = Cursor.W_RESIZE_CURSOR,
        window = window,
        resizeLeft = true,
    )
    ResizeHandle(
        modifier = Modifier.align(Alignment.CenterEnd).width(edge).fillMaxHeight(),
        cursor = Cursor.E_RESIZE_CURSOR,
        window = window,
        resizeRight = true,
    )
}

@Composable
private fun ResizeHandle(
    modifier: Modifier,
    cursor: Int,
    window: Window,
    resizeLeft: Boolean = false,
    resizeTop: Boolean = false,
    resizeRight: Boolean = false,
    resizeBottom: Boolean = false,
) {
    Box(
        modifier = modifier
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(cursor)))
            .pointerInput(window, resizeLeft, resizeTop, resizeRight, resizeBottom) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startPointer = MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
                    val startBounds = window.bounds
                    val minWidth = window.minimumSize.width.coerceAtLeast(320)
                    val minHeight = window.minimumSize.height.coerceAtLeast(240)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val pointer = MouseInfo.getPointerInfo()?.location ?: break
                        val dx = pointer.x - startPointer.x
                        val dy = pointer.y - startPointer.y
                        var x = startBounds.x
                        var y = startBounds.y
                        var w = startBounds.width
                        var h = startBounds.height
                        if (resizeLeft) {
                            val newW = (startBounds.width - dx).coerceAtLeast(minWidth)
                            x = startBounds.x + startBounds.width - newW
                            w = newW
                        } else if (resizeRight) {
                            w = (startBounds.width + dx).coerceAtLeast(minWidth)
                        }
                        if (resizeTop) {
                            val newH = (startBounds.height - dy).coerceAtLeast(minHeight)
                            y = startBounds.y + startBounds.height - newH
                            h = newH
                        } else if (resizeBottom) {
                            h = (startBounds.height + dy).coerceAtLeast(minHeight)
                        }
                        window.setBounds(x, y, w, h)
                    }
                }
            },
    ) {}
}
