package com.linx3141.mexpa.ui.screen.proxy

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** 代理组图标加载的平台钩子：Android 走 IconLoader，Linux 预览版默认不加载。 */
data class ProxyIconLoader(
    val version: Int = 0,
    val load: suspend (String) -> ImageBitmap? = { null },
)

val LocalProxyIconLoader = staticCompositionLocalOf { ProxyIconLoader() }
