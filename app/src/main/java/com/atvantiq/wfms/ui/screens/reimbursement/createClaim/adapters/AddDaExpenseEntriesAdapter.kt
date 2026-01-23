package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemDaEntryBinding
import com.atvantiq.wfms.models.reimbursement.DAExpense

class AddDaExpenseEntriesAdapter(
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<AddDaExpenseEntriesAdapter.AddDaExpenseEntryViewHolder>() {

    private val items = mutableListOf<DAExpense>()

    fun submitList(list: List<DAExpense>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddDaExpenseEntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDaEntryBinding.inflate(inflater, parent, false)
        return AddDaExpenseEntryViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: AddDaExpenseEntryViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    class AddDaExpenseEntryViewHolder(
        private val binding: ItemDaEntryBinding,
        private val onRemoveClick: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DAExpense, position: Int) {
            binding.tvAmount.text = item.amount
            binding.hasAttachments = !item.receiptAttachment.isNullOrEmpty()
            binding.btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
