package org.fossify.messages.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.tabs.TabLayout
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.adapters.VaultCallsAdapter
import org.fossify.messages.adapters.VaultContactsAdapter
import org.fossify.messages.databinding.ActivityVaultBinding
import org.fossify.messages.databinding.DialogAddVaultContactBinding
import org.fossify.messages.extensions.vaultCallDB
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.models.VaultContact

class VaultActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityVaultBinding::inflate)
    private var contactsAdapter: VaultContactsAdapter? = null
    private var callsAdapter: VaultCallsAdapter? = null
    private var currentTabPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Anti-Snoop (Prevent screenshot and overview visibility)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(binding.root)

        setupTheme()
        setupListeners()
    }

    private fun setupTheme() {
        binding.root.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.root)
    }

    private fun setupListeners() {
        binding.vaultAddFab.setOnClickListener {
            ensureBackgroundThread {
                val contacts = vaultContactDB.getAll()
                runOnUiThread {
                    if (org.fossify.messages.helpers.MonetizationConfig.isLimitReached(contacts.size)) {
                        org.fossify.messages.dialogs.DialogUnlockVault(this) {
                            showAddContactDialog()
                        }.show()
                    } else {
                        showAddContactDialog()
                    }
                }
            }
        }

        binding.vaultTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                if (currentTabPosition == 0) {
                    binding.vaultAddFab.beVisibleIf(true)
                    loadContacts()
                } else {
                    binding.vaultAddFab.beGoneIf(true)
                    loadBlockedCalls()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        setupTheme()
        checkBiometricsAndLoad()
    }

    private fun checkBiometricsAndLoad() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val biometricManager = getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            if (biometricManager != null) {
                // Biometrics available
                if (currentTabPosition == 0) {
                    loadContacts()
                } else {
                    loadBlockedCalls()
                }
                return
            }
        }
        
        if (currentTabPosition == 0) {
            loadContacts()
        } else {
            loadBlockedCalls()
        }
    }

    private fun loadContacts() {
        ensureBackgroundThread {
            try {
                val contacts = vaultContactDB.getAll()
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    binding.vaultPlaceholder.beGoneIf(contacts.isNotEmpty())
                    binding.vaultConversationsList.beVisibleIf(contacts.isNotEmpty())

                    contactsAdapter = VaultContactsAdapter(
                        contacts = contacts,
                        onItemClick = { contact -> openVaultThread(contact) },
                        onItemLongClick = { contact -> confirmDeleteContact(contact) }
                    )
                    binding.vaultConversationsList.adapter = contactsAdapter
                }
            } catch (e: Exception) {
                // Prevent background database exceptions from crashing app
            }
        }
    }

    private fun loadBlockedCalls() {
        ensureBackgroundThread {
            try {
                val calls = vaultCallDB.getAll()
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    binding.vaultPlaceholder.beGoneIf(calls.isNotEmpty())
                    binding.vaultConversationsList.beVisibleIf(calls.isNotEmpty())

                    callsAdapter = VaultCallsAdapter(calls)
                    binding.vaultConversationsList.adapter = callsAdapter
                }
            } catch (e: Exception) {
                // Prevent background database exceptions from crashing app
            }
        }
    }

    private fun openVaultThread(contact: VaultContact) {
        val intent = Intent(this, VaultThreadActivity::class.java).apply {
            putExtra("contact_name", contact.name)
            putExtra("phone_number", contact.phoneNumber)
        }
        startActivity(intent)
    }

    private fun confirmDeleteContact(contact: VaultContact) {
        AlertDialog.Builder(this)
            .setTitle("Remover Contacto")
            .setMessage("Deseja remover ${contact.name.ifEmpty { contact.phoneNumber }} do Cofre?")
            .setPositiveButton("Remover") { _, _ ->
                ensureBackgroundThread {
                    vaultContactDB.deleteByPhoneNumber(contact.phoneNumber)
                    loadContacts()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddContactDialog() {
        val dialogBinding = DialogAddVaultContactBinding.inflate(layoutInflater)
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = dialogBinding.addVaultContactName.text.toString().trim()
                val number = dialogBinding.addVaultContactNumber.text.toString().trim()
                val policy = when (dialogBinding.addVaultPolicyGroup.checkedRadioButtonId) {
                    dialogBinding.policyDiscrete.id -> 1
                    dialogBinding.policySelfDestruct.id -> 2
                    else -> 0
                }

                if (number.isNotEmpty()) {
                    ensureBackgroundThread {
                        val newContact = VaultContact(
                            name = name.ifEmpty { number },
                            phoneNumber = number,
                            interactionPolicy = policy
                        )
                        vaultContactDB.insert(newContact)
                        loadContacts()
                    }
                } else {
                    showErrorToast("O número de telefone é obrigatório")
                }
            }
            .setNeutralButton("Backup Automático") { _, _ ->
                showBackupDialog()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showBackupDialog() {
        val dialogBinding = org.fossify.messages.databinding.DialogUserBackupConfigBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("Fechar", null)
            .create()

        dialogBinding.btnAutoBackup.setOnClickListener {
            val username = dialogBinding.backupUsername.text.toString().trim()
            val password = dialogBinding.backupPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val passKey = "$username:$password"
                org.fossify.messages.helpers.VaultAutoBackupManager.performAutoBackup(this, passKey, username) { success ->
                    runOnUiThread {
                        if (success) {
                            toast("Backup encriptado guardado com sucesso!")
                            alertDialog.dismiss()
                        } else {
                            showErrorToast("Falha ao criar o backup")
                        }
                    }
                }
            } else {
                showErrorToast("Preencha o Utilizador e a Palavra-passe")
            }
        }

        dialogBinding.btnAutoRestore.setOnClickListener {
            val username = dialogBinding.backupUsername.text.toString().trim()
            val password = dialogBinding.backupPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val passKey = "$username:$password"
                org.fossify.messages.helpers.VaultAutoBackupManager.restoreAutoBackup(this, passKey) { success, msg ->
                    runOnUiThread {
                        if (success) {
                            toast(msg)
                            loadContacts()
                            alertDialog.dismiss()
                        } else {
                            showErrorToast(msg)
                        }
                    }
                }
            } else {
                showErrorToast("Preencha o Utilizador e a Palavra-passe")
            }
        }

        alertDialog.show()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}
