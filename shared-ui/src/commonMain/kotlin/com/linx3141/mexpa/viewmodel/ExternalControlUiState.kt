package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable

@Immutable
data class ExternalControlUiState(
    val externalController: String? = null,
    val secret: String? = null,
)
