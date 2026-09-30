package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import com.atvantiq.wfms.constants.AppListData
import com.atvantiq.wfms.constants.ReimbursementData
import com.atvantiq.wfms.models.empoyeeByCircle.Data
import com.atvantiq.wfms.models.reimbursement.DAExpense
import com.atvantiq.wfms.models.reimbursement.HotelExpense
import com.atvantiq.wfms.models.reimbursement.OtherExpense
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.models.reimbursement.TravelModeOption
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.reimbursement.detail.Expense
import com.atvantiq.wfms.models.site.detail.SiteDetail
import java.math.BigDecimal

/** A site of the claim being edited, with the ids update requires to keep it linked unchanged. */
data class LockedSite(val id: Long, val workSiteId: Long?, val purchaseOrderIds: List<Long>)

/**
 * The parts of an existing claim the employee cannot change while editing.
 * They are sent back unchanged on update.
 */
data class LockedClaimHeader(
    val claimId: Long,
    val date: String,
    val claimType: String,
    val expenseCategory: String,
    val purpose: String,
    val sites: List<LockedSite>,
    val siteNames: List<String>,
    val projectId: Long,
    val circleId: Long,
    val circleCode: String?
)

data class EditableClaim(
    val header: LockedClaimHeader,
    val remarks: String,
    val travel: List<TravelExpense>,
    val da: List<DAExpense>,
    val hotel: List<HotelExpense>,
    val other: List<OtherExpense>
)

/** Maps a GET claim/{claim_id} response into the create-claim form state used for editing. */
object ClaimEditMapper {

    /**
     * [site] is any site of the claim (GET site/{id}). The claim detail does not carry project/circle,
     * but every site of a claim belongs to the claim's project and circle, and update requires both.
     * Returns null when the claim cannot be updated.
     */
    fun toEditableClaim(claim: ClaimData, site: SiteDetail?): EditableClaim? {
        val claimId = claim.id ?: return null
        val projectId = site?.project?.id ?: return null
        val circleId = site.circle?.id ?: return null
        val sites = claim.sites.orEmpty()

        // A multi-site claim lists the same expense under every site it is split across;
        // claimed_amount is the full amount, so keep each expense once.
        val (withId, withoutId) = sites.flatMap { it.expenses.orEmpty() }
            .filterNotNull()
            .partition { it.expenseId != null }
        val expenses = withId.distinctBy { it.expenseId } + withoutId

        val header = LockedClaimHeader(
            claimId = claimId,
            date = claim.date.orEmpty(),
            claimType = claim.claimType ?: ReimbursementData.CLAIM_SINGLE_SITE,
            expenseCategory = claim.claimCategory ?: ReimbursementData.CLAIM_TYPE_LOCAL,
            purpose = claim.claimPurpose.orEmpty(),
            sites = sites.mapNotNull { s ->
                s.siteId?.let { LockedSite(it, s.workSiteId, s.purchaseOrderIds.orEmpty()) }
            },
            siteNames = sites.mapNotNull { it.siteName },
            projectId = projectId,
            circleId = circleId,
            circleCode = site.circle.code
        )

        return EditableClaim(
            header = header,
            remarks = (claim.remarks as? String).orEmpty(),
            travel = expenses.ofType(ReimbursementData.EXPENSE_TRAVEL).map(::toTravelEntry),
            da = expenses.ofType(ReimbursementData.EXPENSE_DA).mapIndexed { i, e ->
                DAExpense(entryId = (i + 1).toString(), amount = formatAmount(e.claimedAmount))
            },
            hotel = expenses.ofType(ReimbursementData.EXPENSE_HOTEL).mapIndexed { i, e ->
                HotelExpense(entryId = (i + 1).toString(), amount = formatAmount(e.claimedAmount))
            },
            other = expenses.ofType(ReimbursementData.EXPENSE_OTHER).mapIndexed { i, e ->
                OtherExpense(
                    entryId = (i + 1).toString(),
                    category = e.otherExpenseDetail.orEmpty(),
                    amount = formatAmount(e.claimedAmount)
                )
            }
        )
    }

    private fun toTravelEntry(e: Expense) = TravelExpense(
        mode = toTravelMode(e.travelMode),
        amount = formatAmount(e.claimedAmount),
        travelingWith = e.travellingWith.orEmpty().map { Data(code = null, email = null, id = it.id, name = it.name) },
        from = e.startLocation.orEmpty(),
        to = e.endLocation.orEmpty(),
        distanceKm = e.distanceKm,
        distanceSource = e.distanceSource,
        tripRefs = e.tripRefs
    )

    private fun toTravelMode(label: String?): TravelModeOption? {
        if (label.isNullOrBlank()) return null
        return AppListData.outstationTravelModes.firstOrNull { it.label.equals(label, ignoreCase = true) }
            ?: TravelModeOption(label = label, value = label)
    }

    private fun List<Expense>.ofType(type: String) =
        filter { it.expenseType.equals(type, ignoreCase = true) }

    /** 200.0 -> "200", 200.5 -> "200.5", matching what the user typed on create. */
    internal fun formatAmount(amount: Double?): String =
        BigDecimal.valueOf(amount ?: 0.0).stripTrailingZeros().toPlainString()
}
