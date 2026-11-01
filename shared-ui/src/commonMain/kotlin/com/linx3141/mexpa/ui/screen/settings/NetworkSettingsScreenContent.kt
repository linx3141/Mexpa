package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.domain.model.ConfigurationOverride
import com.linx3141.mexpa.domain.model.DnsOverride
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.ListEditDialog
import com.linx3141.mexpa.ui.component.RestartRequiredHint
import com.linx3141.mexpa.ui.component.TriStatePreference
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.viewmodel.NetworkSettingsUiState
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import com.linx3141.mexpa.ui.component.design.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowDialog
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun NetworkSettingsScreenContent(
    uiState: NetworkSettingsUiState = NetworkSettingsUiState(),
    onBack: () -> Unit = {},
    onUpdate: ((com.linx3141.mexpa.domain.model.ConfigurationOverride) -> com.linx3141.mexpa.domain.model.ConfigurationOverride) -> Unit = {},
    onUpdateDns: ((com.linx3141.mexpa.domain.model.DnsOverride) -> com.linx3141.mexpa.domain.model.DnsOverride) -> Unit = {},
    /** 平台专属的卡片（例如 Linux 的系统代理 / TUN 开关）；Android 传 null。 */
    platformContent: (LazyListScope.() -> Unit)? = null,
) {

    val scrollBehavior = rememberMishkaScrollBehavior()

    fun updateTop(transform: (ConfigurationOverride) -> ConfigurationOverride) {
        onUpdate(transform)
    }

    fun updateDns(transform: (DnsOverride) -> DnsOverride) {
        onUpdateDns(transform)
    }

    // 端口编辑 Dialog 状态（setter 在 open 时绑定）
    var showPortDialog by remember { mutableStateOf(false) }
    var editingPortTitle by remember { mutableStateOf("") }
    var editingPortSetter by remember { mutableStateOf<(Int?) -> Unit>({}) }
    val portTextState = rememberTextFieldState()

    // 字符串编辑 Dialog 状态
    var showStringDialog by remember { mutableStateOf(false) }
    var editingStringTitle by remember { mutableStateOf("") }
    var editingStringSetter by remember { mutableStateOf<(String?) -> Unit>({}) }
    val stringTextState = rememberTextFieldState()

    // 列表编辑 Dialog 状态
    var showListDialog by remember { mutableStateOf(false) }
    var editingListTitle by remember { mutableStateOf("") }
    var editingListSetter by remember { mutableStateOf<(List<String>?) -> Unit>({}) }
    val listTextState = rememberTextFieldState()

    fun openPortDialog(title: String, value: Int?, setter: (Int?) -> Unit) {
        editingPortTitle = title
        editingPortSetter = setter
        portTextState.edit { replace(0, length, value?.toString() ?: "") }
        showPortDialog = true
    }

    fun openStringDialog(title: String, value: String?, setter: (String?) -> Unit) {
        editingStringTitle = title
        editingStringSetter = setter
        stringTextState.edit { replace(0, length, value ?: "") }
        showStringDialog = true
    }

    fun openListDialog(title: String, value: List<String>?, setter: (List<String>?) -> Unit) {
        editingListTitle = title
        editingListSetter = setter
        listTextState.edit { replace(0, length, value?.joinToString("\n") ?: "") }
        showListDialog = true
    }

    val dns = uiState.dns

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.network_settings_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(Res.string.common_back),
                        )
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
            platformContent?.invoke(this)
            item { RestartRequiredHint() }

            item { SmallTitle(text = stringResource(Res.string.network_proxy_ports)) }
            groupedCardItems(
                keyPrefix = "network_ports",
                items = listOf(
                    CardItem("http") {
                        val httpPortTitle = stringResource(Res.string.network_http_port)
                        ArrowPreference(
                            title = httpPortTitle,
                            summary = portSummary(uiState.httpPort),
                            onClick = { openPortDialog(httpPortTitle, uiState.httpPort) { v -> updateTop { it.copy(httpPort = v) } } },
                        )
                    },
                    CardItem("socks") {
                        val socksPortTitle = stringResource(Res.string.network_socks_port)
                        ArrowPreference(
                            title = socksPortTitle,
                            summary = portSummary(uiState.socksPort),
                            onClick = { openPortDialog(socksPortTitle, uiState.socksPort) { v -> updateTop { it.copy(socksPort = v) } } },
                        )
                    },
                    CardItem("redir") {
                        val redirPortTitle = stringResource(Res.string.network_redir_port)
                        ArrowPreference(
                            title = redirPortTitle,
                            summary = portSummary(uiState.redirPort),
                            onClick = { openPortDialog(redirPortTitle, uiState.redirPort) { v -> updateTop { it.copy(redirPort = v) } } },
                        )
                    },
                    CardItem("tproxy") {
                        val tproxyPortTitle = stringResource(Res.string.network_tproxy_port)
                        ArrowPreference(
                            title = tproxyPortTitle,
                            summary = portSummary(uiState.tproxyPort),
                            onClick = {
                                openPortDialog(
                                    tproxyPortTitle,
                                    uiState.tproxyPort
                                ) { v -> updateTop { it.copy(tproxyPort = v) } }
                            },
                        )
                    },
                    CardItem("mixed") {
                        val mixedPortTitle = stringResource(Res.string.network_mixed_port)
                        ArrowPreference(
                            title = mixedPortTitle,
                            summary = portSummary(uiState.mixedPort),
                            onClick = { openPortDialog(mixedPortTitle, uiState.mixedPort) { v -> updateTop { it.copy(mixedPort = v) } } },
                        )
                    },
                ),
            )

            item { SmallTitle(text = stringResource(Res.string.network_options)) }
            groupedCardItems(
                keyPrefix = "network_options",
                items = listOf(
                    CardItem("allowLan") {
                        TriStatePreference(
                            title = stringResource(Res.string.network_allow_lan),
                            value = uiState.allowLan,
                            onValueChange = { v -> updateTop { it.copy(allowLan = v) } },
                        )
                    },
                    CardItem("ipv6") {
                        TriStatePreference(
                            title = "IPv6",
                            value = uiState.ipv6,
                            onValueChange = { v -> updateTop { it.copy(ipv6 = v) } },
                        )
                    },
                    CardItem("bindAddress") {
                        val bindAddrTitle = stringResource(Res.string.network_bind_address)
                        ArrowPreference(
                            title = bindAddrTitle,
                            summary = uiState.bindAddress ?: stringResource(Res.string.common_not_modified),
                            onClick = {
                                openStringDialog(
                                    bindAddrTitle,
                                    uiState.bindAddress
                                ) { v -> updateTop { it.copy(bindAddress = v) } }
                            },
                        )
                    },
                    CardItem("logLevel") {
                        LogLevelPreference(
                            value = uiState.logLevel,
                            onValueChange = { v -> updateTop { it.copy(logLevel = v) } },
                        )
                    },
                ),
            )

            // DNS 显式关闭时禁用子项；null（不修改）仍保留可编辑，允许用户预配置子覆写
            val dnsSubEnabled = dns?.enable != false
            item { SmallTitle(text = stringResource(Res.string.network_dns)) }
            groupedCardItems(
                keyPrefix = "network_dns",
                items = listOf(
                    CardItem("enable") {
                        TriStatePreference(
                            title = stringResource(Res.string.network_dns_enable),
                            value = dns?.enable,
                            onValueChange = { v -> updateDns { it.copy(enable = v) } },
                        )
                    },
                    CardItem("listen") {
                        val dnsListenTitle = stringResource(Res.string.network_dns_listen_title)
                        ArrowPreference(
                            title = stringResource(Res.string.network_dns_listen),
                            summary = dns?.listen ?: stringResource(Res.string.common_not_modified),
                            onClick = { openStringDialog(dnsListenTitle, dns?.listen) { v -> updateDns { it.copy(listen = v) } } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("ipv6") {
                        TriStatePreference(
                            title = "DNS IPv6",
                            value = dns?.ipv6,
                            onValueChange = { v -> updateDns { it.copy(ipv6 = v) } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("preferH3") {
                        TriStatePreference(
                            title = stringResource(Res.string.network_dns_prefer_h3),
                            value = dns?.preferH3,
                            onValueChange = { v -> updateDns { it.copy(preferH3 = v) } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("useHosts") {
                        TriStatePreference(
                            title = stringResource(Res.string.network_dns_use_hosts),
                            value = dns?.useHosts,
                            onValueChange = { v -> updateDns { it.copy(useHosts = v) } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("enhancedMode") {
                        DnsEnhancedModePreference(
                            value = dns?.enhancedMode,
                            onValueChange = { v -> updateDns { it.copy(enhancedMode = v) } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("nameserver") {
                        ArrowPreference(
                            title = "Nameserver",
                            summary = listSummary(dns?.nameserver),
                            onClick = { openListDialog("Nameserver", dns?.nameserver) { v -> updateDns { it.copy(nameserver = v) } } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("fallback") {
                        ArrowPreference(
                            title = "Fallback",
                            summary = listSummary(dns?.fallback),
                            onClick = { openListDialog("Fallback", dns?.fallback) { v -> updateDns { it.copy(fallback = v) } } },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("defaultNameserver") {
                        val defaultNsTitle = stringResource(Res.string.network_dns_default_nameserver)
                        ArrowPreference(
                            title = defaultNsTitle,
                            summary = listSummary(dns?.defaultNameserver),
                            onClick = {
                                openListDialog(
                                    defaultNsTitle,
                                    dns?.defaultNameserver
                                ) { v -> updateDns { it.copy(defaultNameserver = v) } }
                            },
                            enabled = dnsSubEnabled,
                        )
                    },
                    CardItem("fakeipFilter") {
                        val fakeipFilterTitle = stringResource(Res.string.network_dns_fakeip_filter)
                        ArrowPreference(
                            title = fakeipFilterTitle,
                            summary = listSummary(dns?.fakeIpFilter),
                            onClick = {
                                openListDialog(
                                    fakeipFilterTitle,
                                    dns?.fakeIpFilter
                                ) { v -> updateDns { it.copy(fakeIpFilter = v) } }
                            },
                            enabled = dnsSubEnabled,
                        )
                    },
                ),
            )

            item {
                Spacer(
                    Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    PortEditDialog(
        show = showPortDialog,
        title = editingPortTitle,
        textState = portTextState,
        onDismiss = { showPortDialog = false },
        onConfirm = { port -> editingPortSetter(port) },
        onReset = {
            editingPortSetter(null)
        },
    )

    StringEditDialog(
        show = showStringDialog,
        title = editingStringTitle,
        textState = stringTextState,
        onDismiss = { showStringDialog = false },
        onConfirm = { value -> editingStringSetter(value) },
        onReset = {
            editingStringSetter(null)
        },
    )

    ListEditDialog(
        show = showListDialog,
        title = editingListTitle,
        textState = listTextState,
        onDismiss = { showListDialog = false },
        onConfirm = { list -> editingListSetter(list) },
        onReset = {
            editingListSetter(null)
        },
    )
}

// === 私有组件 ===

@Composable
private fun LogLevelPreference(
    value: String?,
    onValueChange: (String?) -> Unit,
) {
    val notModifiedStr = stringResource(Res.string.common_not_modified)
    val items = listOf(notModifiedStr, "Info", "Warning", "Error", "Debug", "Silent")
    val values = listOf(null, "info", "warning", "error", "debug", "silent")
    val selectedIndex = values.indexOf(value).coerceAtLeast(0)

    OverlayDropdownPreference(
        title = stringResource(Res.string.network_log_level),
        summary = items[selectedIndex],
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index -> onValueChange(values[index]) },
    )
}

@Composable
private fun DnsEnhancedModePreference(
    value: String?,
    onValueChange: (String?) -> Unit,
    enabled: Boolean = true,
) {
    val notModifiedStr2 = stringResource(Res.string.common_not_modified)
    val items = listOf(notModifiedStr2, "Normal", "FakeIP", "Redir-Host")
    val values = listOf(null, "normal", "fake-ip", "redir-host")
    val selectedIndex = values.indexOf(value).coerceAtLeast(0)

    OverlayDropdownPreference(
        title = stringResource(Res.string.network_dns_enhanced_mode),
        summary = items[selectedIndex],
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index -> onValueChange(values[index]) },
        enabled = enabled,
    )
}

@Composable
private fun PortEditDialog(
    show: Boolean,
    title: String,
    textState: TextFieldState,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
    onReset: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = title,
        summary = stringResource(Res.string.network_port_zero_hint),
        onDismissRequest = onDismiss,
    ) {
        TextField(
            state = textState,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.network_port_label),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_not_modified),
                modifier = Modifier.weight(1f),
                onClick = {
                    onReset()
                    onDismiss()
                },
            )
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val port = textState.text.toString().toIntOrNull()
                    if (port != null && port in 0..65535) {
                        onConfirm(port)
                    }
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun StringEditDialog(
    show: Boolean,
    title: String,
    textState: TextFieldState,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    onReset: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            state = textState,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.network_input_value),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_not_modified),
                modifier = Modifier.weight(1f),
                onClick = {
                    onReset()
                    onDismiss()
                },
            )
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val value = textState.text.toString().trim()
                    onConfirm(value.ifEmpty { null })
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun portSummary(port: Int?): String = if (port == null) stringResource(Res.string.common_not_modified) else "$port"

@Composable
private fun listSummary(list: List<String>?): String {
    if (list == null) return stringResource(Res.string.common_not_modified)
    if (list.isEmpty()) return stringResource(Res.string.common_cleared)
    return pluralStringResource(Res.plurals.common_items_count, list.size, list.size)
}
