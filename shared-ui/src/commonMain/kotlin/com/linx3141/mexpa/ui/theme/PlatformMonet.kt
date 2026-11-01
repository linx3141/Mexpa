package com.linx3141.mexpa.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 平台动态取色种子；桌面没有系统 Monet，返回 null 后使用用户强调色。 */
@Composable
expect fun platformMonetSeedColor(isDark: Boolean): Color?
