package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ReimbursementData
import com.atvantiq.wfms.databinding.ItemClaimsBinding
import com.atvantiq.wfms.models.reimbursement.allClaims.Record
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/**
 * The employee's own claims, with the paging footer. [onClaimClicked] gets the claim and its
 * position when the tap happens.
 */
class AllClaimsAdapter(
    private val onClaimClicked: (claim: Record, position: Int) -> Unit,
) : FooterListAdapter<Record>(diffById { it.claimId }) {

    class AllClaimsViewHolder(val binding: ItemClaimsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AllClaimsViewHolder(ItemClaimsBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: Record) {
        if (holder !is AllClaimsViewHolder) return
        val context = holder.binding.root.context
        holder.binding.claimItem = item

        holder.binding.chipExpenseType.text = context.getString(
            if (item.expenseCategory.equals(ReimbursementData.CLAIM_TYPE_LOCAL, ignoreCase = true)) R.string.local
            else R.string.outstation
        )
        holder.binding.tvTypeValue.text = context.getString(
            if (item.type.equals(ReimbursementData.CLAIM_SINGLE_SITE, ignoreCase = true)) R.string.singleSite
            else R.string.multiSite
        )
        holder.binding.root.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) onClaimClicked(item, position)
        }
        holder.binding.executePendingBindings()
    }
}
