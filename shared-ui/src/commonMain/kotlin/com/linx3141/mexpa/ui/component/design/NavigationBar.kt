@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.linx3141.mexpa.ui.component.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.MishkaColors
import com.linx3141.mexpa.ui.theme.TopBarBlurStyle
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

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
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
) {
    val blurActive = backdrop != null
    val tabsContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(
                    horizontal = FloatingToolbarEdgePadding,
                    vertical = FloatingToolbarVerticalPadding,
                )
                .selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                FloatingNavigationTab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
            }
        }
    }
    Surface(
        color = if (blurActive) Color.Transparent else containerColor,
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

/**
 * 可变宽度 Tab：选中项 = 图标 + 文字，未选中 = 只有图标。
 *
 * 每个 tab 的宽度直接由选中态驱动（选中宽度用 [TextMeasurer] 预先量好），再用弹簧动画过渡；
 * 不要用 `AnimatedVisibility` 的 intrinsic 让父布局推宽度——切换选中项时退场文字仍被组合，
 * intrinsic 会按完整文字宽度算，底栏会先被撑到最长再缩回，而且缩回是跳变没有动画。
 */
@Composable
private fun FloatingNavigationTab(
    item: MishkaNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLargeEmphasized
    val iconOnlyWidth = FloatingToolbarIconSize + FloatingToolbarTabHorizontalPadding * 2
    val selectedWidth = remember(item.label, labelStyle, density, textMeasurer) {
        val labelWidthPx = textMeasurer.measure(
            text = item.label,
            style = labelStyle,
            maxLines = 1,
            softWrap = false,
        ).size.width
        with(density) { iconOnlyWidth + FloatingToolbarIconLabelSpacing + labelWidthPx.toDp() }
    }
    val targetWidthPx = with(density) { (if (selected) selectedWidth else iconOnlyWidth).toPx() }
    val animatedWidthPx by animateFloatAsState(
        targetValue = targetWidthPx,
        animationSpec = folmeSpring(damping = 0.9f, response = 0.35f),
        label = "floatingTabWidth",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = folmeSpring(damping = 0.9f, response = 0.3f),
        label = "floatingTabLabelAlpha",
    )
    val animatedWidth = with(density) { animatedWidthPx.toDp() }
    Row(
        modifier = Modifier
            .height(FloatingToolbarTabHeight)
            .width(animatedWidth)
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
            .clipToBounds()
            .padding(horizontal = FloatingToolbarTabHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = if (selected) null else item.label,
            modifier = Modifier.size(FloatingToolbarIconSize),
            tint = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        // 标签随宽度展开/收起：宽度裁剪 + 淡入淡出，退场结束后不再组合
        if (labelAlpha > 0.01f) {
            Text(
                text = item.label,
                modifier = Modifier
                    .padding(start = FloatingToolbarIconLabelSpacing)
                    .graphicsLayer { alpha = labelAlpha },
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

private val FloatingToolbarEdgePadding: Dp = 8.dp
private val FloatingToolbarTabHorizontalPadding: Dp = 12.dp
private val FloatingToolbarIconSize: Dp = 24.dp
private val FloatingToolbarIconLabelSpacing: Dp = 8.dp
private val FloatingToolbarTabHeight: Dp = 48.dp
private val FloatingToolbarVerticalPadding: Dp = 8.dp
private val FloatingToolbarShadowElevation: Dp = 3.dp

@Composable
fun ExpressiveNavigationRail(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expandContentDescription: String = "",
    collapseContentDescription: String = "",
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
                    contentDescription = if (isExpanded) collapseContentDescription else expandContentDescription,
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
