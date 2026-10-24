package org.fossify.messages.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.messages.databinding.ItemVaultKeywordBinding
import org.fossify.messages.models.VaultKeyword

class VaultKeywordsAdapter(
    private var keywords: List<VaultKeyword>,
    private val onItemLongClick: (VaultKeyword) -> Unit
) : RecyclerView.Adapter<VaultKeywordsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemVaultKeywordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVaultKeywordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kw = keywords[position]
        holder.binding.apply {
            vaultKeywordText.text = "\"${kw.keyword}\""
            vaultKeywordPolicy.text = when (kw.interactionPolicy) {
                1 -> "Discreto"
                2 -> "Autodestruição"
                else -> "Furtivo"
            }

            root.setOnLongClickListener {
                onItemLongClick(kw)
                true
            }
        }
    }

    override fun getItemCount(): Int = keywords.size

    fun updateData(newKeywords: List<VaultKeyword>) {
        keywords = newKeywords
        notifyDataSetChanged()
    }
}
