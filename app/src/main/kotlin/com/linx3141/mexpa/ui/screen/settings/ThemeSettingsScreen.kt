package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.materialkolor.dynamiccolor.ColorSpec
import com.linx3141.mexpa.R
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.theme.BottomBarMode
import com.linx3141.mexpa.ui.theme.FloatingBottomBarStyle
import com.linx3141.mexpa.ui.theme.MaxAnimationSpeed
import com.linx3141.mexpa.ui.theme.MaxDensityScale
import com.linx3141.mexpa.ui.theme.MinAnimationSpeed
import com.linx3141.mexpa.ui.theme.MinDensityScale
import com.linx3141.mexpa.ui.theme.ThemeAccentColor
import com.linx3141.mexpa.ui.theme.ThemeColorModes
import com.linx3141.mexpa.ui.theme.ThemeUiModes
import com.linx3141.mexpa.ui.theme.ThemeConfig
import com.linx3141.mexpa.ui.theme.UiMode
import com.linx3141.mexpa.ui.theme.ThemePaletteStyles
import com.linx3141.mexpa.ui.theme.TopBarBlurStyle
import com.linx3141.mexpa.ui.theme.label
import com.linx3141.mexpa.ui.theme.normalizeAnimationSpeed
import com.linx3141.mexpa.ui.theme.normalizeDensityScale
import com.linx3141.mexpa.ui.theme.summary
import com.linx3141.mexpa.ui.theme.themeColorModeLabel
import com.linx3141.mexpa.ui.theme.writeThemeConfig
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import com.linx3141.mexpa.ui.component.design.OverlayDropdownPreference
import com.linx3141.mexpa.ui.component.design.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import com.linx3141.mexpa.ui.component.design.WindowDialog
import kotlin.math.roundToInt

@Composable
fun ThemeSettingsScreen(
    storage: PlatformStorage,
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onPredictiveBackChange: ((Boolean) -> Unit)? = null,
    swipeDismissEnabled: Boolean = true,
    onSwipeDismissChange: ((Boolean) -> Unit)? = null,
    onBack: () -> Unit = {},
) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    var isPredictiveBackEnabled by remember {
        mutableStateOf(storage.getString(StorageKeys.PREDICTIVE_BACK, "false") == "true")
    }
    var densityScaleDraft by remember { mutableFloatStateOf((themeConfig.densityScale * 100f).roundToInt().toFloat()) }
    var showDensityScaleDialog by remember { mutableStateOf(false) }
    val densityScaleTextState = rememberTextFieldState()
    var animationSpeedDraft by remember {
        mutableFloatStateOf((themeConfig.animationSpeed * 100f).roundToInt().toFloat())
    }
    var showAnimationSpeedDialog by remember { mutableStateOf(false) }
    val animationSpeedTextState = rememberTextFieldState()

    LaunchedEffect(themeConfig.densityScale) {
        densityScaleDraft = (themeConfig.densityScale * 100f).roundToInt().toFloat()
    }
    LaunchedEffect(themeConfig.animationSpeed) {
        animationSpeedDraft = (themeConfig.animationSpeed * 100f).roundToInt().toFloat()
    }

    fun updateTheme(next: ThemeConfig) {
        writeThemeConfig(storage, next)
        onThemeConfigChange(next)
    }

    // 浮动底栏 / 底栏图标模式仍是 Miuix 专属；模糊两套都提供
    val showMiuixOnlyOptions = themeConfig.uiMode == UiMode.Miuix

    fun updateDensityScale(percent: Float) {
        val nextPercent = percent.roundToInt().coerceIn(
            (MinDensityScale * 100f).roundToInt(),
            (MaxDensityScale * 100f).roundToInt(),
        )
        val nextScale = normalizeDensityScale(nextPercent / 100f)
        densityScaleDraft = nextScale * 100f
        updateTheme(themeConfig.copy(densityScale = nextScale))
    }

    fun openDensityScaleDialog() {
        densityScaleTextState.setTextAndPlaceCursorAtEnd(densityScaleDraft.roundToInt().toString())
        showDensityScaleDialog = true
    }

    fun updateAnimationSpeed(percent: Float) {
        val nextPercent = percent.roundToInt().coerceIn(
            (MinAnimationSpeed * 100f).roundToInt(),
            (MaxAnimationSpeed * 100f).roundToInt(),
        )
        val nextSpeed = normalizeAnimationSpeed(nextPercent / 100f)
        animationSpeedDraft = nextSpeed * 100f
        updateTheme(themeConfig.copy(animationSpeed = nextSpeed))
    }

    fun openAnimationSpeedDialog() {
        animationSpeedTextState.setTextAndPlaceCursorAtEnd(animationSpeedDraft.roundToInt().toString())
        showAnimationSpeedDialog = true
    }

    val themeItems = ThemeColorModes.map { mode -> themeColorModeLabel(mode) }
    val paletteStyles = ThemePaletteStyles
    val paletteItems = paletteStyles.map { style -> style.label() }
    val selectedPaletteIndex = paletteStyles.indexOf(themeConfig.paletteStyle).coerceAtLeast(0)
    val colorSpecs = ColorSpec.SpecVersion.entries
    val selectedColorSpecIndex = colorSpecs.indexOf(themeConfig.colorSpec).coerceAtLeast(0)
    val accentOptions = ThemeAccentColor.entries.toList()
    val accentItems = accentOptions.map { accent -> accent.label() }
    val selectedAccentIndex = accentOptions.indexOf(themeConfig.accentColor).coerceAtLeast(0)
    val floatingBottomBarStyles = FloatingBottomBarStyle.entries.toList()
    val floatingBottomBarStyleItems = floatingBottomBarStyles.map { style -> style.label() }
    val selectedFloatingBottomBarStyleIndex =
        floatingBottomBarStyles.indexOf(themeConfig.floatingBottomBarStyle).coerceAtLeast(0)
    val bottomBarModes = BottomBarMode.entries.toList()
    val bottomBarModeItems = bottomBarModes.map { mode -> mode.label() }
    val selectedBottomBarModeIndex = bottomBarModes.indexOf(themeConfig.bottomBarMode).coerceAtLeast(0)
    val pagerInterceptionModes = PagerInterceptionMode.entries.toList()
    val pagerInterceptionModeItems = pagerInterceptionModes.map { mode -> mode.label() }
    val selectedPagerInterceptionModeIndex =
        pagerInterceptionModes.indexOf(themeConfig.pagerInterceptionMode).coerceAtLeast(0)
    val topBarBlurStyles = TopBarBlurStyle.entries.toList()
    val topBarBlurStyleItems = topBarBlurStyles.map { style -> style.label() }
    val topBarBlurStyleSummaries = topBarBlurStyles.map { style -> style.summary() }
    val selectedTopBarBlurStyleIndex = topBarBlurStyles.indexOf(themeConfig.topBarBlurStyle).coerceAtLeast(0)
    val isBlurSupported = isRuntimeShaderSupported()

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.settings_theme_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            item { SmallTitle(text = stringResource(R.string.settings_theme_group_color)) }
            groupedCardItems(
                keyPrefix = "theme_color",
                items = buildList {
                    add(CardItem("mode") {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_theme_mode),
                            summary = themeItems.getOrElse(themeConfig.colorMode) { themeItems.first() },
                            items = themeItems,
                            selectedIndex = themeConfig.colorMode,
                            onSelectedIndexChange = { index ->
                                updateTheme(themeConfig.copy(colorMode = index))
                            },
                        )
                    })
                    add(CardItem("monet") {
                        SwitchPreference(
                            title = stringResource(R.string.settings_theme_monet),
                            summary = stringResource(R.string.settings_theme_monet_summary),
                            checked = themeConfig.useMonet,
                            onCheckedChange = { checked ->
                                updateTheme(themeConfig.copy(useMonet = checked))
                            },
                        )
                        // Miuix 保持原来的「同一张卡 + 展开动画」；Material 下三行各自成段（KSU SegmentedColumn 写法）
                        if (showMiuixOnlyOptions) {
                            AnimatedVisibility(
                                visible = themeConfig.useMonet,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                            ) {
                                Column {
                                    OverlayDropdownPreference(
                                        title = stringResource(R.string.settings_theme_palette_style),
                                        summary = paletteItems.getOrElse(selectedPaletteIndex) {
                                            paletteItems.first()
                                        },
                                        items = paletteItems,
                                        selectedIndex = selectedPaletteIndex,
                                        onSelectedIndexChange = { index ->
                                            updateTheme(themeConfig.copy(paletteStyle = paletteStyles[index]))
                                        },
                                    )
                                    OverlayDropdownPreference(
                                        title = stringResource(R.string.settings_theme_accent_title),
                                        summary = accentItems.getOrElse(selectedAccentIndex) { accentItems.first() },
                                        items = accentItems,
                                        selectedIndex = selectedAccentIndex,
                                        onSelectedIndexChange = { index ->
                                            updateTheme(themeConfig.copy(accentColor = accentOptions[index]))
                                        },
                                    )
                                    SwitchPreference(
                                        title = stringResource(R.string.settings_theme_pure_black),
                                        summary = stringResource(R.string.settings_theme_pure_black_summary),
                                        checked = themeConfig.pureBlack,
                                        onCheckedChange = { checked ->
                                            updateTheme(themeConfig.copy(pureBlack = checked))
                                        },
                                    )
                                }
                            }
                        }
                    })
                    if (!showMiuixOnlyOptions) {
                        add(CardItem("paletteStyle") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_theme_palette_style),
                                summary = paletteItems.getOrElse(selectedPaletteIndex) { paletteItems.first() },
                                items = paletteItems,
                                selectedIndex = selectedPaletteIndex,
                                onSelectedIndexChange = { index ->
                                    updateTheme(themeConfig.copy(paletteStyle = paletteStyles[index]))
                                },
                            )
                        })
                        add(CardItem("colorSpec") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_theme_color_spec),
                                items = colorSpecs.map { it.label() },
                                selectedIndex = selectedColorSpecIndex,
                                onSelectedIndexChange = { index ->
                                    updateTheme(themeConfig.copy(colorSpec = colorSpecs[index]))
                                },
                            )
                        })
                        add(CardItem("accent") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_theme_accent_title),
                                summary = accentItems.getOrElse(selectedAccentIndex) { accentItems.first() },
                                items = accentItems,
                                selectedIndex = selectedAccentIndex,
                                onSelectedIndexChange = { index ->
                                    updateTheme(themeConfig.copy(accentColor = accentOptions[index]))
                                },
                            )
                        })
                        if (themeConfig.useMonet) add(CardItem("pureBlack") {
                            SwitchPreference(
                                title = stringResource(R.string.settings_theme_pure_black),
                                summary = stringResource(R.string.settings_theme_pure_black_summary),
                                checked = themeConfig.pureBlack,
                                onCheckedChange = { checked ->
                                    updateTheme(themeConfig.copy(pureBlack = checked))
                                },
                            )
                        })
                    }
                },
            )

            item { SmallTitle(text = stringResource(R.string.settings_theme_group_interface)) }
            groupedCardItems(
                keyPrefix = "theme_interface",
                items = buildList {
                    add(CardItem("uiMode") {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_theme_ui_mode),
                            summary = themeConfig.uiMode.summary(),
                            items = ThemeUiModes.map { mode -> mode.label() },
                            selectedIndex = ThemeUiModes.indexOf(themeConfig.uiMode).coerceAtLeast(0),
                            onSelectedIndexChange = { index ->
                                updateTheme(themeConfig.copy(uiMode = ThemeUiModes[index]))
                            },
                        )
                    })
                    // M3E 悬浮底栏开关；Miuix 使用下方自己的浮动底栏设置
                    if (themeConfig.uiMode == UiMode.Material) add(CardItem("materialFloatingBar") {
                        SwitchPreference(
                            title = stringResource(R.string.settings_theme_floating_bottom_bar),
                            summary = stringResource(R.string.settings_theme_floating_bottom_bar_summary),
                            checked = themeConfig.materialFloatingBottomBar,
                            onCheckedChange = { checked ->
                                updateTheme(themeConfig.copy(materialFloatingBottomBar = checked))
                            },
                        )
                    })
                    // 节点网格样式只影响 Material 的 Proxy 页；Miuix 保持原有整卡样式
                    if (themeConfig.uiMode == UiMode.Material) add(CardItem("proxyGridSegmented") {
                        SwitchPreference(
                            title = stringResource(R.string.settings_theme_proxy_grid_segmented),
                            summary = stringResource(R.string.settings_theme_proxy_grid_segmented_summary),
                            checked = themeConfig.proxyGridSegmented,
                            onCheckedChange = { checked ->
                                updateTheme(themeConfig.copy(proxyGridSegmented = checked))
                            },
                        )
                    })
                    add(CardItem("blur") {
                        SwitchPreference(
                            title = stringResource(R.string.settings_theme_blur),
                            summary = stringResource(R.string.settings_theme_blur_summary),
                            checked = themeConfig.blurEnabled && isBlurSupported,
                            onCheckedChange = { checked ->
                                updateTheme(themeConfig.copy(blurEnabled = checked))
                            },
                            enabled = isBlurSupported,
                        )
                        // Miuix 保持原来的同卡展开；Material 下两行各自成段（与 Monet 卡片一致）
                        if (showMiuixOnlyOptions) {
                            AnimatedVisibility(
                                visible = themeConfig.blurEnabled && isBlurSupported,
                                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                            ) {
                                OverlayDropdownPreference(
                                    title = stringResource(R.string.settings_theme_blur_style),
                                    summary = topBarBlurStyleSummaries.getOrElse(selectedTopBarBlurStyleIndex) {
                                        topBarBlurStyleSummaries.first()
                                    },
                                    items = topBarBlurStyleItems,
                                    selectedIndex = selectedTopBarBlurStyleIndex,
                                    onSelectedIndexChange = { index ->
                                        updateTheme(themeConfig.copy(topBarBlurStyle = topBarBlurStyles[index]))
                                    },
                                )
                            }
                        }
                    })
                    if (!showMiuixOnlyOptions && themeConfig.blurEnabled && isBlurSupported) {
                        add(CardItem("blurStyle") {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_theme_blur_style),
                                summary = topBarBlurStyleSummaries.getOrElse(selectedTopBarBlurStyleIndex) {
                                    topBarBlurStyleSummaries.first()
                                },
                                items = topBarBlurStyleItems,
                                selectedIndex = selectedTopBarBlurStyleIndex,
                                onSelectedIndexChange = { index ->
                                    updateTheme(themeConfig.copy(topBarBlurStyle = topBarBlurStyles[index]))
                                },
                            )
                        })
                    }
                    if (onPredictiveBackChange != null) {
                        add(CardItem("predictiveBack") {
                            SwitchPreference(
                                title = stringResource(R.string.settings_predictive_back),
                                summary = stringResource(R.string.settings_predictive_back_summary),
                                checked = isPredictiveBackEnabled,
                                onCheckedChange = { checked ->
                                    storage.putString(StorageKeys.PREDICTIVE_BACK, checked.toString())
                                    isPredictiveBackEnabled = checked
                                    onPredictiveBackChange(checked)
                                },
                            )
                        })
                    }
                    add(CardItem("densityScale") {
                        ArrowPreference(
                            title = stringResource(R.string.settings_theme_density_scale),
                            summary = stringResource(R.string.settings_theme_density_scale_summary),
                            endActions = {
                                Text(
                                    text = formatPercent(densityScaleDraft),
                                    style = if (themeConfig.uiMode == UiMode.Material) {
                                        androidx.compose.material3.MaterialTheme.typography.labelLarge
                                    } else {
                                        MiuixTheme.textStyles.body2
                                    },
                                    color = MishkaColors.onSurfaceVariantActions,
                                )
                            },
                            bottomAction = {
                                Slider(
                                    value = densityScaleDraft.coerceIn(
                                        MinDensityScale * 100f,
                                        MaxDensityScale * 100f,
                                    ),
                                    onValueChange = { value ->
                                        densityScaleDraft = value
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    valueRange = (MinDensityScale * 100f)..(MaxDensityScale * 100f),
                                    onValueChangeFinished = {
                                        updateDensityScale(densityScaleDraft)
                                    },
                                    showKeyPoints = true,
                                    keyPoints = listOf(80f, 90f, 100f, 110f),
                                    // magnetThreshold 是 range 占比：0.01 = 1%，仅在关键点附近轻微吸附
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
                            onClick = ::openDensityScaleDialog,
                            holdDownState = showDensityScaleDialog,
                        )
                    })
                    add(CardItem("animationSpeed") {
                        ArrowPreference(
                            title = stringResource(R.string.settings_theme_animation_speed),
                            summary = stringResource(R.string.settings_theme_animation_speed_summary),
                            endActions = {
                                Text(
                                    text = formatPercent(animationSpeedDraft),
                                    style = if (themeConfig.uiMode == UiMode.Material) {
                                        androidx.compose.material3.MaterialTheme.typography.labelLarge
                                    } else {
                                        MiuixTheme.textStyles.body2
                                    },
                                    color = MishkaColors.onSurfaceVariantActions,
                                )
                            },
                            bottomAction = {
                                Slider(
                                    value = animationSpeedDraft.coerceIn(
                                        MinAnimationSpeed * 100f,
                                        MaxAnimationSpeed * 100f,
                                    ),
                                    onValueChange = { value ->
                                        animationSpeedDraft = value
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    valueRange = (MinAnimationSpeed * 100f)..(MaxAnimationSpeed * 100f),
                                    onValueChangeFinished = {
                                        updateAnimationSpeed(animationSpeedDraft)
                                    },
                                    showKeyPoints = true,
                                    keyPoints = listOf(50f, 75f, 100f, 150f, 200f),
                                    magnetThreshold = 0.01f,
                                    hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                                )
                            },
                            onClick = ::openAnimationSpeedDialog,
                            holdDownState = showAnimationSpeedDialog,
                        )
                    })
                },
            )

            item { SmallTitle(text = stringResource(R.string.settings_theme_group_navigation)) }
            groupedCardItems(
                keyPrefix = "theme_navigation",
                items = buildList {
                    add(CardItem("pagerGesture") {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_theme_pager_gesture),
                            summary = pagerInterceptionModeItems.getOrElse(
                                selectedPagerInterceptionModeIndex,
                            ) { pagerInterceptionModeItems.first() },
                            items = pagerInterceptionModeItems,
                            selectedIndex = selectedPagerInterceptionModeIndex,
                            onSelectedIndexChange = { index ->
                                updateTheme(
                                    themeConfig.copy(
                                        pagerInterceptionMode = pagerInterceptionModes[index],
                                    ),
                                )
                            },
                        )
                    })
                    if (onSwipeDismissChange != null) {
                        add(CardItem("swipeDismiss") {
                            SwitchPreference(
                                title = stringResource(R.string.settings_swipe_dismiss),
                                summary = stringResource(R.string.settings_swipe_dismiss_summary),
                                checked = swipeDismissEnabled,
                                onCheckedChange = { checked -> onSwipeDismissChange(checked) },
                            )
                        })
                    }
                    if (showMiuixOnlyOptions) add(CardItem("floating") {
                        SwitchPreference(
                            title = stringResource(R.string.settings_theme_floating_bottom_bar),
                            summary = stringResource(R.string.settings_theme_floating_bottom_bar_summary),
                            checked = themeConfig.floatingBottomBar,
                            onCheckedChange = { checked ->
                                updateTheme(themeConfig.copy(floatingBottomBar = checked))
                            },
                        )
                        AnimatedVisibility(
                            visible = themeConfig.floatingBottomBar,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            OverlayDropdownPreference(
                                title = stringResource(R.string.settings_theme_floating_bottom_bar_style),
                                summary = floatingBottomBarStyleItems.getOrElse(
                                    selectedFloatingBottomBarStyleIndex,
                                ) { floatingBottomBarStyleItems.first() },
                                items = floatingBottomBarStyleItems,
                                selectedIndex = selectedFloatingBottomBarStyleIndex,
                                onSelectedIndexChange = { index ->
                                    updateTheme(
                                        themeConfig.copy(
                                            floatingBottomBarStyle = floatingBottomBarStyles[index],
                                        ),
                                    )
                                },
                            )
                        }
                    })
                    if (showMiuixOnlyOptions) add(CardItem("mode") {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.settings_theme_bottom_bar_mode),
                            summary = bottomBarModeItems.getOrElse(
                                selectedBottomBarModeIndex,
                            ) { bottomBarModeItems.first() },
                            items = bottomBarModeItems,
                            selectedIndex = selectedBottomBarModeIndex,
                            onSelectedIndexChange = { index ->
                                updateTheme(
                                    themeConfig.copy(
                                        bottomBarMode = bottomBarModes[index],
                                    ),
                                )
                            },
                        )
                    })
                },
            )
            item {
                Spacer(
                    Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }

        }
    }

    PercentScaleDialog(
        show = showDensityScaleDialog,
        title = stringResource(R.string.settings_theme_density_scale),
        summary = stringResource(R.string.settings_theme_density_scale_summary),
        textState = densityScaleTextState,
        currentPercent = { densityScaleDraft },
        onDismiss = { showDensityScaleDialog = false },
        onConfirm = { percent ->
            updateDensityScale(percent)
            showDensityScaleDialog = false
        },
    )
    PercentScaleDialog(
        show = showAnimationSpeedDialog,
        title = stringResource(R.string.settings_theme_animation_speed),
        summary = stringResource(R.string.settings_theme_animation_speed_summary),
        textState = animationSpeedTextState,
        currentPercent = { animationSpeedDraft },
        onDismiss = { showAnimationSpeedDialog = false },
        onConfirm = { percent ->
            updateAnimationSpeed(percent)
            showAnimationSpeedDialog = false
        },
    )
}

@Composable
private fun PercentScaleDialog(
    show: Boolean,
    title: String,
    summary: String,
    textState: TextFieldState,
    currentPercent: () -> Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    WindowDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            state = textState,
            modifier = Modifier.fillMaxWidth(),
            inputTransformation = DigitsOnlyTransformation.maxLength(3),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Text(
                    text = "%",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MishkaColors.onSurfaceVariantActions,
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            )
            TextButton(
                text = stringResource(R.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    // 空输入/非法输入回退当前值，确认后总是关闭，不做静默 no-op
                    val percent = textState.text.toString().toIntOrNull()?.toFloat() ?: currentPercent()
                    onConfirm(percent)
                },
            )
        }
    }
}

// 仅允许数字输入，非数字编辑整体回退
private val DigitsOnlyTransformation = InputTransformation {
    if (!asCharSequence().all { it.isDigit() }) revertAllChanges()
}

private fun formatPercent(value: Float): String = "${value.roundToInt()}%"
