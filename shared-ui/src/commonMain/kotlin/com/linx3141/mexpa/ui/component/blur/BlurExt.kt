package com.linx3141.mexpa.ui.component.blur

import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.linx3141.mexpa.ui.theme.LocalUiMode
import com.linx3141.mexpa.ui.theme.TopBarBlurStyle
import com.linx3141.mexpa.ui.theme.UiMode
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalBlurEnabled = staticCompositionLocalOf { true }
val LocalTopBarBlurStyle = staticCompositionLocalOf { TopBarBlurStyle.Gaussian }

@Composable
fun Modifier.defaultBlurEffect(
    backdrop: LayerBackdrop,
    shape: Shape = RectangleShape,
    blendColor: Color? = null,
): Modifier {
    val tint = if (LocalUiMode.current == UiMode.Material) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MishkaColors.surface
    }
    return this.textureBlur(
        backdrop = backdrop,
        shape = shape,
        blurRadius = 25f,
        colors = BlurColors(
            blendColors = listOf(
                BlendColorEntry(color = blendColor ?: tint.copy(0.8f)),
            ),
        ),
    )
}

@Composable
fun rememberBlurEnabled(): State<Boolean> =
    rememberUpdatedState(LocalBlurEnabled.current && isRuntimeShaderSupported())

@Composable
fun rememberBlurBackdrop(enabled: Boolean = LocalBlurEnabled.current): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = if (LocalUiMode.current == UiMode.Material) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MishkaColors.surface
    }
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = rememberBlurEnabled().value,
    blurStyle: TopBarBlurStyle = LocalTopBarBlurStyle.current,
    shape: Shape = RectangleShape,
    blendColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val progressive = blurStyle == TopBarBlurStyle.Progressive
    Box(
        modifier = if (blurActive && backdrop != null && !progressive) {
            Modifier.defaultBlurEffect(backdrop, shape, blendColor)
        } else {
            Modifier
        },
    ) {
        if (blurActive && backdrop != null && progressive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = shape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = BlurColors(
                            blendColors = listOf(
                                BlendColorEntry(
                                    color = blendColor ?: if (LocalUiMode.current == UiMode.Material) {
                                        MaterialTheme.colorScheme.surfaceContainer.copy(0.3f)
                                    } else {
                                        MishkaColors.surface.copy(0.3f)
                                    },
                                ),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}
