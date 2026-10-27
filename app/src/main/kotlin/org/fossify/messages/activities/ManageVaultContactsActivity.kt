package org.fossify.messages.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.adapters.VaultContactsAdapter
import org.fossify.messages.databinding.ActivityManageVaultContactsBinding
import org.fossify.messages.databinding.DialogAddVaultContactBinding
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.models.VaultContact

class ManageVaultContactsActivity : SimpleActivity() {
    override var isSearchBarEnabled = true

    private val binding by viewBinding(ActivityManageVaultContactsBinding::inflate)
    private var contactsAdapter: VaultContactsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(binding.root)
        
        binding.vaultMenu.setupMenu()
        setupTheme()
        setupListeners()
    }

    private fun setupTheme() {
        binding.root.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.root)
        val toolbar = binding.vaultMenu.requireToolbar()
        toolbar.setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
        toolbar.title = "Gerir Filtros de Contacto"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.vaultMenu.onSearchTextChangedListener = { text ->
            // Optionally filter contacts here in the future
        }
        binding.vaultAddFab.setOnClickListener {
            ensureBackgroundThread {
                val contacts = vaultContactDB.getAll()
                runOnUiThread {
                    if (org.fossify.messages.helpers.MonetizationConfig.isLimitReached(contacts.size)) {
                        org.fossify.messages.dialogs.DialogUnlockVault(this) {
                            showSourceSelectionDialog()
                        }.show()
                    } else {
                        showSourceSelectionDialog()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
    }

    private fun loadContacts() {
        ensureBackgroundThread {
            try {
                val contactsWithSnippets = vaultContactDB.getAllWithSnippets()
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    binding.vaultPlaceholder.text = "Nenhum filtro de contacto configurado"
                    binding.vaultPlaceholder.beGoneIf(contactsWithSnippets.isNotEmpty())
                    binding.vaultConversationsList.beVisibleIf(contactsWithSnippets.isNotEmpty())

                    contactsAdapter = VaultContactsAdapter(
                        activity = this,
                        contacts = contactsWithSnippets,
                        onItemClick = { contact -> /* Não abrir chat, apenas editar policy */ editContact(contact) },
                        onItemLongClick = { contact -> confirmDeleteContact(contact) }
                    )
                    binding.vaultConversationsList.adapter = contactsAdapter
                }
            } catch (e: Exception) {}
        }
    }

    private fun editContact(contact: VaultContact) {
        toast("Edição rápida da política de ${contact.name}")
        // Placeholder for edit dialog
    }

    private fun confirmDeleteContact(contact: VaultContact) {
        AlertDialog.Builder(this)
            .setTitle("Remover Contacto")
            .setMessage("Deseja remover ${contact.name.ifEmpty { contact.phoneNumber }} do Cofre?")
            .setPositiveButton("Remover") { _, _ ->
                ensureBackgroundThread {
                    vaultContactDB.deleteByPhoneNumber(contact.phoneNumber)
                    org.fossify.messages.helpers.VaultSyncManager.broadcastDelete(this, contact.phoneNumber)
                    loadContacts()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSourceSelectionDialog() {
        val sourceBinding = org.fossify.messages.databinding.DialogSelectContactSourceBinding.inflate(layoutInflater)
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        bottomSheet.setContentView(sourceBinding.root)

        sourceBinding.sourceManual.setOnClickListener {
            bottomSheet.dismiss()
            showAddContactDialog("", "")
        }

        sourceBinding.sourceContacts.setOnClickListener {
            bottomSheet.dismiss()
            val intent = Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            try {
                startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                showErrorToast("Acesso aos contactos não disponível")
            }
        }

        sourceBinding.sourceCallLog.setOnClickListener {
            bottomSheet.dismiss()
            showCallLogPicker()
        }

        bottomSheet.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val projection = arrayOf(
                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                        android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val numberIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val number = if (numberIdx >= 0) cursor.getString(numberIdx) else ""
                            val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                            showAddContactDialog(name, number)
                        }
                    }
                } catch (e: Exception) {
                    showErrorToast("Erro ao ler contacto")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showCallLogPicker()
        }
    }

    private fun showCallLogPicker() {
        val hasCallLogPerm = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCallLogPerm) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.WRITE_CALL_LOG), 1002)
            return
        }

        ensureBackgroundThread {
            try {
                val projection = arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.CACHED_NAME
                )
                val recentCalls = mutableListOf<Pair<String, String>>()
                
                contentResolver.query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    projection,
                    null, null,
                    android.provider.CallLog.Calls.DATE + " DESC"
                )?.use { cursor ->
                    val numberIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                    
                    while (cursor.moveToNext() && recentCalls.size < 20) {
                        val number = if (numberIdx >= 0) cursor.getString(numberIdx) else ""
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                        val cleanName = if (name.isNullOrBlank()) "Desconhecido" else name
                        if (number.isNotEmpty() && !recentCalls.any { it.second == number }) {
                            recentCalls.add(Pair(cleanName, number))
                        }
                    }
                }

                runOnUiThread {
                    if (recentCalls.isEmpty()) {
                        showErrorToast("Nenhuma chamada recente encontrada")
                        return@runOnUiThread
                    }

                    val items = recentCalls.map { "${it.first}\n${it.second}" }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Chamadas Recentes")
                        .setItems(items) { _, which ->
                            val selected = recentCalls[which]
                            showAddContactDialog(if (selected.first == "Desconhecido") "" else selected.first, selected.second)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread { showErrorToast("Erro ao aceder ao Registo de Chamadas") }
            }
        }
    }

    private fun showAddContactDialog(prefilledName: String, prefilledNumber: String) {
        val dialogBinding = DialogAddVaultContactBinding.inflate(layoutInflater)
        dialogBinding.addVaultContactName.setText(prefilledName)
        dialogBinding.addVaultContactNumber.setText(prefilledNumber)
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Adicionar") { _, _ ->
                val name = dialogBinding.addVaultContactName.text.toString().trim()
                val number = dialogBinding.addVaultContactNumber.text.toString().trim()
                val group = dialogBinding.addVaultContactGroup.text.toString().trim().ifEmpty { "Geral" }
                val blockCalls = dialogBinding.addVaultContactBlockCalls.isChecked
                
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
                            interactionPolicy = policy,
                            groupName = group,
                            blockCalls = blockCalls
                        )
                        vaultContactDB.insert(newContact)
                        org.fossify.messages.helpers.VaultSyncManager.broadcastUpsert(this, newContact)
                        org.fossify.messages.helpers.VaultCleanupHelper.runCleanup(this)
                        loadContacts()
                    }
                } else {
                    showErrorToast("O número de telefone é obrigatório")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
