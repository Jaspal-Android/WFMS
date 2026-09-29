package com.atvantiq.wfms.models.reimbursement.detail


import com.atvantiq.wfms.models.reimbursement.TripRef
import com.google.gson.annotations.SerializedName

data class Expense(
    @SerializedName("amount_per_site")
    val amountPerSite: Double?,
    @SerializedName("claimed_amount")
    val claimedAmount: Double?,
    @SerializedName("expense_id")
    val expenseId: Long?,
    @SerializedName("expense_type")
    val expenseType: String?,
    @SerializedName("travel_mode")
    val travelMode: String? = null,
    @SerializedName("other_expense_detail")
    val otherExpenseDetail: String? = null,
    @SerializedName("start_location")
    val startLocation: String? = null,
    @SerializedName("end_location")
    val endLocation: String? = null,
    @SerializedName("travelling_with")
    val travellingWith: List<TravellingWith>? = null,
    @SerializedName("distance_km")
    val distanceKm: Double? = null,
    @SerializedName("distance_source")
    val distanceSource: String? = null,
    @SerializedName("trip_refs")
    val tripRefs: List<TripRef>? = null
)

data class TravellingWith(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
)