package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.linx3141.mexpa.domain.model.MihomoConfig
import com.linx3141.mexpa.domain.model.SubscriptionInfo
import com.linx3141.mexpa.platform.NetworkAddressType
import com.linx3141.mexpa.platform.TunMode

data class HomeUiState(
    val isRunning: Boolean = false,
    val isStarting: Boolean = false,
    val isStopping: Boolean = false,
    val mode: String = "--",
    val tunStack: String = "",
    val tunMode: TunMode = TunMode.Vpn,
    val ipv6: Boolean = false,
    val config: MihomoConfig? = null,
    val subscription: SubscriptionInfo? = null,
    val providerTraffic: ImmutableList<ProviderTrafficInfo> = persistentListOf(),
    val isProviderTrafficLoading: Boolean = false,
    val providerTrafficLoadFailed: Boolean = false,
    val latencyBaidu: Int = -1,
    val latencyCloudflare: Int = -1,
    val latencyGoogle: Int = -1,
    val isTestingLatency: Boolean = false,
    // false = 本轮退回了组拨测、未经规则引擎，UI 据此标注
    val latencyViaRules: Boolean = true,
    val version: String = "",
    val errorMessage: String = "",
    val needsVpnPermission: Boolean = false,
    /** 当前活跃连接数；<0 表示未知（Android 不显示）。 */
    val connectionCount: Int = -1,
)

@Immutable
data class ProviderTrafficInfo(
    val id: String,
    val name: String,
    val nodeCount: Int,
    val updatedAt: String,
    // subscription-userinfo 缺失时 false：UI 仅隐藏流量面板，不整卡过滤
    val hasTraffic: Boolean,
    val upload: Long,
    val download: Long,
    val total: Long,
    val expire: Long,
)

// mihomo 的 "default" 特殊 provider（proxies: 段运行时聚合）的 vehicleType，非订阅集合，展示与更新都排除
const val VEHICLE_TYPE_COMPATIBLE = "Compatible"


/** 高频流量快照：每 100–500ms 更新，独立 Flow 隔离重组 */
@Immutable
data class SpeedSnapshot(
    val uploadSpeed: String = "-- B/s",
    val downloadSpeed: String = "-- B/s",
    val history: TrafficHistory = TrafficHistory(),
)

/** 主页连通性体检的三个探针；URL 一律用 http 明文，避免 TLS 握手把测量口径拉长 */
enum class LatencyProbe(val url: String) {
    Baidu("http://www.baidu.com"),
    Cloudflare("http://www.cloudflare.com/cdn-cgi/trace"),
    Google("http://www.google.com/generate_204"),
}

/** 高频内存快照 */
@Immutable
data class MemorySnapshot(
    val ramUsage: String = "-- MB",
    val ramTotal: String = "-- MB",
)

/**
 * 单条连接的瞬时速率，由相邻两次 `/connections` 快照的累计字节差分得出。
 * mihomo 只给累计量，不给速率——长连接的累计值会一直很大，据此排序看不出「此刻谁在跑」。
 */
@Immutable
data class ConnectionRate(
    val id: String,
    val host: String,
    val node: String,
    val uploadRate: Long,
    val downloadRate: Long,
)

/** 系统信息快照：网卡 + CPU，2s 一次 */
@Immutable
data class SystemInfoSnapshot(
    val localIp: String = "--",
    val interfaceName: String = "--",
    val addressType: NetworkAddressType = NetworkAddressType.Unknown,
    val cpuUsage: String = "--%",
)
