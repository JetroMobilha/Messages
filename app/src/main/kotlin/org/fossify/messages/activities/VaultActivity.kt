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

import org.fossify.messages.adapters.VaultContactsAdapter
import org.fossify.messages.databinding.ActivityVaultBinding
import org.fossify.messages.databinding.DialogAddVaultContactBinding
import org.fossify.messages.extensions.vaultCallDB
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.extensions.vaultKeywordDB
import org.fossify.messages.models.VaultContact

class VaultActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityVaultBinding::inflate)
    private var contactsAdapter: VaultContactsAdapter? = null
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
        val toolbar = binding.vaultMenu.findViewById<androidx.appcompat.widget.Toolbar>(org.fossify.commons.R.id.top_toolbar)
        toolbar?.inflateMenu(org.fossify.messages.R.menu.menu_vault)
        toolbar?.setOnMenuItemClickListener { item ->
            if (item.itemId == org.fossify.messages.R.id.action_vault_settings) {
                startActivity(Intent(this, VaultSettingsActivity::class.java))
                true
            } else {
                false
            }
        }

        binding.vaultAddFab.setOnClickListener {
            if (currentTabPosition == 2) {
                showAddKeywordDialog()
                return@setOnClickListener
            }

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

        binding.vaultTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                binding.vaultAddFab.beVisibleIf(true)
                if (currentTabPosition == 0) {
                    loadContacts()
                } else {
                    loadKeywords()
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
                    loadKeywords()
                }
                return
            }
        }
        
        if (currentTabPosition == 0) {
            loadContacts()
        } else {
            loadKeywords()
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

    private fun loadKeywords() {
        ensureBackgroundThread {
            try {
                val keywords = vaultKeywordDB.getAll()
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    binding.vaultPlaceholder.beGoneIf(keywords.isNotEmpty())
                    binding.vaultConversationsList.beVisibleIf(keywords.isNotEmpty())

                    binding.vaultConversationsList.adapter = org.fossify.messages.adapters.VaultKeywordsAdapter(keywords) { kw ->
                        confirmDeleteKeyword(kw)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun confirmDeleteKeyword(kw: org.fossify.messages.models.VaultKeyword) {
        AlertDialog.Builder(this)
            .setTitle("Remover Palavra-Chave")
            .setMessage("Deseja remover '${kw.keyword}' do filtro furtivo?")
            .setPositiveButton("Remover") { _, _ ->
                ensureBackgroundThread {
                    vaultKeywordDB.delete(kw.id)
                    loadKeywords()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

    private fun showAddKeywordDialog() {
        val dialogBinding = org.fossify.messages.databinding.DialogAddVaultKeywordBinding.inflate(layoutInflater)
        
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Adicionar") { _, _ ->
                val keyword = dialogBinding.addVaultKeywordText.text.toString().trim()
                val policy = when (dialogBinding.addVaultKeywordPolicyGroup.checkedRadioButtonId) {
                    dialogBinding.policyKwDiscrete.id -> 1
                    dialogBinding.policyKwSelfDestruct.id -> 2
                    else -> 0
                }

                if (keyword.isNotEmpty()) {
                    ensureBackgroundThread {
                        val newKw = org.fossify.messages.models.VaultKeyword(
                            keyword = keyword,
                            interactionPolicy = policy
                        )
                        vaultKeywordDB.insert(newKw)
                        loadKeywords()
                    }
                } else {
                    showErrorToast("A palavra-chave não pode estar vazia")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                    android.provider.CallLog.Calls.DATE + " DESC LIMIT 20"
                )?.use { cursor ->
                    val numberIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    val nameIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                    
                    while (cursor.moveToNext()) {
                        val number = if (numberIdx >= 0) cursor.getString(numberIdx) else ""
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) else ""
                        if (number.isNotEmpty() && !recentCalls.any { it.second == number }) {
                            recentCalls.add(Pair(name ?: "Desconhecido", number))
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

            } catch (e: SecurityException) {
                runOnUiThread { showErrorToast("Permissão do Histórico de Chamadas necessária") }
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
                        org.fossify.messages.helpers.VaultCleanupHelper.runCleanup(this)
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
}
