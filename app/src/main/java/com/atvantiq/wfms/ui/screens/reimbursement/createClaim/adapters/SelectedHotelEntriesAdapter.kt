package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemHotelEntryBinding
import com.atvantiq.wfms.models.reimbursement.HotelExpense

class SelectedHotelEntriesAdapter(
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<SelectedHotelEntriesAdapter.SelectedHotelEntryViewHolder>() {

    private val items = mutableListOf<HotelExpense>()

    fun submitList(list: List<HotelExpense>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedHotelEntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHotelEntryBinding.inflate(inflater, parent, false)
        return SelectedHotelEntryViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SelectedHotelEntryViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    class SelectedHotelEntryViewHolder(
        private val binding: ItemHotelEntryBinding,
        private val onRemoveClick: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HotelExpense, position: Int) {
            binding.tvAmount.text = "₹${item.amount}"
            binding.hasAttachments = !item.receiptAttachments.isNullOrEmpty()
            binding.btnRemove.setOnClickListener { onRemoveClick(position) }
        }
    }
}
