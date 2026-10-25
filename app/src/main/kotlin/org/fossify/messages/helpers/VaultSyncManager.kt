package org.fossify.messages.helpers

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import org.fossify.messages.models.VaultContact

object VaultSyncManager {
    const val ACTION_VAULT_UPSERT = "org.fossify.sync.VAULT_CONTACT_UPSERT"
    const val ACTION_VAULT_DELETE = "org.fossify.sync.VAULT_CONTACT_DELETE"
    const val ACTION_REQUEST_FULL_SYNC = "org.fossify.sync.REQUEST_FULL_SYNC"
    const val ACTION_FULL_SYNC_PAYLOAD = "org.fossify.sync.FULL_SYNC_PAYLOAD"
    
    const val PERMISSION_SYNC = "org.fossify.permission.SYNC_VAULT"
    const val EXTRA_IS_FROM_SYNC = "is_from_sync" // Anti-loop guard
    const val EXTRA_SYNC_SENDER = "sync_sender" // App origin identifier

    private const val APP_IDENTIFIER = "org.fossify.messages"

    fun broadcastUpsert(context: Context, contact: VaultContact) {
        val intent = Intent(ACTION_VAULT_UPSERT).apply {
            putExtra("phone_number", contact.phoneNumber)
            putExtra("name", contact.name)
            putExtra("group_name", contact.groupName)
            putExtra("interaction_policy", contact.interactionPolicy)
            putExtra("block_calls", contact.blockCalls)
            putExtra(EXTRA_IS_FROM_SYNC, true)
            putExtra(EXTRA_SYNC_SENDER, APP_IDENTIFIER)
        }
        context.sendBroadcast(intent, PERMISSION_SYNC)
    }

    fun broadcastDelete(context: Context, phoneNumber: String) {
        val intent = Intent(ACTION_VAULT_DELETE).apply {
            putExtra("phone_number", phoneNumber)
            putExtra(EXTRA_IS_FROM_SYNC, true)
            putExtra(EXTRA_SYNC_SENDER, APP_IDENTIFIER)
        }
        context.sendBroadcast(intent, PERMISSION_SYNC)
    }

    fun requestFullSync(context: Context) {
        val intent = Intent(ACTION_REQUEST_FULL_SYNC).apply {
            putExtra(EXTRA_IS_FROM_SYNC, true)
            putExtra(EXTRA_SYNC_SENDER, APP_IDENTIFIER)
        }
        context.sendBroadcast(intent, PERMISSION_SYNC)
    }

    fun broadcastFullSyncPayload(context: Context, contacts: List<VaultContact>) {
        val jsonPayload = Gson().toJson(contacts)
        val intent = Intent(ACTION_FULL_SYNC_PAYLOAD).apply {
            putExtra("payload", jsonPayload)
            putExtra(EXTRA_IS_FROM_SYNC, true)
            putExtra(EXTRA_SYNC_SENDER, APP_IDENTIFIER)
        }
        context.sendBroadcast(intent, PERMISSION_SYNC)
    }
}
