package com.atvantiq.wfms.constants

object ReimbursementData {
    const val CLAIM_TYPE_LOCAL = "local"
    const val CLAIM_TYPE_OUTSTATION = "outstation"
    const val CLAIM_SINGLE_SITE = "single_site"
    const val CLAIM_MULTI_SITE = "multi_site"

    /* expense_type values returned by GET claim/{claim_id} */
    const val EXPENSE_TRAVEL = "travel"
    const val EXPENSE_DA = "da"
    const val EXPENSE_HOTEL = "hotel"
    const val EXPENSE_OTHER = "other"
}