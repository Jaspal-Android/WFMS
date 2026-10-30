package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemWorkTypeAdminBinding
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkType
import com.atvantiq.wfms.models.workSites.workSiteDetails.isApprovableBy
import com.atvantiq.wfms.utils.DateUtils

/**
 * Work types on Site Work Detail. Only types [role] can act on can be ticked; the others are
 * dimmed. [onSelectionChanged] gets the ticked types after every change.
 */
class WorkTypeAdapterAdmin(
    private val role: String,
    private val onSelectionChanged: (List<WorkType>) -> Unit
) : RecyclerView.Adapter<WorkTypeAdapterAdmin.WorkTypeAdminViewHolder>() {

    private val workTypes = mutableListOf<WorkType>()
    private val selectedTypes = linkedSetOf<WorkType>()

    inner class WorkTypeAdminViewHolder(val binding: ItemWorkTypeAdminBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkTypeAdminViewHolder =
        WorkTypeAdminViewHolder(ItemWorkTypeAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = workTypes.size

    override fun onBindViewHolder(holder: WorkTypeAdminViewHolder, position: Int) {
        val workType = workTypes[position]
        val eligible = workType.isApprovableBy(role)
        val resources = holder.itemView.resources
        with(holder.binding) {
            item = workType
            startedLabel = DateUtils.formatYmdLabel(workType.workStartedAt?.take(YMD_LENGTH))
            card.alpha = ResourcesCompat.getFloat(resources, if (eligible) R.dimen.alpha_enabled else R.dimen.alpha_ineligible)
            cbWorkType.setOnCheckedChangeListener(null)
            cbWorkType.isChecked = workType in selectedTypes
            cbWorkType.isEnabled = eligible
            cbWorkType.setOnCheckedChangeListener { _, isChecked -> setSelected(workType, isChecked) }
            card.setOnClickListener { if (eligible) cbWorkType.toggle() }
            executePendingBindings()
        }
    }

    private fun setSelected(type: WorkType, selected: Boolean) {
        if (selected) selectedTypes.add(type) else selectedTypes.remove(type)
        onSelectionChanged(selectedTypes.toList())
    }

    /** Selects every eligible type, or clears the selection when they are all selected already. */
    fun toggleSelectAll() {
        val eligible = workTypes.filter { it.isApprovableBy(role) }
        val allSelected = eligible.isNotEmpty() && selectedTypes.containsAll(eligible)
        selectedTypes.clear()
        if (!allSelected) selectedTypes.addAll(eligible)
        notifyDataSetChanged()
        onSelectionChanged(selectedTypes.toList())
    }

    fun setData(types: List<WorkType>) {
        workTypes.clear()
        workTypes.addAll(types)
        selectedTypes.clear()
        notifyDataSetChanged()
        onSelectionChanged(emptyList())
    }

    fun getSelectedTypes(): List<WorkType> = selectedTypes.toList()

    private companion object {
        const val YMD_LENGTH = 10
    }
}
