package org.fossify.messages.activities

import android.os.Bundle
import android.view.WindowManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.helpers.VaultSmsSender

class VaultThreadActivity : ThreadActivity() {

    private var vaultPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Anti-Snoop Protection
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
        vaultPhoneNumber = intent.getStringExtra("phone_number").orEmpty()

        // Ensure any existing public conversations with this contact are exported into the vault
        if (vaultPhoneNumber.isNotEmpty()) {
            ensureBackgroundThread {
                org.fossify.messages.helpers.VaultCleanupHelper.runCleanup(this)
            }
        }
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
            }
        } else {
            super.sendNormalMessage(text, subscriptionId)
        }
    }
}
