package org.fossify.messages.activities

import android.os.Bundle
import android.provider.Telephony
import android.view.WindowManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.toSortedMessages
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.helpers.THREAD_NUMBER
import org.fossify.messages.helpers.THREAD_TITLE
import org.fossify.messages.helpers.VaultSmsSender
import org.fossify.messages.models.Message

class VaultThreadActivity : ThreadActivity() {

    private var vaultPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val phone = intent.getStringExtra("phone_number")
            ?: intent.getStringExtra(THREAD_NUMBER)
            ?: ""
        val title = intent.getStringExtra("contact_name")
            ?: intent.getStringExtra(THREAD_TITLE)
            ?: phone

        intent.putExtra(THREAD_NUMBER, phone)
        intent.putExtra(THREAD_TITLE, title)
        intent.putExtra(org.fossify.messages.helpers.THREAD_ID, phone.hashCode().toLong())

        // Set vaultPhoneNumber BEFORE calling super.onCreate so that fetchThreadMessages() has it when super.onCreate() calls loadConversation()
        vaultPhoneNumber = phone

        // Anti-Snoop Protection
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)

        // Ensure any existing public conversations with this contact are exported into the vault
        if (vaultPhoneNumber.isNotEmpty()) {
            ensureBackgroundThread {
                org.fossify.messages.helpers.VaultCleanupHelper.runCleanup(this)
            }
        }
    }

    override fun fetchThreadMessages(): List<Message> {
        if (vaultPhoneNumber.isEmpty()) return emptyList()
        val allMsgs = vaultMessageDB.getAll()
        val vaultMsgs = allMsgs.filter { v ->
            org.fossify.messages.helpers.VaultCleanupHelper.isSamePhoneNumber(v.phoneNumber, vaultPhoneNumber)
        }
        
        // Mark unread messages as read
        vaultMsgs.filter { !it.read }.forEach {
            vaultMessageDB.markAsRead(it.id, System.currentTimeMillis())
        }

        val contactName = intent.getStringExtra("contact_name").orEmpty().ifBlank { vaultPhoneNumber }
        val senderContact = org.fossify.commons.models.SimpleContact(
            rawId = vaultPhoneNumber.hashCode(),
            contactId = vaultPhoneNumber.hashCode(),
            name = contactName,
            photoUri = "",
            phoneNumbers = arrayListOf(org.fossify.commons.models.PhoneNumber(vaultPhoneNumber, 0, "", vaultPhoneNumber)),
            birthdays = ArrayList(),
            anniversaries = ArrayList()
        )

        return vaultMsgs.map { v ->
            Message(
                id = v.id,
                body = v.body,
                type = v.type,
                status = Telephony.Sms.STATUS_NONE,
                participants = arrayListOf(senderContact),
                date = (v.date / 1000L).toInt(),
                read = v.read,
                threadId = vaultPhoneNumber.hashCode().toLong(),
                isMMS = v.isMms,
                attachment = null,
                senderPhoneNumber = v.phoneNumber,
                senderName = contactName,
                senderPhotoUri = "",
                subscriptionId = v.subscriptionId
            )
        }.toSortedMessages()
    }

    override fun sendNormalMessage(text: String, subscriptionId: Int) {
        if (vaultPhoneNumber.isNotEmpty() && text.isNotEmpty()) {
            ensureBackgroundThread {
                VaultSmsSender.sendVaultMessage(
                    context = this,
                    subId = subscriptionId,
                    destination = vaultPhoneNumber,
                    body = text
                )
                
                messages = fetchThreadMessages()
                runOnUiThread {
                    setupAdapter()
                    scrollToBottom()
                    clearCurrentMessage()
                }
            }
        } else {
            super.sendNormalMessage(text, subscriptionId)
        }
    }
}
