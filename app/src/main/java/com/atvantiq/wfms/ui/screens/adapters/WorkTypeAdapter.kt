package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.databinding.ItemWorkTypeBinding
import com.atvantiq.wfms.models.work.workDetail.Type

class WorkTypeAdapter(
    private val onSelectionChanged: ((List<Type>) -> Unit)? = null // callback returns selected types
) : RecyclerView.Adapter<WorkTypeAdapter.WorkTypeViewHolder>() {

    private val workTypes: MutableList<Type> = mutableListOf()
    private val selectedStates = mutableListOf<Boolean>()
    private val selectedTypes = mutableSetOf<Type>() // store selected types
    private var hasEligibleToEnd: Boolean = false

    inner class WorkTypeViewHolder(val binding: ItemWorkTypeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkTypeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemWorkTypeBinding.inflate(inflater, parent, false)
        return WorkTypeViewHolder(binding)
    }

    override fun getItemCount() = workTypes.size

    override fun onBindViewHolder(holder: WorkTypeViewHolder, position: Int) {
        with(holder.binding) {
            tvTypeName.text = workTypes[position].name
            typeStatusInteger = workTypes[position].status?.code
            assignedDate = workTypes[position].assignedAt
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
            showSelectableOption = workTypes[position].status?.code == StatusCodes.WIP && hasEligibleToEnd
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

    fun setData(types: List<Type>, hasEligibleToEnd: Boolean) {
        workTypes.clear()
        workTypes.addAll(types)
        selectedStates.clear()
        selectedStates.addAll(MutableList(types.size) { false })
        selectedTypes.clear()
        this.hasEligibleToEnd = hasEligibleToEnd
        notifyDataSetChanged()
        onSelectionChanged?.invoke(selectedTypes.toList())
    }

    fun getSelectedTypes(): List<Type> = selectedTypes.toList()
}
