package com.linx3141.mexpa.platform

import android.content.Context

/**
 * 外部应用能否通过 intent 控制代理。
 *
 * 只拦 [com.linx3141.mexpa.ExternalControlActivity] 的四个导出 action —— 那是唯一能被第三方
 * 唤起的外部入口。磁贴由系统绑定、[com.linx3141.mexpa.service.VpnPermissionActivity] 是应用内
 * 跳板，两者都不经过这里，所以关掉开关不影响它们。
 */
object ExternalControlPolicy {

    fun isAllowed(context: Context): Boolean = isAllowed(PlatformStorage(context))

    fun isAllowed(storage: PlatformStorage): Boolean =
        storage.getString(StorageKeys.EXTERNAL_CONTROL_ALLOWED, "false") == "true"
}
