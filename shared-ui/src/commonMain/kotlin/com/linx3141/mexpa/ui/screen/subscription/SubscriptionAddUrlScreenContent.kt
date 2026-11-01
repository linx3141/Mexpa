package com.linx3141.mexpa.ui.screen.subscription

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.viewmodel.SubscriptionUiState
import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.AppIcons
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.util.horizontalCutoutPadding
import com.linx3141.mexpa.ui.component.design.ButtonDefaults
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.Icon
import com.linx3141.mexpa.ui.component.design.IconButton
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TextButton
import com.linx3141.mexpa.ui.component.design.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

/**
 * URL 导入配置页面
 */
@Composable
fun SubscriptionAddUrlScreenContent(
    uiState: SubscriptionUiState = SubscriptionUiState(),
    initialUrl: String = "",
    initialName: String = "",
    initialIntervalMinutes: Long = 0,
    onBack: () -> Unit = {},
    onAddSubscription: (
        name: String,
        url: String,
        interval: Long,
        userAgent: String,
        ageSecretKey: String,
        onComplete: () -> Unit,
    ) -> Unit = { _, _, _, _, _, _ -> },
    onSaved: () -> Unit = {},
    onCancelUpdate: () -> Unit = {},
) {

    val scrollBehavior = rememberMishkaScrollBehavior()
    var inputName by remember { mutableStateOf(initialName) }
    var inputUrl by remember { mutableStateOf(initialUrl) }
    var userAgent by remember { mutableStateOf("") }
    var ageSecretKey by remember { mutableStateOf("") }
    var intervalMinutes by remember {
        mutableStateOf(if (initialIntervalMinutes > 0) initialIntervalMinutes.toString() else "")
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.subscription_config),
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
            item(key = "hint") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(top = 12.dp, bottom = 6.dp),
                    insideMargin = PaddingValues(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.Info,
                            contentDescription = null,
                            tint = MishkaColors.onSurfaceVariantSummary,
                        )
                        Text(
                            text = stringResource(Res.string.subscription_url_hint),
                            fontSize = 14.sp,
                            color = MishkaColors.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            item(key = "name_title") {
                SmallTitle(text = stringResource(Res.string.subscription_name))
            }
            item(key = "name_field") {
                TextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 6.dp),
                    label = stringResource(Res.string.subscription_name_auto_placeholder),
                    useLabelAsPlaceholder = true,
                )
            }
            item(key = "url_title") {
                SmallTitle(text = stringResource(Res.string.subscription_url_label))
            }
            item(key = "url_field") {
                TextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 6.dp),
                    label = stringResource(Res.string.subscription_url_placeholder),
                    useLabelAsPlaceholder = true,
                )
            }
            item(key = "user_agent_title") {
                SmallTitle(text = stringResource(Res.string.subscription_user_agent))
            }
            item(key = "user_agent_field") {
                TextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 6.dp),
                    label = stringResource(Res.string.subscription_user_agent_placeholder),
                    useLabelAsPlaceholder = true,
                )
            }
            item(key = "age_secret_key_title") {
                SmallTitle(text = stringResource(Res.string.subscription_age_secret_key))
            }
            item(key = "age_secret_key_field") {
                TextField(
                    value = ageSecretKey,
                    onValueChange = { ageSecretKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 6.dp),
                    label = stringResource(Res.string.subscription_age_secret_key_placeholder),
                    useLabelAsPlaceholder = true,
                )
            }
            item(key = "interval_field") {
                SmallTitle(text = stringResource(Res.string.subscription_auto_update))
                TextField(
                    value = intervalMinutes,
                    onValueChange = { intervalMinutes = it.filter { c -> c.isDigit() } },
                    label = stringResource(Res.string.subscription_auto_update_placeholder),
                    useLabelAsPlaceholder = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 12.dp),
                )
            }
            item(key = "save") {
                TextButton(
                    text = stringResource(Res.string.common_save),
                    onClick = {
                        val intervalMs = (intervalMinutes.toLongOrNull() ?: 0) * 60000
                        onAddSubscription(
                            inputName.trim(),
                            inputUrl,
                            intervalMs,
                            userAgent.trim(),
                            ageSecretKey.trim(),
                            onSaved,
                        )
                    },
                    enabled = inputUrl.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
            if (uiState.error.isNotEmpty()) {
                item(key = "error") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding),
                        insideMargin = PaddingValues(16.dp),
                    ) {
                        Text(
                            text = uiState.error,
                            fontSize = 14.sp,
                            color = MishkaColors.primary,
                        )
                    }
                }
            }
            item(key = "bottom_spacer") {
                Spacer(
                    Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    ImportProgressDialog(
        show = uiState.isLoading,
        step = uiState.importProgress?.let { importStepLabel(it) } ?: stringResource(Res.string.common_processing),
        onCancel = { onCancelUpdate() },
    )
}
