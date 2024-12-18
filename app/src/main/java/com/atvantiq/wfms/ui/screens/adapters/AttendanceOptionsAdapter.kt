package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemAttendanceOptionBinding

class AttendanceOptionsAdapter(var options: List<Pair<String, String>>,var onOptionSelected:(position: Int)->Unit) :
    RecyclerView.Adapter<AttendanceOptionsAdapter.MyTargetViewHolder>() {

    inner class MyTargetViewHolder(var binding:ItemAttendanceOptionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTargetViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemAttendanceOptionBinding  = DataBindingUtil.inflate(infalter, R.layout.item_attendance_option,parent,false)
        return MyTargetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyTargetViewHolder, position: Int) {
        holder.binding.titleText.text = options[position].first
        holder.binding.subtitleText.text = options[position].second
        holder.binding.root.setOnClickListener {
            onOptionSelected.invoke(position)
        }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = options.size
}