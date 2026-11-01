package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.util.WideContentBox
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SmallTitle
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import com.linx3141.mexpa.ui.component.design.OverlayDropdownPreference
import com.linx3141.mexpa.ui.component.design.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    state: SettingsUiState = SettingsUiState(),
    capabilities: SettingsCapabilities = SettingsCapabilities(),
    onNavigateVpnSettings: () -> Unit = {},
    onNavigateRootSettings: () -> Unit = {},
    onNavigateNetworkSettings: () -> Unit = {},
    onNavigateMetaSettings: () -> Unit = {},
    onNavigateExternalControl: () -> Unit = {},
    onNavigateAutomation: () -> Unit = {},
    onNavigateAppProxy: () -> Unit = {},
    onNavigateWifiPolicy: () -> Unit = {},
    onNavigateThemeSettings: () -> Unit = {},
    onNavigateFileManager: () -> Unit = {},
    onNavigateOverrides: () -> Unit = {},
    onNavigateBackup: () -> Unit = {},
    onNavigateAbout: () -> Unit = {},
    onTunModeChange: (Int) -> Unit = {},
    onAutoStartChange: (Boolean) -> Unit = {},
    onAutoConnectChange: (Boolean) -> Unit = {},
    onDynamicNotificationChange: (Boolean) -> Unit = {},
    onUpdateViaProxyChange: (Boolean) -> Unit = {},
    onRestartAfterUpdateChange: (Boolean) -> Unit = {},
    onHideTaskCardChange: ((Boolean) -> Unit)? = null,
) {

    val scrollBehavior = rememberMishkaScrollBehavior()

    val tunModeItems = listOf(
        stringResource(Res.string.settings_tun_mode_vpn),
        stringResource(Res.string.settings_tun_mode_root_tun),
        stringResource(Res.string.settings_tun_mode_root_tproxy),
    )

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        modifier = modifier,
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.settings_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .platformScrollEndHaptic()
                    .platformOverScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = sidePadding,
                    end = sidePadding,
                    bottom = bottomPadding,
                ),
            ) {
                item {
                    SmallTitle(text = stringResource(Res.string.settings_network))
                }
                groupedCardItems(
                    keyPrefix = "settings_network",
                    items = buildList {
                        if (capabilities.tunMode && state.hasRootPermission) {
                            add(CardItem("tunMode") {
                                OverlayDropdownPreference(
                                    title = stringResource(Res.string.settings_tun_mode),
                                    summary = when (state.tunModeIndex) {
                                        1 -> stringResource(Res.string.settings_tun_root_tun_summary)
                                        2 -> stringResource(Res.string.settings_tun_root_tproxy_summary)
                                        else -> stringResource(Res.string.settings_tun_vpn_summary)
                                    },
                                    items = tunModeItems,
                                    selectedIndex = state.tunModeIndex,
                                    onSelectedIndexChange = { index -> onTunModeChange(index) },
                                    enabled = !state.isProxyRunning,
                                )
                            })
                        }
                        if (capabilities.vpnSettings && state.tunModeIndex == 0) {
                            add(CardItem("vpnSettings") {
                                ArrowPreference(
                                    title = stringResource(Res.string.settings_vpn_settings),
                                    summary = stringResource(Res.string.settings_vpn_summary),
                                    onClick = onNavigateVpnSettings,
                                )
                            })
                        }
                        if (capabilities.rootSettings && (state.tunModeIndex == 1 || state.tunModeIndex == 2)) {
                            add(CardItem("rootSettings") {
                                ArrowPreference(
                                    title = stringResource(Res.string.root_settings_title),
                                    summary = stringResource(Res.string.root_settings_summary),
                                    onClick = onNavigateRootSettings,
                                )
                            })
                        }
                        add(CardItem("override") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_override_settings),
                                summary = stringResource(Res.string.settings_override_summary),
                                onClick = onNavigateNetworkSettings,
                            )
                        })
                        if (capabilities.metaSettings) add(CardItem("meta") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_meta_settings),
                                summary = stringResource(Res.string.settings_meta_summary),
                                onClick = onNavigateMetaSettings,
                            )
                        })
                        add(CardItem("externalControl") {
                            ArrowPreference(
                                title = stringResource(Res.string.external_control_title),
                                summary = stringResource(Res.string.settings_external_control_summary),
                                onClick = onNavigateExternalControl,
                            )
                        })
                        add(CardItem("automation") {
                            ArrowPreference(
                                title = stringResource(Res.string.automation_title),
                                summary = stringResource(Res.string.settings_automation_summary),
                                onClick = onNavigateAutomation,
                            )
                        })
                        if (capabilities.appProxy) add(CardItem("appProxy") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_app_proxy),
                                summary = stringResource(Res.string.settings_app_proxy_summary),
                                onClick = onNavigateAppProxy,
                            )
                        })
                        if (capabilities.wifiPolicy) add(CardItem("wifiPolicy") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_wifi_policy),
                                summary = stringResource(Res.string.settings_wifi_policy_summary),
                                onClick = onNavigateWifiPolicy,
                            )
                        })
                        if (capabilities.fileManager) add(CardItem("fileManager") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_file_manager),
                                summary = stringResource(Res.string.settings_file_manager_summary),
                                onClick = onNavigateFileManager,
                            )
                        })
                        if (capabilities.overrides) add(CardItem("overrides") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_overrides),
                                summary = stringResource(Res.string.settings_overrides_summary),
                                onClick = onNavigateOverrides,
                            )
                        })
                        add(CardItem("backup") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_backup),
                                summary = stringResource(Res.string.settings_backup_summary),
                                onClick = onNavigateBackup,
                            )
                        })
                    },
                )
                item {
                    SmallTitle(text = stringResource(Res.string.settings_general))
                }
                groupedCardItems(
                    keyPrefix = "settings_general",
                    outerBottomPadding = 12.dp,
                    items = buildList {
                        if (capabilities.hideTaskCard && onHideTaskCardChange != null) {
                            add(CardItem("hideTaskCard") {
                                SwitchPreference(
                                    title = stringResource(Res.string.settings_hide_task_card),
                                    summary = stringResource(Res.string.settings_hide_task_card_summary),
                                    checked = state.hideTaskCardEnabled,
                                    onCheckedChange = { checked -> onHideTaskCardChange(checked) },
                                )
                            })
                        }
                        if (capabilities.dynamicNotification) {
                            add(CardItem("dynamicNotification") {
                                val isVpnMode = state.tunModeIndex == 0
                                SwitchPreference(
                                    title = stringResource(Res.string.settings_dynamic_notification),
                                    summary = stringResource(
                                        if (isVpnMode) Res.string.settings_dynamic_notification_summary
                                        else Res.string.settings_dynamic_notification_summary_root_unsupported
                                    ),
                                    checked = state.dynamicNotificationEnabled && isVpnMode,
                                    enabled = isVpnMode,
                                    onCheckedChange = { checked -> onDynamicNotificationChange(checked) },
                                )
                            })
                        }
                        if (capabilities.androidGeneralOptions) add(CardItem("subscriptionViaProxy") {
                            SwitchPreference(
                                title = stringResource(Res.string.settings_subscription_via_proxy),
                                summary = stringResource(Res.string.settings_subscription_via_proxy_summary),
                                checked = state.updateViaProxyEnabled,
                                onCheckedChange = { checked -> onUpdateViaProxyChange(checked) },
                            )
                        })
                        if (capabilities.androidGeneralOptions) add(CardItem("restartAfterUpdate") {
                            SwitchPreference(
                                title = stringResource(Res.string.settings_restart_after_update),
                                summary = stringResource(Res.string.settings_restart_after_update_summary),
                                checked = state.restartAfterUpdateEnabled,
                                onCheckedChange = { checked -> onRestartAfterUpdateChange(checked) },
                            )
                        })
                        if (capabilities.androidGeneralOptions) add(CardItem("autoConnect") {
                            SwitchPreference(
                                title = stringResource(Res.string.settings_auto_connect),
                                summary = stringResource(Res.string.settings_auto_connect_summary),
                                checked = state.autoConnectEnabled,
                                onCheckedChange = { checked -> onAutoConnectChange(checked) },
                            )
                        })
                        if (capabilities.bootStart) {
                            add(CardItem("autoRestart") {
                                SwitchPreference(
                                    title = stringResource(Res.string.settings_auto_restart),
                                    summary = stringResource(Res.string.settings_auto_restart_summary),
                                    checked = state.autoStartEnabled,
                                    onCheckedChange = { checked -> onAutoStartChange(checked) },
                                )
                            })
                        }
                        add(CardItem("theme") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_theme_title),
                                summary = stringResource(Res.string.settings_theme_summary),
                                onClick = onNavigateThemeSettings,
                            )
                        })
                        add(CardItem("about") {
                            ArrowPreference(
                                title = stringResource(Res.string.settings_about),
                                summary = "${stringResource(Res.string.app_name)} v${state.versionName}",
                                onClick = onNavigateAbout,
                            )
                        })
                    },
                )
            }
        }
    }
}
