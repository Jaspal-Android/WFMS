package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemOptionBinding

class AttendanceOptionsAdapter(var options: List<Pair<String, String>>,var onOptionSelected:(position: Int)->Unit) :
    RecyclerView.Adapter<AttendanceOptionsAdapter.MyTargetViewHolder>() {

    inner class MyTargetViewHolder(var binding:ItemOptionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTargetViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemOptionBinding  = DataBindingUtil.inflate(infalter, R.layout.item_option,parent,false)
        return MyTargetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyTargetViewHolder, position: Int) {
        holder.binding.titleText.text = options[position].first
        holder.binding.root.setOnClickListener {
            onOptionSelected.invoke(position)
        }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = options.size
}