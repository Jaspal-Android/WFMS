package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemWorkTypeBinding

class WorkTypeAdapter(
    private val workTypes: List<String> = listOf("Type 1", "Type 2", "Type 3"),
    private val onSelectionChanged: ((Boolean) -> Unit)? = null
) : RecyclerView.Adapter<WorkTypeAdapter.WorkTypeViewHolder>() {

    private val selectedStates = MutableList(workTypes.size) { false }

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
            tvTypeName.text = workTypes[position]
            cbWorkType.setOnCheckedChangeListener(null)
            cbWorkType.isChecked = selectedStates[position]
            cbWorkType.setOnCheckedChangeListener { _, isChecked ->
                selectedStates[position] = isChecked
                onSelectionChanged?.invoke(selectedStates.all { it })
            }
        }
    }

    fun setAllSelected(selected: Boolean) {
        selectedStates.replaceAll { selected }
        notifyDataSetChanged()
    }

    fun areAllSelected(): Boolean = selectedStates.all { it }
}
