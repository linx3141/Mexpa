package com.linx3141.mexpa.desktop

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.linx3141.mexpa.ui.theme.LocalAppDarkMode
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.desktop.liquid.IosLiquidGlassNavigationBar
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.MishkaNavigationItem
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.theme.MishkaColors
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState

@Composable
fun MiuixBottomNavigation(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    NavigationBar(color = MishkaColors.surface) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                icon = item.selectedIcon,
                label = item.label,
            )
        }
    }
}

@Composable
fun MiuixFloatingBottomNavigation(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop? = null,
    showLabels: Boolean = true,
    liquidGlass: Boolean = false,
) {
    if (liquidGlass) {
        val navigationItems = items.map { item ->
            top.yukonga.miuix.kmp.basic.NavigationItem(
                label = item.label,
                icon = item.selectedIcon,
            )
        }
        IosLiquidGlassNavigationBar(
            items = navigationItems,
            selectedIndex = selectedIndex,
            onItemClick = onSelect,
            backdrop = backdrop,
            isBlurActive = backdrop != null,
            isDark = LocalAppDarkMode.current,
            showLabels = showLabels,
        )
        return
    }
    val isDark = LocalAppDarkMode.current
    val shape = RoundedCornerShape(50.dp)
    val modifier = if (backdrop != null) {
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = 25f,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(
                    BlendColorEntry(color = MishkaColors.surfaceContainer.copy(alpha = 0.6f)),
                ),
            ),
            highlight = if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight,
        )
    } else {
        Modifier
    }
    FloatingNavigationBar(
        modifier = modifier,
        color = if (backdrop != null) Color.Transparent else MishkaColors.surfaceContainer,
        cornerRadius = 50.dp,
    ) {
        items.forEachIndexed { index, item ->
            MiuixFloatingNavigationItem(
                item = item,
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                showLabel = showLabels,
            )
        }
    }
}

@Composable
fun MiuixNavigationRail(
    items: List<MishkaNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val railState = rememberNavigationRailState(NavigationRailValue.Collapsed)
    NavigationRail(state = railState) {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                icon = item.selectedIcon,
                label = item.label,
            )
        }
    }
}

@Composable
private fun MiuixFloatingNavigationItem(
    item: MishkaNavigationItem,
    selected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val base = MishkaColors.onSurfaceContainer
    val tint = when {
        isPressed -> base.copy(alpha = if (selected) 0.7f else 0.5f)
        selected -> base
        else -> base.copy(alpha = 0.6f)
    }
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = if (showLabel) 56.dp else 48.dp, minHeight = 48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = if (showLabel) 8.dp else 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = if (showLabel) null else item.label,
            tint = tint,
        )
        if (showLabel) {
            Text(text = item.label, fontSize = 11.sp, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
