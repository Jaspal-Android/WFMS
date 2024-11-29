package com.atvantiq.wfms.ui.screens.adapters

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemMarqueeBinding
import com.atvantiq.wfms.databinding.ItemMyTargetsBinding

class MyTargetAdapter() :
    RecyclerView.Adapter<MyTargetAdapter.MyTargetViewHolder>() {

    inner class MyTargetViewHolder(var binding:ItemMyTargetsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTargetViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemMyTargetsBinding  = DataBindingUtil.inflate(infalter, R.layout.item_my_targets,parent,false)
        return MyTargetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyTargetViewHolder, position: Int) {
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = 4
}