package com.linx3141.mexpa.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.linx3141.mexpa.platform.PlatformStorage
import com.linx3141.mexpa.platform.StorageKeys
import com.linx3141.mexpa.platform.WifiPolicyIntents

class WifiPolicyBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val storage = PlatformStorage(context)
                val enabled = storage.getString(StorageKeys.WIFI_POLICY_ENABLED, "false") == "true"
                val pendingRestart = storage.getString(StorageKeys.WIFI_POLICY_PENDING_RESTART, "false") == "true"
                if (!enabled && !pendingRestart) return
                val serviceIntent = Intent(context, WifiPolicyMonitorService::class.java).apply {
                    action = WifiPolicyIntents.ACTION_START
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
