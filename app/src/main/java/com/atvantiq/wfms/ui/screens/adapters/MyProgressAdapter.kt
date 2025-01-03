package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemMyProgressBinding
import com.atvantiq.wfms.databinding.ItemSignInBinding

class MyProgressAdapter(var onItemClick:()->Unit) : RecyclerView.Adapter<MyProgressAdapter.MyProgressViewHolder>() {

    inner class MyProgressViewHolder(var binding:ItemMyProgressBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProgressViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemMyProgressBinding  = DataBindingUtil.inflate(infalter, R.layout.item_my_progress,parent,false)
        return MyProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyProgressViewHolder, position: Int) {
        holder.binding.root.setOnClickListener {
            onItemClick.invoke()
        }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = 4
}