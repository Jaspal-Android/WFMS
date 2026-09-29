package com.atvantiq.wfms.models.reimbursement.detail


import com.google.gson.annotations.SerializedName

data class Site(
    @SerializedName("amount_site")
    val amountSite: Double?,
    @SerializedName("expenses")
    val expenses: List<Expense?>?,
    @SerializedName("site_id")
    val siteId: Long?,
    @SerializedName("site_name")
    val siteName: String?,
    @SerializedName("work_site_id")
    val workSiteId: Long? = null,
    @SerializedName("purchase_order_ids")
    val purchaseOrderIds: List<Long>? = null
)