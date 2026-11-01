package com.linx3141.mexpa.ui.screen.home

import com.linx3141.mexpa.shared.resources.*



import com.linx3141.mexpa.ui.theme.MishkaColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linx3141.mexpa.ui.component.design.Card
import com.linx3141.mexpa.ui.component.design.SmallTitle
import com.linx3141.mexpa.ui.component.design.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

fun LazyListScope.quickEntriesSection(
    isRunning: Boolean,
    connectionCount: Int? = null,
    onNavigateLog: () -> Unit = {},
    onNavigateProvider: () -> Unit = {},
    onNavigateConnection: () -> Unit = {},
    onNavigateDnsQuery: () -> Unit = {},
) {
    item(key = "quick_entries_title") {
        SmallTitle(text = stringResource(Res.string.home_tools))
    }
    item(key = "quick_entries") {
        Column(
            modifier = Modifier
                .padding(horizontal = HomeCardHorizontalPadding)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickEntryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.home_providers),
                    subtitle = stringResource(Res.string.home_providers_subtitle),
                    isRunning = isRunning,
                    onClick = onNavigateProvider,
                )
                QuickEntryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.home_connections),
                    subtitle = if (connectionCount != null && connectionCount >= 0) {
                        stringResource(Res.string.home_connections_count, connectionCount)
                    } else {
                        stringResource(Res.string.home_connections_subtitle)
                    },
                    isRunning = isRunning,
                    onClick = onNavigateConnection,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QuickEntryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.home_logs),
                    subtitle = stringResource(Res.string.home_logs_subtitle),
                    isRunning = isRunning,
                    onClick = onNavigateLog,
                )
                QuickEntryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.home_dns),
                    subtitle = stringResource(Res.string.home_dns_subtitle),
                    isRunning = isRunning,
                    onClick = onNavigateDnsQuery,
                )
            }
        }
    }
}

@Composable
private fun QuickEntryCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(16.dp),
        onClick = onClick,
        showIndication = isRunning,
        pressFeedbackType = if (isRunning) PressFeedbackType.Sink else PressFeedbackType.None,
    ) {
        HomeCardTitle(title, miuixFontSize = 15.sp)
        HomeCardSubtitle(subtitle)
    }
}
