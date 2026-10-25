package org.fossify.messages.activities

import android.os.Bundle
import android.view.WindowManager
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.adapters.VaultMessagesAdapter
import org.fossify.messages.databinding.ActivityThreadBinding
import org.fossify.messages.extensions.vaultMessageDB
import org.fossify.messages.helpers.VaultSmsSender

class VaultThreadActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityThreadBinding::inflate)
    private var phoneNumber: String = ""
    private var contactName: String = ""
    private var adapter: VaultMessagesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Anti-Snoop Protection
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(binding.root)

        contactName = intent.getStringExtra("contact_name").orEmpty()
        phoneNumber = intent.getStringExtra("phone_number").orEmpty()

        setupTheme()
        setupListeners()
    }

    private fun setupTheme() {
        binding.root.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.root)
        
        val toolbar = binding.threadAppbar.findViewById<androidx.appcompat.widget.Toolbar>(org.fossify.messages.R.id.thread_toolbar)
        toolbar?.title = contactName.ifEmpty { phoneNumber }
        toolbar?.setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
        toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.messageHolder.threadSendMessage.setOnClickListener {
            val text = binding.messageHolder.threadTypeMessage.text.toString().trim()
            if (text.isNotEmpty() && phoneNumber.isNotEmpty()) {
                ensureBackgroundThread {
                    VaultSmsSender.sendVaultMessage(
                        context = this,
                        subId = -1, // default sub
                        destination = phoneNumber,
                        body = text
                    )
                    runOnUiThread {
                        binding.messageHolder.threadTypeMessage.setText("")
                        loadMessages()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTheme()
        loadMessages()
    }

    private fun loadMessages() {
        if (phoneNumber.isEmpty()) return
        ensureBackgroundThread {
            try {
                val messages = vaultMessageDB.getMessagesByPhoneNumber(phoneNumber)
                
                // Mark received unread messages as read to trigger self-destruct timer if active
                messages.filter { !it.read }.forEach {
                    vaultMessageDB.markAsRead(it.id, System.currentTimeMillis())
                }

                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    if (adapter == null) {
                        adapter = VaultMessagesAdapter(messages)
                        binding.threadMessagesList.adapter = adapter
                    } else {
                        adapter?.updateData(messages)
                    }
                    if (messages.isNotEmpty()) {
                        binding.threadMessagesList.scrollToPosition(messages.size - 1)
                    }
                }
            } catch (_: Exception) {
                // Defensive catch against background exceptions
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}
