package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemVendorViewAllBinding

class VendorViewAllListAdapter(var onItemClick:()->Unit) :
    RecyclerView.Adapter<VendorViewAllListAdapter.ApprovalsViewHolder>() {

    inner class ApprovalsViewHolder(var binding:ItemVendorViewAllBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApprovalsViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemVendorViewAllBinding  = DataBindingUtil.inflate(infalter, R.layout.item_vendor_view_all_,parent,false)
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