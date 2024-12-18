package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemSignInBinding

class SignInListAdapter() :
    RecyclerView.Adapter<SignInListAdapter.SignInViewHolder>() {

    inner class SignInViewHolder(var binding:ItemSignInBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignInViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemSignInBinding  = DataBindingUtil.inflate(infalter, R.layout.item_sign_in,parent,false)
        return SignInViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignInViewHolder, position: Int) {
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = 4
}