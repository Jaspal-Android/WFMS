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
    suspend fun projectListByClientId(clientId: Long, ): ProjectListByClientResponse
    suspend fun poNumberListByProject(projectId: Long): PoListByProjectResponse
    suspend fun circleByProject(projectId: Long): CircleListByProjectResponse
    suspend fun siteListByProject(projectId: Long): SiteListByProjectResponse
    suspend fun typeListByPo(poId: Long): TypeListByProjectResponse
    suspend fun activityListByPoType(poId: Long, typeId: Long): ActivityListByProjectTypeResponse
}