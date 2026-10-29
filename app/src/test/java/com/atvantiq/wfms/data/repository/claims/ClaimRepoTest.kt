package com.atvantiq.wfms.data.repository.claims

import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimApproveResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class ClaimRepoTest {

    private val api = mockk<ApiService>()
    private val repo = ClaimRepo(api)

    @Test
    fun `a search is trimmed before it reaches the server`() = runTest {
        val answer = mockk<ClaimReviewListResponse>()
        coEvery { api.claimsForReview(1, 25, "Asha") } returns answer

        assertSame(answer, repo.claimsForReview(1, 25, "  Asha \n"))
        coVerify(exactly = 1) { api.claimsForReview(1, 25, "Asha") }
    }

    @Test
    fun `a blank or missing search is sent as no filter`() = runTest {
        val answer = mockk<ClaimReviewListResponse>()
        coEvery { api.claimsForReview(2, 25, null) } returns answer

        assertSame(answer, repo.claimsForReview(2, 25, "   "))
        assertSame(answer, repo.claimsForReview(2, 25, null))
        coVerify(exactly = 2) { api.claimsForReview(2, 25, null) }
    }

    @Test
    fun `a claim is fetched by its id`() = runTest {
        val answer = mockk<ClaimDetailResponse>()
        coEvery { api.claimById(42L) } returns answer

        assertSame(answer, repo.claimById(42L))
    }

    @Test
    fun `an approval sends the body unchanged`() = runTest {
        val body = JsonObject().apply { addProperty("k", "v") }
        val answer = mockk<ClaimApproveResponse>()
        coEvery { api.approveClaim(body) } returns answer

        assertSame(answer, repo.approveClaim(body))
    }

    @Test
    fun `a network failure reaches the caller`() = runTest {
        coEvery { api.claimById(any()) } throws IOException("offline")

        try {
            repo.claimById(1L)
            fail("expected the IOException")
        } catch (e: IOException) {
            assertEquals("offline", e.message)
        }
    }
}
