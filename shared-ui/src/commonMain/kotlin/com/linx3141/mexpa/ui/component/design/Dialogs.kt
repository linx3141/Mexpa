@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.window.WindowBottomSheet as MiuixWindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog

/**
 * 对话框/底部面板的主题适配。Miuix 模式逐参数委托 miuix；
 * Material 模式换成 M3 `BasicAlertDialog` / `ModalBottomSheet`。
 *
 * `onDismissRequest == null` 表示不可关闭（导入进度、provider 刷新），两种模式都保持该语义：
 * M3 侧关掉点击外部与返回键。
 */

@Composable
fun WindowDialog(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    insideMargin: DpSize = DpSize(24.dp, 24.dp),
    content: @Composable () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixWindowDialog(
            show = show,
            modifier = modifier,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            insideMargin = insideMargin,
            content = content,
        )
        return
    }
    if (!show) return
    BasicAlertDialog(
        onDismissRequest = onDismissRequest ?: {},
        properties = DialogProperties(
            dismissOnBackPress = onDismissRequest != null,
            dismissOnClickOutside = onDismissRequest != null,
        ),
    ) {
        Surface(
            modifier = modifier.widthIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (summary != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (title != null || summary != null) {
                    Spacer(Modifier.height(20.dp))
                }
                CompositionLocalProvider(
                    LocalDialogActions provides true,
                    LocalDismissState provides { onDismissRequest?.invoke() },
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun WindowBottomSheet(
    show: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    onDismissRequest: (() -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixWindowBottomSheet(
            show = show,
            modifier = modifier,
            title = title,
            startAction = startAction,
            endAction = endAction,
            onDismissRequest = onDismissRequest,
            content = content,
        )
        return
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keepMounted by remember { mutableStateOf(show) }
    LaunchedEffect(show) {
        if (show) {
            keepMounted = true
        } else if (keepMounted) {
            sheetState.hide()
            keepMounted = false
        }
    }
    if (!keepMounted) return
    ModalBottomSheet(
        onDismissRequest = onDismissRequest ?: {},
        modifier = modifier,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        CompositionLocalProvider(LocalDismissState provides { onDismissRequest?.invoke() }) {
            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                if (title != null || startAction != null || endAction != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (title != null) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        startAction?.invoke()
                        endAction?.invoke()
                    }
                    Spacer(Modifier.height(12.dp))
                }
                content()
            }
        }
    }
}
