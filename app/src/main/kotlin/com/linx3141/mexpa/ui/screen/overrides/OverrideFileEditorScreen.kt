package com.linx3141.mexpa.ui.screen.overrides

import com.linx3141.mexpa.R



import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.AppIcons
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linx3141.mexpa.domain.model.OverrideFormat
import com.linx3141.mexpa.platform.showToast
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.theme.LocalAppDarkMode
import com.linx3141.mexpa.ui.theme.StatusColors
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.CircularProgressIndicator
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SmallTopAppBar
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.scripta.editor.CodeEditor
import top.yukonga.scripta.editor.EditorColors
import top.yukonga.scripta.editor.EditorLanguage
import top.yukonga.scripta.editor.rememberCodeEditorController

@Composable
fun OverrideFileEditorScreen(
    overrideId: String,
    viewModel: OverrideProfileViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profiles.find { it.id == overrideId }
    if (profile == null) {
        LaunchedEffect(overrideId) { onBack() }
        return
    }

    val controller = rememberCodeEditorController()
    val context = LocalContext.current
    var saving by remember { mutableStateOf(false) }
    ClearOverrideErrorOnExit(viewModel)

    LaunchedEffect(overrideId) {
        viewModel.clearError()
        controller.setDocument(viewModel.readContent(overrideId))
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) saving = false
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                SmallTopAppBar(
                    title = profile.name,
                    color = barColor,
                    navigationIcon = { OverrideBackButton(onBack) },
                    actions = {
                        val canSave = controller.isModified && !state.isLoading && !saving
                        IconButton(
                            enabled = canSave,
                            onClick = {
                                val version = controller.documentVersion
                                val content = controller.getText(controller.lineEnding)
                                saving = true
                                viewModel.saveContent(overrideId, content) {
                                    controller.markSaved(version)
                                    saving = false
                                    showToast(context.getString(R.string.file_manager_saved))
                                }
                            },
                        ) {
                            if (saving || state.isLoading) {
                                CircularProgressIndicator(size = 20.dp, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = AppIcons.Ok,
                                    contentDescription = stringResource(R.string.common_save),
                                    tint = if (canSave) MishkaColors.onSurface
                                    else MishkaColors.disabledOnSecondaryVariant,
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            if (state.error.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 12.dp),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Text(
                        text = state.error,
                        color = StatusColors.danger,
                        fontSize = 13.sp,
                    )
                }
            }

            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                CodeEditor(
                    controller = controller,
                    language = if (profile.format == OverrideFormat.Yaml) EditorLanguage.Yaml else EditorLanguage.JavaScript,
                    colors = if (LocalAppDarkMode.current) EditorColors.Default else EditorColors.Light,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}
