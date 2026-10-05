package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemClaimReviewBinding
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewRecord
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.widgets.FooterListAdapter
import com.atvantiq.wfms.widgets.diffById

/** Claims Approval rows, with the paging footer. A claim with no id can't be opened. */
class ClaimReviewAdapter(private val onReview: (ClaimReviewRecord) -> Unit) :
    FooterListAdapter<ClaimReviewRecord>(diffById { it.claimId }) {

    class Holder(val binding: ItemClaimReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        Holder(ItemClaimReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindItemHolder(holder: RecyclerView.ViewHolder, item: ClaimReviewRecord) {
        if (holder !is Holder) return
        val claim = item
        val context = holder.itemView.context
        val canOpen = claim.claimId != null
        with(holder.binding) {
            title = claim.employeeName ?: claim.claimNumber.orEmpty()
            subtitle = claim.employeeCode ?: claim.claimId?.let { context.getString(R.string.claim_id_format, it) }
            status = claim.status
            dateLabel = DateUtils.formatYmdLabel((claim.date ?: claim.createdAt)?.take(YMD_LENGTH))
                ?: context.getString(R.string.not_available_value)
            amount = context.getString(R.string.rupee_format, claim.totalAmount ?: 0.0)
            root.alpha = if (canOpen) ResourcesCompat.getFloat(context.resources, R.dimen.alpha_enabled)
                else ResourcesCompat.getFloat(context.resources, R.dimen.alpha_disabled)
            reviewRow.isEnabled = canOpen
            reviewRow.setOnClickListener { if (canOpen) onReview(claim) }
            executePendingBindings()
        }
    }

    private companion object {
        const val YMD_LENGTH = 10
    }
}
