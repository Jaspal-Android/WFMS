package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ItemWorkTypeAdminBinding
import com.atvantiq.wfms.databinding.ItemWorkTypeBinding
import com.atvantiq.wfms.models.work.workDetail.Type
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkType

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
            assignedDate = workTypes[position].workStartedAt
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

            // Eligibility logic for approve/reject as per role and status
            var eligibleToApprove = false
            val pmStatus = workType?.pm?.status
            val opsStatus = workType?.ops?.status
            val adminStatus = workType?.admin?.status

            when (employeeRole.lowercase()) {
                ValConstants.ROLE_PM.lowercase() -> {
                    if (pmStatus == 0 && adminStatus == 0) {
                        eligibleToApprove = true
                    }
                }
                ValConstants.ROLE_OPS.lowercase() -> {
                    if (opsStatus == 0 && adminStatus == 0) {
                        eligibleToApprove = true
                    }
                }
                ValConstants.ROLE_Admin.lowercase() -> {
                    if (adminStatus == 0) {
                        eligibleToApprove = true
                    }
                }
            }
            showSelectableOption = workTypes[position].status?.code in listOf(StatusCodes.WIP, StatusCodes.COMPLETED) && eligibleToApprove
        }
    }

    fun setAllSelected(selected: Boolean) {
        selectedStates.replaceAll { selected }
        selectedTypes.clear()
        if (selected) {
            selectedTypes.addAll(workTypes)
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
}
