package com.atvantiq.wfms.models.reimbursement

import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimDecision
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.atvantiq.wfms.models.reimbursement.review.ExpenseDecisionInput
import com.atvantiq.wfms.models.reimbursement.review.canReviewClaim
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Claims Approval (spec 8): lenient list decoding, the decision rules and the request body. */
class ClaimReviewTest {

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private fun list(data: String) = gson.fromJson(
        """{ "code": 200, "message": "Claims fetched successfully.", "success": true, "data": $data }""",
        ClaimReviewListResponse::class.java
    ).data!!

    @Test
    fun `the real Dev answer for the PM is an empty page`() {
        val page = list("""{ "page": 1, "page_size": 25, "total_count": 1, "total_records": 0, "total_pages": 0, "records": [] }""")
        assertTrue(page.records.isEmpty())
        assertEquals(0, page.totalRecords)
    }

    @Test
    fun `data may be an array, or an object holding records or claims`() {
        val claim = """{ "claim_id": 627890811548, "status": "Submitted",
            "employee": { "employee_id": 7, "employee_name": "Jaspal", "employee_code": "JAXX029" } }"""
        assertEquals(627890811548L, list("[$claim]").records.single().claimId)
        assertEquals("Jaspal", list("""{ "records": [$claim] }""").records.single().employeeName)
        assertEquals("JAXX029", list("""{ "claims": [$claim] }""").records.single().employeeCode)
    }

    @Test
    fun `ids, employee fields and status are read under either name`() {
        val record = list("""[{ "id": 5, "total_amount": "920.5", "expense_category": "local_conveyance",
            "status": { "name": "Approved by PM" },
            "employee": { "id": 9, "name": "Kamal", "code": "KS01" } }]""").records.single()
        assertEquals(5L, record.claimId)
        assertEquals(9L, record.employeeId)
        assertEquals("Kamal", record.employeeName)
        assertEquals("KS01", record.employeeCode)
        assertEquals(920.5, record.totalAmount!!, 0.0)
        assertEquals("Approved by PM", record.status)
    }

    @Test
    fun `the detail's status is read as text or from an object`() {
        val json = """{ "code": 200, "message": "ok", "success": true, "data": { "id": 1, "status": %s } }"""
        assertEquals("Submitted", gson.fromJson(json.format("\"Submitted\""), ClaimDetailResponse::class.java).data?.status)
        assertEquals("APPROVED", gson.fromJson(json.format("""{ "code": "APPROVED" }"""), ClaimDetailResponse::class.java).data?.status)
    }

    @Test
    fun `who can act on a claim`() {
        assertTrue(canReviewClaim("Submitted", AppRole.PM))
        assertFalse(canReviewClaim("approved by pm", AppRole.PM))
        assertTrue(canReviewClaim("Approved by PM", AppRole.OPS))
        assertFalse(canReviewClaim("Rejected by OPS", AppRole.ADMIN))
        assertFalse(canReviewClaim("Submitted", AppRole.EMPLOYEE))
        assertFalse(canReviewClaim("Submitted", AppRole.OTHER))
    }

    private fun input(id: Long? = 991L, claimed: Double = 20.0, typed: String = "20.0", remarks: String = "") =
        ExpenseDecisionInput(1, "Testing 4", "T4", id, "travel", claimed, typed, remarks)

    @Test
    fun `the decision is checked in the spec's order`() {
        assertEquals(ClaimDecision.Invalid(R.string.no_valid_expenses), ClaimDecision.build(1, listOf(input(id = null))))
        assertEquals(ClaimDecision.Invalid(R.string.enter_valid_approved_amounts), ClaimDecision.build(1, listOf(input(typed = ""))))
        assertEquals(ClaimDecision.Invalid(R.string.enter_valid_approved_amounts), ClaimDecision.build(1, listOf(input(typed = "abc"))))
        assertEquals(ClaimDecision.Invalid(R.string.approved_amount_exceeds_claimed), ClaimDecision.build(1, listOf(input(typed = "20.01"))))
    }

    @Test
    fun `the body nests claims and expenses, and leaves out empty remarks`() {
        val decision = ClaimDecision.build(
            627890811548L,
            listOf(input(typed = "15", remarks = " ok "), input(id = 992L, claimed = 900.0, typed = "900", remarks = "  "))
        ) as ClaimDecision.Valid

        val claim = decision.body.getAsJsonArray("claims").single().asJsonObject
        assertEquals(627890811548L, claim.get("claim_id").asLong)
        val expenses = claim.getAsJsonArray("expenses")
        assertEquals(991L, expenses[0].asJsonObject.get("expense_id").asLong)
        assertEquals(15.0, expenses[0].asJsonObject.get("amount").asDouble, 0.0)
        assertEquals("ok", expenses[0].asJsonObject.get("remarks").asString)
        assertFalse(expenses[1].asJsonObject.has("remarks"))
        assertEquals(915.0, decision.approvedTotal, 0.0)
    }

    @Test
    fun `each expense starts at its claimed amount, written as typed (200, not 200_0)`() {
        val claim = Gson().fromJson(
            """{ "id": 1, "sites": [ { "site_name": "MOGA", "amount_site": 200.0,
                 "expenses": [ { "expense_id": 991, "expense_type": "travel", "claimed_amount": 200.0 },
                               { "expense_id": 992, "expense_type": "da", "claimed_amount": 12.5 } ] } ] }""",
            com.atvantiq.wfms.models.reimbursement.detail.ClaimData::class.java
        )
        val inputs = ClaimDecision.inputsFor(claim)
        assertEquals(listOf("200", "12.5"), inputs.map { it.amountText })
        assertEquals(1, inputs.first().siteNumber)
        assertEquals(200.0, inputs.first().siteAmount!!, 0.0)
        assertEquals(212.5, ClaimDecision.total(inputs), 0.0)
    }
}
