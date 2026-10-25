package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.helpers.VaultCleanupHelper
import org.fossify.messages.helpers.VaultSyncManager
import org.fossify.messages.models.VaultContact

class VaultSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // Safety guard: only process if the broadcast has the sync flag
        if (!intent.getBooleanExtra(VaultSyncManager.EXTRA_IS_FROM_SYNC, false)) {
            return
        }

        ensureBackgroundThread {
            when (action) {
                VaultSyncManager.ACTION_VAULT_UPSERT -> {
                    val phone = intent.getStringExtra("phone_number") ?: return@ensureBackgroundThread
                    val name = intent.getStringExtra("name") ?: "Desconhecido"
                    val group = intent.getStringExtra("group_name") ?: "Geral"
                    val policy = intent.getIntExtra("interaction_policy", 0)
                    val blockCalls = intent.getBooleanExtra("block_calls", true)

                    val contact = VaultContact(
                        name = name,
                        phoneNumber = phone,
                        interactionPolicy = policy,
                        groupName = group,
                        blockCalls = blockCalls
                    )
                    
                    val existing = context.vaultContactDB.getByPhoneNumber(phone)
                    if (existing == null) {
                        context.vaultContactDB.insert(contact)
                        // Optional: purge public traces when receiving a sync insertion from Phone Pro
                        VaultCleanupHelper.runCleanup(context)
                    } else {
                        // If it already exists, just update policy/group if needed (we'll delete and re-insert for simplicity in this demo)
                        context.vaultContactDB.deleteByPhoneNumber(phone)
                        context.vaultContactDB.insert(contact)
                    }
                }
                VaultSyncManager.ACTION_VAULT_DELETE -> {
                    val phone = intent.getStringExtra("phone_number") ?: return@ensureBackgroundThread
                    context.vaultContactDB.deleteByPhoneNumber(phone)
                }
                VaultSyncManager.ACTION_REQUEST_FULL_SYNC -> {
                    // Send our full database back to the requester
                    val contacts = context.vaultContactDB.getAll()
                    if (contacts.isNotEmpty()) {
                        VaultSyncManager.broadcastFullSyncPayload(context, contacts)
                    }
                }
                VaultSyncManager.ACTION_FULL_SYNC_PAYLOAD -> {
                    // Parse the payload and insert any missing contacts
                    val payload = intent.getStringExtra("payload") ?: return@ensureBackgroundThread
                    try {
                        val contactsType = object : com.google.gson.reflect.TypeToken<List<VaultContact>>() {}.type
                        val receivedContacts: List<VaultContact> = com.google.gson.Gson().fromJson(payload, contactsType)

                        var addedAny = false
                        for (contact in receivedContacts) {
                            if (context.vaultContactDB.getByPhoneNumber(contact.phoneNumber) == null) {
                                context.vaultContactDB.insert(contact)
                                addedAny = true
                            }
                        }
                        
                        if (addedAny) {
                            VaultCleanupHelper.runCleanup(context)
                        }
                    } catch (e: Exception) {
                        // Ignore JSON parsing errors
                    }
                }
            }
        }
    }
}
