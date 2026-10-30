package com.linx3141.mexpa.ui.screen.overrides

import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import com.linx3141.mexpa.R
import com.linx3141.mexpa.domain.model.OverrideFormat
import com.linx3141.mexpa.domain.model.OverrideProfile
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ClearOverrideErrorOnExit(viewModel: OverrideProfileViewModel) {
    DisposableEffect(viewModel) { onDispose { viewModel.clearError() } }
}

@Composable
internal fun OverrideBackButton(onBack: () -> Unit) {
    val layoutDirection = LocalLayoutDirection.current
    MishkaBackButton(
        onClick = onBack,
        contentDescription = stringResource(R.string.common_back),
    )
}

@Composable
internal fun overrideSummary(profile: OverrideProfile): String = stringResource(
    if (profile.isRemote) R.string.override_source_remote else R.string.override_source_local,
) + " · " + stringResource(
    if (profile.format == OverrideFormat.Yaml) {
        R.string.override_format_yaml
    } else {
        R.string.override_format_javascript
    },
)
