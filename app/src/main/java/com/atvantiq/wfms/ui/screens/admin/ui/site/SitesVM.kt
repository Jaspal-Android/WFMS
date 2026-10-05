package com.atvantiq.wfms.ui.screens.admin.ui.site

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SitesVM @Inject constructor(application: Application, private val creationRepo: ICreationRepo,) : BaseViewModel(application) {

    var clickEvents = LiveEvent<SitesEventClicks>()

    /** Active sites */
    val sites = PagedList<SitesListAllResponse, Site>(
        fetch = { page, pageSize -> creationRepo.siteListAll(page, pageSize, StatusCodes.SITE_ACTIVE) },
        pageItems = { if (it.code == ValConstants.SUCCESS_CODE) it.data.sites else null }
    )

    fun onAddSiteClick() {
        clickEvents.value = SitesEventClicks.ON_ADD_STIE_CLICK
    }

}