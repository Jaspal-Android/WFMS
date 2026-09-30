package com.atvantiq.wfms.data.repository.creation

import com.atvantiq.wfms.models.allProjects.AllProjectsResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.models.site.create.CreateSiteResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.network.ApiService
import com.google.gson.JsonObject
import com.atvantiq.wfms.models.site.detail.SiteDetailResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreationRepo @Inject constructor(private val apiService: ApiService) : ICreationRepo {

    override suspend fun clientList(): ClientListResponse = apiService.clientList()

    override suspend fun projectListByClientId(clientId: Long): ProjectListByClientResponse  = apiService.projectListByClientId(
        clientId = clientId
    )

    override suspend fun poNumberListByProject(projectId: Long): PoListByProjectResponse = apiService.poNumberListByProject(
        projectId = projectId
    )

    override suspend fun circleByProject(projectId: Long): CircleListByProjectResponse  = apiService.circleByProject(
        projectId = projectId
    )

    override suspend fun siteListByProject(projectId: Long): SiteListByProjectResponse = apiService.siteListByProject(
        projectId = projectId
    )

    override suspend fun typeListByPo(poId: Long): TypeListByProjectResponse = apiService.typeListByPo(
        poId = poId
    )

    override suspend fun activityListByPoType(poId: Long, typeId: Long)  = apiService.activityListByPoType(
        poId = poId,
        typeId = typeId
    )

    override suspend fun siteListAll(page: Int, limit: Int, is_active: Int): SitesListAllResponse = apiService.siteListAll(
            page = page,
            limit = limit,
            is_active = is_active
    )

    override suspend fun createSite(params: JsonObject): CreateSiteResponse  = apiService.createSite(
        params = params
    )

    override suspend fun allProjects(): AllProjectsResponse = apiService.allProjects()

    override suspend fun siteById(siteId: Long): SiteDetailResponse = apiService.siteById(
        siteId = siteId
    )
}
