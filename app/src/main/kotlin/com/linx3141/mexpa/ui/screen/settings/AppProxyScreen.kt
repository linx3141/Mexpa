package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.R



import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.UiMode
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.linx3141.mexpa.platform.showToast
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.component.ListPopupDefaults.MenuPositionProvider
import com.linx3141.mexpa.ui.component.SearchBarFake
import com.linx3141.mexpa.ui.component.SearchBox
import com.linx3141.mexpa.ui.component.SearchPager
import com.linx3141.mexpa.ui.component.SearchResultStatusEffect
import com.linx3141.mexpa.ui.component.SearchStatus
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.rememberSearchBarTopPadding
import com.linx3141.mexpa.ui.component.rememberSearchScreenStatus
import com.linx3141.mexpa.ui.platform.AppIcon
import com.linx3141.mexpa.ui.platform.getPlainText
import com.linx3141.mexpa.ui.platform.setPlainText
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.viewmodel.AppProxyMode
import com.linx3141.mexpa.viewmodel.AppProxyViewModel
import com.linx3141.mexpa.ui.component.design.BasicComponent
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.Checkbox
import com.linx3141.mexpa.ui.component.design.DropdownImpl
import com.linx3141.mexpa.ui.component.design.HorizontalDivider
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.MishkaSearchAppBar
import com.linx3141.mexpa.ui.component.design.InfiniteProgressIndicator
import com.linx3141.mexpa.ui.component.design.ListPopupColumn
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import com.linx3141.mexpa.ui.component.design.PullToRefresh
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SegmentedItem
import com.linx3141.mexpa.ui.component.design.SegmentedListItem
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import com.linx3141.mexpa.ui.component.design.RadioButtonLocation
import com.linx3141.mexpa.ui.component.design.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.linx3141.mexpa.ui.component.design.WindowListPopup

@Composable
fun AppProxyScreen(
    viewModel: AppProxyViewModel,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = rememberMishkaScrollBehavior()
    val showPopup = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()

    val appliedMsg = stringResource(R.string.app_proxy_applied)
    val isMaterial = LocalUiMode.current == UiMode.Material
    val searchStatusState = rememberSearchScreenStatus(stringResource(R.string.app_proxy_search))
    var searchStatus by searchStatusState
    var materialSearchText by remember { mutableStateOf("") }

    // 搜索过滤（直接传入 searchText，避免组合期间写入 ViewModel 状态）
    val searchText = if (isMaterial) materialSearchText else searchStatus.searchText

    // 同步搜索词到 ViewModel（selectAll/invertSelection 等操作需要）
    LaunchedEffect(searchText) {
        viewModel.setSearchQuery(searchText)
    }

    // 使用 ViewModel 缓存的 filteredAppsFlow（不依赖 selectedPackages，勾选不引起排序重算）
    val filteredApps by viewModel.filteredAppsFlow.collectAsStateWithLifecycle()
    if (!isMaterial) SearchResultStatusEffect(searchStatusState, filteredApps.isEmpty())

    val dynamicTopPadding = rememberSearchBarTopPadding(scrollBehavior)

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    val backButton: @Composable () -> Unit = {
                            MishkaBackButton(
                                onClick = {
                                    if (viewModel.applyIfChanged()) {
                                        showToast(appliedMsg)
                                    }
                                    onBack()
                                },
                                contentDescription = stringResource(R.string.common_back),
                            )
                        }
    val actionsContent: @Composable () -> Unit = {
                            // 仅在非「允许所有应用」模式下显示菜单
                            if (uiState.mode != AppProxyMode.AllowAll) {
                                IconButton(
                                    onClick = { showPopup.value = true },
                                    holdDownState = showPopup.value,
                                ) {
                                    Icon(
                                        imageVector = AppIcons.MoreCircle,
                                        contentDescription = stringResource(R.string.common_more),
                                        tint = MishkaColors.onSurface,
                                    )
                                }

                                // 顶栏下拉菜单
                                WindowListPopup(
                                    show = showPopup.value,
                                    popupPositionProvider = MenuPositionProvider,
                                    alignment = PopupPositionProvider.Align.TopEnd,
                                    onDismissRequest = { showPopup.value = false },
                                ) {
                                    ListPopupColumn {
                                        DropdownImpl(
                                            text = stringResource(R.string.app_proxy_select_all),
                                            optionSize = 6,
                                            isSelected = false,
                                            index = 0,
                                            onSelectedIndexChange = {
                                                viewModel.selectAll()
                                                showPopup.value = false
                                            },
                                        )
                                        DropdownImpl(
                                            text = stringResource(R.string.app_proxy_deselect_all),
                                            optionSize = 6,
                                            isSelected = false,
                                            index = 1,
                                            onSelectedIndexChange = {
                                                viewModel.deselectAll()
                                                showPopup.value = false
                                            },
                                        )
                                        DropdownImpl(
                                            text = stringResource(R.string.app_proxy_invert),
                                            optionSize = 6,
                                            isSelected = false,
                                            index = 2,
                                            onSelectedIndexChange = {
                                                viewModel.invertSelection()
                                                showPopup.value = false
                                            },
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 4.dp),
                                            thickness = 1.5.dp,
                                        )
                                        DropdownImpl(
                                            text = if (uiState.showSystemApps) stringResource(R.string.app_proxy_hide_system) else stringResource(
                                                R.string.app_proxy_show_system
                                            ),
                                            optionSize = 6,
                                            isSelected = uiState.showSystemApps,
                                            index = 3,
                                            onSelectedIndexChange = {
                                                viewModel.setShowSystemApps(!uiState.showSystemApps)
                                                showPopup.value = false
                                            },
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .padding(horizontal = 20.dp, vertical = 4.dp),
                                            thickness = 1.5.dp,
                                        )
                                        DropdownImpl(
                                            text = stringResource(R.string.app_proxy_import),
                                            optionSize = 6,
                                            isSelected = false,
                                            index = 4,
                                            onSelectedIndexChange = {
                                                clipboardScope.launch {
                                                    val text = clipboard.getPlainText()
                                                    if (!text.isNullOrBlank()) {
                                                        viewModel.importPackages(text)
                                                    }
                                                }
                                                showPopup.value = false
                                            },
                                        )
                                        DropdownImpl(
                                            text = stringResource(R.string.app_proxy_export),
                                            optionSize = 6,
                                            isSelected = false,
                                            index = 5,
                                            onSelectedIndexChange = {
                                                val exported = viewModel.exportPackages()
                                                if (exported.isNotEmpty()) {
                                                    clipboardScope.launch { clipboard.setPlainText(exported) }
                                                }
                                                showPopup.value = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
    val materialSearchResults: @Composable (Dp) -> Unit = { bottomPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp + bottomPadding,
            ),
        ) {
            if (filteredApps.isEmpty()) {
                item(key = "search_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.app_proxy_no_match),
                            fontSize = 16.sp,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredApps,
                    key = { _, app -> app.packageName },
                ) { index, app ->
                    SegmentedItem(index = index, count = filteredApps.size) {
                        AppItem(
                            appName = app.appName,
                            packageName = app.packageName,
                            isSelected = app.packageName in uiState.selectedPackages,
                            onToggle = { viewModel.toggleApp(app.packageName) },
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (isMaterial) {
                MishkaSearchAppBar(
                    title = { Text(stringResource(R.string.app_proxy_title)) },
                    searchText = materialSearchText,
                    onSearchTextChange = { materialSearchText = it },
                    onClearClick = { materialSearchText = "" },
                    navigationIcon = { backButton() },
                    actions = { actionsContent() },
                    scrollBehavior = scrollBehavior.material,
                    // 未输入搜索词时展示全部应用（KSU SearchAppBar 的 defaultContent 语义）
                    defaultContent = { bottomPadding, _ -> materialSearchResults(bottomPadding) },
                    searchContent = { bottomPadding, _ -> materialSearchResults(bottomPadding) },
                )
            } else {
                BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                    searchStatus.TopAppBarAnim(
                        backgroundColor = if (blurActive) Color.Transparent else MishkaColors.surface,
                    ) {
                        AdaptiveTopAppBar(
                            title = stringResource(R.string.app_proxy_title),
                            color = barColor,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = { backButton() },
                            actions = { actionsContent() },
                            bottomContent = {
                                Box(
                                    modifier = Modifier
                                        .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                        .onGloballyPositioned { coordinates ->
                                            with(density) {
                                                val newOffsetY = coordinates.positionInWindow().y.toDp()
                                                if (searchStatus.offsetY != newOffsetY) {
                                                    searchStatus = searchStatus.copy(offsetY = newOffsetY)
                                                }
                                            }
                                        }
                                        .then(
                                            if (searchStatus.isCollapsed()) {
                                                Modifier.pointerInput(Unit) {
                                                    detectTapGestures {
                                                        searchStatus = searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                                    }
                                                }
                                            } else Modifier,
                                        ),
                                ) {
                                    SearchBarFake(searchStatus.label, dynamicTopPadding)
                                }
                            },
                        )
                    }
                }
            }
        },
        popupHost = if (isMaterial) {
            null
        } else {
            {
                searchStatus.SearchPager(
                    onSearchStatusChange = { searchStatus = it },
                    defaultResult = {},
                    searchBarTopPadding = dynamicTopPadding,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .overScrollVertical()
                            .imePadding(),
                    ) {
                        item { Spacer(Modifier.height(6.dp)) }

                        if (filteredApps.isEmpty()) {
                            item(key = "search_empty") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(R.string.app_proxy_no_match),
                                        fontSize = 16.sp,
                                        color = MishkaColors.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredApps,
                                key = { it.packageName },
                                contentType = { "app" },
                            ) { app ->
                                AppItem(
                                    appName = app.appName,
                                    packageName = app.packageName,
                                    isSelected = app.packageName in uiState.selectedPackages,
                                    onToggle = { viewModel.toggleApp(app.packageName) },
                                )
                            }
                        }

                        item {
                            Spacer(
                                Modifier
                                    .height(24.dp)
                                    .navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        val appList: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalCutoutPadding()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                ),
            ) {
                groupedCardItems(
                    keyPrefix = "app_proxy_mode",
                    outerTopPadding = 6.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("allowAll") {
                            RadioButtonPreference(
                                title = stringResource(R.string.app_proxy_allow_all),
                                summary = stringResource(R.string.app_proxy_allow_all_summary),
                                selected = uiState.mode == AppProxyMode.AllowAll,
                                onClick = { viewModel.setMode(AppProxyMode.AllowAll) },
                                radioButtonLocation = RadioButtonLocation.End,
                            )
                        },
                        CardItem("allowSelected") {
                            RadioButtonPreference(
                                title = stringResource(R.string.app_proxy_allow_selected),
                                summary = stringResource(R.string.app_proxy_allow_selected_summary),
                                selected = uiState.mode == AppProxyMode.AllowSelected,
                                onClick = { viewModel.setMode(AppProxyMode.AllowSelected) },
                                radioButtonLocation = RadioButtonLocation.End,
                            )
                        },
                        CardItem("denySelected") {
                            RadioButtonPreference(
                                title = stringResource(R.string.app_proxy_deny_selected),
                                summary = stringResource(R.string.app_proxy_deny_selected_summary),
                                selected = uiState.mode == AppProxyMode.DenySelected,
                                onClick = { viewModel.setMode(AppProxyMode.DenySelected) },
                                radioButtonLocation = RadioButtonLocation.End,
                            )
                        },
                    ),
                )

                // 应用列表（AllowAll 模式下不显示）
                if (uiState.mode != AppProxyMode.AllowAll) {
                    if (uiState.isLoading) {
                        // 加载中 → 进度指示器
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                InfiniteProgressIndicator()
                            }
                        }
                    } else {
                        item(key = "apps_title") {
                            val selectedCount = remember(filteredApps, uiState.selectedPackages) {
                                filteredApps.count { it.packageName in uiState.selectedPackages }
                            }
                            SmallTitle(
                                text = stringResource(
                                    R.string.app_proxy_app_list,
                                    selectedCount,
                                    filteredApps.size
                                )
                            )
                        }
                        if (isMaterial) {
                            itemsIndexed(
                                items = filteredApps,
                                key = { _, app -> app.packageName },
                                contentType = { _, _ -> "app" },
                            ) { index, app ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(top = if (index == 0) 0.dp else 2.dp),
                                ) {
                                    SegmentedItem(index = index, count = filteredApps.size) {
                                        AppItem(
                                            appName = app.appName,
                                            packageName = app.packageName,
                                            isSelected = app.packageName in uiState.selectedPackages,
                                            onToggle = { viewModel.toggleApp(app.packageName) },
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = filteredApps,
                                key = { it.packageName },
                                contentType = { "app" },
                            ) { app ->
                                AppItem(
                                    appName = app.appName,
                                    packageName = app.packageName,
                                    isSelected = app.packageName in uiState.selectedPackages,
                                    onToggle = { viewModel.toggleApp(app.packageName) },
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(
                        Modifier
                            .height(24.dp)
                            .navigationBarsPadding()
                    )
                }
            }
        }
        val content: @Composable () -> Unit = {
            if (uiState.mode == AppProxyMode.AllowAll) {
                appList()
            } else {
                PullToRefresh(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refreshApps,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 6.dp),
                    refreshTexts = listOf(
                        stringResource(R.string.refresh_pulling),
                        stringResource(R.string.refresh_release),
                        stringResource(R.string.refresh_refreshing),
                        stringResource(R.string.refresh_complete),
                    ),
                    content = appList,
                )
            }
        }
        if (isMaterial) content() else searchStatus.SearchBox { content() }
    }
}

@Composable
private fun AppItem(
    appName: String,
    packageName: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Material) {
        SegmentedListItem(
            onClick = onToggle,
            leadingContent = {
                AppIcon(
                    packageName = packageName,
                    size = 40.dp,
                )
            },
            headlineContent = {
                Text(
                    text = appName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = packageName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingContent = {
                Checkbox(
                    state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggle,
                )
            },
        )
        return
    }
    Card(
        modifier = Modifier
            .padding(horizontal = PageHorizontalPadding)
            .padding(bottom = 12.dp),
    ) {
        BasicComponent(
            title = appName,
            summary = packageName,
            startAction = {
                AppIcon(
                    packageName = packageName,
                    modifier = Modifier.padding(end = 6.dp),
                    size = 40.dp,
                )
            },
            endActions = {
                Checkbox(
                    state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggle,
                )
            },
            onClick = onToggle,
        )
    }
}
