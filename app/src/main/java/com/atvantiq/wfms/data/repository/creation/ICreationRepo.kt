package com.atvantiq.wfms.data.repository.creation

import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.type.TypeListByProjectResponse

interface ICreationRepo {

    suspend fun clientList(): ClientListResponse
    suspend fun projectListByClientId(clientId: Int, ): ProjectListByClientResponse
    suspend fun poNumberListByProject(projectId: Int): PoListByProjectResponse
    suspend fun circleByProject(projectId: Int): CircleListByProjectResponse
    suspend fun siteListByProject(projectId: Int): SiteListByProjectResponse
    suspend fun typeListByProject(projectId: Int): TypeListByProjectResponse
    suspend fun activityListByProjectType(projectId: Int, typeId: Int): ActivityListByProjectTypeResponse
}