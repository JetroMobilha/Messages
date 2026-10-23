package org.fossify.messages.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.databinding.DialogCheckoutPaymentBinding
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DialogCheckoutPayment(
    private val activity: Activity,
    private val onPaymentSuccess: () -> Unit
) {
    private var dialog: AlertDialog? = null
    private val API_BASE_URL = "http://10.0.2.2/messages-api/public/api/v1"

    fun show() {
        val binding = DialogCheckoutPaymentBinding.inflate(activity.layoutInflater)

        binding.checkoutStartBtn.setOnClickListener {
            val username = binding.checkoutUsername.text.toString().trim()
            val phone = binding.checkoutPhone.text.toString().trim()

            if (username.isEmpty()) {
                activity.showErrorToast("O nome de utilizador é obrigatório")
                return@setOnClickListener
            }

            binding.checkoutStartBtn.isEnabled = false
            binding.checkoutStartBtn.text = "A processar..."

            ensureBackgroundThread {
                try {
                    val url = URL("$API_BASE_URL/payment/checkout")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true

                    val jsonBody = Gson().toJson(mapOf(
                        "username" to username,
                        "package_id" to "pro_unlimited",
                        "customer_phone" to phone
                    ))

                    OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonMap = Gson().fromJson(responseText, Map::class.java)
                        val gateway = jsonMap["gateway"] as? String
                        val details = jsonMap["payment_details"] as? Map<*, *>

                        activity.runOnUiThread {
                            binding.checkoutStartBtn.beGoneIf(true)
                            
                            if (gateway == "appy_pay") {
                                // Angola Multicaixa
                                binding.multicaixaDetailsHolder.beVisibleIf(true)
                                binding.mcEntity.text = "Entidade: ${details?.get("reference_entity") ?: "10203"}"
                                binding.mcReference.text = "Referência: ${details?.get("reference_number") ?: "---"}"
                                binding.mcAmount.text = "Valor: 5.000,00 AOA"
                                activity.toast("Referência Multicaixa gerada com sucesso!")
                            } else {
                                // International Paddle
                                binding.internationalDetailsHolder.beVisibleIf(true)
                                binding.intAmount.text = "Valor: $4.99 USD"
                                activity.toast("Acesso Internacional ativado para $username!")
                            }

                            // Trigger unlock in Pro config
                            org.fossify.messages.helpers.MonetizationConfig.isProUnlocked = true
                            onPaymentSuccess()
                        }
                    } else {
                        activity.runOnUiThread {
                            binding.checkoutStartBtn.isEnabled = true
                            binding.checkoutStartBtn.text = "Processar Pagamento Automático"
                            activity.showErrorToast("Falha ao comunicar com o servidor de pagamento")
                        }
                    }
                } catch (e: Exception) {
                    activity.runOnUiThread {
                        binding.checkoutStartBtn.isEnabled = true
                        binding.checkoutStartBtn.text = "Processar Pagamento Automático"
                        activity.showErrorToast("Erro de ligação à rede")
                    }
                }
            }
        }

        dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setNegativeButton("Fechar", null)
            .create()

        dialog?.show()
    }
}
