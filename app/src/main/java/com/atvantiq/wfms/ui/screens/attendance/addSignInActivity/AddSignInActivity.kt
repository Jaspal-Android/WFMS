package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import AutoCompleteTempAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.widget.MultiAutoCompleteTextView
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
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.PermissionUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException


@AndroidEntryPoint

class AddSignInActivity : BaseActivity<ActivityAddSignInBinding, AddSignInVM>() {

    private lateinit var clientAdapter:AutoCompleteTempAdapter<Client>
    private lateinit var projectAdapter:AutoCompleteTempAdapter<ProjectData>
    private lateinit var poAdapter:AutoCompleteTempAdapter<PoData>
    private lateinit var circleAdapter:AutoCompleteTempAdapter<CircleData>
    private lateinit var siteAdapter:AutoCompleteTempAdapter<SiteData>
    private lateinit var typeAdapter:AutoCompleteTempAdapter<TypeData>
    private lateinit var activityAdapter:AutoCompleteTempAdapter<ActivityData>

    //---------------------------------------------------//

    /*Location API Variables*/
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //--------------------------------------------------//

    // Image Picker Code
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pickMediaHelper.handleCameraResult(success)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pickMediaHelper.handleGalleryResult(result.data)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
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

    private fun setUpPlaceLocations(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestCurrentLocation()
    }

    private fun setUpToolbar() {
        binding.addSignInToolbar.toolbarTitle.text = getString(R.string.self_assigned_work)
        binding.addSignInToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setDateTimeAttendance(){
        binding.dateString = DateUtils.getCurrentDate()
        binding.timeString = DateUtils.getCurrentTime()
    }

    private fun setImagePicker(){
        pickMediaHelper = PickMediaHelper(this, cameraLauncher, galleryLauncher, permissionLauncher, object : PickMediaHelper.Callback {
            override fun onImagePicked(path: String, request: Int) {
                if(!path.isNullOrBlank()){
                    binding.hasPreviewImage = true
                    var bitmap = pickMediaHelper.decodeBitmap(path)
                    binding.capturedImagePreview.setImageBitmap(bitmap)
                }
            }

            override fun onError(message: String) {
                binding.hasPreviewImage = false
            }
        })
    }

    private fun initListeners() {
        binding.clientEt.setOnClickListener {
            binding.clientEt.showDropDown()
        }

        binding.clientEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.clientEt.showDropDown()
            }
        }

        binding.clientEt.setOnItemClickListener { parent, view, position, id ->
            val selectedClient = parent.getItemAtPosition(position) as Client
            viewModel.selectedClient = selectedClient
            getProjectListByClientId(viewModel.selectedClient?.id ?: 0)
        }


        binding.projectEt.setOnClickListener {
            binding.projectEt.showDropDown()
        }

        binding.projectEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.projectEt.showDropDown()
            }
        }

        binding.projectEt.setOnItemClickListener { parent, view, position, id ->
            val selectedProject = parent.getItemAtPosition(position) as ProjectData
            viewModel.selectedProjectId = selectedProject.id
            getPoNumberListByProject(selectedProject.id)
            getCircleListByProject(selectedProject.id)
            getSiteListByProject(selectedProject.id)
            getTypeListByProject(selectedProject.id)
        }

        binding.poEt.setOnClickListener {
            binding.poEt.showDropDown()
        }

        binding.poEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.poEt.showDropDown()
            }
        }

        binding.poEt.setOnItemClickListener { parent, view, position, id ->
            val selectedPo = parent.getItemAtPosition(position) as PoData
            viewModel.selectedPoNumberId = selectedPo.id
        }

        binding.circleEt.setOnClickListener {
            binding.circleEt.showDropDown()
        }

        binding.circleEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.circleEt.showDropDown()
            }
        }

        binding.circleEt.setOnItemClickListener { parent, view, position, id ->
            val selectedCircle = parent.getItemAtPosition(position) as CircleData
            viewModel.selectedCircleId = selectedCircle.id
        }

        binding.siteEt.setOnClickListener {
            binding.siteEt.showDropDown()
        }
        binding.siteEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.siteEt.showDropDown()
            }
        }
        binding.siteEt.setOnItemClickListener { parent, view, position, id ->
            val selectedSite = parent.getItemAtPosition(position) as SiteData
            viewModel.selectedSiteId = selectedSite.id
        }

        binding.typeEt.setOnClickListener {
            binding.typeEt.showDropDown()
        }

        binding.typeEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.typeEt.showDropDown()
            }
        }

        binding.typeEt.setOnItemClickListener { parent, view, position, id ->
            val selectedType = parent.getItemAtPosition(position) as TypeData
            viewModel.selectedTypeIdList?.clear()
            viewModel.selectedActivityIdList?.clear()
            viewModel.selectedTypeIdList?.add(selectedType.id)
            viewModel.selectedProjectId?.let { getActivityListByProjectType(it, selectedType.id) }
        }

        binding.activitiesEt.setOnClickListener {
            binding.activitiesEt.showDropDown()
        }
        binding.activitiesEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.activitiesEt.showDropDown()
            }
        }
        binding.activitiesEt.setOnItemClickListener { parent, view, position, id ->
            val selectedActivity = parent.getItemAtPosition(position) as ActivityData
            viewModel.selectedActivityIdList?.add(selectedActivity.id)
        }

    }



    override fun subscribeToEvents(vm: AddSignInVM) {
        binding.vm = vm
        vm.clickEvents.observe(this) {
            when (it) {
                AddSignInClickEvents.ON_CAMERA_CLICK ->{
                    pickMediaHelper.showDialog()
                }
                AddSignInClickEvents.ON_SAVE_CLICK -> {
                    finish()
                }
                AddSignInClickEvents.ON_CANCEL_CLICK ->{
                    finish()
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
                            clientAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, clients)
                            binding.clientEt.setAdapter(clientAdapter)

                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
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
                    dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val projects = response.response?.data ?: emptyList()
                            projectAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, projects)
                            binding.projectEt.setAdapter(projectAdapter)
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }

        vm.poNumberListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                   // dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val poNumbers = response.response?.data ?: emptyList()
                            poAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, poNumbers)
                            binding.poEt.setAdapter(poAdapter)
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    //dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                   // showProgress()
                }
            }
        }

        vm.circleListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                   // dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            val circles = response.response?.data ?: emptyList()
                            circleAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, circles)
                            binding.circleEt.setAdapter(circleAdapter)
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                   // dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                   // showProgress()
                }
            }
        }

        vm.siteListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    //dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val sites = response.response?.data ?: emptyList()
                            siteAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, sites)
                            binding.siteEt.setAdapter(siteAdapter)
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    //dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    //showProgress()
                }
            }
        }

        vm.typeListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    //dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val types = response.response?.data ?: emptyList()
                            typeAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
                            binding.typeEt.setAdapter(typeAdapter)
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    //dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    //showProgress()
                }
            }
        }

        vm.activityListByProjectTypeResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    //dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val activities = response.response?.data ?: emptyList()
                            activityAdapter = AutoCompleteTempAdapter(this, android.R.layout.simple_dropdown_item_1line, activities)
                            binding.activitiesEt.setAdapter(activityAdapter)
                            binding.activitiesEt.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    //dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    //showProgress()
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
                            showToast(this, response.response?.message ?:getString(R.string.work_assigned_successfully))
                            finish()
                        }
                        401 -> {
                            tokenExpiresAlert()
                        }
                        else -> {
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    if ((response.throwable as HttpException).code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }

    }

    private fun getClientList() {
        viewModel.getClientList()

    }

    /*
    * Get project list by client id
    * */
    private fun getProjectListByClientId(clientId: Int) {
        viewModel.getProjectListByClientId(clientId)

    }

    /*
    * Get PO number list by project id
    * */
    private fun getPoNumberListByProject(projectId: Int) {
        viewModel.getPoNumberListByProject(projectId)
    }

    /*
    * Get Circle list by project id
    * */
    private fun getCircleListByProject(projectId: Int) {
        viewModel.getCircleListByProject(projectId)
    }

    /*
    * Get Site list by project id
    * */
    private fun getSiteListByProject(projectId: Int) {
        viewModel.getSiteListByProject(projectId)
    }

    /*
    * Get Type list by project id
    * */
    private fun getTypeListByProject(projectId: Int) {
        viewModel.getTypeListByProject(projectId)
    }

    /*
    * Get Activity list by project id and type id
    * */
    private fun getActivityListByProjectType(projectId: Int, typeId: Int) {
        viewModel.getActivityListByProjectType(projectId, typeId)
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation(){
        if( PermissionUtils.hasLocationPermissions(this)){
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                       var locationString =  location?.latitude.toString() +" "+location?.longitude.toString()
                        binding.locationString = locationString
                    } ?: run {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                    }
                }

        }else{
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        when {
            PermissionUtils.hasLocationPermissions(this) ->{
                requestCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(PermissionUtils.LOCATION_PERMISSIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
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
// Shikhar@Atvantiq