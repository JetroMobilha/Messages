package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.helpers.VaultCleanupHelper
import org.fossify.messages.helpers.VaultConfig

class VaultBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val vaultConfig = VaultConfig.getInstance(context)
            if (vaultConfig.backgroundSweeperEnabled) {
                VaultCleanupHelper.scheduleSweeper(context, vaultConfig.sweeperIntervalMinutes)
            }
            // Run an immediate purge check upon boot
            VaultCleanupHelper.runCleanup(context)
        }
    }
}
