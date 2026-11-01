package com.linx3141.mexpa.ui.screen.overrides

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.linx3141.mexpa.domain.model.OverrideFormat
import com.linx3141.mexpa.domain.model.OverrideProfile
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ClearOverrideErrorOnExit(onClearError: () -> Unit) {
    DisposableEffect(Unit) { onDispose { onClearError() } }
}

@Composable
fun OverrideBackButton(onBack: () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    MishkaBackButton(
        onClick = onBack,
        contentDescription = stringResource(Res.string.common_back),
    )
}

@Composable
fun overrideSummary(profile: OverrideProfile): String = stringResource(
    if (profile.isRemote) Res.string.override_source_remote else Res.string.override_source_local,
) + " · " + stringResource(
    if (profile.format == OverrideFormat.Yaml) {
        Res.string.override_format_yaml
    } else {
        Res.string.override_format_javascript
    },
)
