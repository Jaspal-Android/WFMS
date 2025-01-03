package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemApprovalsBinding
import com.atvantiq.wfms.databinding.ItemClaimsApprovalsBinding
import com.atvantiq.wfms.databinding.ItemMyClaimsBinding

class ClaimApprovalsListAdapter(var onItemClick:()->Unit) : RecyclerView.Adapter<ClaimApprovalsListAdapter.ApprovalsViewHolder>() {

    inner class ApprovalsViewHolder(var binding:ItemClaimsApprovalsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalsViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemClaimsApprovalsBinding  = DataBindingUtil.inflate(infalter, R.layout.item_claims_approvals,parent,false)
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