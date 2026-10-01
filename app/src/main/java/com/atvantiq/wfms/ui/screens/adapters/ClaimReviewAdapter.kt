package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemClaimReviewBinding
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewRecord
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.FooterRecyclerView

/** Claims Approval rows, with the paging footer. A claim with no id can't be opened. */
class ClaimReviewAdapter(private val onReview: (ClaimReviewRecord) -> Unit) : FooterRecyclerView() {

    private val claims = mutableListOf<ClaimReviewRecord>()

    inner class Holder(val binding: ItemClaimReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun count(): Int = claims.size

    override fun viewType(): Int = VIEW_TYPE_ITEM

    override fun onCreateHolderMethod(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        Holder(ItemClaimReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolderMethod(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is Holder) return
        val claim = claims[position]
        val context = holder.itemView.context
        val canOpen = claim.claimId != null
        with(holder.binding) {
            title = claim.employeeName ?: claim.claimNumber.orEmpty()
            subtitle = claim.employeeCode ?: claim.claimId?.let { context.getString(R.string.claim_id_format, it) }
            status = claim.status
            dateLabel = DateUtils.formatYmdLabel((claim.date ?: claim.createdAt)?.take(YMD_LENGTH))
                ?: context.getString(R.string.not_available_value)
            category = Utils.humanize(claim.expenseCategory)
            amount = context.getString(R.string.rupee_format, claim.totalAmount ?: 0.0)
            root.alpha = if (canOpen) ResourcesCompat.getFloat(context.resources, R.dimen.alpha_enabled)
                else ResourcesCompat.getFloat(context.resources, R.dimen.alpha_disabled)
            reviewRow.isEnabled = canOpen
            reviewRow.setOnClickListener { if (canOpen) onReview(claim) }
            executePendingBindings()
        }
    }

    fun submitList(items: List<ClaimReviewRecord>) {
        claims.clear()
        claims.addAll(items)
        notifyDataSetChanged()
    }

    private companion object {
        const val VIEW_TYPE_ITEM = 1
        const val YMD_LENGTH = 10
    }
}
