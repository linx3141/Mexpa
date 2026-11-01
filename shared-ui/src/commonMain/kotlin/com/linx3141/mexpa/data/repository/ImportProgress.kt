package com.linx3141.mexpa.data.repository

enum class ImportStep { Downloading, Prefetching, Validating, Other }

data class ImportProgress(
    val step: ImportStep,
    /** FetchProviders 阶段正在预取的 provider 名，用于「更新 {name} ({i}/{n})」文案。 */
    val providerName: String = "",
    /** [ImportStep.Other] 时携带 mihomo 原始 action 字符串。 */
    val rawLabel: String = "",
    val current: Int = 0,
    val total: Int = 0,
)
