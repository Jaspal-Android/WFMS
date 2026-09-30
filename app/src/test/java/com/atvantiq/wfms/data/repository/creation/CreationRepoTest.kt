package com.atvantiq.wfms.data.repository.creation

import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.network.ApiService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class CreationRepoTest {

    private lateinit var apiService: ApiService
    private lateinit var repo: CreationRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        repo = CreationRepo(apiService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `clientList calls apiService `() = runTest {
        val expectedResponse = mockk<ClientListResponse>()
        coEvery { apiService.clientList() } returns expectedResponse

        val result = repo.clientList()

        coVerify { apiService.clientList() }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `projectListByClientId calls apiService with clientId`() = runTest {
        val clientId = 123L
        val expectedResponse = mockk<ProjectListByClientResponse>()
        coEvery { apiService.projectListByClientId(clientId) } returns expectedResponse

        val result = repo.projectListByClientId(clientId)

        coVerify { apiService.projectListByClientId(clientId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `poNumberListByProject calls apiService with projectId`() = runTest {
        val projectId = 456L
        val expectedResponse = mockk<PoListByProjectResponse>()
        coEvery { apiService.poNumberListByProject(projectId) } returns expectedResponse

        val result = repo.poNumberListByProject(projectId)

        coVerify { apiService.poNumberListByProject(projectId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `circleByProject calls apiService with projectId`() = runTest {
        val projectId = 789L
        val expectedResponse = mockk<CircleListByProjectResponse>()
        coEvery { apiService.circleByProject(projectId) } returns expectedResponse

        val result = repo.circleByProject(projectId)

        coVerify { apiService.circleByProject(projectId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `siteListByProject calls apiService with projectId`() = runTest {
        val projectId = 1011L
        val expectedResponse = mockk<SiteListByProjectResponse>()
        coEvery { apiService.siteListByProject(projectId) } returns expectedResponse

        val result = repo.siteListByProject(projectId)

        coVerify { apiService.siteListByProject(projectId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `typeListByPo calls apiService with poId`() = runTest {
        val poId = 1213L
        val expectedResponse = mockk<TypeListByProjectResponse>()
        coEvery { apiService.typeListByPo(poId) } returns expectedResponse

        val result = repo.typeListByPo(poId)

        coVerify { apiService.typeListByPo(poId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `activityListByPoType calls apiService with poId and typeId`() = runTest {
        val poId = 1415L
        val typeId = 1617L
        val expectedResponse = mockk<ActivityListByProjectTypeResponse>()
        coEvery { apiService.activityListByPoType(poId, typeId) } returns expectedResponse

        val result = repo.activityListByPoType(poId, typeId)

        coVerify { apiService.activityListByPoType(poId, typeId) }
        assertEquals(expectedResponse, result)
    }
}
