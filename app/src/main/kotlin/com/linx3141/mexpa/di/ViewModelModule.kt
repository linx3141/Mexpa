package com.linx3141.mexpa.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.linx3141.mexpa.domain.repository.SubscriptionRepository
import com.linx3141.mexpa.viewmodel.AppProxyViewModel
import com.linx3141.mexpa.viewmodel.BackupViewModel
import com.linx3141.mexpa.viewmodel.ConnectionViewModel
import com.linx3141.mexpa.viewmodel.DnsQueryViewModel
import com.linx3141.mexpa.viewmodel.AutomationViewModel
import com.linx3141.mexpa.viewmodel.ExternalControlViewModel
import com.linx3141.mexpa.viewmodel.HomeViewModel
import com.linx3141.mexpa.viewmodel.LogViewModel
import com.linx3141.mexpa.viewmodel.MetaSettingsViewModel
import com.linx3141.mexpa.viewmodel.NetworkSettingsViewModel
import com.linx3141.mexpa.viewmodel.OverrideProfileViewModel
import com.linx3141.mexpa.viewmodel.ProviderViewModel
import com.linx3141.mexpa.viewmodel.ProxyViewModel
import com.linx3141.mexpa.viewmodel.SubscriptionViewModel

/**
 * ViewModel 装配。单 Activity 应用，ViewModel 生命周期与进程一致，用 single；
 * 组合根（MainActivity）从 Koin 取图后透传给 App，屏幕保持参数化，无需 koinViewModel。
 */
val viewModelModule = module {
    single {
        HomeViewModel(
            serviceController = get(),
            overrideStore = get(),
            connectionManager = get(),
            latencyTester = get(),
            systemInfo = get(),
            getActiveSubscriptionId = { get<SubscriptionRepository>().getActive()?.id },
            activeSubscription = get<SubscriptionRepository>().activeSubscription,
            onLiveProviderInfo = get<SubscriptionRepository>()::setLiveProviderInfo,
        )
    }
    single { SubscriptionViewModel(get(), get(), get(), get(), androidContext()) }
    single { OverrideProfileViewModel(get(), get(), get(), androidContext()) }
    single {
        ProxyViewModel(
            selectionDao = get(),
            getActiveUuid = { get<SubscriptionRepository>().getActive()?.id },
            storage = get(),
        )
    }
    single { AppProxyViewModel(get(), get(), get()) }
    single { NetworkSettingsViewModel(get()) }
    single { MetaSettingsViewModel(get()) }
    single { ExternalControlViewModel(get()) }
    single { AutomationViewModel(get()) }
    single { LogViewModel() }
    single { ProviderViewModel() }
    single { ConnectionViewModel() }
    single { DnsQueryViewModel() }
    single { BackupViewModel(get(), get(), androidContext()) }
}
