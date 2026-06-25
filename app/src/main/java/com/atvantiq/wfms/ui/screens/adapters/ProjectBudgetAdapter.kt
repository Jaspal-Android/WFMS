package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemProjectBudgetBinding
import com.atvantiq.wfms.models.targets.ProjectBudgetItem

class ProjectBudgetAdapter : RecyclerView.Adapter<ProjectBudgetAdapter.ViewHolder>() {

    private val items = mutableListOf<ProjectBudgetItem>()

    fun submitList(data: List<ProjectBudgetItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemProjectBudgetBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemProjectBudgetBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_project_budget,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        val res = ctx.resources

        holder.binding.tvProjectName.text = item.project?.name ?: "-"

        // "Client · Circle"
        val clientPart = item.client?.name.orEmpty()
        val circlePart = item.circle?.name.orEmpty()
        holder.binding.tvClientLocation.text = when {
            clientPart.isNotBlank() && circlePart.isNotBlank() -> "$clientPart · $circlePart"
            clientPart.isNotBlank() -> clientPart
            circlePart.isNotBlank() -> circlePart
            else -> "-"
        }

        // Progress from revenue achievement_percentage string e.g. "0.0%"
        val revPct = stripPct(item.revenue?.achievementPercentage).toInt().coerceIn(0, 100)
        holder.binding.progressProject.progress = revPct

        // Sites: "achieved / target"
        val sitesAchieved = item.sites?.achieved ?: 0
        val sitesTarget = item.sites?.target ?: 0
        holder.binding.tvSitesRatio.text = "$sitesAchieved/$sitesTarget"

        // Revenue: "$0 of $108K"
        val revAchieved = item.revenue?.achieved ?: 0.0
        val revTarget = item.revenue?.target ?: 0.0
        holder.binding.tvRevenue.text = "${formatCurrency(revAchieved)} of ${formatCurrency(revTarget)}"

        // Status badge
        val status = item.status?.lowercase() ?: ""
        val (bgRes, textColorRes, label) = when {
            status == "on_track" -> Triple(
                R.drawable.bg_status_on_track,
                R.color.status_present_text,
                ctx.getString(R.string.on_track)
            )
            status == "above_target" -> Triple(
                R.drawable.bg_status_on_track,
                R.color.status_present_text,
                ctx.getString(R.string.status_above_target)
            )
            else -> Triple(
                R.drawable.bg_status_under_target,
                R.color.status_holiday_text,
                ctx.getString(R.string.status_under_target)
            )
        }
        holder.binding.tvStatus.apply {
            text = label
            setBackgroundResource(bgRes)
            setTextColor(res.getColor(textColorRes, ctx.theme))
        }

        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = items.size

    private fun stripPct(value: String?): Double =
        value?.trimEnd('%')?.toDoubleOrNull() ?: 0.0

    private fun formatCurrency(amount: Double): String {
        val abs = Math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_000_000 -> "${sign}$${String.format("%.1f", abs / 1_000_000)}M"
            abs >= 1_000 -> "${sign}$${String.format("%.0f", abs / 1_000)}K"
            else -> "${sign}$${String.format("%.0f", abs)}"
        }
    }
}
