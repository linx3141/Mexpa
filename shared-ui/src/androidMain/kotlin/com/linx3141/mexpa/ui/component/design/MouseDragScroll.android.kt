package com.linx3141.mexpa.ui.component.design

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.mouseDragScroll(state: ScrollableState, enabled: Boolean): Modifier = this
