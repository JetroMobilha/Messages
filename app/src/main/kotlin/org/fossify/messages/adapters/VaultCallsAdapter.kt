package org.fossify.messages.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.messages.databinding.ItemVaultCallBinding
import org.fossify.messages.models.VaultCall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultCallsAdapter(
    private var calls: List<VaultCall>
) : RecyclerView.Adapter<VaultCallsAdapter.ViewHolder>() {

    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class ViewHolder(val binding: ItemVaultCallBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVaultCallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val call = calls[position]
        holder.binding.apply {
            vaultCallNumber.text = call.phoneNumber
            vaultCallDate.text = dateTimeFormat.format(Date(call.date))
        }
    }

    override fun getItemCount(): Int = calls.size

    fun updateData(newCalls: List<VaultCall>) {
        calls = newCalls
        notifyDataSetChanged()
    }
}
