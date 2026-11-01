package com.linx3141.mexpa.ui.platform

import androidx.compose.runtime.Composable

/** 共享 UI 的返回键入口：Android 接 Activity BackHandler，桌面端由桌面外壳处理。 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
