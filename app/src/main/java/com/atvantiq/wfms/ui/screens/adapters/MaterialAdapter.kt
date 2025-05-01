package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemMaterialBinding
import com.atvantiq.wfms.models.material.MaterialRecord

class MaterialAdapter(private val materials: List<MaterialRecord>) :
    RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val binding = ItemMaterialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MaterialViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        holder.bind(materials[position])
    }

    override fun getItemCount(): Int = materials.size

    class MaterialViewHolder(private val binding: ItemMaterialBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(record: MaterialRecord) {
            with(binding) {
                tvDate.text = record.date
                tvSite.text = record.site
                tvMaterial.text = record.material
                tvUnit.text = record.unit
                tvTotal.text = "${record.total} ${record.unit}"
                tvConsumed.text = "${record.consumed} ${record.unit}"
                tvPending.text = "${record.total - record.consumed} ${record.unit}"
                progressBar.progress = ((record.consumed / record.total) * 100).toInt()
            }
        }
    }
}