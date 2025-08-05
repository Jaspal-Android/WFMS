package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.NoInternetException
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AddSignInVM @Inject constructor(application: Application,private val creationRepo: ICreationRepo, private val workRepo: IWorkRepo) : AndroidViewModel(application) {

    /*Variables Init*/
    var clickEvents  = MutableLiveData<AddSignInClickEvents>()

    /*Variables declaration*/
    var selectedClient:Client? = null
    var selectedProjectId:Int? = null
    var selectedPoNumberId:Int? = null
    var selectedCircleId:Int? = null
    var selectedSiteId:Int? = null

    /*
    * Select Type and Activity list will go in array
    * */
    var selectedTypeIdList:ArrayList<Int>? = ArrayList()
    var selectedActivityIdList:ArrayList<Int>? = ArrayList()


    /*Methods declaration*/
    fun onCameraClick(){
        clickEvents.value = AddSignInClickEvents.ON_CAMERA_CLICK
    }

    fun onSaveClick(){
        getWorkAssigned()
    }

    fun onCancelClick(){
        clickEvents.value = AddSignInClickEvents.ON_CANCEL_CLICK
    }

    /*
   * Get as work assigned all API
   * */

    var clientListResponse = MutableLiveData<ApiState<ClientListResponse>>()
    fun getClientList() {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                clientListResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.clientList()
                    clientListResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    clientListResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            clientListResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Get as project list by client id API
    * */
    var projectListByClientResponse = MutableLiveData<ApiState<ProjectListByClientResponse>>()
    fun getProjectListByClientId(clientId: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                projectListByClientResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.projectListByClientId(clientId)
                    projectListByClientResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    projectListByClientResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            projectListByClientResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Get PO number list by project id API
    * */
    var poNumberListByProjectResponse = MutableLiveData<ApiState<PoListByProjectResponse>>()
    fun getPoNumberListByProject(projectId: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                poNumberListByProjectResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.poNumberListByProject(projectId)
                    poNumberListByProjectResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    poNumberListByProjectResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            poNumberListByProjectResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Get Circle list by project id API
    * */
    var circleListByProjectResponse = MutableLiveData<ApiState<CircleListByProjectResponse>>()
    fun getCircleListByProject(projectId: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                circleListByProjectResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.circleByProject(projectId)
                    circleListByProjectResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    circleListByProjectResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            circleListByProjectResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Get site list by circle id API
    * */
    var siteListByProjectResponse = MutableLiveData<ApiState<SiteListByProjectResponse>>()
    fun getSiteListByProject(projectId: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                siteListByProjectResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.siteListByProject(projectId)
                    siteListByProjectResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    siteListByProjectResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            siteListByProjectResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Get Type list by project id API
    * */
    var typeListByProjectResponse = MutableLiveData<ApiState<com.atvantiq.wfms.models.type.TypeListByProjectResponse>>()
    fun getTypeListByProject(projectId: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                typeListByProjectResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.typeListByProject(projectId)
                    typeListByProjectResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    typeListByProjectResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            typeListByProjectResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }

    /*
    * Get activity list by project id and type id API
    * */
    var activityListByProjectTypeResponse = MutableLiveData<ApiState<com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse>>()
    fun getActivityListByProjectType(projectId: Int, typeId: Int) {
        if (Utils.isInternet(getApplication())) {
            viewModelScope.launch {
                activityListByProjectTypeResponse.postValue(ApiState.loading())
                try {
                    var response = creationRepo.activityListByProjectType(projectId, typeId)
                    activityListByProjectTypeResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    activityListByProjectTypeResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            activityListByProjectTypeResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }


    /*
    * Work assigned API
    * */
    var workAssignedResponse = MutableLiveData<ApiState<SelfAssignResponse>>()
    fun getWorkAssigned() {
        if (Utils.isInternet(getApplication())) {
            val params = JsonObject().apply {
                addProperty("circle_id", selectedClient?.id)
                addProperty("project_id", selectedProjectId)
                addProperty("po_id", selectedPoNumberId)
                addProperty("client_id", selectedCircleId)
                // Site array
                val siteArray = com.google.gson.JsonArray()
                val siteObj = JsonObject()
                siteObj.addProperty("id", selectedSiteId)
                siteArray.add(siteObj)
                add("site", siteArray)

                // Type array
                val typeArray = com.google.gson.JsonArray()
                for (type in selectedTypeIdList ?: emptyList()) {
                    val typeObj = JsonObject()
                    typeObj.addProperty("id", type)

                    // Dynamic activity array inside type
                    val activityArray = com.google.gson.JsonArray()
                    for (activity in selectedActivityIdList ?: emptyList()) {
                        val activityObj = JsonObject()
                        activityObj.addProperty("id",activity)
                        activityArray.add(activityObj)
                    }
                    typeObj.add("activity", activityArray)
                    typeArray.add(typeObj)
                }
                add("type", typeArray)
            }

            viewModelScope.launch {
                workAssignedResponse.postValue(ApiState.loading())
                try {

                    var response = workRepo.workSelfAssign(params)
                    workAssignedResponse.postValue(ApiState.success(response))
                } catch (e: Exception) {
                    workAssignedResponse.postValue(ApiState.error(e))
                }
            }
        } else {
            workAssignedResponse.postValue(ApiState.error(NoInternetException("No Internet Connection")))
        }
    }


}