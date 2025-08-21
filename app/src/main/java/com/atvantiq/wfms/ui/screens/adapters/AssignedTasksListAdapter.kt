package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ItemAssignedTasksBinding
import com.atvantiq.wfms.models.work.assignedAll.Type
import com.atvantiq.wfms.models.work.assignedAll.WorkRecord
import com.atvantiq.wfms.widgets.FooterRecyclerView

class AssignedTasksListAdapter(
    var hideButtons:Boolean,
    var onViewAssignedTask: (assignedTask: WorkRecord, position: Int) -> Unit,
    var onAcceptTask: (assignedTask: WorkRecord, position: Int) -> Unit,
    var onStartWork: (assignedTask: WorkRecord, position: Int) -> Unit,
    var onEndWork: (assignedTask: WorkRecord, position: Int) -> Unit
) : FooterRecyclerView() {

    private var assignedTasks: MutableList<WorkRecord>? = mutableListOf()
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
            holder.binding.itemWorkRecord = assignedTask

            holder.binding.isOpenAssignment = assignedTask?.status == ValConstants.OPEN

            holder.binding.isAcceptedAssignment = assignedTask?.status == ValConstants.ACCEPTED

            holder.binding.isWorkEnded = assignedTask?.status == ValConstants.WIP

            getTypesWithActivities(assignedTask?.type)?.let { types ->
                holder.binding.tvTasks.text = types
            }

            holder.binding.root.setOnClickListener {
                assignedTask?.let { task ->
                    onViewAssignedTask(task,position)
                }
            }

            holder.binding.btnAccept.setOnClickListener {
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
            }

            holder.binding.executePendingBindings()
        }
    }

    private fun getTypesWithActivities(type: List<Type>?): String {
        if (type != null) {
            return type.joinToString("\n") { t ->
                val activities = t.activity.joinToString { it.name }
                "${t.name} (${activities})"
            }
        } else {
            return ""
        }
    }

    fun addData(assignedTasks: List<WorkRecord>) {
        this.assignedTasks?.addAll(assignedTasks)
        notifyDataSetChanged()
    }

    fun setUpdateStatus(position: Int, status: String) {
        assignedTasks?.get(position)?.status = status
        notifyItemChanged(position)
    }

    fun submitList(newItems: List<WorkRecord>) {
        this.assignedTasks?.clear()
        this.assignedTasks?.addAll(newItems)
        notifyDataSetChanged()
    }

}