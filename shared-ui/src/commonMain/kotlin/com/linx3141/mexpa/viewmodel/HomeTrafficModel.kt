package com.linx3141.mexpa.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 折线图窗口点数；mihomo `/traffic` 为 1Hz 推送，即约 1 分钟历史 */
const val TRAFFIC_HISTORY_CAPACITY = 60

/**
 * 流量历史窗口：最近 [TRAFFIC_HISTORY_CAPACITY] 个采样点的原始字节速率，用于绘制折线图。
 * [seq] 单调递增，UI 侧既拿它作滑入动画的 key 也作动画目标值。
 */
@Immutable
data class TrafficHistory(
    val up: ImmutableList<Long> = persistentListOf(),
    val down: ImmutableList<Long> = persistentListOf(),
    val seq: Int = 0,
)
