@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.TopBarBlurStyle
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * Material 模式的主 Tab 导航件，逐项对齐 KernelSU：
 * - 底栏：[ShortNavigationBar]，选中/未选中使用 filled/outlined 两套图标；
 * - 宽屏：[WideNavigationRail]，可展开/收起，header 放切换按钮，图标同样分选中态。
 * Miuix 模式仍走 miuix 的 NavigationBar/NavigationRail（毛玻璃、浮动底栏、可展开侧栏）。
 */
@Immutable
data class MishkaNavigationItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun ExpressiveNavigationBar(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    ShortNavigationBar(
        modifier = modifier,
        containerColor = containerColor,
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun FloatingExpressiveNavigationBar(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
) {
    val scrollState = rememberScrollState()
    val blurActive = backdrop != null
    val tabsContent: @Composable () -> Unit = {
        ScrollableTabsLayout(
            tabs = items.mapIndexed { index, item ->
                {
                    FloatingNavigationTab(
                        item = item,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                    )
                }
            },
            scrollState = scrollState,
            selectedTabIndex = selectedIndex,
        )
    }
    Surface(
        color = if (blurActive) Color.Transparent else MaterialTheme.colorScheme.surfaceBright,
        shadowElevation = FloatingToolbarShadowElevation,
        shape = CircleShape,
        modifier = modifier.wrapContentSize(Alignment.Center),
    ) {
        if (blurActive && backdrop != null) {
            BlurredBar(
                backdrop = backdrop,
                blurActive = true,
                blurStyle = TopBarBlurStyle.Gaussian,
                shape = CircleShape,
                blendColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.78f),
            ) {
                tabsContent()
            }
        } else {
            tabsContent()
        }
    }
}

@Composable
private fun FloatingNavigationTab(
    item: MishkaNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onClick()
                },
            )
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center),
        ) {
            Icon(
                imageVector = item.selectedIcon,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = item.label,
            style = if (selected) {
                MaterialTheme.typography.labelLargeEmphasized
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

private val FloatingToolbarMaxWidth: Dp = 380.dp
private val FloatingToolbarEdgePadding: Dp = 8.dp
private val FloatingToolbarMinTabWidth: Dp = 70.dp
private val FloatingToolbarVerticalPadding: Dp = 8.dp
private val FloatingToolbarShadowElevation: Dp = 3.dp

@Composable
private fun ScrollableTabsLayout(
    tabs: List<@Composable () -> Unit>,
    scrollState: ScrollState,
    selectedTabIndex: Int,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabsScrollState = remember(scrollState, coroutineScope) {
        TabsScrollState(scrollState = scrollState, coroutineScope = coroutineScope)
    }

    val tabsContent: @Composable () -> Unit = { tabs.forEach { it() } }
    Layout(
        contents = listOf(tabsContent),
        modifier = Modifier
            .widthIn(max = FloatingToolbarMaxWidth)
            .wrapContentSize(align = Alignment.Center)
            .padding(vertical = FloatingToolbarVerticalPadding)
            .horizontalScroll(scrollState)
            .selectableGroup()
            .clipToBounds(),
    ) { (tabMeasurables), constraints ->
        val edgePaddingPx = FloatingToolbarEdgePadding.roundToPx()
        val minTabWidthPx = FloatingToolbarMinTabWidth.roundToPx()
        val layoutHeightPx = tabMeasurables.maxIntrinsicHeight()

        val tabConstraints = constraints.copy(
            minWidth = minTabWidthPx,
            minHeight = layoutHeightPx,
            maxHeight = layoutHeightPx,
        )
        val tabPlaceables = tabMeasurables.fastMap { it.measure(tabConstraints) }

        val (accumulatedWidthPx, tabPositions) =
            tabPlaceables.fold(
                initial = Pair(edgePaddingPx, mutableListOf<TabPosition>()),
            ) { (accumulatedWidth, accumulatedPositions), placeable ->
                val tabWidthPx = maxOf(minTabWidthPx, placeable.width)
                accumulatedPositions.add(
                    TabPosition(leftPx = accumulatedWidth, widthPx = tabWidthPx),
                )
                Pair(accumulatedWidth + tabWidthPx, accumulatedPositions)
            }
        val totalLayoutWidthPx = accumulatedWidthPx + edgePaddingPx

        layout(totalLayoutWidthPx, layoutHeightPx) {
            tabPlaceables.fastForEachIndexed { index, placeable ->
                placeable.placeRelative(x = tabPositions[index].leftPx, y = 0)
            }
            tabsScrollState.scrollToSelectedTab(
                tabPositions = tabPositions,
                selectedTab = selectedTabIndex,
                totalWidthPx = totalLayoutWidthPx,
            )
        }
    }
}

private data class TabPosition(val leftPx: Int, val widthPx: Int)

private class TabsScrollState(
    private val scrollState: ScrollState,
    private val coroutineScope: CoroutineScope,
) {
    private var selectedTab: Int? = null

    fun scrollToSelectedTab(tabPositions: List<TabPosition>, selectedTab: Int, totalWidthPx: Int) {
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab
            tabPositions.getOrNull(selectedTab)?.let { selectedTabPosition ->
                val targetScrollOffset = offsetRequiredToCenterTheTab(
                    tabPosition = selectedTabPosition,
                    totalToolbarWidth = totalWidthPx,
                    visibleRightOffset = scrollState.maxValue,
                )
                if (scrollState.value != targetScrollOffset) {
                    coroutineScope.launch { scrollState.animateScrollTo(targetScrollOffset) }
                }
            }
        }
    }

    private fun offsetRequiredToCenterTheTab(
        tabPosition: TabPosition,
        totalToolbarWidth: Int,
        visibleRightOffset: Int,
    ): Int {
        val visibleToolbarWidth = totalToolbarWidth - visibleRightOffset
        val currentTabStart = tabPosition.leftPx
        val visibleToolbarCenter = visibleToolbarWidth / 2
        val tabHalfWidth = tabPosition.widthPx / 2
        val centeredTabOffset = currentTabStart - (visibleToolbarCenter - tabHalfWidth)
        return centeredTabOffset.coerceIn(0, visibleRightOffset.coerceAtLeast(0))
    }
}

private fun List<Measurable>.maxIntrinsicHeight() =
    fastFold(initial = 0) { current, measurable ->
        maxOf(current, measurable.maxIntrinsicHeight(Constraints.Infinity))
    }

@Composable
fun ExpressiveNavigationRail(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberWideNavigationRailState(
        initialValue = if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed,
    )
    val scope = rememberCoroutineScope()
    val isExpanded = state.targetValue == WideNavigationRailValue.Expanded
    LaunchedEffect(state.targetValue) {
        onExpandedChange(state.targetValue == WideNavigationRailValue.Expanded)
    }
    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = state,
        colors = WideNavigationRailDefaults.colors().copy(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Start + WindowInsetsSides.Vertical,
        ),
        contentPadding = PaddingValues(vertical = 20.dp),
        header = {
            IconButton(
                modifier = Modifier.padding(start = 24.dp),
                onClick = {
                    scope.launch {
                        if (isExpanded) state.collapse() else state.expand()
                    }
                },
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.nav_rail_collapse else R.string.nav_rail_expand,
                    ),
                )
            }
        },
    ) {
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            WideNavigationRailItem(
                railExpanded = isExpanded,
                selected = selected,
                onClick = { if (!selected) onSelect(index) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}

/**
 * 顶栏返回键，对齐 KernelSU 的 `TopBarBackButton`：
 * Miuix 模式是原来的 miuix IconButton（RTL 由调用方镜像，这里收进来统一处理）；
 * Material 模式是 `surfaceContainerHighest` 底的 filled tonal IconButton。
 */
@Composable
fun MishkaBackButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    if (LocalUiMode.current == UiMode.Miuix) {
        MiuixIconButton(onClick = onClick, modifier = modifier) {
            val layoutDirection = LocalLayoutDirection.current
            MiuixIcon(
                imageVector = MiuixIcons.Back,
                contentDescription = contentDescription,
                tint = if (tint == Color.Unspecified) MishkaColors.onSurface else tint,
                modifier = Modifier.graphicsLayer {
                    scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                },
            )
        }
        return
    }
    IconButton(
        // KernelSU TopBarBackButton 同款：横向 10dp 留白，避免和标题/屏幕边缘挤在一起
        modifier = modifier.padding(horizontal = 10.dp),
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
        )
    }
}
