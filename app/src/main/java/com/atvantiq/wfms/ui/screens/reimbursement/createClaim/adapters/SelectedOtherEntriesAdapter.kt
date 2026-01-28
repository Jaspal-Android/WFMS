package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemOtherEntryBinding
import com.atvantiq.wfms.models.reimbursement.OtherExpense

class SelectedOtherEntriesAdapter(
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<SelectedOtherEntriesAdapter.SelectedOtherEntryViewHolder>() {

    private val items = mutableListOf<OtherExpense>()

    fun submitList(list: List<OtherExpense>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedOtherEntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemOtherEntryBinding.inflate(inflater, parent, false)
        return SelectedOtherEntryViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SelectedOtherEntryViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    class SelectedOtherEntryViewHolder(
        private val binding: ItemOtherEntryBinding,
        private val onRemoveClick: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OtherExpense, position: Int) {
            binding.tvCategory.text = item.category
            binding.tvAmount.text = "₹${item.amount}"
            binding.hasAttachments = !item.receiptAttachment.isNullOrEmpty()
            binding.btnRemove.setOnClickListener { onRemoveClick(position) }
        }
    }
}
