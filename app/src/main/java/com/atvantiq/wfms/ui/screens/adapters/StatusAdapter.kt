package com.atvantiq.wfms.ui.screens.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemMyTargetsBinding
import com.atvantiq.wfms.databinding.ItemStatusBinding
import com.atvantiq.wfms.models.StatusOption

class StatusAdapter(
    private val items: List<StatusOption>,
    private val onItemClick: (StatusOption) -> Unit
) : RecyclerView.Adapter<StatusAdapter.StatusViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class StatusViewHolder(var binding: ItemStatusBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        var infalter = LayoutInflater.from(parent.context)
        var binding: ItemStatusBinding =
            DataBindingUtil.inflate(infalter, R.layout.item_status, parent, false)
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val item = items[position]
        val isSelected = selectedPosition == position
        val statusName = holder.binding.statusName

        statusName.text = item.name
        if (isSelected) {
            statusName.setBackgroundResource(R.drawable.bg_selected_status)
            statusName.setTextColor(Color.WHITE)
        } else {
            statusName.setBackgroundColor(Color.TRANSPARENT)
            statusName.setTextColor(Color.BLACK)
        }

        holder.binding.root.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
