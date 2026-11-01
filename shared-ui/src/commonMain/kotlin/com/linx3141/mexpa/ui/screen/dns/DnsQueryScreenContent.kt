package com.linx3141.mexpa.ui.screen.dns

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.component.design.BadgeText
import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.component.design.themeBadgeBackground
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.domain.model.DnsAnswer
import com.linx3141.mexpa.viewmodel.DnsQueryUiState
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.ui.component.design.BasicComponent
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowDialog
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun DnsQueryScreenContent(
    uiState: DnsQueryUiState = DnsQueryUiState(),
    onBack: () -> Unit = {},
    onSetQueryName: (String) -> Unit = {},
    onQuery: () -> Unit = {},
    onSetQueryType: (String) -> Unit = {},
    onFlushCache: () -> Unit = {},
    onFlushFakeIp: () -> Unit = {},
) {

    val scrollBehavior = rememberMishkaScrollBehavior()
    val textFieldState = rememberTextFieldState()
    var showCacheDialog by remember { mutableStateOf(false) }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.dns_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(Res.string.common_back),
                        )
                    },
                    actions = {
                        IconButton(onClick = { showCacheDialog = true }) {
                            Icon(
                                imageVector = AppIcons.Delete,
                                contentDescription = stringResource(Res.string.dns_clear_cache),
                                tint = MishkaColors.onSurface,
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
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
            // 输入区域
            item(key = "input") {
                Column(
                    modifier = Modifier.padding(all = 12.dp)
                ) {
                    TextField(
                        state = textFieldState,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(Res.string.dns_domain),
                        useLabelAsPlaceholder = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        onKeyboardAction = {
                            onSetQueryName(textFieldState.text.toString())
                            onQuery()
                        },
                    )

                    Spacer(Modifier.height(12.dp))

                    // 查询类型选择（3+3 两行全宽）
                    val types = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        types.take(3).forEach { type ->
                            TextButton(
                                text = type,
                                modifier = Modifier.weight(1f),
                                onClick = { onSetQueryType(type) },
                                colors = if (uiState.queryType == type) {
                                    ButtonDefaults.textButtonColorsPrimary()
                                } else {
                                    ButtonDefaults.textButtonColors()
                                },
                                minHeight = 36.dp,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        types.drop(3).forEach { type ->
                            TextButton(
                                text = type,
                                modifier = Modifier.weight(1f),
                                onClick = { onSetQueryType(type) },
                                colors = if (uiState.queryType == type) {
                                    ButtonDefaults.textButtonColorsPrimary()
                                } else {
                                    ButtonDefaults.textButtonColors()
                                },
                                minHeight = 36.dp,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    TextButton(
                        text = if (uiState.isQuerying) stringResource(Res.string.dns_querying) else stringResource(Res.string.dns_query),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = textFieldState.text.isNotBlank() && !uiState.isQuerying,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            onSetQueryName(textFieldState.text.toString())
                            onQuery()
                        },
                    )
                }
            }

            if (uiState.error.isNotEmpty()) {
                item(key = "error") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(top = 12.dp, bottom = 6.dp),
                        insideMargin = PaddingValues(16.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.dns_query_failed, uiState.error),
                            color = StatusColors.danger,
                        )
                    }
                }
            }

            // 结果区域
            if (uiState.status != null) {
                item(key = "result_title") {
                    SmallTitle(text = stringResource(Res.string.dns_results))
                }

                if (uiState.answers.isEmpty()) {
                    item(key = "no_result") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PageHorizontalPadding)
                                .padding(bottom = 12.dp),
                            insideMargin = PaddingValues(16.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.dns_no_record),
                                fontSize = 14.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    // 每条 DNS 记录独立成段，视觉上仍拼为一张连续卡片；记录多时只组合可见段
                    groupedCardItems(
                        keyPrefix = "dns_result",
                        outerBottomPadding = 12.dp,
                        items = uiState.answers.mapIndexed { index, answer ->
                            CardItem("$index:${answer.data}") {
                                DnsAnswerItem(answer)
                            }
                        },
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    // 缓存清除 Dialog
    WindowDialog(
        show = showCacheDialog,
        title = stringResource(Res.string.dns_clear_cache),
        onDismissRequest = { showCacheDialog = false },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = stringResource(Res.string.dns_clear_dns),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onFlushCache()
                    showCacheDialog = false
                },
            )
            TextButton(
                text = stringResource(Res.string.dns_clear_fakeip),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onFlushFakeIp()
                    showCacheDialog = false
                },
            )
        }
    }
}

@Composable
private fun DnsAnswerItem(answer: DnsAnswer) {
    BasicComponent(
        title = answer.data,
        summary = "TTL: ${answer.TTL}s",
        startAction = {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .themeBadgeBackground(MishkaColors.onSurface.copy(alpha = 0.08f), 3.dp)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                BadgeText(
                    text = dnsTypeToString(answer.type),
                    color = MishkaColors.onSurfaceVariantSummary,
                )
            }
        },
    )
}

private fun dnsTypeToString(type: Int): String = when (type) {
    1 -> "A"
    2 -> "NS"
    5 -> "CNAME"
    6 -> "SOA"
    15 -> "MX"
    16 -> "TXT"
    28 -> "AAAA"
    33 -> "SRV"
    else -> "TYPE$type"
}
