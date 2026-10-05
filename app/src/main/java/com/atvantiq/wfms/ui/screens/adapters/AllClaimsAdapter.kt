package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ReimbursementData
import com.atvantiq.wfms.databinding.ItemClaimsBinding
import com.atvantiq.wfms.models.reimbursement.allClaims.Record
import com.atvantiq.wfms.utils.DateUtils
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
        val category = context.getString(
            if (item.expenseCategory.equals(ReimbursementData.CLAIM_TYPE_LOCAL, ignoreCase = true)) R.string.local
            else R.string.outstation
        )
        val date = DateUtils.formatApiDateToMonthDayYear(item.createdAt)
        with(holder.binding) {
            claimItem = item
            meta = date?.let { context.getString(R.string.claim_meta_format, it, category) } ?: category
            approvedLabel = context.getString(
                R.string.claim_approved_format,
                context.getString(R.string.rupee_format, item.latestApprovedAmount ?: 0.0)
            )
            root.setOnClickListener {
                val position = holder.adapterPosition
                if (position != RecyclerView.NO_POSITION) onClaimClicked(item, position)
            }
            executePendingBindings()
        }
    }
}
