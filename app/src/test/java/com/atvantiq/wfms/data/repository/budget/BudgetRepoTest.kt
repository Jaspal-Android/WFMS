package com.atvantiq.wfms.data.repository.budget

import com.atvantiq.wfms.models.targets.MyProjectsResponse
import com.atvantiq.wfms.models.targets.MyTargetsResponse
import com.atvantiq.wfms.network.ApiService
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class BudgetRepoTest {

    private val api = mockk<ApiService>()
    private val repo = BudgetRepo(api)

    @Test
    fun `targets and projects are requested for the given month`() = runTest {
        val targets = mockk<MyTargetsResponse>()
        val projects = mockk<MyProjectsResponse>()
        coEvery { api.myTargets("2026-10") } returns targets
        coEvery { api.myProjects("2026-10") } returns projects

        assertSame(targets, repo.myTargets("2026-10"))
        assertSame(projects, repo.myProjects("2026-10"))
    }

    @Test
    fun `no month means the server's current month`() = runTest {
        val targets = mockk<MyTargetsResponse>()
        coEvery { api.myTargets(null) } returns targets

        assertSame(targets, repo.myTargets(null))
    }

    @Test(expected = IOException::class)
    fun `a failure reaches the caller`() = runTest {
        coEvery { api.myProjects(any()) } throws IOException("offline")

        repo.myProjects("2026-10")
    }
}
