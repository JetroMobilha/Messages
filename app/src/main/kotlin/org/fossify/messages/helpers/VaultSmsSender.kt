package org.fossify.messages.helpers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import org.fossify.messages.extensions.smsSender
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.models.VaultMessage

object VaultSmsSender {

    fun sendVaultMessage(
        context: Context,
        subId: Int,
        destination: String,
        body: String
    ) {
        val date = System.currentTimeMillis()
        
        // 1. Insert into Vault DB first to get an ID
        val message = VaultMessage(
            phoneNumber = destination,
            body = body,
            date = date,
            type = Telephony.Sms.MESSAGE_TYPE_OUTBOX,
            read = true,
            isMms = false
        )
        val vaultId = context.vaultMessageDB.insert(message)
        
        // 2. We use a custom URI scheme to identify vault messages in the broadcast receivers
        val vaultUri = Uri.parse("content://vault_sms/$vaultId")
        
        // 3. Send using the existing SmsSender
        context.smsSender.sendMessage(
            subId = subId,
            destination = destination,
            body = body,
            serviceCenter = null,
            requireDeliveryReport = false,
            messageUri = vaultUri
        )
    }
}
