package com.linx3141.mexpa.ui.screen.provider

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.viewmodel.RefreshProgress
import com.linx3141.mexpa.ui.component.design.CircularProgressIndicator
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowDialog

/**
 * Provider 刷新进度对话框。不可手动关闭，调用方根据 refresh == null 控制 show。
 *
 * - `singleName != null`：显示 "正在更新 xxx…"（单条刷新）
 * - `singleName == null`：显示 "completed / total"（updateAll 并发刷新，每完成一条计数 +1）
 */
@Composable
fun ProviderRefreshDialog(
    show: Boolean,
    progress: RefreshProgress?,
) {
    WindowDialog(
        show = show,
        title = stringResource(Res.string.provider_updating_title),
        onDismissRequest = null,
        content = {
            val text = when {
                progress == null -> ""
                progress.singleName != null -> stringResource(
                    Res.string.provider_updating_single,
                    progress.singleName,
                )

                else -> stringResource(
                    Res.string.provider_updating_progress,
                    progress.completed,
                    progress.total,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(Modifier.width(16.dp))
                Text(
                    text = text,
                    fontSize = 15.sp,
                    color = MishkaColors.onSurface,
                )
            }
        },
    )
}
