package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityAddSignInBinding
import com.atvantiq.wfms.models.activity.ActivityData
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.po.PoData
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.type.TypeData
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.dialogs.MultiSelectBottomSheetDialog
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.util.Locale


@AndroidEntryPoint
class AddSignInActivity : BaseActivity<ActivityAddSignInBinding, AddSignInVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_add_sign_in, AddSignInVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setDateTimeAttendance()
        getClientList()
        initListeners()
    }

    private fun setUpToolbar() {
        binding.addSignInToolbar.toolbarTitle.text = getString(R.string.self_assigned_work)
        binding.addSignInToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setDateTimeAttendance() {
        binding.dateString = DateUtils.getCurrentDate()
        binding.timeString = DateUtils.getCurrentTime()
    }

    private fun initListeners() {
        binding.clientEt.setOnClickListener {
            showClientSelectionDialog(viewModel.clients)
        }

        binding.projectEt.setOnClickListener {
            showProjectSelectionDialog(viewModel.projects)
        }

        binding.poEt.setOnClickListener {
            showPOSelectionDialog(viewModel.poNumbers)
        }

        binding.circleEt.setOnClickListener {
            showCircleSelectionDialog(viewModel.circles)
        }

        binding.siteEt.setOnClickListener {
            showSiteSelectionDialog(viewModel.sites)
        }

        binding.typeEt.setOnClickListener {
            showTypeSelectionDialog(viewModel.types)
        }

        binding.activitiesEt.setOnClickListener {
            if (viewModel.selectedTypeIdList.isNullOrEmpty()) {
                binding.activitiesEt.error = getString(R.string.select_type)
                showToast(this, getString(R.string.select_type))
            } else {
                showActivitySelectionDialog(viewModel.activities)
            }
        }

    }

    private fun onClientSelected(selectedClient: Client) {
        viewModel.selectedClient = selectedClient
        binding.clientEt.setText(selectedClient.companyName)
        binding.projectEt.setText("")
        binding.poEt.setText("")
        binding.circleEt.setText("")
        binding.siteEt.setText("")
        binding.typeEt.setText("")
        binding.activitiesEt.setText("")
        viewModel.selectedProjectId = null
        viewModel.selectedPoNumberId = null
        viewModel.selectedCircleId = null
        viewModel.selectedSiteId = null
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()
        getProjectListByClientId(selectedClient.id)
    }

    private fun onProjectSelected(selectedProject: ProjectData) {
        viewModel.selectedProjectId = selectedProject.id
        binding.projectEt.setText(selectedProject.name)
        binding.poEt.setText("")
        binding.circleEt.setText("")
        binding.siteEt.setText("")
        binding.typeEt.setText("")
        binding.activitiesEt.setText("")
        viewModel.selectedPoNumberId = null
        viewModel.selectedCircleId = null
        viewModel.selectedSiteId = null
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()
        getPoNumberListByProject(selectedProject.id)
        getCircleListByProject(selectedProject.id)
        getSiteListByProject(selectedProject.id)
    }

    private fun onPoSelected(selectedPo: PoData) {
        viewModel.selectedPoNumberId = selectedPo.id
        binding.poEt.setText(selectedPo.poNumber)
        binding.typeEt.setText("")
        binding.activitiesEt.setText("")
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()
        getTypeListByPo(selectedPo.id)
    }

    override fun subscribeToEvents(vm: AddSignInVM) {
        binding.vm = vm

        vm.clickEvents.observe(this) {
            when (it) {
                AddSignInClickEvents.ON_SAVE_CLICK -> {
                    finish()
                }

                AddSignInClickEvents.ON_CANCEL_CLICK -> {
                    finish()
                }
            }
        }

        vm.errorHandler.observe(this) { error ->
            when (error) {
                AssignTaskError.ON_CLIENT_ERROR -> {
                    binding.clientEt.error = getString(R.string.select_client)
                    showToast(this, getString(R.string.select_client))
                }

                AssignTaskError.ON_PROJECT_ERROR -> {
                    binding.projectEt.error = getString(R.string.select_project)
                    showToast(this, getString(R.string.select_project))
                }

                AssignTaskError.ON_PO_NUMBER_ERROR -> {
                    binding.poEt.error = getString(R.string.select_po_number)
                    showToast(this, getString(R.string.select_po_number))
                }

                AssignTaskError.ON_CIRCLE_ERROR -> {
                    binding.circleEt.error = getString(R.string.select_circle)
                    showToast(this, getString(R.string.select_circle))
                }

                AssignTaskError.ON_SITE_ERROR -> {
                    binding.siteEt.error = getString(R.string.select_site)
                    showToast(this, getString(R.string.select_site))
                }

                AssignTaskError.ON_TYPE_ERROR -> {
                    binding.typeEt.error = getString(R.string.select_type)
                    showToast(this, getString(R.string.select_type))
                }

                AssignTaskError.ON_ACTIVITY_ERROR -> {
                    binding.activitiesEt.error = getString(R.string.select_activity)
                    showToast(this, getString(R.string.select_activity))
                }
            }
        }

        vm.clientListResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.onSubmitCompleted()
                    dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            val clients = response.response?.data?.clients ?: emptyList()
                            viewModel.clients = clients
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.onSubmitCompleted()
                    dismissProgress()
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }

        vm.projectListByClientResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isProjectLoading.set(false)
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val projects = response.response?.data ?: emptyList()
                            viewModel.projects = projects
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isProjectLoading.set(false)
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    vm.isProjectLoading.set(true)
                }
            }
        }

        vm.poNumberListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isPoLoading.set(false)
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val poNumbers = response.response?.data ?: emptyList()
                            viewModel.poNumbers = poNumbers
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isPoLoading.set(false)
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    vm.isPoLoading.set(true)
                }
            }
        }

        vm.circleListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isCircleLoading.set(false)
                    when (response.response?.code) {
                        200 -> {
                            val circles = response.response?.data ?: emptyList()
                            viewModel.circles = circles
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isCircleLoading.set(false)
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    vm.isCircleLoading.set(true)
                }
            }
        }

        vm.siteListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isSiteLoading.set(false)
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val sites = response.response?.data ?: emptyList()
                            viewModel.sites = sites
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isSiteLoading.set(false)
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    vm.isSiteLoading.set(true)
                }
            }
        }

        vm.typeListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isTypeLoading.set(false)
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val types = response.response?.data ?: emptyList()
                            viewModel.types = types
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isTypeLoading.set(false)
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    vm.isTypeLoading.set(true)
                }
            }
        }

        vm.activityListByProjectTypeResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.isActivityLoading.set(false)
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val activities = response.response?.data ?: emptyList()
                            viewModel.activities = activities
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    vm.isActivityLoading.set(false)
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    vm.isActivityLoading.set(true)
                }
            }
        }

        vm.workAssignedResponse.observe(this)
        { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            showToast(
                                this,
                                response.response?.message
                                    ?: getString(R.string.work_assigned_successfully)
                            )
                            setResult(Activity.RESULT_OK)
                            finish()
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }
    }

    // Generic dialog creator for single selection


    private fun showClientSelectionDialog(clients: List<Client>) {
        showSelectionDialog(
            items = clients,
            title = getString(R.string.select_client),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, client ->
                view.findViewById<TextView>(R.id.text1).text = client.companyName
            },
            onItemSelected = {
                binding.clientEt.error = null
                onClientSelected(it)
            },
            filterCondition = { client, query ->
                client.companyName.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_clients_available),
            retryAction = { getClientList() },
            tag = "ClientSelectionDialog"
        )
    }

    private fun showProjectSelectionDialog(projects: List<ProjectData>) {
        showSelectionDialog(
            items = projects,
            title = getString(R.string.select_project),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, project ->
                view.findViewById<TextView>(R.id.text1).text = project.name
            },
            onItemSelected = {
                binding.projectEt.error = null
                onProjectSelected(it)
            },
            filterCondition = { project, query ->
                project.name.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_projects_available),
            retryAction = { getProjectListByClientId(viewModel.selectedClient?.id ?: 0L) },
            tag = "ProjectSelectionDialog"
        )
    }

    private fun showPOSelectionDialog(poNumbers: List<PoData>) {
        showSelectionDialog(
            items = poNumbers,
            title = getString(R.string.select_po_number),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, poNumber ->
                view.findViewById<TextView>(R.id.text1).text = poNumber.poNumber
            },
            onItemSelected = {
                binding.poEt.error = null
                onPoSelected(it)
            },
            filterCondition = { poNumber, query ->
                poNumber.poNumber.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_po_numbers_available),
            retryAction = { getPoNumberListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "POSelectionDialog"
        )
    }

    private fun showCircleSelectionDialog(circles: List<CircleData>) {
        showSelectionDialog(
            items = circles,
            title = getString(R.string.select_circle),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, circle ->
                view.findViewById<TextView>(R.id.text1).text = circle.name
            },
            onItemSelected = {
                binding.circleEt.error = null
                viewModel.selectedCircleId = it.id
                binding.circleEt.setText(it.name)
            },
            filterCondition = { circle, query ->
                circle.name.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_circles_available),
            retryAction = { getCircleListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "CircleSelectionDialog"
        )
    }

    private fun showSiteSelectionDialog(sites: List<SiteData>) {
        showSelectionDialog(
            items = sites,
            title = getString(R.string.select_site),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, site ->
                view.findViewById<TextView>(R.id.text1).text = site.name
            },
            onItemSelected = {
                binding.siteEt.error = null
                viewModel.selectedSiteId = it.id
                binding.siteEt.setText(it.name)
            },
            filterCondition = { site, query ->
                site.name.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_sites_available),
            retryAction = { getSiteListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "SiteSelectionDialog"
        )
    }

    private fun showTypeSelectionDialog(types: List<TypeData>) {
        if (types.isEmpty()) {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_types_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getTypeListByPo(viewModel.selectedPoNumberId ?: 0L)
                },
            )
        }else{
            val preSelectedTypes = viewModel.selectedTypeIdList?.mapNotNull { type ->
                types.find { it.id == type?.id }
            }?.toSet() ?: emptySet()

            val dialog = MultiSelectBottomSheetDialog(
                context = this,
                items = types,
                preSelectedItems = preSelectedTypes,
                bind = { view, type, isSelected ->
                    view.findViewById<TextView>(R.id.textView).text = type.name
                    view.findViewById<CheckBox>(R.id.checkBox).isChecked = isSelected
                },
                onSelectionChanged = { selectedTypes ->
                    binding.typeEt.error = null
                    updateSelectedTypes(selectedTypes)
                },
                onSubmit = { selectedTypes ->
                    binding.typeEt.error = null
                    updateSelectedTypes(selectedTypes)
                },
                filterCondition = { type, query ->
                    type.name?.lowercase(Locale.getDefault())
                        ?.contains(query.lowercase(Locale.getDefault()))?:false
                },
                title = getString(R.string.select_type)
            )
            dialog.show(supportFragmentManager, "TypeSelectionDialog")
        }
    }

    private fun showActivitySelectionDialog(activities: List<ActivityData>) {
        if (activities.isNotEmpty()) {
            val preSelectedActivities = viewModel.selectedActivityIdList?.mapNotNull { id ->
                activities.find { it.id == id }
            }?.toSet() ?: emptySet()

            val dialog = MultiSelectBottomSheetDialog(
                context = this,
                items = activities,
                preSelectedItems = preSelectedActivities,
                bind = { view, activity, isSelected ->
                    view.findViewById<TextView>(R.id.textView).text = activity.name
                    view.findViewById<CheckBox>(R.id.checkBox).isChecked = isSelected
                },
                onSelectionChanged = { selectedActivities ->
                    binding.activitiesEt.error = null
                    updateSelectedActivities(selectedActivities)
                },
                onSubmit = { selectedActivities ->
                    binding.activitiesEt.error = null
                    updateSelectedActivities(selectedActivities)
                },
                filterCondition = { activity, query ->
                    activity.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_activities)
            )
            dialog.show(supportFragmentManager, "ActivitySelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_activities_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getActivityListByPoType(
                        viewModel.selectedPoNumberId ?: 0L,
                        viewModel.selectedTypeIdList?.firstOrNull()?.id ?: 0L
                    )
                },
            )
        }
    }

    private fun updateSelectedTypes(selectedTypes: Set<TypeData>) {
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedTypeIdList?.addAll(selectedTypes)
        viewModel.selectedActivityIdList?.clear()
        binding.activitiesEt.setText("")
        binding.typeEt.setText(selectedTypes.joinToString(", ") { it.name.toString() })
        val firstTypeId = selectedTypes.firstOrNull()?.id
        if (firstTypeId != null) {
            getActivityListByPoType(viewModel.selectedPoNumberId ?: 0L, firstTypeId)
        } else {
            viewModel.activities = emptyList()
        }
    }


    private fun updateSelectedActivities(selectedActivities: Set<ActivityData>) {
        viewModel.selectedActivityIdList?.clear()
        viewModel.selectedActivityIdList?.addAll(selectedActivities.map { it.id })
        binding.activitiesEt.setText(selectedActivities.joinToString(", ") { it.name })
    }

    private fun getClientList() {
        viewModel.getClientList()
    }

    /*
    * Get project list by client id
    * */
    private fun getProjectListByClientId(clientId: Long) {
        viewModel.getProjectListByClientId(clientId)

    }

    /*
    * Get PO number list by project id
    * */
    private fun getPoNumberListByProject(projectId: Long) {
        viewModel.getPoNumberListByProject(projectId)
    }

    /*
    * Get Circle list by project id
    * */
    private fun getCircleListByProject(projectId: Long) {
        viewModel.getCircleListByProject(projectId)
    }

    /*
    * Get Site list by project id
    * */
    private fun getSiteListByProject(projectId: Long) {
        viewModel.getSiteListByProject(projectId)
    }

    /*
    * Get Type list by project id
    * */
    private fun getTypeListByPo(poId: Long) {
        viewModel.getTypeListByPo(poId)
    }

    /*
    * Get Activity list by project id and type id
    * */
    private fun getActivityListByPoType(poId: Long, typeId: Long) {
        viewModel.getActivityListByPoType(poId, typeId)
    }
}
