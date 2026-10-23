package org.fossify.messages.dialogs

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.toast
import org.fossify.messages.databinding.DialogUnlockVaultBinding

class DialogUnlockVault(
    private val activity: Activity,
    private val onUnlocked: () -> Unit
) {
    private var dialog: AlertDialog? = null

    fun show() {
        val binding = DialogUnlockVaultBinding.inflate(activity.layoutInflater)

        binding.unlockWatchAdBtn.setOnClickListener {
            binding.unlockWatchAdBtn.isEnabled = false
            binding.unlockWatchAdBtn.text = "A carregar vídeo..."
            
            // Simulate 30s rewarded video ad watching
            Handler(Looper.getMainLooper()).postDelayed({
                dialog?.dismiss()
                activity.toast("Recompensa recebida! Slot de contacto adicionado.")
                onUnlocked()
            }, 3000) // 3 seconds simulated for quick testing
        }

        binding.unlockProBtn.setOnClickListener {
            dialog?.dismiss()
            DialogCheckoutPayment(activity) {
                onUnlocked()
            }.show()
        }

        dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog?.show()
    }
}
