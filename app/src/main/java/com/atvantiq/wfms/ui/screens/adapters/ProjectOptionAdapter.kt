package com.atvantiq.wfms.ui.screens.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemProjectOptionBinding
import com.atvantiq.wfms.ui.screens.projectPicker.ProjectOption
import com.atvantiq.wfms.widgets.diffById
import com.google.android.material.color.MaterialColors

/** The project picker's list; one project can be chosen. */
class ProjectOptionAdapter(
    private val onSelect: (Long) -> Unit
) : ListAdapter<ProjectOptionAdapter.Row, ProjectOptionAdapter.Holder>(diffById { it.project.id }) {

    data class Row(val project: ProjectOption, val isSelected: Boolean)

    class Holder(val binding: ItemProjectOptionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemProjectOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (project, isSelected) = getItem(position)
        with(holder.binding) {
            val context = root.context
            val accent = MaterialColors.getColor(row, R.attr.wfmsColorAccent)
            val outline = MaterialColors.getColor(row, R.attr.wfmsColorOutline)
            val muted = MaterialColors.getColor(row, R.attr.wfmsColorOnSurfaceVariant)
            name.text = project.name
            circle.text = project.circleName
            circle.isVisible = !project.circleName.isNullOrBlank()
            radio.setImageResource(if (isSelected) R.drawable.ic_check_circle else R.drawable.ic_radio_unchecked)
            ImageViewCompat.setImageTintList(radio, ColorStateList.valueOf(if (isSelected) accent else muted))
            row.strokeColor = if (isSelected) accent else outline
            // TalkBack reads the row as one item and says "selected" for the chosen one.
            row.isSelected = isSelected
            row.contentDescription = project.circleName?.takeIf { it.isNotBlank() }
                ?.let { context.getString(R.string.project_option_with_circle, project.name, it) }
                ?: project.name
            row.setOnClickListener { onSelect(project.id) }
        }
    }

    fun submit(projects: List<ProjectOption>, selectedId: Long?) =
        submitList(projects.map { Row(it, it.id == selectedId) })
}
