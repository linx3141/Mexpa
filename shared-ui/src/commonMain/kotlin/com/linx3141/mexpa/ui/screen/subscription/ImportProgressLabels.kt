package com.linx3141.mexpa.ui.screen.subscription

import com.linx3141.mexpa.shared.resources.*



import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.linx3141.mexpa.data.repository.ImportProgress
import com.linx3141.mexpa.data.repository.ImportStep

/**
 * 把 data 层的结构化 [ImportProgress] 映射为本地化文案。
 * 本地化归 UI 层：data 只发 [ImportStep] 语义步骤，不依赖字符串资源。
 */
@Composable
fun importStepLabel(p: ImportProgress): String = when (p.step) {
    ImportStep.Downloading -> stringResource(Res.string.subscription_downloading)
    ImportStep.Prefetching ->
        if (p.providerName.isNotEmpty() && p.total > 0) {
            stringResource(Res.string.subscription_updating_progress, p.providerName, p.current + 1, p.total)
        } else {
            stringResource(Res.string.subscription_prefetching)
        }

    ImportStep.Validating -> stringResource(Res.string.subscription_validating)
    ImportStep.Other -> p.rawLabel
}
