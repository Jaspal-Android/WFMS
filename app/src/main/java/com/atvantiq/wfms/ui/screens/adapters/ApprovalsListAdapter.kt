package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemApprovalsBinding

class ApprovalsListAdapter() :
    RecyclerView.Adapter<ApprovalsListAdapter.ApprovalsViewHolder>() {

    inner class ApprovalsViewHolder(var binding:ItemApprovalsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalsViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemApprovalsBinding  = DataBindingUtil.inflate(infalter, R.layout.item_approvals,parent,false)
        return ApprovalsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApprovalsViewHolder, position: Int) {
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = 6
}