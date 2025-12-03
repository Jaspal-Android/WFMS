package com.atvantiq.wfms.ui.screens.admin.ui.site

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.network.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SitesVM @Inject constructor(application: Application, private val creationRepo: ICreationRepo,) : BaseViewModel(application) {

    var clickEvents = MutableLiveData<SitesEventClicks>()

    var allSitesResponse = MutableLiveData<ApiState<SitesListAllResponse>>()

    fun getAllSites(page:Int, limit:Int) {
        executeApiCall(
            apiCall = { creationRepo.siteListAll(page, limit, 1) },
            liveData = allSitesResponse,
        )
    }

    fun onAddSiteClick() {
        clickEvents.value = SitesEventClicks.ON_ADD_STIE_CLICK
    }

}