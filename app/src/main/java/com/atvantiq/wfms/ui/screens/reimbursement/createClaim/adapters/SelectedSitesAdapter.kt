package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemSelectedSiteBinding
import com.atvantiq.wfms.models.reimbursement.MultipleSite

class SelectedSitesAdapter(
    private val onRemoveClick: (MultipleSite) -> Unit
) : RecyclerView.Adapter<SelectedSitesAdapter.SelectedSiteViewHolder>() {

    private val items = mutableListOf<MultipleSite>()

    fun submitList(list: List<MultipleSite>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedSiteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSelectedSiteBinding.inflate(inflater, parent, false)
        return SelectedSiteViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SelectedSiteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SelectedSiteViewHolder(
        private val binding: ItemSelectedSiteBinding,
        private val onRemoveClick: (MultipleSite) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MultipleSite){
            binding.btnRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }
}
