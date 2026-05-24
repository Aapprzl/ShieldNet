package com.shieldnet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shieldnet.utils.Constants
import com.shieldnet.vpn.ShieldVpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean(Constants.PREF_AUTO_START, false)

            if (autoStart) {
                ShieldVpnService.start(context)
            }
        }
    }
}
