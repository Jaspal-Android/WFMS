package com.atvantiq.wfms.data.repository.creation

import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse
import com.atvantiq.wfms.network.ApiService
import com.ssas.jibli.data.prefs.PrefKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreationRepo @Inject constructor(private val apiService: ApiService, private val prefMain: SecurePrefMain) : ICreationRepo {

    override suspend fun clientList(): ClientListResponse = apiService.clientList(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
    )

    override suspend fun projectListByClientId(clientId: Long): ProjectListByClientResponse  = apiService.projectListByClientId(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        clientId = clientId
    )

    override suspend fun poNumberListByProject(projectId: Long): PoListByProjectResponse = apiService.poNumberListByProject(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        projectId = projectId
    )

    override suspend fun circleByProject(projectId: Long): CircleListByProjectResponse  = apiService.circleByProject(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        projectId = projectId
    )

    override suspend fun siteListByProject(projectId: Long): SiteListByProjectResponse = apiService.siteListByProject(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        projectId = projectId
    )

    override suspend fun typeListByPo(poId: Long): TypeListByProjectResponse = apiService.typeListByPo(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        poId = poId
    )

    override suspend fun activityListByPoType(poId: Long, typeId: Long)  = apiService.activityListByPoType(
        token = "Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN,""),
        poId = poId,
        typeId = typeId
    )

}