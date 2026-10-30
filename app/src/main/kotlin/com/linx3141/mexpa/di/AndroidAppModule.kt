package com.linx3141.mexpa.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.linx3141.mexpa.platform.ProfileFileManager
import com.linx3141.mexpa.service.AndroidProfileFileManager

/**
 * app 壳（Android）层绑定：ProfileFileManager 的实现属服务层，故在此绑定而非 data 层。
 */
val androidAppModule = module {
    single<ProfileFileManager> { AndroidProfileFileManager(androidContext()) }
}
