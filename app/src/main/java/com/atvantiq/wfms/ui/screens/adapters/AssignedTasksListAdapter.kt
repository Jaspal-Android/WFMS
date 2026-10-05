package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemAssignedTasksBinding
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/**
 * Work assigned to the employee, with the paging footer. [onViewAssignedTask] gets the task and
 * its position when the tap happens.
 */
class AssignedTasksListAdapter(
    private val hideButtons: Boolean,
    private val onViewAssignedTask: (assignedTask: Site, position: Int) -> Unit,
) : FooterListAdapter<Site>(diffById { it.workSiteId }) {

    class AssignedTasksViewHolder(val binding: ItemAssignedTasksBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssignedTasksViewHolder(ItemAssignedTasksBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: Site) {
        if (holder !is AssignedTasksViewHolder) return
        holder.binding.hideButtons = hideButtons
        holder.binding.siteItem = item
        holder.binding.root.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) onViewAssignedTask(item, position)
        }

        holder.binding.typeFlow.removeAllViews()
        item.type.forEach { type ->
            val chip = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_type_chip, holder.binding.typeFlow, false) as TextView
            chip.text = type.name
            holder.binding.typeFlow.addView(chip)
        }
        holder.binding.executePendingBindings()
    }

    /** A status changed on a detail screen: updated in place and redrawn. */
    fun setUpdateStatus(position: Int, status: Int) {
        if (position !in 0 until super.getItemCount()) return
        getItem(position).status.code = status
        notifyItemChanged(position)
    }
}
