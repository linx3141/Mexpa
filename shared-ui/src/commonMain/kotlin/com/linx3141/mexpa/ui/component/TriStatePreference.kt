package com.linx3141.mexpa.ui.component

import com.linx3141.mexpa.shared.resources.*



import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.linx3141.mexpa.ui.component.design.OverlayDropdownPreference

/**
 * 三态偏好组件，用于覆写设置中的三态 Boolean。
 * - null: 不修改（使用订阅配置原值）
 * - true: 启用
 * - false: 禁用
 */
@Composable
fun TriStatePreference(
    title: String,
    value: Boolean?,
    onValueChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val notModified = stringResource(Res.string.common_not_modified)
    val enabledLabel = stringResource(Res.string.common_enabled)
    val disabledLabel = stringResource(Res.string.common_disabled)
    val items = remember(notModified, enabledLabel, disabledLabel) {
        listOf(notModified, enabledLabel, disabledLabel)
    }
    val selectedIndex = when (value) {
        null -> 0
        true -> 1
        false -> 2
    }

    OverlayDropdownPreference(
        title = title,
        modifier = modifier,
        summary = items[selectedIndex],
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            onValueChange(
                when (index) {
                    1 -> true
                    2 -> false
                    else -> null
                }
            )
        },
        enabled = enabled,
    )
}
