package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import AutoCompleteTempAdapter
import GenericBottomSheetDialog
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.widget.CheckBox
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.atvantiq.wfms.utils.PermissionUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.util.Locale


@AndroidEntryPoint

class AddSignInActivity : BaseActivity<ActivityAddSignInBinding, AddSignInVM>() {

    /*Location API Variables*/
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //--------------------------------------------------//

    // Image Picker Code
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            pickMediaHelper.handleCameraResult(success)
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                pickMediaHelper.handleGalleryResult(result.data)
            }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            pickMediaHelper.handlePermissionResult(permissions)
        }

    private lateinit var pickMediaHelper: PickMediaHelper
    //---------------------------------------------------------//


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
        setUpPlaceLocations()
        setDateTimeAttendance()
        setImagePicker()
        getClientList()
        initListeners()
    }

    private fun setUpPlaceLocations() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestCurrentLocation()
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

    private fun setImagePicker() {
        pickMediaHelper = PickMediaHelper(
            this,
            cameraLauncher,
            galleryLauncher,
            permissionLauncher,
            object : PickMediaHelper.Callback {
                override fun onImagePicked(path: String, request: Int) {
                    if (!path.isNullOrBlank()) {
                        binding.hasPreviewImage = true
                        var bitmap = pickMediaHelper.decodeBitmap(path)
                        //binding.capturedImagePreview.setImageBitmap(bitmap)
                    }
                }

                override fun onError(message: String) {
                    binding.hasPreviewImage = false
                }
            })
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
        getTypeListByProject(selectedProject.id)
    }

    private fun onTypeSelected(selectedType: TypeData) {
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()
        binding.activitiesEt.setText("")
        viewModel.selectedTypeIdList?.add(selectedType.id)
        binding.typeEt.setText(selectedType.name)
        viewModel.selectedProjectId?.let { getActivityListByProjectType(it, selectedType.id) }
    }

    override fun subscribeToEvents(vm: AddSignInVM) {
        binding.vm = vm

        vm.clickEvents.observe(this) {
            when (it) {
                AddSignInClickEvents.ON_CAMERA_CLICK -> {
                    pickMediaHelper.showDialog()
                }

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
                    dismissProgress()
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    }else {
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
    private fun <T> showSelectionDialog(
        items: List<T>,
        title: String,
        layoutResId: Int,
        bind: (view: android.view.View, item: T) -> Unit,
        onItemSelected: (T) -> Unit,
        filterCondition: (T, String) -> Boolean,
        emptyMessage: String,
        retryAction: () -> Unit,
        tag: String
    ) {
        if (items.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = items,
                layoutResId = layoutResId,
                bind = bind,
                onItemSelected = {
                    onItemSelected(it)
                },
                filterCondition = filterCondition,
                title = title
            )
            dialog.show(supportFragmentManager, tag)
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                emptyMessage,
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ -> retryAction() },
            )
        }
    }

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
                client.companyName.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
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
                project.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
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
                viewModel.selectedPoNumberId = it.id
                binding.poEt.setText(it.poNumber)
            },
            filterCondition = { poNumber, query ->
                poNumber.poNumber.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
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
                circle.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
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
                site.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_sites_available),
            retryAction = { getSiteListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "SiteSelectionDialog"
        )
    }

    private fun showTypeSelectionDialog(types: List<TypeData>) {
        showSelectionDialog(
            items = types,
            title = getString(R.string.select_type),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, type ->
                view.findViewById<TextView>(R.id.text1).text = type.name
            },
            onItemSelected = {
                binding.typeEt.error = null
                onTypeSelected(it)
            },
            filterCondition = { type, query ->
                type.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_types_available),
            retryAction = { getTypeListByProject(viewModel.selectedProjectId ?: 0L) },
            tag = "TypeSelectionDialog"
        )
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
                    activity.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
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
                    getActivityListByProjectType(
                        viewModel.selectedProjectId ?: 0L,
                        viewModel.selectedTypeIdList?.firstOrNull() ?: 0L
                    )
                },
            )
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
    private fun getTypeListByProject(projectId: Long) {
        viewModel.getTypeListByProject(projectId)
    }

    /*
    * Get Activity list by project id and type id
    * */
    private fun getActivityListByProjectType(projectId: Long, typeId: Long) {
        viewModel.getActivityListByProjectType(projectId, typeId)
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        if (PermissionUtils.hasLocationPermissions(this)) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        var locationString =
                            location?.latitude.toString() + " " + location?.longitude.toString()
                        binding.locationString = locationString
                    } ?: run {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                    }
                }

        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        when {
            PermissionUtils.hasLocationPermissions(this) -> {
                requestCurrentLocation()
            }

            else -> {
                requestPermissionLauncher.launch(PermissionUtils.LOCATION_PERMISSIONS)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { entry ->
                val permissionName = entry.key
                val isGranted = entry.value
                if (isGranted) {
                    requestCurrentLocation()
                } else {
                    showLocationPermissionDialog()
                }
            }
        }

    private fun showLocationPermissionDialog() {
        alertDialogShow(this,
            getString(R.string.warning_gps_needed),
            getString(R.string.warning_location_permission),
            getString(R.string.go_to_settings),
            okLister = DialogInterface.OnClickListener { p0, p1 ->
                Utils.openAppSettings(this)
            },
            canelLister = DialogInterface.OnClickListener { p0, p1 ->
            }
        )
    }
}

