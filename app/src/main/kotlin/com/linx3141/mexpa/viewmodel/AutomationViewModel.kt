package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.linx3141.mexpa.platform.ExternalControlPolicy
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys

/**
 * AutomationScreen 的 ViewModel：外部应用能否通过 intent 控制代理。
 *
 * 读写都经 [ExternalControlPolicy] 与 [StorageKeys.EXTERNAL_CONTROL_ALLOWED]，与
 * ExternalControlActivity 的判定同一处来源。
 */
class AutomationViewModel(
    private val storage: PlatformStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AutomationUiState(allowed = ExternalControlPolicy.isAllowed(storage))
    )
    val state: StateFlow<AutomationUiState> = _state.asStateFlow()

    fun setAllowed(allowed: Boolean) {
        storage.putString(StorageKeys.EXTERNAL_CONTROL_ALLOWED, allowed.toString())
        _state.value = AutomationUiState(allowed = allowed)
    }
}

@Immutable
data class AutomationUiState(
    val allowed: Boolean = false,
)
