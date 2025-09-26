package com.atvantiq.wfms.data.repository.creation

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.network.ApiService
import com.ssas.jibli.data.prefs.PrefKeys
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
    private lateinit var prefMain: SecurePrefMain
    private lateinit var repo: CreationRepo
    private val token = "test_token"

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        prefMain = mockk(relaxed = true)
        every { prefMain.get(PrefKeys.LOGIN_TOKEN, any<String>()) } returns token
        repo = CreationRepo(apiService, prefMain)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `clientList calls apiService with correct token`() = runTest {
        val expectedResponse = mockk<ClientListResponse>()
        coEvery { apiService.clientList(any()) } returns expectedResponse

        val result = repo.clientList()

        coVerify { apiService.clientList("Bearer $token") }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `projectListByClientId calls apiService with correct token and clientId`() = runTest {
        val clientId = 123L
        val expectedResponse = mockk<ProjectListByClientResponse>()
        coEvery { apiService.projectListByClientId(any(), clientId) } returns expectedResponse

        val result = repo.projectListByClientId(clientId)

        coVerify { apiService.projectListByClientId("Bearer $token", clientId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `poNumberListByProject calls apiService with correct token and projectId`() = runTest {
        val projectId = 456L
        val expectedResponse = mockk<PoListByProjectResponse>()
        coEvery { apiService.poNumberListByProject(any(), projectId) } returns expectedResponse

        val result = repo.poNumberListByProject(projectId)

        coVerify { apiService.poNumberListByProject("Bearer $token", projectId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `circleByProject calls apiService with correct token and projectId`() = runTest {
        val projectId = 789L
        val expectedResponse = mockk<CircleListByProjectResponse>()
        coEvery { apiService.circleByProject(any(), projectId) } returns expectedResponse

        val result = repo.circleByProject(projectId)

        coVerify { apiService.circleByProject("Bearer $token", projectId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `siteListByProject calls apiService with correct token and projectId`() = runTest {
        val projectId = 1011L
        val expectedResponse = mockk<SiteListByProjectResponse>()
        coEvery { apiService.siteListByProject(any(), projectId) } returns expectedResponse

        val result = repo.siteListByProject(projectId)

        coVerify { apiService.siteListByProject("Bearer $token", projectId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `typeListByPo calls apiService with correct token and poId`() = runTest {
        val poId = 1213L
        val expectedResponse = mockk<TypeListByProjectResponse>()
        coEvery { apiService.typeListByPo(any(), poId) } returns expectedResponse

        val result = repo.typeListByPo(poId)

        coVerify { apiService.typeListByPo("Bearer $token", poId) }
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `activityListByPoType calls apiService with correct token, poId and typeId`() = runTest {
        val poId = 1415L
        val typeId = 1617L
        val expectedResponse = mockk<ActivityListByProjectTypeResponse>()
        coEvery { apiService.activityListByPoType(any(), poId, typeId) } returns expectedResponse

        val result = repo.activityListByPoType(poId, typeId)

        coVerify { apiService.activityListByPoType("Bearer $token", poId, typeId) }
        assertEquals(expectedResponse, result)
    }
}
