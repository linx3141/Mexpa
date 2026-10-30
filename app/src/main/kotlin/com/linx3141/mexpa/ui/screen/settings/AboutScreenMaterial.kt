package com.linx3141.mexpa.ui.screen.settings

import com.linx3141.mexpa.ui.component.design.PageHorizontalPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.BuildConfig
import com.linx3141.mexpa.R
import com.linx3141.mexpa.ui.component.design.MishkaBackButton
import com.linx3141.mexpa.ui.component.design.Scaffold
import com.linx3141.mexpa.ui.component.design.SegmentedColumn
import com.linx3141.mexpa.ui.component.design.SegmentedListItem
import com.linx3141.mexpa.ui.component.design.Text
import com.linx3141.mexpa.ui.component.design.TopAppBar
import com.linx3141.mexpa.ui.component.design.rememberMishkaScrollBehavior

/**
 * Material 3 Expressive 的关于页，结构对齐 KernelSU `AboutScreenMaterial`：
 * 顶栏 + 居中的 App 图标/名称/版本，下面是一组 SegmentedColumn 链接。
 */
@Composable
fun AboutScreenMaterial(
    onBack: () -> Unit = {},
    mihomoVersion: String = "",
    onOpenUrl: (String) -> Unit = {},
) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    val links = aboutOssProjects(mihomoVersion)

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.about_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = innerPadding.calculateTopPadding(),
            ),
        ) {
            item(key = "hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                    ) {
                        // ic_launcher_foreground 是白色矢量；白底上必须重新着色，否则是空白
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.requiredSize(112.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color(0xFF1B1B1B)),
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            item(key = "links") {
                SegmentedColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    content = links.map { (name, url) ->
                        {
                            SegmentedListItem(
                                onClick = { onOpenUrl(url) },
                                headlineContent = { Text(name) },
                                supportingContent = { Text(url.removePrefix("https://")) },
                            )
                        }
                    },
                )
                Spacer(
                    Modifier
                        .height(16.dp)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}
