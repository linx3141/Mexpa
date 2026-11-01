package com.linx3141.mexpa.desktop

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.theme.MishkaColors
import java.awt.Frame

/** 桌面窗口三键：注入到各页面 TopAppBar 的 actions 尾部，不再单独绘制标题栏。 */
@Composable
fun WindowControlActions(frame: Frame, onClose: () -> Unit) {
    IconButton(
        onClick = { frame.extendedState = frame.extendedState or Frame.ICONIFIED },
        minWidth = 32.dp,
        minHeight = 32.dp,
    ) {
        Icon(
            imageVector = Icons.Rounded.Remove,
            contentDescription = "最小化",
            modifier = Modifier.size(16.dp),
            tint = MishkaColors.onSurface,
        )
    }
    IconButton(
        onClick = { toggleMaximize(frame) },
        minWidth = 32.dp,
        minHeight = 32.dp,
    ) {
        Icon(
            imageVector = Icons.Rounded.CropSquare,
            contentDescription = "最大化",
            modifier = Modifier.size(14.dp),
            tint = MishkaColors.onSurface,
        )
    }
    IconButton(
        onClick = onClose,
        minWidth = 32.dp,
        minHeight = 32.dp,
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "关闭",
            modifier = Modifier.size(16.dp),
            tint = MishkaColors.onSurface,
        )
    }
}

private fun toggleMaximize(frame: Frame) {
    frame.extendedState = if (frame.extendedState and Frame.MAXIMIZED_BOTH != 0) {
        Frame.NORMAL
    } else {
        Frame.MAXIMIZED_BOTH
    }
}
