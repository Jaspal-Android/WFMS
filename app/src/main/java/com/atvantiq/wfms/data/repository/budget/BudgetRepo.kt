package com.atvantiq.wfms.data.repository.budget

import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.models.targets.MyProjectsResponse
import com.atvantiq.wfms.models.targets.MyTargetsResponse
import com.atvantiq.wfms.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepo @Inject constructor(
    private val apiService: ApiService,
    private val prefMain: SecurePrefMain
) : IBudgetRepo {

    override suspend fun myTargets(month: String?): MyTargetsResponse =
        apiService.myTargets("Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""), month)

    override suspend fun myProjects(month: String?): MyProjectsResponse =
        apiService.myProjects("Bearer " + prefMain.get(PrefKeys.LOGIN_TOKEN, ""), month)
}
