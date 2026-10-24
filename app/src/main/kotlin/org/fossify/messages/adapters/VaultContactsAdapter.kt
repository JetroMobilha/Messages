package org.fossify.messages.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.messages.databinding.ItemVaultContactBinding
import org.fossify.messages.models.VaultContact

class VaultContactsAdapter(
    private var contacts: List<VaultContact>,
    private val onItemClick: (VaultContact) -> Unit,
    private val onItemLongClick: (VaultContact) -> Unit
) : RecyclerView.Adapter<VaultContactsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemVaultContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVaultContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.apply {
            vaultContactName.text = contact.name.ifEmpty { contact.phoneNumber }
            vaultContactNumber.text = contact.phoneNumber
            vaultContactPolicy.text = when (contact.interactionPolicy) {
                1 -> "Discreto"
                2 -> "Autodestruição"
                else -> "Furtivo"
            }
            vaultContactGroup.text = contact.groupName

            if (contact.blockCalls) {
                vaultContactBlockIcon.visibility = android.view.View.VISIBLE
            } else {
                vaultContactBlockIcon.visibility = android.view.View.GONE
            }

            vaultContactDialerBtn.setOnClickListener {
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${contact.phoneNumber}"))
                it.context.startActivity(intent)
            }

            root.setOnClickListener { onItemClick(contact) }
            root.setOnLongClickListener {
                onItemLongClick(contact)
                true
            }
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateData(newContacts: List<VaultContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
