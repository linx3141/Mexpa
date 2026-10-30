package com.linx3141.mexpa.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.linx3141.mexpa.domain.model.ConnectionsResponse
import com.linx3141.mexpa.domain.model.DelayResult
import com.linx3141.mexpa.domain.model.DnsQueryResponse
import com.linx3141.mexpa.domain.model.GroupsResponse
import com.linx3141.mexpa.domain.model.LogMessage
import com.linx3141.mexpa.domain.model.MemoryData
import com.linx3141.mexpa.domain.model.MihomoConfig
import com.linx3141.mexpa.domain.model.MihomoVersion
import com.linx3141.mexpa.domain.model.ProvidersResponse
import com.linx3141.mexpa.domain.model.ProxiesResponse
import com.linx3141.mexpa.domain.model.RuleProvidersResponse
import com.linx3141.mexpa.domain.model.RulesResponse
import com.linx3141.mexpa.domain.model.TrafficData

/**
 * mihomo runtime 客户端门面：REST + WebSocket 流。实现见 data 层 `MihomoRepositoryImpl`。
 */
interface MihomoRepository {

    val connectionState: StateFlow<Boolean>

    fun trafficFlow(): Flow<TrafficData>
    fun logsFlow(level: String = "info"): Flow<LogMessage>
    fun memoryFlow(): Flow<MemoryData>
    fun connectionsFlow(): Flow<ConnectionsResponse>

    suspend fun getVersion(): Result<MihomoVersion>
    suspend fun getConfig(): Result<MihomoConfig>
    suspend fun getProxies(): Result<ProxiesResponse>
    suspend fun getGroups(): Result<GroupsResponse>
    suspend fun selectProxy(group: String, name: String): Result<Unit>
    suspend fun unfixProxy(group: String): Result<Unit>
    suspend fun getGroupDelay(
        name: String,
        testUrl: String = "http://www.gstatic.com/generate_204",
        timeout: Int = 5000,
    ): Result<Map<String, Int>>

    suspend fun getProxyDelay(
        name: String,
        testUrl: String = "http://www.gstatic.com/generate_204",
        timeout: Int = 5000,
    ): Result<DelayResult>

    suspend fun getProviderProxyDelay(
        provider: String,
        name: String,
        testUrl: String = "http://www.gstatic.com/generate_204",
        timeout: Int = 5000,
    ): Result<DelayResult>

    suspend fun getRules(): Result<RulesResponse>
    suspend fun getConnections(): Result<ConnectionsResponse>
    suspend fun closeAllConnections(): Result<Unit>
    suspend fun closeConnection(id: String): Result<Unit>
    suspend fun getProviders(): Result<ProvidersResponse>
    suspend fun updateProvider(name: String): Result<Unit>
    suspend fun getRuleProviders(): Result<RuleProvidersResponse>
    suspend fun updateRuleProvider(name: String): Result<Unit>
    suspend fun queryDns(name: String, type: String = "A"): Result<DnsQueryResponse>
    suspend fun updateSystemDns(servers: List<String>): Result<Unit>
    suspend fun flushFakeIp(): Result<Unit>
    suspend fun flushDnsCache(): Result<Unit>

    fun close()
}
