package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.linx3141.mexpa.domain.model.OverrideProfile

data class OverrideProfileUiState(
    val profiles: ImmutableList<OverrideProfile> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String = "",
)
