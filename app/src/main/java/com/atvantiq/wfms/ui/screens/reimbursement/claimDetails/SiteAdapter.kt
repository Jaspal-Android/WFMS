package com.atvantiq.wfms.ui.screens.reimbursement.claimDetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemSiteBinding
import com.atvantiq.wfms.models.reimbursement.detail.Site

class SiteAdapter : ListAdapter<Site, SiteAdapter.SiteAdapterViewHolder>(DIFF) {

    private val expanded = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteAdapterViewHolder {
        val binding = ItemSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SiteAdapterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiteAdapterViewHolder, position: Int) {
        holder.bind(
            site = getItem(position),
            position = position,
            isExpanded = expanded.contains(position),
            onHeaderClick = {
                if (expanded.contains(position)) expanded.remove(position) else expanded.add(position)
                notifyItemChanged(position)
            }
        )
    }

    class SiteAdapterViewHolder(private val binding: ItemSiteBinding) : RecyclerView.ViewHolder(binding.root) {
        private val expenseAdapter = ExpenseAdapter()
        init {
            binding.recyclerViewExpenses.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = expenseAdapter
                setHasFixedSize(false)
            }
        }
        fun bind(
            site: Site,
            position: Int,
            isExpanded: Boolean,
            onHeaderClick: () -> Unit
        ) {
            binding.site = site
            binding.position = position
            binding.isExpanded = isExpanded
            binding.siteHeader.setOnClickListener { onHeaderClick() }
            expenseAdapter.submitList(site.expenses)
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Site>() {
            override fun areItemsTheSame(oldItem: Site, newItem: Site): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: Site, newItem: Site): Boolean = oldItem == newItem
        }
    }
}
