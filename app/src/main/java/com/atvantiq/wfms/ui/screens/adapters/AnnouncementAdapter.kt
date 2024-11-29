package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemAnnouncementsBinding
import com.atvantiq.wfms.databinding.ItemMarqueeBinding

class AnnouncementAdapter(private val items: List<String>) :
    RecyclerView.Adapter<AnnouncementAdapter.MarqueeViewHolder>() {

    inner class MarqueeViewHolder(var binding:ItemAnnouncementsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarqueeViewHolder {
        var infalter =LayoutInflater.from(parent.context)
        var binding:ItemAnnouncementsBinding  = DataBindingUtil.inflate(infalter, R.layout.item_announcements,parent,false)
        return MarqueeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarqueeViewHolder, position: Int) {
        holder.binding.announcementText.text = items[position]
        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = items.size
}