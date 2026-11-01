package com.linx3141.mexpa.ui.screen.proxy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.ui.platform.IconLoader
import com.linx3141.mexpa.viewmodel.ProxyUiState
import com.linx3141.mexpa.viewmodel.ProxyViewModel
import com.linx3141.mexpa.ui.screen.proxy.LocalProxyIconLoader
import com.linx3141.mexpa.ui.screen.proxy.ProxyIconLoader

/** Android 适配层：收集 ProxyViewModel 状态，布局在 shared-ui 的 ProxyScreenContent。 */
@Composable
fun ProxyScreen(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    viewModel: ProxyViewModel? = null,
) {
    val uiState = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: ProxyUiState()
    val sortOption = viewModel?.sortOption?.collectAsStateWithLifecycle()?.value ?: 0
    val singleColumn = viewModel?.singleColumn?.collectAsStateWithLifecycle()?.value == true
    val showGroupIcons = viewModel?.showGroupIcons?.collectAsStateWithLifecycle()?.value ?: true
    val iconVersion by IconLoader.iconVersion.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalProxyIconLoader provides ProxyIconLoader(
            version = iconVersion,
            load = { icon -> IconLoader.loadIcon(icon) },
        ),
    ) {
        ProxyScreenContent(
            modifier = modifier,
            bottomPadding = bottomPadding,
            uiState = uiState,
            sortOption = sortOption,
            singleColumn = singleColumn,
            showGroupIcons = showGroupIcons,
            onUpdateSortOption = { viewModel?.updateSortOption(it) },
            onUpdateSingleColumn = { viewModel?.updateSingleColumn(it) },
            onUpdateShowGroupIcons = { viewModel?.updateShowGroupIcons(it) },
            onSelectProxy = { group, proxy -> viewModel?.selectProxy(group, proxy) },
            onTestGroupDelay = { viewModel?.testGroupDelay(it) },
            onTestNodeDelay = { viewModel?.testNodeDelay(it) },
            onUnfixProxy = { viewModel?.unfixProxy(it) },
        )
    }
}
