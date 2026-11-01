package com.linx3141.mexpa.ui.screen.subscription

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.viewmodel.SubscriptionUiState
import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.domain.model.ProfileType
import com.linx3141.mexpa.ui.component.AdaptiveTopAppBar
import com.linx3141.mexpa.ui.component.CardItem
import com.linx3141.mexpa.ui.component.blur.BlurredBar
import com.linx3141.mexpa.ui.component.blur.rememberBlurBackdrop
import com.linx3141.mexpa.ui.component.groupedCardItems
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
import com.linx3141.mexpa.ui.component.design.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.linx3141.mexpa.ui.component.design.platformScrollEndHaptic
import com.linx3141.mexpa.ui.component.design.platformOverScrollVertical

/**
 * 订阅编辑页面 —— 编辑名称、URL、自动更新间隔
 */
@Composable
fun SubscriptionEditScreenContent(
    uuid: String,
    uiState: SubscriptionUiState = SubscriptionUiState(),
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {},
    onNavigateOverrides: () -> Unit = {},
    onEditSubscription: (
        uuid: String,
        name: String,
        source: String,
        interval: Long,
        userAgent: String,
        ageSecretKey: String,
        onComplete: () -> Unit,
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onCancelUpdate: () -> Unit = {},
) {

    val subscription = uiState.subscriptions.find { it.id == uuid }
    val scrollBehavior = rememberMishkaScrollBehavior()

    var name by rememberSaveable(uuid) { mutableStateOf(subscription?.name ?: "") }
    var url by rememberSaveable(uuid) { mutableStateOf(subscription?.url ?: "") }
    var userAgent by rememberSaveable(uuid) { mutableStateOf(subscription?.userAgent ?: "") }
    var ageSecretKey by rememberSaveable(uuid) { mutableStateOf(subscription?.ageSecretKey ?: "") }
    var intervalMinutes by rememberSaveable(uuid) {
        mutableStateOf(
            if ((subscription?.interval ?: 0) > 0) ((subscription?.interval ?: 0) / 60000).toString() else ""
        )
    }

    if (subscription == null) {
        onBack()
        return
    }

    val isFile = subscription.type == ProfileType.File
    val hasChanges = name != subscription.name ||
            url != subscription.url ||
            userAgent.trim() != subscription.userAgent ||
            ageSecretKey.trim() != subscription.ageSecretKey ||
            intervalMinutes != (if (subscription.interval > 0) (subscription.interval / 60000).toString() else "")

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MishkaColors.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(Res.string.subscription_edit),
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
            item {
                SmallTitle(text = stringResource(Res.string.subscription_config_name))
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 6.dp),
                )
                if (!isFile) {
                    SmallTitle(text = stringResource(Res.string.subscription_sub_url))
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(bottom = 6.dp),
                    )
                    SmallTitle(text = stringResource(Res.string.subscription_user_agent))
                    TextField(
                        value = userAgent,
                        onValueChange = { userAgent = it },
                        label = stringResource(Res.string.subscription_user_agent_placeholder),
                        useLabelAsPlaceholder = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(bottom = 6.dp),
                    )
                    SmallTitle(text = stringResource(Res.string.subscription_age_secret_key))
                    TextField(
                        value = ageSecretKey,
                        onValueChange = { ageSecretKey = it },
                        label = stringResource(Res.string.subscription_age_secret_key_placeholder),
                        useLabelAsPlaceholder = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(bottom = 6.dp),
                    )
                    SmallTitle(text = stringResource(Res.string.subscription_auto_update))
                    TextField(
                        value = intervalMinutes,
                        onValueChange = { intervalMinutes = it.filter { c -> c.isDigit() } },
                        label = stringResource(Res.string.subscription_auto_update_placeholder),
                        useLabelAsPlaceholder = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(bottom = 6.dp),
                    )
                }
            }

            if (uiState.error.isNotEmpty()) {
                item(key = "error") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PageHorizontalPadding)
                            .padding(bottom = 6.dp),
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

            groupedCardItems(
                keyPrefix = "subscription_overrides",
                outerTopPadding = 6.dp,
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("open") {
                        ArrowPreference(
                            title = stringResource(Res.string.subscription_overrides),
                            summary = if (subscription.overrideIds.isEmpty()) {
                                stringResource(Res.string.subscription_override_none)
                            } else {
                                pluralStringResource(
                                    Res.plurals.subscription_override_count,
                                    subscription.overrideIds.size,
                                    subscription.overrideIds.size,
                                )
                            },
                            onClick = onNavigateOverrides,
                        )
                    },
                ),
            )

            item {
                TextButton(
                    text = stringResource(Res.string.common_save),
                    onClick = {
                        val intervalMs = (intervalMinutes.toLongOrNull() ?: 0) * 60000
                        onEditSubscription(
                            uuid,
                            name,
                            url,
                            intervalMs,
                            userAgent.trim(),
                            ageSecretKey.trim(),
                            onSaved,
                        )
                    },
                    enabled = hasChanges && !uiState.isLoading && name.isNotBlank(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(bottom = 12.dp),
                )
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
}
