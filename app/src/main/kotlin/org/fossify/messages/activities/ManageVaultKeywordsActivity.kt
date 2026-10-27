package org.fossify.messages.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.adapters.VaultKeywordsAdapter
import org.fossify.messages.databinding.ActivityManageVaultKeywordsBinding
import org.fossify.messages.databinding.DialogAddVaultKeywordBinding
import org.fossify.messages.extensions.vaultKeywordDB

class ManageVaultKeywordsActivity : SimpleActivity() {
    override var isSearchBarEnabled = true

    private val binding by viewBinding(ActivityManageVaultKeywordsBinding::inflate)

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
        toolbar.title = "Gerir Palavras Furtivas"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        binding.vaultMenu.onSearchTextChangedListener = { text ->
            // Optionally filter keywords here in the future
        }
        binding.vaultAddFab.setOnClickListener {
            showAddKeywordDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadKeywords()
    }

    private fun loadKeywords() {
        ensureBackgroundThread {
            try {
                val keywords = vaultKeywordDB.getAll()
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    binding.vaultPlaceholder.text = "Nenhuma palavra-chave furtiva configurada"
                    binding.vaultPlaceholder.beGoneIf(keywords.isNotEmpty())
                    binding.vaultConversationsList.beVisibleIf(keywords.isNotEmpty())

                    binding.vaultConversationsList.adapter = VaultKeywordsAdapter(keywords) { kw ->
                        confirmDeleteKeyword(kw)
                    }
                }
            } catch (e: Exception) {}
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

    private fun showAddKeywordDialog() {
        val dialogBinding = DialogAddVaultKeywordBinding.inflate(layoutInflater)
        
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
}
