@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.linx3141.mexpa.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.component.design.InfiniteProgressIndicator
import com.linx3141.mexpa.ui.theme.LocalAnimationSpeed
import kotlin.math.roundToInt

/**
 * 首页 Material 的全局状态控制层：放在 MaterialMainShell 的 Scaffold 之上，
 * scrim 能盖住顶栏/底栏，FAB menu 也保持同一个实例的展开动画。
 */
@Composable
fun HomeFabOverlay(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isRunning: Boolean,
    isStarting: Boolean,
    isStopping: Boolean,
    onStart: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    onReload: () -> Unit,
    bottomPadding: Dp,
    pagerState: PagerState,
) {
    val animationSpeed = LocalAnimationSpeed.current.coerceAtLeast(0.01f)
    val scrimAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.42f else 0f,
        animationSpec = tween(durationMillis = (300 / animationSpeed).roundToInt().coerceAtLeast(1)),
        label = "home_fab_scrim",
    )
    BackHandler(expanded) { onExpandedChange(false) }
    val fabContainerColor = MaterialTheme.colorScheme.primaryContainer
    val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val fabDescription = when {
        isRunning -> if (expanded) {
            stringResource(R.string.common_close)
        } else {
            stringResource(R.string.common_more)
        }
        isStarting -> stringResource(R.string.home_starting)
        isStopping -> stringResource(R.string.home_stopping)
        else -> stringResource(R.string.home_start)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (scrimAlpha > 0f) {
            val closeLabel = stringResource(R.string.common_close)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = closeLabel,
                    ) { onExpandedChange(false) }
                    .semantics { contentDescription = closeLabel },
            )
        }

        FloatingActionButtonMenu(
            expanded = expanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = bottomPadding + 8.dp)
                .graphicsLayer {
                    // 让 FAB 跟着 Home 页一起被 pager 平移，而不是切页时瞬间消失
                    val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    translationX = -pageOffset * pagerState.layoutInfo.viewportSize.width
                },
            button = {
                when {
                    isRunning -> ToggleFloatingActionButton(
                        checked = expanded,
                        onCheckedChange = { onExpandedChange(it) },
                        modifier = Modifier.semantics { contentDescription = fabDescription },
                        containerColor = { fabContainerColor },
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = fabContentColor,
                        )
                    }

                    isStarting || isStopping -> Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = fabDescription
                                disabled()
                            },
                        shape = MaterialTheme.shapes.large,
                        color = fabContainerColor,
                        contentColor = fabContentColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            InfiniteProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    else -> FloatingActionButton(
                        onClick = onStart,
                        modifier = Modifier.semantics { contentDescription = fabDescription },
                        containerColor = fabContainerColor,
                        contentColor = fabContentColor,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                        )
                    }
                }
            },
        ) {
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onReload()
                },
                text = { Text(stringResource(R.string.home_reload)) },
                icon = { Icon(Icons.Filled.Sync, null) },
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onRestart()
                },
                text = { Text(stringResource(R.string.home_restart)) },
                icon = { Icon(Icons.Filled.RestartAlt, null) },
            )
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onStop()
                },
                text = { Text(stringResource(R.string.home_stop)) },
                icon = { Icon(Icons.Filled.Stop, null) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
