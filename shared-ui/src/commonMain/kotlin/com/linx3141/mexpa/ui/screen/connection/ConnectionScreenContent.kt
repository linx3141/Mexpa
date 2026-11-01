package com.linx3141.mexpa.ui.screen.connection

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.component.design.BadgeText
import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.component.design.themeBadgeBackground
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.domain.model.ConnectionInfo
import com.linx3141.mexpa.viewmodel.ConnectionUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.SearchBarFake
import com.linx3141.mexpa.ui.component.SearchBox
import com.linx3141.mexpa.ui.component.SearchPager
import com.linx3141.mexpa.ui.component.SearchResultStatusEffect
import com.linx3141.mexpa.ui.component.SearchStatus
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.rememberSearchBarTopPadding
import com.linx3141.mexpa.ui.component.rememberSearchScreenStatus
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.util.FormatUtils
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.HorizontalDivider
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowDialog
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun ConnectionScreenContent(
    uiState: ConnectionUiState = ConnectionUiState(),
    onBack: () -> Unit = {},
    onStartObserving: () -> Unit = {},
    onStopObserving: () -> Unit = {},
    onCloseConnection: (String) -> Unit = {},
    onCloseAllConnections: () -> Unit = {},
) {

    val scrollBehavior = rememberMishkaScrollBehavior()
    var showCloseAllDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // WS 订阅只在本页存续，理由见 startObserving
    DisposableEffect(Unit) {
        onStartObserving()
        onDispose { onStopObserving() }
    }

    val searchStatusState = rememberSearchScreenStatus(stringResource(Res.string.connection_search))
    var searchStatus by searchStatusState

    // 搜索过滤（直接传入 searchText，避免组合期间写入 ViewModel 状态）
    val searchText = searchStatus.searchText
    val filteredConnections = remember(searchText, uiState.connections) {
        filteredConnections(uiState.connections, searchText)
    }
    SearchResultStatusEffect(searchStatusState, filteredConnections.isEmpty())

    val dynamicTopPadding = rememberSearchBarTopPadding(scrollBehavior)

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                searchStatus.TopAppBarAnim(
                    backgroundColor = if (blurActive) Color.Transparent else MishkaColors.surface,
                ) {
                    AdaptiveTopAppBar(
                        title = stringResource(Res.string.connection_title),
                        color = barColor,
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            MishkaBackButton(
                                onClick = onBack,
                                contentDescription = stringResource(Res.string.common_back),
                            )
                        },
                        actions = {
                            IconButton(onClick = { showCloseAllDialog = true }) {
                                Icon(
                                    imageVector = AppIcons.Delete,
                                    contentDescription = stringResource(Res.string.connection_close_all),
                                    tint = MishkaColors.onSurface,
                                )
                            }
                        },
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
        },
        popupHost = {
            searchStatus.SearchPager(
                onSearchStatusChange = { searchStatus = it },
                defaultResult = {},
                searchBarTopPadding = dynamicTopPadding,
            ) {
                // 搜索结果列表
                val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .platformOverScrollVertical(),
                ) {
                    item {
                        Spacer(Modifier.height(6.dp))
                    }

                    if (filteredConnections.isEmpty()) {
                        item(key = "search_empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(Res.string.connection_no_match),
                                    fontSize = 16.sp,
                                    color = MishkaColors.onSurfaceVariantSummary,
                                )
                            }
                        }
                    } else {
                        items(filteredConnections, key = { it.id }) { conn ->
                            ConnectionItem(
                                connection = conn,
                                onClose = { onCloseConnection(conn.id) },
                            )
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
        },
    ) { innerPadding ->
        searchStatus.SearchBox {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalCutoutPadding()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .platformScrollEndHaptic()
                    .platformOverScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                ),
            ) {
                if (uiState.connections.isEmpty() && !uiState.isConnected) {
                    item(key = "empty") {
                        Column(
                            modifier = Modifier.fillParentMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.connection_no_active),
                                fontSize = 16.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                            )
                            Text(
                                text = stringResource(Res.string.connection_start_first),
                                modifier = Modifier.padding(top = 6.dp),
                                fontSize = 14.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    item(key = "stats", contentType = "stats") {
                        StatsCard(
                            connectionCount = uiState.connections.size,
                            uploadTotal = uiState.uploadTotal,
                            downloadTotal = uiState.downloadTotal,
                        )
                    }

                    item(key = "connection_title", contentType = "title") {
                        SmallTitle(text = stringResource(Res.string.connection_list))
                    }

                    items(
                        items = uiState.connections,
                        key = { it.id },
                        contentType = { "connection" },
                    ) { conn ->
                        ConnectionItem(
                            connection = conn,
                            onClose = { onCloseConnection(conn.id) },
                        )
                    }

                    item(key = "bottom_spacer", contentType = "spacer") {
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }
        }
    }

    // 关闭全部确认 Dialog
    WindowDialog(
        show = showCloseAllDialog,
        title = stringResource(Res.string.connection_close_all_title),
        summary = stringResource(Res.string.connection_close_all_summary),
        onDismissRequest = { showCloseAllDialog = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showCloseAllDialog = false },
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    onCloseAllConnections()
                    showCloseAllDialog = false
                },
            )
        }
    }
}

@Composable
private fun StatsCard(
    connectionCount: Int,
    uploadTotal: Long,
    downloadTotal: Long,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem(label = stringResource(Res.string.connection_active), value = "$connectionCount")
            StatItem(label = stringResource(Res.string.connection_upload_total), value = FormatUtils.formatBytes(uploadTotal))
            StatItem(label = stringResource(Res.string.connection_download_total), value = FormatUtils.formatBytes(downloadTotal))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MishkaColors.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MishkaColors.onSurface,
        )
    }
}

@Composable
private fun ConnectionItem(
    connection: ConnectionInfo,
    onClose: () -> Unit,
) {
    val meta = connection.metadata

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PageHorizontalPadding)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        // 标题行：Host + 关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .themeBadgeBackground(MishkaColors.onSurface.copy(alpha = 0.08f), 3.dp)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                BadgeText(
                    text = meta.network.uppercase(),
                    color = MishkaColors.onSurfaceVariantSummary,
                )
            }
            Text(
                text = meta.host.ifEmpty { "${meta.destinationIP}:${meta.destinationPort}" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MishkaColors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Image(
                modifier = Modifier
                    .size(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onClose,
                    ),
                imageVector = AppIcons.Close,
                contentDescription = stringResource(Res.string.common_close),
                colorFilter = ColorFilter.tint(MishkaColors.onSurfaceVariantSummary),
            )
        }

        if (connection.chains.isNotEmpty()) {
            Text(
                text = connection.chains.joinToString(" → "),
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MishkaColors.primary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val ruleText = buildString {
            append(connection.rule)
            if (connection.rulePayload.isNotEmpty()) {
                append("(${connection.rulePayload})")
            }
        }
        Text(
            text = ruleText,
            modifier = Modifier.padding(top = 2.dp),
            fontSize = 12.sp,
            color = MishkaColors.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 底部信息行：流量 + 进程名
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "↑ ${FormatUtils.formatBytes(connection.upload)}  ↓ ${FormatUtils.formatBytes(connection.download)}",
                fontSize = 12.sp,
                color = MishkaColors.onSurfaceVariantSummary,
            )
            if (meta.process.isNotEmpty()) {
                Text(
                    text = meta.process,
                    fontSize = 12.sp,
                    color = MishkaColors.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun filteredConnections(
    connections: ImmutableList<ConnectionInfo>,
    searchQuery: String,
): ImmutableList<ConnectionInfo> {
    val query = searchQuery.lowercase()
    if (query.isBlank()) return connections
    return connections.filter { conn ->
        conn.metadata.host.lowercase().contains(query) ||
            conn.metadata.process.lowercase().contains(query) ||
            conn.rule.lowercase().contains(query) ||
            conn.metadata.destinationIP.contains(query) ||
            conn.chains.any { it.lowercase().contains(query) }
    }.toPersistentList()
}
