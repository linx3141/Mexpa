package com.linx3141.mexpa.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import com.linx3141.mexpa.data.backup.BackupManager
import com.linx3141.mexpa.data.database.getAppDatabase
import com.linx3141.mexpa.platform.AppListProvider
import com.linx3141.mexpa.platform.BootStartManager
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.PlatformSystemInfo
import com.linx3141.mexpa.platform.ProxyServiceController
import com.linx3141.mexpa.platform.WifiPolicyController
import com.linx3141.mexpa.service.ProfileUpdateScheduler

/**
 * Android 平台单例：均绑定 application Context（`androidContext()`）。
 * 需 Activity 上下文的资源（FilePicker / VPN 授权 launcher）由 MainActivity 直接持有，不入 Koin。
 */
val androidPlatformModule: Module = module {
    single { getAppDatabase(androidContext()) }
    single { PlatformStorage(androidContext()) }
    single { PlatformSystemInfo(androidContext()) }
    single { ProxyServiceController(androidContext()) }
    single { AppListProvider(androidContext()) }
    single { WifiPolicyController(androidContext()) }
    single { BootStartManager(androidContext()) }
    single { BackupManager(androidContext(), get(), get(), get(), get()) }
    single { ProfileUpdateScheduler(androidContext(), get(), get()) }
}
