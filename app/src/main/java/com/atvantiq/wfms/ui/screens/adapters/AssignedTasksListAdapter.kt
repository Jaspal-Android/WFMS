package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemAssignedTasksBinding
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.atvantiq.wfms.widgets.FooterRecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemTypeChipBinding


class AssignedTasksListAdapter(
    var hideButtons:Boolean,
    var onViewAssignedTask: (assignedTask: Site, position: Int) -> Unit,
) : FooterRecyclerView() {

    private var assignedTasks: MutableList<Site>? = mutableListOf()
    private val VIEW_TYPE_ITEM = 1

    inner class AssignedTasksViewHolder(var binding: ItemAssignedTasksBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun count(): Int {
        return assignedTasks?.size ?: 0
    }

    override fun viewType(): Int {
        return VIEW_TYPE_ITEM
    }

    override fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var infalter = LayoutInflater.from(parent.context)
        var binding: ItemAssignedTasksBinding =
            DataBindingUtil.inflate(infalter, R.layout.item_assigned_tasks, parent, false)
        return AssignedTasksViewHolder(binding)
    }

    override fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AssignedTasksViewHolder) {
            holder.binding.hideButtons = hideButtons
            val assignedTask = assignedTasks?.get(position)
            holder.binding.siteItem = assignedTask

           // holder.binding.isOpenAssignment = assignedTask?.status == ValConstants.OPEN

           // holder.binding.isAcceptedAssignment = assignedTask?.status == ValConstants.ACCEPTED

           // holder.binding.isWorkEnded = assignedTask?.status == ValConstants.WIP

           /* getTypesWithActivities(assignedTask?.type)?.let { types ->
                holder.binding.tvTasks.text = types
            }*/

            holder.binding.root.setOnClickListener {
                assignedTask?.let { task ->
                    onViewAssignedTask(task,position)
                }
            }

            /*holder.binding.btnAccept.setOnClickListener {
                assignedTask?.let { task ->
                    onAcceptTask.invoke(assignedTask,position)
                }
            }

            holder.binding.btnStartWork.setOnClickListener {
                assignedTask?.let { task ->
                    onStartWork.invoke(assignedTask, position)
                }
            }

            holder.binding.btnEndWork.setOnClickListener {
                assignedTask?.let { task ->
                    onEndWork.invoke(assignedTask, position)
                }
            }*/

            holder.binding.typeFlow.removeAllViews()

            assignedTask?.type?.forEach { type ->
                val chip = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_type_chip, holder.binding.typeFlow, false) as TextView

                chip.text = type.name
                holder.binding.typeFlow.addView(chip)
            }
            holder.binding.executePendingBindings()
        }
    }

    fun addData(assignedTasks: List<Site>) {
        this.assignedTasks?.addAll(assignedTasks)
        notifyDataSetChanged()
    }

    fun setUpdateStatus(position: Int, status: String) {
        //assignedTasks?.get(position)?.status = status
        notifyItemChanged(position)
    }

    fun submitList(newItems: List<Site>) {
        this.assignedTasks?.clear()
        this.assignedTasks?.addAll(newItems)
        notifyDataSetChanged()
    }

}