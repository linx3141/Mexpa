package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

const val MODE_GLOBAL = "global"
const val MODE_DIRECT = "direct"

data class ProxyGroupUi(
    val name: String = "",
    val type: String = "",
    val now: String = "",
    val all: ImmutableList<String> = persistentListOf(),
    val delays: ImmutableMap<String, Int> = persistentMapOf(),
    val nodeTypes: ImmutableMap<String, String> = persistentMapOf(),
    val icon: String = "",
    val fixed: String = "",
    val testUrl: String = "",
) {
    /** 对应 mihomo 的 `outboundgroup.SelectAble`；LoadBalance / Relay 不实现，PUT 返回 400 */
    val isSelectable: Boolean
        get() = isSelector ||
                type.equals("URLTest", ignoreCase = true) ||
                type.equals("Fallback", ignoreCase = true)

    /** 选中写 now；其余可选组写 fixed（now 仍是自动择优结果） */
    val isSelector: Boolean get() = type.equals("Selector", ignoreCase = true)

    val isFixed: Boolean get() = fixed.isNotEmpty()
}

@Immutable
data class ProxyUiState(
    val groups: ImmutableList<ProxyGroupUi> = persistentListOf(),
    val testingGroups: ImmutableSet<String> = persistentSetOf(),
    val testingNodes: ImmutableSet<String> = persistentSetOf(),
    val error: String = "",
    // mihomo 出站模式，小写；取不到配置时为空串
    val mode: String = "",
)
