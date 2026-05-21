package com.atvantiq.wfms.ui.screens.reimbursement.claimDetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemExpenseBinding
import com.atvantiq.wfms.models.reimbursement.detail.Expense

class ExpenseAdapter : ListAdapter<Expense, ExpenseAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Expense) {
            binding.expense = item
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean = oldItem == newItem
        }
    }
}
