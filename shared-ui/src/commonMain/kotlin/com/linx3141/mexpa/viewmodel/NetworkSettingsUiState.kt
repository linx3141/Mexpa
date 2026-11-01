package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import com.linx3141.mexpa.domain.model.DnsOverride

@Immutable
data class NetworkSettingsUiState(
    val httpPort: Int? = null,
    val socksPort: Int? = null,
    val redirPort: Int? = null,
    val tproxyPort: Int? = null,
    val mixedPort: Int? = null,
    val allowLan: Boolean? = null,
    val ipv6: Boolean? = null,
    val bindAddress: String? = null,
    val logLevel: String? = null,
    val dns: DnsOverride? = null,
)
