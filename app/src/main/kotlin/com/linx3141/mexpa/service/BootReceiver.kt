package com.linx3141.mexpa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.ProxyServiceBridge
import com.linx3141.mexpa.platform.ProxyServiceController
import com.linx3141.mexpa.platform.ProxyState
import com.linx3141.mexpa.platform.StorageKeys

/**
 * 开机/升级自动重启接收器。开关由 BootStartManager 控制 enabled/disabled 状态。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val storage = PlatformStorage(context)
                val wasRunning = storage.getString(StorageKeys.SERVICE_WAS_RUNNING, "false") == "true"
                if (!wasRunning) return
                // 部分 ROM 不向未运行的 app 投递 BOOT_COMPLETED，而是等进程起来后补发，那时
                // 自动连接往往已拉起过一次。已在跑就不必再发；Starting 仍要发——Service 侧幂等
                // 会挡掉重复的 fresh START，而进行中的若是 attach-only，这一发正好抢占它
                if (ProxyServiceBridge.state.value.state == ProxyState.Running) return
                ProxyServiceController(context).start()
            }
        }
    }
}
