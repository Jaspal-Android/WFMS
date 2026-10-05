package com.atvantiq.wfms.ui.screens.more

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.empDetail.EmpData
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.DashboardTab
import com.atvantiq.wfms.ui.screens.DashboardTabs
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The signed-in user's profile, for the app shell's tabs, the More tab and My Profile. */
@HiltViewModel
class ProfileVM @Inject constructor(
    application: Application,
    private val authRepo: IAuthRepo,
    private val prefMain: SecurePrefMain
) : BaseViewModel(application) {

    val clickEvents = LiveEvent<MoreClickEvents>()

    /** The cached profile straight away, replaced by the server's once [refresh] answers. */
    val profile = MutableLiveData<EmpData?>(PrefMethods.getEmpDetailResponse(prefMain))

    val profileResponse = MutableLiveData<ApiState<EmpDetailResponse>>()

    /**
     * The bottom tabs for this user. Until a profile is cached they follow the role saved at
     * login, without permissions, since permissions are only ever taken from `GET /employee/me`.
     */
    val tabs: LiveData<List<DashboardTab>> = profile.map { profile ->
        val role = profile?.role ?: PrefMethods.getUserData(prefMain)?.role
        DashboardTabs.forRole(AppRole.from(role), profile?.permissions)
    }.distinctUntilChanged()

    fun onViewProfileClick() = postClickEvent(MoreClickEvents.VIEW_PROFILE)
    fun onAppearanceClick() = postClickEvent(MoreClickEvents.APPEARANCE)
    fun onLogoutClick() = postClickEvent(MoreClickEvents.LOGOUT)

    private fun postClickEvent(event: MoreClickEvents) {
        clickEvents.value = event
    }

    /** Fetches `GET /employee/me` and updates the cache the dashboard also reads. */
    fun refresh() {
        executeApiCall(
            apiCall = { authRepo.empDetails() },
            liveData = profileResponse,
            onSuccess = { response ->
                if (response.code == ValConstants.SUCCESS_CODE) {
                    PrefMethods.saveEmpDetailResponse(prefMain, response.data)
                    profile.value = response.data
                }
            },
            cancelPrevious = true
        )
    }
}
