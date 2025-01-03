package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemApprovalsBinding
import com.atvantiq.wfms.databinding.ItemMyClaimsBinding

class MyClaimsListAdapter(var onItemClick:()->Unit) : RecyclerView.Adapter<MyClaimsListAdapter.ApprovalsViewHolder>() {

    inner class ApprovalsViewHolder(var binding:ItemMyClaimsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalsViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemMyClaimsBinding  = DataBindingUtil.inflate(infalter, R.layout.item_my_claims,parent,false)
        return ApprovalsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApprovalsViewHolder, position: Int) {
        holder.binding.root.setOnClickListener {
            onItemClick.invoke()
        }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = 6
}