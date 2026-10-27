package org.fossify.messages.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.models.Conversation

class VaultActivity : MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Anti-Snoop (Prevent screenshot and overview visibility)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        // Activate top navigation arrow to return to MainActivity
        val toolbar = findViewById<org.fossify.commons.views.MySearchMenu>(R.id.main_menu)?.requireToolbar()
        toolbar?.setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
        toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    override fun setupOptionsMenu() {
        val menuView = findViewById<org.fossify.commons.views.MySearchMenu>(R.id.main_menu)
        menuView?.requireToolbar()?.inflateMenu(R.menu.menu_vault)
        menuView?.toggleHideOnScroll(true)
        menuView?.setupMenu()

        menuView?.requireToolbar()?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_vault_settings -> {
                    startActivity(Intent(this, VaultSettingsActivity::class.java))
                    true
                }
                R.id.action_vault_manage_contacts -> {
                    startActivity(Intent(this, ManageVaultContactsActivity::class.java))
                    true
                }
                R.id.action_vault_manage_keywords -> {
                    startActivity(Intent(this, ManageVaultKeywordsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun refreshMenuItems() {
        // Vault menu items do not depend on public recycle bin or archive config
    }

    override fun getCachedConversations() {
        ensureBackgroundThread {
            val vaultContacts = try {
                vaultContactDB.getAllWithSnippets()
            } catch (_: Exception) {
                emptyList()
            }

            val conversations = vaultContacts.map { item ->
                val contact = item.contact
                Conversation(
                    threadId = contact.phoneNumber.hashCode().toLong(),
                    snippet = item.snippet ?: contact.groupName,
                    date = ((item.date ?: contact.addedDate) / 1000).toInt(),
                    read = item.unreadCount == 0,
                    title = contact.name,
                    photoUri = "",
                    isGroupConversation = false,
                    phoneNumber = contact.phoneNumber,
                    unreadCount = item.unreadCount
                )
            } as ArrayList<Conversation>

            runOnUiThread {
                setupConversations(conversations, cached = true)
            }
        }
    }

    override fun handleConversationClick(any: Any) {
        val conversation = any as Conversation
        Intent(this, VaultThreadActivity::class.java).apply {
            putExtra(org.fossify.messages.helpers.THREAD_ID, conversation.threadId)
            putExtra(org.fossify.messages.helpers.THREAD_TITLE, conversation.title)
            putExtra(org.fossify.messages.helpers.THREAD_NUMBER, conversation.phoneNumber)
            putExtra("contact_name", conversation.title)
            putExtra("phone_number", conversation.phoneNumber)
            startActivity(this)
        }
    }

    override fun launchNewConversation() {
        Intent(this, NewConversationActivity::class.java).apply {
            putExtra("is_vault_mode", true)
            startActivity(this)
        }
    }
}
