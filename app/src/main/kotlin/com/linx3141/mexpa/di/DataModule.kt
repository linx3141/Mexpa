package com.linx3141.mexpa.di

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.linx3141.mexpa.R
import com.linx3141.mexpa.data.api.MihomoConnectionManager
import com.linx3141.mexpa.data.api.RuleLatencyTester
import com.linx3141.mexpa.data.database.AppDatabase
import com.linx3141.mexpa.data.repository.OverrideJsonStore
import com.linx3141.mexpa.data.repository.OverrideProfileRepositoryImpl
import com.linx3141.mexpa.data.repository.ProfileProcessor
import com.linx3141.mexpa.data.repository.SubscriptionProxyResolver
import com.linx3141.mexpa.data.repository.SubscriptionRepositoryImpl
import com.linx3141.mexpa.data.store.OverrideProfileStore
import com.linx3141.mexpa.data.store.ProfileTransformWriter
import com.linx3141.mexpa.domain.repository.OverrideProfileRepository
import com.linx3141.mexpa.domain.repository.SubscriptionRepository

/**
 * data 层 Koin 装配：仓库 / 存储 / 处理器。
 * Android 单例（AppDatabase / PlatformStorage / 各控制器）由 `androidPlatformModule` 提供；
 * ProfileFileManager 实现由 `androidAppModule` 绑定。
 */
val dataModule = module {
    // SupervisorJob 只隔离兄弟协程，救不了协程自己：未捕获异常会冒到默认 handler 打崩进程。
    // 各消费方仍要在 collect 体内自行兜住副作用（见 SubscriptionRepositoryImpl），这里是最后一道网
    single<CoroutineScope> {
        val handler = CoroutineExceptionHandler { _, e ->
            Log.e("AppScope", "uncaught in shared scope", e)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }

    single { get<AppDatabase>().importedDao() }
    single { get<AppDatabase>().pendingDao() }
    single { get<AppDatabase>().selectionDao() }

    single { OverrideJsonStore(get(), get()) }
    single { OverrideProfileStore(get()) }
    single { ProfileTransformWriter(get(), get()) }
    single { SubscriptionProxyResolver(get(), get()) }
    single<OverrideProfileRepository> {
        OverrideProfileRepositoryImpl(get(), get(), get(), get(), get(), get())
    }
    single { RuleLatencyTester(get()) }
    single { MihomoConnectionManager(get()) }

    single { SubscriptionRepositoryImpl(get(), get(), get(), get(), getOrNull(), get(), get()) }
    single<SubscriptionRepository> { get<SubscriptionRepositoryImpl>() }

    factory {
        ProfileProcessor(
            repo = get(),
            fileManager = get(),
            defaultProfileName = androidContext().getString(R.string.subscription_default_name),
            proxyResolver = get(),
        )
    }
}
