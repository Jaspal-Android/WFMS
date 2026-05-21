package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemTravelEntryBinding
import com.atvantiq.wfms.models.reimbursement.TravelExpense

class SelectedTravelingEntriesAdapter(
    private val onRemoveClick: (position:Int) -> Unit
) : RecyclerView.Adapter<SelectedTravelingEntriesAdapter.SelectedTravelingEntryViewHolder>() {

    private val items = mutableListOf<TravelExpense>()

    fun submitList(list: List<TravelExpense>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedTravelingEntryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTravelEntryBinding.inflate(inflater, parent, false)
        return SelectedTravelingEntryViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: SelectedTravelingEntryViewHolder, position: Int) {
        holder.bind(items[position],position)
    }

    override fun getItemCount(): Int = items.size

    class SelectedTravelingEntryViewHolder(
        private val binding: ItemTravelEntryBinding,
        private val onRemoveClick: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TravelExpense, position: Int){
            binding.tvEntryNumber.text = "Entry ${position + 1}"
            binding.tvMode.text = item.mode?.label?: "-"
            binding.tvRoute .text = "${item.from}  →  ${item.to}"
            binding.tvAmount.text = item.amount
            binding.isTravelingWith= !item.travelingWith.isNullOrEmpty()
            binding.tvWith.text = item.travelingWith?.joinToString(", ") { it.name.toString() } ?: "-"
            binding.hasAttachments = !item.receiptAttachments.isNullOrEmpty()
            binding.btnRemove.setOnClickListener {
                onRemoveClick(position)
            }
        }
    }
}
