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

    private lateinit var clientAdapter: AutoCompleteTempAdapter<Client>
    private lateinit var projectAdapter: AutoCompleteTempAdapter<ProjectData>
    private lateinit var poAdapter: AutoCompleteTempAdapter<PoData>
    private lateinit var circleAdapter: AutoCompleteTempAdapter<CircleData>
    private lateinit var siteAdapter: AutoCompleteTempAdapter<SiteData>
    private lateinit var typeAdapter: AutoCompleteTempAdapter<TypeData>
    private lateinit var activityAdapter: AutoCompleteTempAdapter<ActivityData>

    //---------------------------------------------------//

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
            //binding.clientEt.showDropDown()
            showClientSelectionDialog(viewModel.clients)
        }

       /* binding.clientEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.clientEt.showDropDown()
            }
        }*/

       /* binding.clientEt.setOnItemClickListener { parent, view, position, id ->
            binding.clientEt.error = null
            val selectedClient = parent.getItemAtPosition(position) as Client
            onClientSelected(selectedClient)
        }*/


        binding.projectEt.setOnClickListener {
            showProjectSelectionDialog(viewModel.projects)
        }
        /*binding.projectEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.projectEt.showDropDown()
            }
        }

        binding.projectEt.setOnItemClickListener { parent, view, position, id ->
            binding.projectEt.error = null
            val selectedProject = parent.getItemAtPosition(position) as ProjectData
            onProjectSelected(selectedProject)
        }*/

        binding.poEt.setOnClickListener {
           // binding.poEt.showDropDown()
            showPOSelectionDialog(viewModel.poNumbers)
        }

       /* binding.poEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.poEt.showDropDown()
            }
        }

        binding.poEt.setOnItemClickListener { parent, view, position, id ->
            binding.poEt.error = null
            val selectedPo = parent.getItemAtPosition(position) as PoData
            viewModel.selectedPoNumberId = selectedPo.id
        }*/

        binding.circleEt.setOnClickListener {
           // binding.circleEt.showDropDown()
            showCircleSelectionDialog(viewModel.circles)
        }

       /* binding.circleEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.circleEt.showDropDown()
            }
        }

        binding.circleEt.setOnItemClickListener { parent, view, position, id ->
            val selectedCircle = parent.getItemAtPosition(position) as CircleData
            viewModel.selectedCircleId = selectedCircle.id
        }*/

        binding.siteEt.setOnClickListener {
            //binding.siteEt.showDropDown()
            showSiteSelectionDialog(viewModel.sites)
        }
       /* binding.siteEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.siteEt.showDropDown()
            }
        }
        binding.siteEt.setOnItemClickListener { parent, view, position, id ->
            binding.siteEt.error = null
            val selectedSite = parent.getItemAtPosition(position) as SiteData
            viewModel.selectedSiteId = selectedSite.id
        }*/

        binding.typeEt.setOnClickListener {
            //binding.typeEt.showDropDown()
            showTypeSelectionDialog(viewModel.types)
        }

      /*  binding.typeEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.typeEt.showDropDown()
            }
        }

        binding.typeEt.setOnItemClickListener { parent, view, position, id ->
            binding.typeEt.error = null
            val selectedType = parent.getItemAtPosition(position) as TypeData
            onTypeSelected(selectedType)
        }*/

        binding.activitiesEt.setOnClickListener {
            //binding.activitiesEt.showDropDown()
            if (viewModel.selectedTypeIdList.isNullOrEmpty()) {
                binding.activitiesEt.error = getString(R.string.select_type)
                showToast(this, getString(R.string.select_site))
            } else {
                showActivitySelectionDialog(viewModel.activities)
            }
        }
       /* binding.activitiesEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.activitiesEt.showDropDown()
            }
        }
        binding.activitiesEt.setOnItemClickListener { parent, view, position, id ->
            binding.activitiesEt.error = null
            val selectedActivity = parent.getItemAtPosition(position) as ActivityData
            viewModel.selectedActivityIdList?.add(selectedActivity.id)
        }*/

    }

    private fun onClientSelected(selectedClient: Client) {
        viewModel.selectedClient = selectedClient
        binding.clientEt.setText(selectedClient.companyName)
        // Clear dependent fields and adapters
        binding.projectEt.setText("")
        binding.poEt.setText("")
        binding.circleEt.setText("")
        binding.siteEt.setText("")
        binding.typeEt.setText("")
        binding.activitiesEt.setText("")
       // binding.projectEt.setAdapter(null)
        //binding.poEt.setAdapter(null)
       // binding.circleEt.setAdapter(null)
       // binding.siteEt.setAdapter(null)
       // binding.typeEt.setAdapter(null)
       // binding.activitiesEt.setAdapter(null)

        viewModel.selectedProjectId = null
        viewModel.selectedPoNumberId = null
        viewModel.selectedCircleId = null
        viewModel.selectedSiteId = null
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()

        // Load new options for the selected client
        getProjectListByClientId(selectedClient.id)
    }

    private fun onProjectSelected(selectedProject: ProjectData) {
        viewModel.selectedProjectId = selectedProject.id
        binding.projectEt.setText(selectedProject.name)
        // Clear dependent fields and adapters
        binding.poEt.setText("")
        binding.circleEt.setText("")
        binding.siteEt.setText("")
        binding.typeEt.setText("")
        binding.activitiesEt.setText("")
        //binding.poEt.setAdapter(null)
       // binding.circleEt.setAdapter(null)
       // binding.siteEt.setAdapter(null)
        //binding.typeEt.setAdapter(null)
       // binding.activitiesEt.setAdapter(null)

        viewModel.selectedPoNumberId = null
        viewModel.selectedCircleId = null
        viewModel.selectedSiteId = null
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()

        // Load new options for the selected project
        getPoNumberListByProject(selectedProject.id)
        getCircleListByProject(selectedProject.id)
        getSiteListByProject(selectedProject.id)
        getTypeListByProject(selectedProject.id)
    }

    private fun onTypeSelected(selectedType: TypeData) {
        viewModel.selectedTypeIdList?.clear()
        viewModel.selectedActivityIdList?.clear()
        binding.activitiesEt.setText("")
       // binding.activitiesEt.setAdapter(null)
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
                           /* clientAdapter = AutoCompleteTempAdapter(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                clients
                            )
                            binding.clientEt.setAdapter(clientAdapter)*/
                            //showClientSelectionDialog(clients)

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
                    dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val projects = response.response?.data ?: emptyList()
                            viewModel.projects = projects

                            /*if (projects.isEmpty()) {
                                showToast(this, getString(R.string.no_projects_found))
                            } else {
                                projectAdapter = AutoCompleteTempAdapter(
                                    this,
                                    android.R.layout.simple_dropdown_item_1line,
                                    projects
                                )
                                binding.projectEt.setAdapter(projectAdapter)
                            }*/
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

        vm.poNumberListByProjectResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    // dismissProgress()
                    when (response.response?.code) {
                        200 -> {
                            // Handle success
                            val poNumbers = response.response?.data ?: emptyList()
                            viewModel.poNumbers = poNumbers
                           /* poAdapter = AutoCompleteTempAdapter(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                poNumbers
                            )
                            binding.poEt.setAdapter(poAdapter)*/
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
                    //dismissProgress()
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
                            viewModel.circles = circles

                            /* circleAdapter = AutoCompleteTempAdapter(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                circles
                            )
                            binding.circleEt.setAdapter(circleAdapter)*/
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
                    // dismissProgress()
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
                            viewModel.sites = sites
                            /*siteAdapter = AutoCompleteTempAdapter(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                sites
                            )
                            binding.siteEt.setAdapter(siteAdapter)*/
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
                    //dismissProgress()
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
                            viewModel.types = types
                            /*typeAdapter = AutoCompleteTempAdapter(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                types
                            )
                            binding.typeEt.setAdapter(typeAdapter)*/
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
                    //dismissProgress()
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
                            viewModel.activities = activities
                            /*activityAdapter = AutoCompleteTempAdapter(
                                this,
                                android.R.layout.simple_dropdown_item_1line,
                                activities
                            )
                            binding.activitiesEt.setAdapter(activityAdapter)
                            binding.activitiesEt.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())*/
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
                    //dismissProgress()
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
                }
            )
            dialog.show(supportFragmentManager, "ClientSelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
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
                }
            )
            dialog.show(supportFragmentManager, "ClientSelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.no_projects_found), Toast.LENGTH_SHORT).show()
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
                    viewModel.selectedPoNumberId = selectedPoNumber.id
                    binding.poEt.setText(selectedPoNumber.poNumber)
                },
                filterCondition = { poNumber, query ->
                    poNumber.poNumber.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                }
            )
            dialog.show(supportFragmentManager, "ClientSelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
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
                    viewModel.selectedCircleId = selectedCircle.id
                    binding.circleEt.setText(selectedCircle.name)
                },
                filterCondition = { circle, query ->
                    circle.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                }
            )
            dialog.show(supportFragmentManager, "CircleSelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
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
                    viewModel.selectedSiteId = selectedSite.id
                    binding.siteEt.setText(selectedSite.name)
                },
                filterCondition = { site, query ->
                    site.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                }
            )
            dialog.show(supportFragmentManager, "SiteSelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
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
                }
            )
            dialog.show(supportFragmentManager, "TypeSelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
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
                    updateSelectedActivities(selectedActivities)
                },
                onSubmit = { selectedActivities ->
                    updateSelectedActivities(selectedActivities)
                },
                filterCondition = { activity, query ->
                    activity.name.lowercase(Locale.getDefault())
                        .contains(query.lowercase(Locale.getDefault()))
                }
            )
            dialog.show(supportFragmentManager, "ActivitySelectionDialog")
        } else {
            Toast.makeText(this, getString(R.string.not_available), Toast.LENGTH_SHORT).show()
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
