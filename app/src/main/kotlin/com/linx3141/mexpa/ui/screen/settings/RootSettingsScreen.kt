package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.R



import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.RestartRequiredHint
import com.linx3141.mexpa.ui.component.TetherInterfaceEditDialog
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.component.tetherInterfaceSummary
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.CardDefaults
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
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import com.linx3141.mexpa.ui.component.design.OverlayDropdownPreference
import com.linx3141.mexpa.ui.component.design.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.linx3141.mexpa.ui.component.design.WindowDialog

/**
 * ROOT 模式专属设置页。仅在 ROOT 模式下可见。
 *
 * 内容：
 * - TUN 设备名称（mihomo 创建的 TUN 接口名，代理运行时不可改）
 * - 热点客户端流量处置（绕过代理 / 走代理）
 * - 热点接口名列表（手填 + 扫描辅助）
 */
@Composable
fun RootSettingsScreen(
    storage: PlatformStorage,
    isProxyRunning: Boolean,
    onBack: () -> Unit = {},
) {
    val scrollBehavior = rememberMishkaScrollBehavior()

    var tunDevice by remember {
        mutableStateOf(storage.getString(StorageKeys.ROOT_TUN_DEVICE, DEFAULT_TUN_DEVICE))
    }
    var showTunDeviceDialog by remember { mutableStateOf(false) }
    val tunDeviceTextState = rememberTextFieldState()

    var jumboMtu by remember {
        mutableStateOf(storage.getString(StorageKeys.ROOT_TUN_JUMBO_MTU, "true") == "true")
    }

    var allowIpv6 by remember {
        mutableStateOf(storage.getString(StorageKeys.VPN_ALLOW_IPV6, "false") == "true")
    }

    var forceReapply by remember {
        mutableStateOf(storage.getString(StorageKeys.ROOT_ATTACH_FORCE_REAPPLY, "false") == "true")
    }

    val bypassLabel = stringResource(R.string.root_tether_mode_bypass)
    val proxyLabel = stringResource(R.string.root_tether_mode_proxy)
    val tetherModeValues = listOf("bypass", "proxy")
    var tetherMode by remember { mutableStateOf(storage.getString(StorageKeys.ROOT_TETHER_MODE, "bypass")) }
    var tetherIfaces by remember {
        mutableStateOf(storage.getString(StorageKeys.ROOT_TETHER_IFACES, DEFAULT_TETHER_IFACES))
    }
    var showTetherDialog by remember { mutableStateOf(false) }

    // 读一次 submode：TPROXY 下隐藏 tether mode 下拉，显示说明文案
    // （用户改 TUN_MODE 必须先停代理，不需要热更新）
    val isTproxy = storage.getString(StorageKeys.TUN_MODE, "vpn") == "root_tproxy"

    // xt_TPROXY 内核能力探测结果，仅在 TPROXY submode 或 PROXY 热点模式下显示告警
    val tproxyCapable = storage.getString(StorageKeys.ROOT_TPROXY_KERNEL_CAPABLE, "")
    val showTproxyWarning = tproxyCapable == "false" && (isTproxy || tetherMode == "proxy")

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.root_settings_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.common_back),
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
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            item { RestartRequiredHint() }
            item { SmallTitle(text = stringResource(R.string.root_section_device)) }
            groupedCardItems(
                keyPrefix = "root_device",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("tunDevice") {
                        ArrowPreference(
                            title = stringResource(R.string.settings_tun_device),
                            summary = tunDevice,
                            onClick = {
                                tunDeviceTextState.edit { replace(0, length, tunDevice) }
                                showTunDeviceDialog = true
                            },
                            enabled = !isProxyRunning,
                        )
                    },
                    CardItem("jumboMtu") {
                        SwitchPreference(
                            title = stringResource(R.string.root_tun_jumbo_mtu_title),
                            summary = stringResource(R.string.root_tun_jumbo_mtu_summary),
                            checked = jumboMtu,
                            onCheckedChange = {
                                jumboMtu = it
                                storage.putString(StorageKeys.ROOT_TUN_JUMBO_MTU, it.toString())
                            },
                        )
                    },
                    CardItem("allowIpv6") {
                        SwitchPreference(
                            title = stringResource(R.string.vpn_allow_ipv6),
                            summary = stringResource(R.string.vpn_allow_ipv6_summary),
                            checked = allowIpv6,
                            onCheckedChange = {
                                allowIpv6 = it
                                storage.putString(StorageKeys.VPN_ALLOW_IPV6, it.toString())
                            },
                        )
                    },
                ),
            )
            item { SmallTitle(text = stringResource(R.string.root_section_tether)) }
            if (showTproxyWarning) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(
                            color = MishkaColors.errorContainer,
                            contentColor = MishkaColors.onErrorContainer,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.root_tether_tproxy_unavailable_warning),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            groupedCardItems(
                keyPrefix = "root_tether",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("mode") {
                        if (isTproxy) {
                            // TPROXY 模式：整条链路都由 iptables 透明代理，不需要 BYPASS/PROXY 切换
                            Text(
                                text = stringResource(R.string.root_tproxy_tether_note),
                                fontSize = 13.sp,
                                color = MishkaColors.onSurfaceVariantSummary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        } else {
                            val tetherItems = listOf(bypassLabel, proxyLabel)
                            val selectedIndex = tetherModeValues.indexOf(tetherMode).coerceAtLeast(0)
                            OverlayDropdownPreference(
                                title = stringResource(R.string.root_tether_mode_title),
                                summary = tetherItems[selectedIndex],
                                items = tetherItems,
                                selectedIndex = selectedIndex,
                                onSelectedIndexChange = { index ->
                                    val value = tetherModeValues[index]
                                    tetherMode = value
                                    storage.putString(StorageKeys.ROOT_TETHER_MODE, value)
                                },
                            )
                        }
                    },
                    CardItem("ifaces") {
                        ArrowPreference(
                            title = stringResource(R.string.root_tether_ifaces_title),
                            summary = tetherInterfaceSummary(tetherIfaces),
                            onClick = { showTetherDialog = true },
                        )
                    },
                    CardItem("forceReapply") {
                        SwitchPreference(
                            title = stringResource(R.string.root_attach_force_reapply_title),
                            summary = stringResource(R.string.root_attach_force_reapply_summary),
                            checked = forceReapply,
                            onCheckedChange = {
                                forceReapply = it
                                storage.putString(StorageKeys.ROOT_ATTACH_FORCE_REAPPLY, it.toString())
                            },
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

    WindowDialog(
        show = showTunDeviceDialog,
        title = stringResource(R.string.settings_tun_device),
        onDismissRequest = { showTunDeviceDialog = false },
    ) {
        TextField(
            state = tunDeviceTextState,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.network_input_value),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showTunDeviceDialog = false },
            )
            TextButton(
                text = stringResource(R.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val value = tunDeviceTextState.text.toString().trim().ifEmpty { DEFAULT_TUN_DEVICE }
                    storage.putString(StorageKeys.ROOT_TUN_DEVICE, value)
                    tunDevice = value
                    showTunDeviceDialog = false
                },
            )
        }
    }

    TetherInterfaceEditDialog(
        show = showTetherDialog,
        title = stringResource(R.string.root_tether_ifaces_title),
        initialValue = tetherIfaces,
        onDismiss = { showTetherDialog = false },
        onConfirm = { csv ->
            tetherIfaces = csv
            storage.putString(StorageKeys.ROOT_TETHER_IFACES, csv)
        },
    )
}

private const val DEFAULT_TUN_DEVICE = "Mishka"

private const val DEFAULT_TETHER_IFACES = "wlan1,wlan2"
