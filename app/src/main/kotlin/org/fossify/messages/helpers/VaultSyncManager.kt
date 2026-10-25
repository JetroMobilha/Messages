package org.fossify.messages.helpers

import android.content.Context
import android.content.Intent
import org.fossify.messages.models.VaultContact

object VaultSyncManager {
    const val ACTION_VAULT_UPSERT = "org.fossify.sync.VAULT_CONTACT_UPSERT"
    const val ACTION_VAULT_DELETE = "org.fossify.sync.VAULT_CONTACT_DELETE"
    const val PERMISSION_SYNC = "org.fossify.permission.SYNC_VAULT"
    const val EXTRA_IS_FROM_SYNC = "is_from_sync" // Anti-loop guard

    fun broadcastUpsert(context: Context, contact: VaultContact) {
        val intent = Intent(ACTION_VAULT_UPSERT).apply {
            putExtra("phone_number", contact.phoneNumber)
            putExtra("name", contact.name)
            putExtra("group_name", contact.groupName)
            putExtra("interaction_policy", contact.interactionPolicy)
            putExtra("block_calls", contact.blockCalls)
            putExtra(EXTRA_IS_FROM_SYNC, true) // Tell receiver this is an automated broadcast
        }
        context.sendBroadcast(intent, PERMISSION_SYNC)
    }

    fun broadcastDelete(context: Context, phoneNumber: String) {
        val intent = Intent(ACTION_VAULT_DELETE).apply {
            putExtra("phone_number", phoneNumber)
            putExtra(EXTRA_IS_FROM_SYNC, true)
        }
        context.sendBroadcast(intent, PERMISSION_SYNC)
    }
}
