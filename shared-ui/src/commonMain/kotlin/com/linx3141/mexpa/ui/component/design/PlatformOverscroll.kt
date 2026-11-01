package com.linx3141.mexpa.ui.component.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Miuix 的滚动回弹与滚动结束触觉反馈是移动端手势的视觉/触觉补偿。
 * 桌面端没有触摸、也不该在鼠标滚轮/拖动时出现橡皮筋回弹，直接返回原 Modifier。
 */
@Composable
expect fun Modifier.platformOverScrollVertical(): Modifier

@Composable
expect fun Modifier.platformScrollEndHaptic(): Modifier
