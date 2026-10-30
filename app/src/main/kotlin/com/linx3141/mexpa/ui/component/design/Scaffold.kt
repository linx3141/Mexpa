@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar as MaterialTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏/页面骨架的主题适配层。
 *
 * 滚动行为统一收口在 [MishkaScrollBehavior]：页面只从 [rememberMishkaScrollBehavior] 取一个
 * 实例，同时喂给顶栏与 `LazyColumn` 的 `nestedScroll`，两套主题各自折叠自己的顶栏。
 * 这对应 KernelSU 里 `MiuixScrollBehavior` / `TopAppBarDefaults.exitUntilCollapsedScrollBehavior`
 * 在两种页面实现中的分工。
 */
class MishkaScrollBehavior internal constructor(
    internal val miuix: ScrollBehavior?,
    internal val material: TopAppBarScrollBehavior?,
) {
    val nestedScrollConnection: NestedScrollConnection
        get() = miuix?.nestedScrollConnection ?: requireNotNull(material).nestedScrollConnection

    /** 顶栏折叠比例，0 = 完全展开、1 = 收起。搜索框的顶部间距按它计算。 */
    val collapsedFraction: Float
        get() = miuix?.state?.collapsedFraction ?: material?.state?.collapsedFraction ?: 0f
}

@Composable
fun rememberMishkaScrollBehavior(canScroll: () -> Boolean = { true }): MishkaScrollBehavior {
    val uiMode = LocalUiMode.current
    val miuixBehavior = if (uiMode == UiMode.Miuix) MiuixScrollBehavior(canScroll = canScroll) else null
    val materialBehavior = if (uiMode == UiMode.Material) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = canScroll,
        )
    } else {
        null
    }
    return remember(uiMode, miuixBehavior, materialBehavior) {
        MishkaScrollBehavior(miuix = miuixBehavior, material = materialBehavior)
    }
}

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    popupHost: (@Composable () -> Unit)? = null,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    content: @Composable (PaddingValues) -> Unit,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        // popupHost 为空时必须走 miuix 默认的 MiuixPopupHost；传 {} 会让下拉浮层失去宿主
        if (popupHost != null) {
            MiuixScaffold(
                modifier = modifier,
                topBar = topBar,
                bottomBar = bottomBar,
                popupHost = popupHost,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        } else {
            MiuixScaffold(
                modifier = modifier,
                topBar = topBar,
                bottomBar = bottomBar,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        }
        return
    }
    // popupHost 承载搜索覆盖层（SearchPager）；M3 下没有 miuix 的 popup host，直接叠在 Scaffold 上
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        MaterialScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            content = content,
        )
        popupHost?.invoke()
    }
}

@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    },
    titleColor: Color = if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    },
    scrollBehavior: MishkaScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    defaultWindowInsetsPadding: Boolean = true,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTopAppBar(
            title = title,
            modifier = modifier,
            color = color,
            titleColor = titleColor,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior?.miuix,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            bottomContent = bottomContent,
        )
        return
    }
    Column {
        // 模糊时页面会传 Transparent；其余情况仍用 M3 的 surfaceContainer
        val barColor = if (color == Color.Transparent) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
        LargeFlexibleTopAppBar(
            // AOSP SettingsLib Expressive: expandedTitleMarginStart = 24dp；标题槽默认 16dp，再补 8dp
            title = { Text(title, modifier = Modifier.padding(horizontal = 8.dp)) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = if (defaultWindowInsetsPadding) {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            } else {
                WindowInsets(0, 0, 0, 0)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = barColor,
                scrolledContainerColor = barColor,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            scrollBehavior = scrollBehavior?.material,
        )
        bottomContent()
    }
}

@Composable
fun SmallTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    },
    titleColor: Color = if (LocalUiMode.current == UiMode.Miuix) {
        MiuixTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    },
    scrollBehavior: MishkaScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    defaultWindowInsetsPadding: Boolean = true,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixSmallTopAppBar(
            title = title,
            modifier = modifier,
            color = color,
            titleColor = titleColor,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior?.miuix,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            bottomContent = bottomContent,
        )
        return
    }
    Column {
        val barColor = if (color == Color.Transparent) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
        MaterialTopAppBar(
            title = { Text(title, modifier = Modifier.padding(horizontal = 8.dp)) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = if (defaultWindowInsetsPadding) {
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            } else {
                WindowInsets(0, 0, 0, 0)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = barColor,
                scrolledContainerColor = barColor,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            scrollBehavior = scrollBehavior?.material,
        )
        bottomContent()
    }
}
