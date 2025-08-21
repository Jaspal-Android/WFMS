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
                    vm.setProjectLoading(false)
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
                    vm.setProjectLoading(false)
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
                    vm.setProjectLoading(true)
                }
            }
        }

        vm.poNumberListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.setPoLoading(false)
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
                    vm.setPoLoading(false)
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
                    vm.setPoLoading(true)
                }
            }
        }

        vm.circleListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.setCircleLoading(false)
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
                    vm.setCircleLoading(false)
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
                    vm.setCircleLoading(true)
                }
            }
        }

        vm.siteListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.setSiteLoading(false)
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
                    vm.setSiteLoading(false)
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
                    vm.setSiteLoading(true)
                }
            }
        }

        vm.typeListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.setTypeLoading(false)
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
                    vm.setTypeLoading(false)
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
                    vm.setTypeLoading(true)
                }
            }
        }

        vm.activityListByProjectTypeResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    vm.setActivityLoading(false)
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
                    vm.setActivityLoading(false)
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
                    vm.setActivityLoading(true)
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


    private fun showClientSelectionDialog(clients: List<Client>) {
        if (clients.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = clients,
                layoutResId = R.layout.item_generic_adapter,
                bind = { view, client ->
                    view.findViewById<TextView>(R.id.text1).text = client.companyName
                },
                onItemSelected = { selectedClient ->
                    binding.clientEt.error = null
                    onClientSelected(selectedClient)
                },
                filterCondition = { client, query ->
                    client.companyName.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_client)
            )
            dialog.show(supportFragmentManager, "ClientSelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_clients_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getClientList()
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
            )
        }
    }

    private fun showProjectSelectionDialog(projects: List<ProjectData>) {
        if (projects.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = projects,
                layoutResId = R.layout.item_generic_adapter,
                bind = { view, project ->
                    view.findViewById<TextView>(R.id.text1).text = project.name
                },
                onItemSelected = { selectedProject ->
                    binding.projectEt.error = null
                    onProjectSelected(selectedProject)
                },
                filterCondition = { project, query ->
                    project.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_project)
            )
            dialog.show(supportFragmentManager, "ClientSelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_projects_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getProjectListByClientId(viewModel.selectedClient?.id ?: 0L)
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
            )
        }
    }

    private fun showPOSelectionDialog(poNumbers: List<PoData>) {
        if (poNumbers.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = poNumbers,
                layoutResId = R.layout.item_generic_adapter,
                bind = { view, poNumber ->
                    view.findViewById<TextView>(R.id.text1).text = poNumber.poNumber
                },
                onItemSelected = { selectedPoNumber ->
                    binding.poEt.error = null
                    viewModel.selectedPoNumberId = selectedPoNumber.id
                    binding.poEt.setText(selectedPoNumber.poNumber)
                },
                filterCondition = { poNumber, query ->
                    poNumber.poNumber.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_po_number)
            )
            dialog.show(supportFragmentManager, "ClientSelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_po_numbers_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getPoNumberListByProject(viewModel.selectedProjectId ?: 0L)
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
            )
        }
    }

    private fun showCircleSelectionDialog(circles: List<CircleData>) {
        if (circles.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = circles,
                layoutResId = R.layout.item_generic_adapter,
                bind = { view, circle ->
                    view.findViewById<TextView>(R.id.text1).text = circle.name
                },
                onItemSelected = { selectedCircle ->
                    binding.circleEt.error = null
                    viewModel.selectedCircleId = selectedCircle.id
                    binding.circleEt.setText(selectedCircle.name)
                },
                filterCondition = { circle, query ->
                    circle.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_circle)
            )
            dialog.show(supportFragmentManager, "CircleSelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_circles_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getCircleListByProject(viewModel.selectedProjectId ?: 0L)
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
            )
        }
    }

    private fun showSiteSelectionDialog(sites: List<SiteData>) {
        if (sites.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = sites,
                layoutResId = R.layout.item_generic_adapter,
                bind = { view, site ->
                    view.findViewById<TextView>(R.id.text1).text = site.name
                },
                onItemSelected = { selectedSite ->
                    binding.siteEt.error = null
                    viewModel.selectedSiteId = selectedSite.id
                    binding.siteEt.setText(selectedSite.name)
                },
                filterCondition = { site, query ->
                    site.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_site)
            )
            dialog.show(supportFragmentManager, "SiteSelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_sites_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getSiteListByProject(viewModel.selectedProjectId ?: 0L)
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
            )
        }
    }

    private fun showTypeSelectionDialog(types: List<TypeData>) {
        if (types.isNotEmpty()) {
            val dialog = GenericBottomSheetDialog(
                context = this,
                items = types,
                layoutResId = R.layout.item_generic_adapter,
                bind = { view, type ->
                    view.findViewById<TextView>(R.id.text1).text = type.name
                },
                onItemSelected = { selectedType ->
                    binding.typeEt.error = null
                    onTypeSelected(selectedType)
                },
                filterCondition = { type, query ->
                    type.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                },
                title = getString(R.string.select_type)
            )
            dialog.show(supportFragmentManager, "TypeSelectionDialog")
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_types_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    getTypeListByProject(viewModel.selectedProjectId ?: 0L)
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
            )
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
                    val textView = view.findViewById<TextView>(R.id.textView)
                    textView.text = activity.name
                    val checkBox = view.findViewById<CheckBox>(R.id.checkBox)
                    checkBox.isChecked = isSelected
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
                    getActivityListByProjectType(
                        viewModel.selectedProjectId ?: 0L,
                        viewModel.selectedTypeIdList?.firstOrNull() ?: 0L
                    )
                },
                canelLister = DialogInterface.OnClickListener { _, _ ->

                }
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
