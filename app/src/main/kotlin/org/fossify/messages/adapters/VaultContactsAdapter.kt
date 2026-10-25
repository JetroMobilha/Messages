package org.fossify.messages.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.databinding.ItemVaultContactBinding
import org.fossify.messages.models.VaultContact
import org.fossify.messages.models.VaultContactWithSnippet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VaultContactsAdapter(
    private val activity: SimpleActivity,
    private var contacts: List<VaultContactWithSnippet>,
    private val onItemClick: (VaultContact) -> Unit,
    private val onItemLongClick: (VaultContact) -> Unit
) : RecyclerView.Adapter<VaultContactsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemVaultContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVaultContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = contacts[position]
        val contact = item.contact
        holder.binding.apply {
            vaultContactName.text = contact.name.ifEmpty { contact.phoneNumber }
            vaultContactSnippet.text = item.snippet ?: contact.groupName
            
            if (item.date != null) {
                vaultContactDate.text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(item.date))
            } else {
                vaultContactDate.text = ""
            }

            vaultContactUnreadCount.text = item.unreadCount.toString()
            vaultContactUnreadCount.beVisibleIf(item.unreadCount > 0)
            vaultContactBlockIcon.beVisibleIf(contact.blockCalls)

            val photoUri = SimpleContactsHelper(activity).getPhotoUriFromPhoneNumber(contact.phoneNumber)
            SimpleContactsHelper(activity).loadContactImage(photoUri, vaultContactImage, contact.name)

            root.setOnClickListener { onItemClick(contact) }
            root.setOnLongClickListener {
                onItemLongClick(contact)
                true
            }
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateData(newContacts: List<VaultContactWithSnippet>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
