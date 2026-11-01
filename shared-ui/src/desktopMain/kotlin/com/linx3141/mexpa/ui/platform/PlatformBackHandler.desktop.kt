package com.linx3141.mexpa.ui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Compose Desktop 的窗口级返回键后续在桌面外壳中处理；共享页面先不绑定。
}
