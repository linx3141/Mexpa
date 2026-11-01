package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.linx3141.mexpa.domain.model.DnsAnswer

@Immutable
data class DnsQueryUiState(
    val queryName: String = "",
    val queryType: String = "A",
    val answers: ImmutableList<DnsAnswer> = persistentListOf(),
    val status: Int? = null,
    val isQuerying: Boolean = false,
    val error: String = "",
)
