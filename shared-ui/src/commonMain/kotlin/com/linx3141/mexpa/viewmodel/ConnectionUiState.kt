package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.linx3141.mexpa.domain.model.ConnectionInfo

data class ConnectionUiState(
    val connections: ImmutableList<ConnectionInfo> = persistentListOf(),
    val downloadTotal: Long = 0,
    val uploadTotal: Long = 0,
    val searchQuery: String = "",
    val isConnected: Boolean = false,
    val error: String = "",
)
