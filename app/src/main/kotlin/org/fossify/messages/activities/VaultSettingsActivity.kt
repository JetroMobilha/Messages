package org.fossify.messages.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.messages.databinding.ActivityVaultSettingsBinding
import org.fossify.messages.helpers.VaultCleanupHelper
import org.fossify.messages.helpers.VaultConfig
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityVaultSettingsBinding::inflate)
    private lateinit var vaultConfig: VaultConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(binding.root)
        vaultConfig = VaultConfig.getInstance(this)

        setupTheme()
        setupUI()
        setupListeners()
    }

    private fun setupTheme() {
        binding.vaultSettingsHolder.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.vaultSettingsHolder)
        
        binding.vaultSettingsToolbar.setNavigationIcon(org.fossify.commons.R.drawable.ic_arrow_left_vector)
        binding.vaultSettingsToolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        binding.vaultSweeperSwitch.isChecked = vaultConfig.backgroundSweeperEnabled
        binding.vaultSweeperSlider.value = vaultConfig.sweeperIntervalMinutes.toFloat()

        refreshHealthStatus()
        refreshPermissionsUI()
    }

    private fun setupListeners() {
        binding.vaultSweeperSwitch.setOnCheckedChangeListener { _, isChecked ->
            vaultConfig.backgroundSweeperEnabled = isChecked
            if (isChecked) {
                VaultCleanupHelper.scheduleSweeper(this, vaultConfig.sweeperIntervalMinutes)
            } else {
                VaultCleanupHelper.cancelSweeper(this)
            }
            refreshHealthStatus()
        }

        binding.vaultSweeperSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                vaultConfig.sweeperIntervalMinutes = value.toInt()
                if (vaultConfig.backgroundSweeperEnabled) {
                    VaultCleanupHelper.scheduleSweeper(this, value.toInt())
                }
            }
        }

        binding.vaultPermBatteryBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            try {
                startActivity(intent)
            } catch (_: Exception) {}
        }

        binding.vaultPermCallBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        binding.vaultChangePinBtn.setOnClickListener {
            showChangePinDialog(isFake = false)
        }

        binding.vaultChangeFakePinBtn.setOnClickListener {
            showChangePinDialog(isFake = true)
        }

        binding.vaultPermContactsBtn.setOnClickListener {
            requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), 101)
        }

        binding.vaultPermRoleBtn.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(android.app.role.RoleManager::class.java)
                if (roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING) == false) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                    startActivityForResult(intent, 102)
                }
            } else {
                toast("Filtro de chamadas ativado nativamente.")
            }
        }
    }

    private fun showChangePinDialog(isFake: Boolean) {
        val title = if (isFake) "Alterar PIN de Emergência" else "Alterar PIN do Cofre"
        val message = if (isFake) "Introduza o novo PIN (apenas números) que abrirá o Cofre Falso:" else "Introduza o novo PIN (apenas números) para o Cofre Real:"
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newPin = input.text.toString().trim()
                if (newPin.isNotEmpty() && newPin.all { it.isDigit() }) {
                    if (isFake) vaultConfig.fakeVaultPin = newPin else vaultConfig.vaultPin = newPin
                    toast("PIN alterado com sucesso.")
                } else {
                    showErrorToast("O PIN deve conter apenas números.")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshHealthStatus() {
        if (vaultConfig.backgroundSweeperEnabled) {
            binding.vaultHealthStatus.text = "Status: Otimização Ativa (${vaultConfig.sweeperIntervalMinutes}m)"
            binding.vaultHealthStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            binding.vaultHealthStatus.text = "Status: Otimização Inativa"
            binding.vaultHealthStatus.setTextColor(android.graphics.Color.parseColor("#E53935"))
        }

        val lastRun = vaultConfig.lastSweeperExecution
        if (lastRun > 0) {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.vaultHealthLastRun.text = "Última Varredura: ${format.format(Date(lastRun))}"
        } else {
            binding.vaultHealthLastRun.text = "Última Varredura: Nunca"
        }
    }

    private fun refreshPermissionsUI() {
        val pm = getSystemService(android.os.PowerManager::class.java)
        val isIgnoringBattery = pm?.isIgnoringBatteryOptimizations(packageName) ?: false
        binding.vaultPermBatteryBtn.isEnabled = !isIgnoringBattery
        binding.vaultPermBatteryBtn.text = if (isIgnoringBattery) "Bateria Otimizada (OK)" else "Ignorar Otimização de Bateria"

        val hasCallLog = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                         androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
        binding.vaultPermCallBtn.isEnabled = !hasCallLog
        binding.vaultPermCallBtn.text = if (hasCallLog) "Registo de Chamadas (OK)" else "Permissão de Registo de Chamadas"

        val hasContacts = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        binding.vaultPermContactsBtn.isEnabled = !hasContacts
        binding.vaultPermContactsBtn.text = if (hasContacts) "Contactos (OK)" else "Permissão de Contactos"

        var hasRole = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            hasRole = roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING) == true
        }
        binding.vaultPermRoleBtn.isEnabled = !hasRole
        binding.vaultPermRoleBtn.text = if (hasRole) "Filtro de Chamadas (OK)" else "Ativar Filtro de Chamadas"
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionsUI()
        refreshHealthStatus()
    }
}
