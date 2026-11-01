package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.RestartRequiredHint
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.FooterHint
import com.linx3141.mexpa.ui.component.groupedCardItems
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.viewmodel.ExternalControlUiState
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.WindowDialog
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

@Composable
fun ExternalControlScreenContent(
    uiState: ExternalControlUiState = ExternalControlUiState(),
    onBack: () -> Unit = {},
    onSetExternalController: (String?) -> Unit = {},
    onSetSecret: (String?) -> Unit = {},
    onResetDone: () -> Unit = {},
) {

    val scrollBehavior = rememberMishkaScrollBehavior()
    val notModifiedStr = stringResource(Res.string.common_not_modified)

    var showEditDialog by remember { mutableStateOf(false) }
    val controllerTextState = rememberTextFieldState()

    var showSecretDialog by remember { mutableStateOf(false) }
    val secretTextState = rememberTextFieldState()

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.external_control_title),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        MishkaBackButton(
                            onClick = onBack,
                            contentDescription = stringResource(Res.string.common_back),
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
                .platformScrollEndHaptic()
                .platformOverScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            item { RestartRequiredHint() }

            item {
                Spacer(Modifier.height(6.dp))
            }

            groupedCardItems(
                keyPrefix = "external_control",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("controller") {
                        ArrowPreference(
                            title = stringResource(Res.string.external_control_ip),
                            summary = uiState.externalController ?: notModifiedStr,
                            onClick = {
                                controllerTextState.edit {
                                    replace(0, length, uiState.externalController ?: "")
                                }
                                showEditDialog = true
                            },
                        )
                    },
                    CardItem("secret") {
                        ArrowPreference(
                            title = stringResource(Res.string.external_control_secret),
                            summary = uiState.secret ?: notModifiedStr,
                            onClick = {
                                secretTextState.edit {
                                    replace(0, length, uiState.secret ?: "")
                                }
                                showSecretDialog = true
                            },
                        )
                    },
                ),
            )

            item {
                FooterHint(
                    texts = listOf(
                        stringResource(Res.string.external_control_controller_hint),
                        stringResource(Res.string.external_control_secret_hint),
                    ),
                )
            }

            item {
                Spacer(
                    Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    WindowDialog(
        show = showEditDialog,
        title = stringResource(Res.string.network_external_controller),
        onDismissRequest = { showEditDialog = false },
    ) {
        TextField(
            state = controllerTextState,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.network_input_value),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_not_modified),
                modifier = Modifier.weight(1f),
                onClick = {
                    onSetExternalController(null)
                    onResetDone()
                    showEditDialog = false
                },
            )
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showEditDialog = false },
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val value = controllerTextState.text.toString().trim().takeIf { it.isNotEmpty() }
                    onSetExternalController(value)
                    showEditDialog = false
                },
            )
        }
    }

    WindowDialog(
        show = showSecretDialog,
        title = stringResource(Res.string.external_control_secret),
        onDismissRequest = { showSecretDialog = false },
    ) {
        TextField(
            state = secretTextState,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.network_input_value),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                text = stringResource(Res.string.common_not_modified),
                modifier = Modifier.weight(1f),
                onClick = {
                    onSetSecret(null)
                    onResetDone()
                    showSecretDialog = false
                },
            )
            TextButton(
                text = stringResource(Res.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { showSecretDialog = false },
            )
            TextButton(
                text = stringResource(Res.string.common_confirm),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val value = secretTextState.text.toString().trim().takeIf { it.isNotEmpty() }
                    onSetSecret(value)
                    showSecretDialog = false
                },
            )
        }
    }
}
