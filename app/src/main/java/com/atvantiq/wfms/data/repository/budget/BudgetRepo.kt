package com.atvantiq.wfms.data.repository.budget

import com.atvantiq.wfms.models.targets.MyProjectsResponse
import com.atvantiq.wfms.models.targets.MyTargetsResponse
import com.atvantiq.wfms.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepo @Inject constructor(
    private val apiService: ApiService
) : IBudgetRepo {

    override suspend fun myTargets(month: String?): MyTargetsResponse =
        apiService.myTargets(month)

    override suspend fun myProjects(month: String?): MyProjectsResponse =
        apiService.myProjects(month)
}
