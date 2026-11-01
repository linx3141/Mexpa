package com.linx3141.mexpa.ui.component.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
actual fun Modifier.platformOverScrollVertical(): Modifier = overScrollVertical()

@Composable
actual fun Modifier.platformScrollEndHaptic(): Modifier = scrollEndHaptic()
