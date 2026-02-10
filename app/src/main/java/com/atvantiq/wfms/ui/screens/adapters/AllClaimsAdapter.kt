package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.databinding.ItemAssignedTasksBinding
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.atvantiq.wfms.widgets.FooterRecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ReimbursementData
import com.atvantiq.wfms.databinding.ItemClaimsBinding
import com.atvantiq.wfms.databinding.ItemTypeChipBinding
import com.atvantiq.wfms.models.reimbursement.allClaims.Record


class AllClaimsAdapter(
    var onClaimClicked: (claim:Record, position: Int) -> Unit,
) : FooterRecyclerView() {

    private var allClaims: MutableList<Record>? = mutableListOf()
    private val VIEW_TYPE_ITEM = 1

    inner class AllClaimsViewHolder(var binding: ItemClaimsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun count(): Int {
        return allClaims?.size ?: 0
    }

    override fun viewType(): Int {
        return VIEW_TYPE_ITEM
    }

    override fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var infalter = LayoutInflater.from(parent.context)
        var binding: ItemClaimsBinding =
            DataBindingUtil.inflate(infalter, R.layout.item_claims, parent, false)
        return AllClaimsViewHolder(binding)
    }

    override fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AllClaimsViewHolder) {
            val claim = allClaims?.get(position)
            holder.binding.claimItem = claim

            if (claim?.expenseCategory.equals(ReimbursementData.CLAIM_TYPE_LOCAL,ignoreCase = true)){
                holder.binding.ivCategoryIcon.text = "📍"
            }else{
                holder.binding.ivCategoryIcon.text = "✈️"
            }

            if (claim?.type.equals(ReimbursementData.CLAIM_SINGLE_SITE,ignoreCase = true)) {
                holder.binding.tvTypeValue.text = holder.binding.root.context.getString(R.string.singleSite)
            }else{
                holder.binding.tvTypeValue.text = holder.binding.root.context.getString(R.string.multiSite)
            }
            holder.binding.root.setOnClickListener {
                claim?.let { claim ->
                    onClaimClicked(claim,position)
                }
            }
            holder.binding.executePendingBindings()
        }
    }

    fun addData(claims: List<Record>) {
        this.allClaims?.addAll(claims)
        notifyDataSetChanged()
    }


    fun submitList(newItems: List<Record>) {
        this.allClaims?.clear()
        this.allClaims?.addAll(newItems)
        notifyDataSetChanged()
    }

}