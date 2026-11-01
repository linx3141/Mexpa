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
import com.linx3141.mexpa.ui.component.design.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linx3141.mexpa.shared.resources.*
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
    appName: String,
    versionName: String,
    versionCode: Int,
    appIcon: Painter? = null,
    onBack: () -> Unit = {},
    mihomoVersion: String = "",
    onOpenUrl: (String) -> Unit = {},
) {
    val scrollBehavior = rememberMishkaScrollBehavior()
    val links = aboutOssProjects(mihomoVersion)

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.about_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    MishkaBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(Res.string.common_back),
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
                        if (appIcon != null) {
                            // ic_launcher_foreground 是白色矢量；白底上必须重新着色，否则是空白
                            Image(
                                painter = appIcon,
                                contentDescription = null,
                                modifier = Modifier.requiredSize(112.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(Color(0xFF1B1B1B)),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color(0xFF1B1B1B),
                            )
                        }

                    }
                    Text(
                        text = appName,
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "v$versionName ($versionCode)",
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
