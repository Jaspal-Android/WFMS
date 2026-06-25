package com.atvantiq.wfms.data.repository.budget

import com.atvantiq.wfms.models.targets.MyProjectsResponse
import com.atvantiq.wfms.models.targets.MyTargetsResponse

interface IBudgetRepo {

    suspend fun myTargets(month: String?): MyTargetsResponse

    suspend fun myProjects(month: String?): MyProjectsResponse
}
