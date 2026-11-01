package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import com.linx3141.mexpa.domain.model.LogMessage

@Immutable
data class LogUiState(
    val isConnected: Boolean = false,
    val level: String = "info",
)

@Immutable
data class IndexedLog(val id: Long, val message: LogMessage)
