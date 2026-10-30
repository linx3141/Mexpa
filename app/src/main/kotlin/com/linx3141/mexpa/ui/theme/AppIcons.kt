package com.linx3141.mexpa.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.MoreCircle
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Unpin
import top.yukonga.miuix.kmp.icon.extended.UploadCloud

/**
 * 语义图标：Miuix 模式返回原来的 miuix 图标，Material 模式返回 Material 图标。
 *
 * KernelSU 的 Material 页面用 `Icons.Filled/Outlined`，Miuix 页面用 miuix 图标；
 * 这里按 [LocalUiMode] 二选一，调用点只写 `AppIcons.Xxx`。
 */
object AppIcons {

    @Composable
    private fun material(): Boolean = LocalUiMode.current == UiMode.Material

    val Refresh: ImageVector
        @Composable get() = if (material()) Icons.Filled.Refresh else MiuixIcons.Refresh

    val Delete: ImageVector
        @Composable get() = if (material()) Icons.Filled.Delete else MiuixIcons.Delete

    val MoreCircle: ImageVector
        @Composable get() = if (material()) Icons.Filled.MoreHoriz else MiuixIcons.MoreCircle

    val Ok: ImageVector
        @Composable get() = if (material()) Icons.Filled.Check else MiuixIcons.Ok

    val Close: ImageVector
        @Composable get() = if (material()) Icons.Filled.Close else MiuixIcons.Close

    val Add: ImageVector
        @Composable get() = if (material()) Icons.Filled.Add else MiuixIcons.Add

    val UploadCloud: ImageVector
        @Composable get() = if (material()) Icons.Filled.CloudUpload else MiuixIcons.UploadCloud

    val Tune: ImageVector
        @Composable get() = if (material()) Icons.Filled.Tune else MiuixIcons.Tune

    val Settings: ImageVector
        @Composable get() = if (material()) Icons.Filled.Settings else MiuixIcons.Settings

    val Import: ImageVector
        @Composable get() = if (material()) Icons.Filled.FileDownload else MiuixIcons.Import

    val Home: ImageVector
        @Composable get() = if (material()) Icons.Filled.Home else MiuixIcons.Home

    val Pin: ImageVector
        @Composable get() = if (material()) Icons.Filled.PushPin else MiuixIcons.Pin

    val Unpin: ImageVector
        @Composable get() = if (material()) Icons.Outlined.PushPin else MiuixIcons.Unpin

    val Sort: ImageVector
        @Composable get() = if (material()) Icons.AutoMirrored.Filled.Sort else MiuixIcons.Sort

    val Info: ImageVector
        @Composable get() = if (material()) Icons.Filled.Info else MiuixIcons.Info

    val Backup: ImageVector
        @Composable get() = if (material()) Icons.Filled.Backup else MiuixIcons.Backup

    val ChevronRight: ImageVector
        @Composable get() = if (material()) {
            Icons.AutoMirrored.Filled.KeyboardArrowRight
        } else {
            MiuixIcons.Basic.ArrowRight
        }
}
