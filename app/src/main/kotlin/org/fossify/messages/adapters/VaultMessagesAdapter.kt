package org.fossify.messages.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemVaultMessageBinding
import org.fossify.messages.models.VaultMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultMessagesAdapter(
    private var messages: List<VaultMessage>
) : RecyclerView.Adapter<VaultMessagesAdapter.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class ViewHolder(val binding: ItemVaultMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVaultMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val msg = messages[position]
        holder.binding.apply {
            vaultMessageBody.text = msg.body
            vaultMessageTime.text = timeFormat.format(Date(msg.date))

            val isSent = msg.type == android.provider.Telephony.Sms.MESSAGE_TYPE_SENT || msg.type == android.provider.Telephony.Sms.MESSAGE_TYPE_OUTBOX
            val params = vaultMessageBubble.layoutParams as LinearLayout.LayoutParams

            if (isSent) {
                params.gravity = Gravity.END
                vaultMessageBubble.setBackgroundResource(R.drawable.item_sent_background)
            } else {
                params.gravity = Gravity.START
                vaultMessageBubble.setBackgroundResource(R.drawable.item_received_background)
            }
            vaultMessageBubble.layoutParams = params
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateData(newMessages: List<VaultMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}
