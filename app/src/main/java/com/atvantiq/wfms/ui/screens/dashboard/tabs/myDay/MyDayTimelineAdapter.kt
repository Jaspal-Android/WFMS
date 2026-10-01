package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemMyDayTimelineBinding
import com.atvantiq.wfms.utils.DateUtils
import com.google.android.material.color.MaterialColors

class MyDayTimelineAdapter : RecyclerView.Adapter<MyDayTimelineAdapter.EntryHolder>() {

    private var entries: List<TimelineEntry> = emptyList()

    inner class EntryHolder(val binding: ItemMyDayTimelineBinding) : RecyclerView.ViewHolder(binding.root)

    fun submitList(newEntries: List<TimelineEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryHolder =
        EntryHolder(ItemMyDayTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EntryHolder, position: Int) {
        val entry = entries[position]
        val context = holder.binding.root.context
        val style = TimelineStyle.of(entry.kind)
        with(holder.binding) {
            ivIcon.setImageResource(style.icon)
            ivIcon.backgroundTintList = ColorStateList.valueOf(tintOf(context, style))
            ivIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            tvTitle.setText(style.title)
            tvTime.text = timeOf(context, entry)
            val subtitle = subtitleOf(context, entry)
            tvSubtitle.text = subtitle
            tvSubtitle.visibility = if (subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            val distance = entry.distanceKm?.takeIf { entry.kind == TimelineKind.TRIP }
            tvDistance.text = distance?.let { context.getString(R.string.trip_distance, it) }
            tvDistance.visibility = if (distance == null) View.GONE else View.VISIBLE
            connector.visibility = if (position == entries.lastIndex) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun timeOf(context: Context, entry: TimelineEntry): String {
        val start = DateUtils.formatShortTime(entry.atMillis)
        val end = entry.endMillis ?: return start
        return context.getString(R.string.trip_time, start, DateUtils.formatShortTime(end))
    }

    private fun subtitleOf(context: Context, entry: TimelineEntry): String? =
        if (entry.kind == TimelineKind.TRIP) {
            val from = entry.fromName.orEmpty()
            val to = entry.toName.orEmpty()
            if (from.isBlank() && to.isBlank()) null else context.getString(R.string.trip_route, from, to)
        } else {
            entry.siteName
        }

    companion object {
        fun tintOf(context: Context, style: TimelineStyle): Int =
            if (style.tintAttr != 0) {
                MaterialColors.getColor(context, style.tintAttr, ContextCompat.getColor(context, R.color.black))
            } else {
                ContextCompat.getColor(context, style.tintColor)
            }
    }
}
