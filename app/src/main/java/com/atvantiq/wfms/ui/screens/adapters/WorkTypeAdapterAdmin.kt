package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ItemWorkTypeAdminBinding
import com.atvantiq.wfms.databinding.ItemWorkTypeBinding
import com.atvantiq.wfms.models.work.workDetail.Type
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkType
import com.atvantiq.wfms.utils.DateUtils

class WorkTypeAdapterAdmin(
    private var employeeRole:String,
    private val onSelectionChanged: ((List<WorkType>) -> Unit)? = null // callback returns selected types
) : RecyclerView.Adapter<WorkTypeAdapterAdmin.WorkTypeAdminViewHolder>() {

    private val workTypes: MutableList<WorkType> = mutableListOf()
    private val selectedStates = mutableListOf<Boolean>()
    private val selectedTypes = mutableSetOf<WorkType>() // store selected types

    inner class WorkTypeAdminViewHolder(val binding: ItemWorkTypeAdminBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkTypeAdminViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemWorkTypeAdminBinding.inflate(inflater, parent, false)
        return WorkTypeAdminViewHolder(binding)
    }

    override fun getItemCount() = workTypes.size

    override fun onBindViewHolder(holder: WorkTypeAdminViewHolder, position: Int) {
        with(holder.binding) {
            var workType = workTypes[position]
            tvTypeName.text = workTypes[position].name
            typeStatusInteger = workTypes[position].status?.code
            assignedDate = tvTypeId.context.getString(R.string.started)+": "+ DateUtils.formatApiDateToMonthDayYear(workTypes[position].workStartedAt)
            pmStatus = workTypes[position].pm?.status ?: -1
            opsStatus = workTypes[position].ops?.status ?: -1
            adminStatus = workTypes[position].admin?.status ?: -1

            cbWorkType.setOnCheckedChangeListener(null)
            cbWorkType.isChecked = selectedStates[position]
            cbWorkType.setOnCheckedChangeListener { _, isChecked ->
                selectedStates[position] = isChecked
                val type = workTypes[position]
                if (isChecked) {
                    selectedTypes.add(type)
                } else {
                    selectedTypes.remove(type)
                }
                onSelectionChanged?.invoke(selectedTypes.toList())
            }

            showSelectableOption = isSelectable(employeeRole, workType)
        }
    }

    // "Select all" must only pick rows the admin could tick individually. Rows whose checkbox is
    // hidden (not in progress / completed, or already actioned by this role) are skipped;
    // otherwise a bulk approve or reject is sent for types the admin cannot act on.
    fun setAllSelected(selected: Boolean) {
        selectedTypes.clear()
        val selectable = selectableTypes(employeeRole, workTypes)
        workTypes.forEachIndexed { index, type ->
            val pick = selected && type in selectable
            selectedStates[index] = pick
            if (pick) selectedTypes.add(type)
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedTypes.toList())
    }

    fun areAllSelected(): Boolean = selectedStates.all { it }

    fun setData(types: List<WorkType>, hasEligibleToEnd: Boolean) {
        workTypes.clear()
        workTypes.addAll(types)
        selectedStates.clear()
        selectedStates.addAll(MutableList(types.size) { false })
        selectedTypes.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedTypes.toList())
    }

    fun getSelectedTypes(): List<WorkType> = selectedTypes.toList()

    companion object {
        /** The subset of [workTypes] that "Select all" may pick for [employeeRole]. */
        fun selectableTypes(employeeRole: String, workTypes: List<WorkType>): List<WorkType> =
            workTypes.filter { isSelectable(employeeRole, it) }

        /**
         * A work type can be approved or rejected by [employeeRole] when it is in progress or
         * completed and that role (and the admin) has not actioned it yet.
         */
        fun isSelectable(employeeRole: String, workType: WorkType): Boolean {
            val eligibleToApprove = when (employeeRole.lowercase()) {
                ValConstants.ROLE_PM.lowercase() ->
                    workType.pm?.status == 0 && workType.admin?.status == 0
                ValConstants.ROLE_OPS.lowercase() ->
                    workType.ops?.status == 0 && workType.admin?.status == 0
                ValConstants.ROLE_Admin.lowercase() -> workType.admin?.status == 0
                else -> false
            }
            return eligibleToApprove &&
                workType.status?.code in listOf(StatusCodes.WIP, StatusCodes.COMPLETED)
        }
    }
}
