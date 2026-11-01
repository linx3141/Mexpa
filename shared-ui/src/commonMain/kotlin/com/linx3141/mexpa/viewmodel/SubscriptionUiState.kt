package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.linx3141.mexpa.data.repository.ImportProgress
import com.linx3141.mexpa.domain.model.Subscription

enum class ProfileOperation { Import, Update, Edit }

@Immutable
data class SubscriptionUiState(
    val subscriptions: ImmutableList<Subscription> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String = "",
    val showAddDialog: Boolean = false,
    val importProgress: ImportProgress? = null,
    val updateAll: UpdateAllProgress? = null,
    val operation: ProfileOperation? = null,
)

/**
 * 批量"全部更新"进度。`completed` = 已完成条目数（0-based 正在处理的是第 completed+1 条）。
 * `currentStep` 由 ProfileProcessor.onProgress 回调推进，null 表示当前订阅还未开始内部阶段；
 * 本地化文案在 UI 层按 [ImportProgress] 映射。
 */
@Immutable
data class UpdateAllProgress(
    val completed: Int,
    val total: Int,
    val currentName: String,
    val currentStep: ImportProgress? = null,
)
