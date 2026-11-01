package com.linx3141.mexpa.ui.component

import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.linx3141.mexpa.ui.component.design.MishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.SmallTopAppBar
import com.linx3141.mexpa.ui.component.design.TopAppBar
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import com.linx3141.mexpa.ui.util.rememberIsWideScreen
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏自适应：宽屏用固定的 [SmallTopAppBar]（侧边 NavigationRail 取代底栏后纵向空间紧张，标题栏不再折叠），
 * 手机用可折叠的大标题 [TopAppBar]。参数面覆盖项目内所有 TopAppBar 调用点。
 *
 * 两个实现都来自 `ui.component.design`：Miuix 模式下逐参数委托 miuix，Material 模式下换成
 * M3 Expressive 的大标题顶栏。
 */
@Composable
fun AdaptiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = if (LocalUiMode.current == UiMode.Miuix) {
        MishkaColors.surface
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
    },
    scrollBehavior: MishkaScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    if (rememberIsWideScreen()) {
        SmallTopAppBar(
            title = title,
            modifier = modifier,
            color = color,
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    } else {
        TopAppBar(
            title = title,
            modifier = modifier,
            color = color,
            scrollBehavior = scrollBehavior,
            navigationIcon = navigationIcon,
            actions = actions,
            bottomContent = bottomContent,
        )
    }
}
