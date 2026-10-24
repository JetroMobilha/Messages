package org.fossify.messages.helpers

import android.content.Context
import android.content.SharedPreferences

class VaultConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("VaultPrefs", Context.MODE_PRIVATE)

    var backgroundSweeperEnabled: Boolean
        get() = prefs.getBoolean("sweeper_enabled", false)
        set(value) = prefs.edit().putBoolean("sweeper_enabled", value).apply()

    var sweeperIntervalMinutes: Int
        get() = prefs.getInt("sweeper_interval", 15)
        set(value) = prefs.edit().putInt("sweeper_interval", value).apply()

    var vaultRetentionDays: Int
        get() = prefs.getInt("vault_retention_days", 7)
        set(value) = prefs.edit().putInt("vault_retention_days", value).apply()

    var lastSweeperExecution: Long
        get() = prefs.getLong("last_sweeper_execution", 0L)
        set(value) = prefs.edit().putLong("last_sweeper_execution", value).apply()

    var vaultPin: String
        get() = prefs.getString("vault_pin", "0000") ?: "0000"
        set(value) = prefs.edit().putString("vault_pin", value).apply()

    var fakeVaultPin: String
        get() = prefs.getString("fake_vault_pin", "9999") ?: "9999"
        set(value) = prefs.edit().putString("fake_vault_pin", value).apply()

    companion object {
        @Volatile
        private var instance: VaultConfig? = null

        fun getInstance(context: Context): VaultConfig {
            return instance ?: synchronized(this) {
                instance ?: VaultConfig(context.applicationContext).also { instance = it }
            }
        }
    }
}
