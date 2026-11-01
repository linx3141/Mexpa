package com.linx3141.mexpa.ui.screen.log

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.UiMode
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.domain.model.LogMessage
import com.linx3141.mexpa.viewmodel.IndexedLog
import com.linx3141.mexpa.viewmodel.LogUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.SearchBarFake
import com.linx3141.mexpa.ui.component.SearchBox
import com.linx3141.mexpa.ui.component.SearchPager
import com.linx3141.mexpa.ui.component.SearchResultStatusEffect
import com.linx3141.mexpa.ui.component.SearchStatus
import com.linx3141.mexpa.ui.component.rememberSearchBarTopPadding
import com.linx3141.mexpa.ui.component.rememberSearchScreenStatus
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.BadgeText
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.themeBadgeBackground
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.MishkaSearchAppBar
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun LogScreenContent(
    uiState: LogUiState = LogUiState(),
    logs: ImmutableList<IndexedLog> = persistentListOf(),
    onBack: () -> Unit = {},
    onConnect: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onClear: () -> Unit = {},
) {

    val scrollBehavior = rememberMishkaScrollBehavior()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isMaterial = LocalUiMode.current == UiMode.Material
    var materialSearchText by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onConnect()
        onDispose { onDisconnect() }
    }

    val searchStatusState = rememberSearchScreenStatus(stringResource(Res.string.log_search))
    var searchStatus by searchStatusState

    // 搜索词只作为过滤 key，不写回 ViewModel，避免组合期写状态
    val searchText = if (isMaterial) materialSearchText else searchStatus.searchText
    val filteredLogs = remember(searchText, logs) { filterLogs(logs, searchText) }
    if (!isMaterial) SearchResultStatusEffect(searchStatusState, filteredLogs.isEmpty())

    val dynamicTopPadding = rememberSearchBarTopPadding(scrollBehavior)

    // 仅当用户已滚到底部时自动跟随新日志，否则不抢夺滚动位置
    val autoScrollEnabled by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) true
            else {
                val lastVisible = visible.last().index
                // bottom_spacer 或最后一条 log 在可视范围内即视为"在底部"
                lastVisible >= info.totalItemsCount - 2
            }
        }
    }

    // key 用单调递增的末条 id：缓冲写满后 size 恒定，不能再以 size 变化驱动自动跟随
    val lastLogId = logs.lastOrNull()?.id
    LaunchedEffect(lastLogId) {
        // 搜索态下主列表被弹出层盖住，滚它也看不见，收起后还会停在错位置
        if (lastLogId != null && autoScrollEnabled && searchStatus.isCollapsed()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    val backButton: @Composable () -> Unit = {
        MishkaBackButton(
            onClick = onBack,
            contentDescription = stringResource(Res.string.common_back),
        )
    }
    val actionsContent: @Composable () -> Unit = {
        IconButton(onClick = { onClear() }) {
            Icon(
                imageVector = AppIcons.Delete,
                contentDescription = stringResource(Res.string.log_clear),
                tint = MishkaColors.onSurface,
            )
        }
    }

    val materialSearchResults: @Composable (Dp) -> Unit = { bottomPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = PageHorizontalPadding,
                end = PageHorizontalPadding,
                bottom = 16.dp + bottomPadding,
            ),
        ) {
            if (filteredLogs.isEmpty()) {
                item(key = "search_empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(Res.string.log_no_match),
                            fontSize = 16.sp,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                items(
                    items = filteredLogs,
                    key = { it.id },
                    contentType = { "log" },
                ) { indexedLog ->
                    LogCard(indexedLog.message)
                }
            }
        }
    }

    val logList: @Composable (PaddingValues) -> Unit = { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .platformScrollEndHaptic()
                .platformOverScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (logs.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Column(
                        modifier = Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (uiState.isConnected) {
                                stringResource(Res.string.log_waiting)
                            } else {
                                stringResource(Res.string.log_not_connected)
                            },
                            fontSize = 16.sp,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                    }
                }
            } else {
                item(key = "top_spacer", contentType = "spacer") {
                    Spacer(Modifier)
                }
            }

            items(
                items = logs,
                key = { it.id },
                contentType = { "log" },
            ) { indexedLog ->
                LogCard(indexedLog.message)
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    Scaffold(
        topBar = {
            if (isMaterial) {
                MishkaSearchAppBar(
                    title = { Text(stringResource(Res.string.log_title)) },
                    searchText = materialSearchText,
                    onSearchTextChange = { materialSearchText = it },
                    onClearClick = { materialSearchText = "" },
                    navigationIcon = { backButton() },
                    actions = { actionsContent() },
                    scrollBehavior = scrollBehavior.material,
                    defaultContent = { bottomPadding, _ -> materialSearchResults(bottomPadding) },
                    searchContent = { bottomPadding, _ -> materialSearchResults(bottomPadding) },
                )
            } else {
                BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                    searchStatus.TopAppBarAnim(
                        backgroundColor = if (blurActive) Color.Transparent else MishkaColors.surface,
                    ) {
                        AdaptiveTopAppBar(
                            title = stringResource(Res.string.log_title),
                            color = barColor,
                            scrollBehavior = scrollBehavior,
                            navigationIcon = backButton,
                            actions = { actionsContent() },
                            bottomContent = {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer { alpha = if (searchStatus.isCollapsed()) 1f else 0f }
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
        popupHost = if (isMaterial) null else {
            {
                searchStatus.SearchPager(
                onSearchStatusChange = { searchStatus = it },
                defaultResult = {},
                searchBarTopPadding = dynamicTopPadding,
            ) {
                val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .platformOverScrollVertical(),
                ) {
                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    if (filteredLogs.isEmpty()) {
                        item(key = "search_empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(Res.string.log_no_match),
                                    fontSize = 16.sp,
                                    color = MishkaColors.onSurfaceVariantSummary,
                                )
                            }
                        }
                    } else {
                        items(
                            items = filteredLogs,
                            key = { it.id },
                            contentType = { "log" },
                        ) { indexedLog ->
                            LogCard(indexedLog.message)
                        }
                    }

                    item {
                        Spacer(
                            Modifier
                                .height(imeBottomPadding)
                                .navigationBarsPadding(),
                        )
                    }
                }
            }
        }
        },
    ) { innerPadding ->
        if (isMaterial) {
            logList(innerPadding)
        } else {
            searchStatus.SearchBox {
                logList(innerPadding)
            }
        }
    }
}

private fun filterLogs(logs: ImmutableList<IndexedLog>, searchQuery: String): ImmutableList<IndexedLog> {
    val query = searchQuery.trim().lowercase()
    if (query.isEmpty()) return logs
    return logs.filter { log ->
        log.message.payload.lowercase().contains(query) ||
                log.message.type.lowercase().contains(query)
    }.toPersistentList()
}

/**
 * 解析后的日志条目
 */
private data class ParsedLog(
    val protocol: String = "",
    val source: String = "",
    val target: String = "",
    val rule: String = "",
    val proxy: String = "",
    val raw: String = "",
)

/**
 * 解析 mihomo 日志 payload
 * 格式: [TCP] 198.18.0.1:45552 --> statusapi.micloud.xiaomi.com:443 match GeoSite(cn) using 全球直连(DIRECT)
 */
private fun parsePayload(payload: String): ParsedLog {
    val raw = payload.trim()

    // 提取协议 [TCP] / [UDP]
    val protocolMatch = Regex("^\\[(\\w+)]").find(raw)
    val protocol = protocolMatch?.groupValues?.get(1) ?: ""
    val rest = if (protocolMatch != null) raw.substring(protocolMatch.range.last + 1).trim() else raw

    // 提取 source --> target
    val arrowIdx = rest.indexOf("-->")
    if (arrowIdx < 0) return ParsedLog(raw = raw)

    val source = rest.substring(0, arrowIdx).trim()
    val afterArrow = rest.substring(arrowIdx + 3).trim()

    // 提取 match ... using ...
    val matchIdx = afterArrow.indexOf(" match ")
    if (matchIdx < 0) {
        return ParsedLog(protocol = protocol, source = source, target = afterArrow, raw = raw)
    }

    val target = afterArrow.substring(0, matchIdx).trim()
    val matchPart = afterArrow.substring(matchIdx + 7).trim()

    val usingIdx = matchPart.indexOf(" using ")
    val rule: String
    val proxy: String
    if (usingIdx >= 0) {
        rule = matchPart.substring(0, usingIdx).trim()
        proxy = matchPart.substring(usingIdx + 7).trim()
    } else {
        rule = matchPart
        proxy = ""
    }

    return ParsedLog(protocol = protocol, source = source, target = target, rule = rule, proxy = proxy, raw = raw)
}

@Composable
private fun LogCard(log: LogMessage) {
    val levelInfo = getLevelInfo(log.type)
    val isMaterial = LocalUiMode.current == UiMode.Material
    val levelColor = if (isMaterial) materialLevelColor(log.type) else levelInfo.color
    val parsed = remember(log.payload) { parsePayload(log.payload) }
    val isParsed = parsed.target.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageHorizontalPadding),
        insideMargin = PaddingValues(12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 顶行：级别标签 + 协议
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LevelBadge(levelInfo, log.type)

                Spacer(Modifier.width(8.dp))

                if (isParsed && parsed.protocol.isNotEmpty()) {
                    ProtocolBadge(parsed.protocol)
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = levelInfo.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = levelColor,
                )
            }

            if (isParsed) {
                // 目标地址
                Text(
                    text = parsed.target,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MishkaColors.onSurface,
                )

                // 规则 + 代理
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (parsed.rule.isNotEmpty()) {
                        Text(
                            text = parsed.rule,
                            fontSize = 11.sp,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                    }
                    if (parsed.proxy.isNotEmpty()) {
                        Text(
                            text = parsed.proxy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = levelColor.copy(alpha = 0.8f),
                        )
                    }
                }
            } else {
                // 无法解析，原样显示
                Text(
                    text = log.payload,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MishkaColors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun LevelBadge(levelInfo: LevelInfo, type: String) {
    val isMaterial = LocalUiMode.current == UiMode.Material
    val backgroundColor = if (isMaterial) materialLevelBadgeColor(type) else levelInfo.color
    val contentColor = if (isMaterial) materialLevelContent(type) else Color.White
    Box(
        modifier = Modifier
            .size(
                width = if (isMaterial) 24.dp else 20.dp,
                height = if (isMaterial) 18.dp else 16.dp,
            )
            .themeBadgeBackground(backgroundColor, 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        BadgeText(
            text = levelInfo.label,
            color = contentColor,
            textAlign = if (isMaterial) TextAlign.Center else null,
        )
    }
}

@Composable
private fun materialLevelColor(type: String): Color = when (type.lowercase()) {
    "error" -> MaterialTheme.colorScheme.error
    "warning" -> MaterialTheme.colorScheme.tertiary
    "info" -> MaterialTheme.colorScheme.primary
    "debug" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun materialLevelBadgeColor(type: String): Color = when (type.lowercase()) {
    "error" -> MaterialTheme.colorScheme.error
    "warning" -> MaterialTheme.colorScheme.tertiary
    "info" -> MaterialTheme.colorScheme.primary
    "debug" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.surfaceContainerHighest
}

@Composable
private fun materialLevelContent(type: String): Color = when (type.lowercase()) {
    "error" -> MaterialTheme.colorScheme.onError
    "warning" -> MaterialTheme.colorScheme.onTertiary
    "info" -> MaterialTheme.colorScheme.onPrimary
    "debug" -> MaterialTheme.colorScheme.onSecondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ProtocolBadge(protocol: String) {
    Box(
        modifier = Modifier
            .themeBadgeBackground(MishkaColors.onSurface.copy(alpha = 0.08f), 3.dp)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        BadgeText(
            text = protocol,
            color = MishkaColors.onSurfaceVariantSummary,
        )
    }
}

private data class LevelInfo(val label: String, val name: String, val color: Color)

private fun getLevelInfo(type: String): LevelInfo {
    return when (type.lowercase()) {
        "error" -> LevelInfo("E", "Error", Color(0xFFFF5252))
        "warning" -> LevelInfo("W", "Warning", Color(0xFFFFB74D))
        "info" -> LevelInfo("I", "Info", Color(0xFF69C0FF))
        "debug" -> LevelInfo("D", "Debug", Color(0xFF81C784))
        else -> LevelInfo("V", "Verbose", Color(0xFFBDBDBD))
    }
}
