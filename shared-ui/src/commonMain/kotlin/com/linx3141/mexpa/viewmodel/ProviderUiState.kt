package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class ProviderItemUi(
    val name: String,
    // 技术类型原值（proxy provider 取 type，rule provider 取 vehicleType）；
    // 展示成「代理(…)/规则(…)」由屏幕按 isRuleProvider 本地化拼装
    val type: String,
    val vehicleType: String,
    val updatedAt: String,
    val isRuleProvider: Boolean,
)

@Immutable
data class ProviderErrorKey(
    val name: String,
    val isRuleProvider: Boolean,
)

/**
 * 单次刷新会话的进度。null 表示当前没有进行中的刷新。
 * [singleName] 非 null 时为单条刷新（dialog 显示 "正在更新 xxx…"），null 时为 updateAll
 * （dialog 显示 "done / total"）。
 */
data class RefreshProgress(
    val completed: Int,
    val total: Int,
    val singleName: String? = null,
)

@Immutable
data class ProviderUiState(
    val providers: ImmutableList<ProviderItemUi> = persistentListOf(),
    val isLoading: Boolean = false,
    val providerErrors: ImmutableMap<ProviderErrorKey, String> = persistentMapOf(),
    val refresh: RefreshProgress? = null,
)
