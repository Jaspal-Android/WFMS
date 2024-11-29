package com.atvantiq.wfms.ui.screens.adapters

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemMarqueeBinding

class MarqueeAdapter(private val items: List<String>) :
    RecyclerView.Adapter<MarqueeAdapter.MarqueeViewHolder>() {

    inner class MarqueeViewHolder(var binding:ItemMarqueeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarqueeViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemMarqueeBinding  = DataBindingUtil.inflate(infalter, R.layout.item_marquee,parent,false)
        return MarqueeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarqueeViewHolder, position: Int) {
        holder.binding.marqueText.text = items[position]
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = items.size
}