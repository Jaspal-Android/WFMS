package com.atvantiq.wfms.ui.screens.attendance.assignedTasks

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityAssignedTaskDetailBinding
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.work.workDetail.Type
import com.atvantiq.wfms.models.work.workDetail.WorkDetailData
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkTypeAdapter
import com.atvantiq.wfms.ui.screens.attendance.AttendanceViewModel
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.endWork.EndWorkBottomSheet
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.startWork.StartWorkBottomSheet
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class AssignedTaskDetailActivity :
    BaseActivity<ActivityAssignedTaskDetailBinding, AttendanceViewModel>() {

    private var itemPosition: Int = -1
    private var workSiteId: Long? = null
    private var projectId: Long? = null
    private var itemTypeAdapter: WorkTypeAdapter? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var workActionInFlight = false
    private var pendingLocationPermissionAction: (() -> Unit)? = null

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(
            R.layout.activity_assigned_task_detail,
            AttendanceViewModel::class.java
        )

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        initListeners()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupWokTypeRecyclerView()
        setupSelectAllCheckbox()
        fetchIntentData()
    }

    private fun setToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.tvBack.text = getString(R.string.details)
    }

    private fun initListeners() {
        binding.btnAccept.setOnClickListener {
            val id = validWorkSiteIdOrShowError() ?: return@setOnClickListener
            if (!beginWorkAction()) return@setOnClickListener
            viewModel.workAccept(id, position = itemPosition)
        }
        binding.btnStartWork.setOnClickListener {
            val id = validWorkSiteIdOrShowError() ?: return@setOnClickListener
            if (!beginWorkAction()) return@setOnClickListener
            checkAttendanceStatus(id, position = itemPosition)
        }
        binding.btnEndWork.setOnClickListener {
            val id = validWorkSiteIdOrShowError() ?: return@setOnClickListener
            val selectedTypes = itemTypeAdapter?.getSelectedTypes().orEmpty()
            if (selectedTypes.isEmpty()) {
                showToast(this, getString(R.string.select_type_to_end_work))
                return@setOnClickListener
            }
            if (!beginWorkAction()) return@setOnClickListener
            endWorkWithLocationPermissions(
                id,
                selectedTypes,
                itemPosition
            )
        }
    }

    private fun fetchIntentData() {
        workSiteId = intent.getLongExtra(SharingKeys.WORK_ID, -1)
        if ((workSiteId ?: -1L) > 0L) {
            itemPosition = intent.getIntExtra(SharingKeys.WORK_POSITION, -1)
            viewModel.itemPosition.value = itemPosition
            getWorkDetailsById()
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.something_went_wrong)
            )
        }
    }

    private fun getWorkDetailsById() {
        validWorkSiteIdOrShowError()?.let { viewModel.workById(it) }
    }

    private fun setupWokTypeRecyclerView() {
        itemTypeAdapter = WorkTypeAdapter { allSelected ->
            binding.cbSelectAllWorkTypes.setOnCheckedChangeListener(null)
            binding.cbSelectAllWorkTypes.isChecked = allSelected.isNullOrEmpty().not()
            binding.cbSelectAllWorkTypes.setOnCheckedChangeListener { _, isChecked ->
                itemTypeAdapter?.setAllSelected(isChecked)
            }
        }
        binding.rvWorkTypes.apply {
            layoutManager = LinearLayoutManager(this@AssignedTaskDetailActivity)
            adapter = itemTypeAdapter
        }
    }

    private fun setupSelectAllCheckbox() {
        binding.cbSelectAllWorkTypes.setOnCheckedChangeListener { _, isChecked ->
            itemTypeAdapter?.setAllSelected(isChecked)
        }
    }

    private fun setupUI(record: WorkDetailData?) {
        projectId = record?.project?.id
        binding.tvProject.text = record?.project?.name ?: getString(R.string.not_available)
        binding.siteStatusInteger = record?.status?.code
        binding.tvCircle.text = record?.circle?.name ?: getString(R.string.not_available)
        binding.tvSiteName.text = record?.name ?: getString(R.string.not_available)
        binding.tvSiteCode.text = record?.siteId ?: getString(R.string.not_available)
        val canRestart = record?.canRestart ?: false

        when (record?.status?.code) {
            StatusCodes.OPEN -> {
                binding.isOpenAssignment = true
                binding.isAcceptedAssignment = false
                binding.showEndAssignment = false
            }

            StatusCodes.ACCEPTED -> {
                binding.isOpenAssignment = false
                binding.isAcceptedAssignment = true
                binding.showEndAssignment = false
            }

            StatusCodes.WIP -> {
                binding.isOpenAssignment = false
                binding.isAcceptedAssignment = canRestart
                if (canRestart) binding.showEndAssignment = false
            }

            StatusCodes.COMPLETED -> {
                binding.isOpenAssignment = false
                binding.isAcceptedAssignment = false
                binding.showEndAssignment = false
            }

            else -> {
                binding.isOpenAssignment = false
                binding.isAcceptedAssignment = false
                binding.showEndAssignment = false
            }

        }

        val hasEligibleToEnd = record?.status?.code in listOf(StatusCodes.WIP) && record?.canRestart == false

        if (record?.type?.isNullOrEmpty() == true) {
            binding.showSelectAll = false
        } else {
            val hasOpenWorkType = record?.type?.any { it.status?.code == StatusCodes.WIP && (it.endedToday != true)}

            if (hasOpenWorkType == true && hasEligibleToEnd) {
                binding.showSelectAll = true
                binding.showEndAssignment = true
            }else{
                binding.showSelectAll = false
                binding.showEndAssignment = false
            }
        }
        itemTypeAdapter?.setData(record?.type ?: emptyList(), hasEligibleToEnd)
    }


    override fun subscribeToEvents(vm: AttendanceViewModel) {

        vm.workByIdResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    if (response.response?.code == 200) {
                        setupUI(response.response.data)
                    } else if (response.response?.code == 401) {
                        tokenExpiresAlert()
                    } else {
                        alertDialogShow(
                            this,
                            getString(R.string.alert),
                            response.response?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    val throwable = response.throwable
                    if (throwable is HttpException && throwable.code() == 401) {
                        tokenExpiresAlert()
                    } else {
                        showToast(
                            this,
                            throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }

        vm.workAcceptResponse.observe(this) { response ->
            handleAcceptWorkResponse(response, R.string.work_accepted)
        }

        vm.workStartResponse.observe(this) { response ->
            handleStartWorkResponse(response, R.string.work_started)
        }

        vm.attendanceCheckInStatusResponse.observe(this) { response ->
            handleAttendanceCheckInResponse(response)
        }

        vm.workEndResponse.observe(this) { response ->
            handleWorkEndResponse(response)
        }

    }


    private fun handleAcceptWorkResponse(
        response: ApiState<WorkDetailResponse>,
        successMessage: Int,
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                finishWorkAction()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(this, it.message ?: getString(successMessage))
                        handleStatusUpdateResponse(it.data, true)
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }

            Status.ERROR -> {
                finishWorkAction()
                handleError(response.throwable)
            }
            Status.LOADING -> showProgress()
        }
    }

    private fun handleStartWorkResponse(
        response: ApiState<WorkDetailResponse>,
        successMessage: Int,
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                finishWorkAction()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(this, it.message ?: getString(successMessage))
                        it.data?.canRestart = false
                        handleStatusUpdateResponse(it.data)
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }

            Status.ERROR -> {
                finishWorkAction()
                handleError(response.throwable)
            }
            Status.LOADING -> showProgress()
        }
    }

    private fun handleWorkEndResponse(response: ApiState<WorkDetailResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                finishWorkAction()
                response.response?.let {
                    when (it.code) {
                        200 -> {
                            showToast(this, it.message ?: getString(R.string.work_ended))
                            handleStatusUpdateResponse(it.data, isEndWorkCase = true)
                        }
                        206 -> {
                            showToast(this, it.message ?: getString(R.string.work_ended))
                            handleStatusUpdateResponse(it.data, isEndWorkCase = true)
                        }
                        else -> {
                            handleErrorResponse(it.code, it.message)
                        }
                    }
                }
            }

            Status.ERROR -> {
                finishWorkAction()
                handleError(response.throwable)
            }
            Status.LOADING -> showProgress()
        }
    }


    private fun handleStatusUpdateResponse(data: WorkDetailData?, isEndWorkCase: Boolean = false) {
        val resultIntent = Intent().apply {
            putExtra(SharingKeys.WORK_POSITION, viewModel.itemPosition.value)
            putExtra(
                SharingKeys.UPDATED_STATUS,
                data?.status?.code
            )
        }
        setResult(RESULT_OK, resultIntent)
        /*Refreshing UI Date*/
        if (isEndWorkCase) {
            getWorkDetailsById()
        } else {
            setupUI(data)
        }
    }

    private fun checkAttendanceStatus(id: Long, position: Int) {
        viewModel.currentWorkId = id
        viewModel.itemPosition.value = position // Set the current item position
        viewModel.checkInStatusAttendance()
    }

    private fun handleAttendanceCheckInResponse(response: ApiState<CheckInStatusResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200 && it.data?.checkedIn == true) {
                        finishWorkAction()
                        startWorkWithLocationPermissions(
                            viewModel.currentWorkId ?: -1,
                            viewModel.itemPosition.value ?: -1
                        )
                    } else {
                        finishWorkAction()
                        alertDialogShow(
                            this,
                            getString(R.string.alert),
                            getString(R.string.please_check_in_first),
                            okLister = DialogInterface.OnClickListener { dialog, _ ->
                                dialog.dismiss()
                                setResult(ValConstants.RESULT_MARK_ATTENDANCE)
                                finish()
                            })
                    }
                }
            }

            Status.ERROR -> {
                finishWorkAction()
                handleError(response.throwable)
            }
            Status.LOADING -> showProgress()
        }
    }

    private fun handleErrorResponse(code: Int?, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(
            this,
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong)
        )
    }

    private fun handleError(throwable: Throwable?) {
        dismissProgress()
        finishWorkAction()
        if (throwable is HttpException && throwable.code() == 401) {
            tokenExpiresAlert()
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {
                val pendingAction = pendingLocationPermissionAction
                pendingLocationPermissionAction = null
                pendingAction?.invoke()
            }

            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> {
                pendingLocationPermissionAction = null
                finishWorkAction()
                showPermissionDeniedPermanently()
            }

            else -> {
                pendingLocationPermissionAction = null
                finishWorkAction()
                showPermissionRationale()
            }
        }
    }

    private fun openApplicationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", this.packageName, null)
        startActivity(intent)
    }

    private fun handleLocationPermissions(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit = { showPermissionRationale() }
    ) {
        val permissions = getRequiredPermissions()
        when {
            hasAllPermissions(permissions) -> onPermissionsGranted()
            permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                pendingLocationPermissionAction = onPermissionsGranted
                onPermissionsDenied()
            }
            else ->{
                pendingLocationPermissionAction = onPermissionsGranted
                Utils.showBackgroundLocationDisclosureDialog(this,getString(R.string.location_permission_needed),getString(R.string.start_end_work_location_permission_msg)){
                    permissionLauncher.launch(permissions)
                }
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.retry) { _, _ ->
                // Launch permission request after showing rationale
                permissionLauncher.launch(getRequiredPermissions())
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                pendingLocationPermissionAction = null
                finishWorkAction()
            }
            .show()
    }

    private fun showPermissionDeniedPermanently() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_permanently)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openApplicationSettings()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> finishWorkAction() }
            .show()
    }


    @SuppressLint("MissingPermission")
    private fun startWorkWithLocationPermissions(workSiteId: Long, position: Int) {
        handleLocationPermissions(
            onPermissionsGranted = {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    finishWorkAction()
                    if (location != null) {
                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        StartWorkBottomSheet.newInstance(latitude, longitude).apply {
                            onImageSelected = { imagePath ->
                                viewModel.workStart(
                                    workSiteId.toString(),
                                    latitude,
                                    longitude,
                                    imagePath,
                                    position
                                )
                            }
                        }.show(supportFragmentManager, "START_WORK_BOTTOM_SHEET_TAG")
                    } else {
                        showToast(this, getString(R.string.location_not_found))
                    }
                }.addOnFailureListener {
                    finishWorkAction()
                    showToast(this, getString(R.string.location_error))
                }
            }
        )
    }


    @SuppressLint("MissingPermission")
    private fun endWorkWithLocationPermissions(
        workSiteId: Long,
        types: List<Type>,
        position: Int
    ) {
        handleLocationPermissions(
            onPermissionsGranted = {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    finishWorkAction()
                    if (location != null) {
                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        val bottomSheet = EndWorkBottomSheet.newInstance(
                            latitude = latitude,
                            longitude = longitude,
                            workId = workSiteId,
                            projectId = projectId ?: -1
                        )
                        bottomSheet.onSubmitDetails = { statusId, remarks ->
                            viewModel.workEnd(
                                workSiteId, latitude.toDouble(), longitude.toDouble(),
                                types, statusId, remarks, position, null
                            )
                        }
                        bottomSheet.onMaterialFlowCompleted = { statusId, remarks, usedMaterials ->
                            viewModel.workEnd(
                                workSiteId, latitude.toDouble(), longitude.toDouble(),
                                types, statusId, remarks, position, usedMaterials
                            )
                        }
                        bottomSheet.show(supportFragmentManager, "EndWorkBottomSheet")
                    } else {
                        showToast(this, getString(R.string.location_not_found))
                    }
                }.addOnFailureListener {
                    finishWorkAction()
                    showToast(this, getString(R.string.location_error))
                }
            }
        )
    }

    private fun beginWorkAction(): Boolean {
        if (workActionInFlight) return false
        workActionInFlight = true
        setWorkButtonsEnabled(false)
        return true
    }

    private fun finishWorkAction() {
        if (!workActionInFlight) return
        workActionInFlight = false
        setWorkButtonsEnabled(true)
    }

    private fun setWorkButtonsEnabled(enabled: Boolean) {
        listOf(binding.btnAccept, binding.btnStartWork, binding.btnEndWork).forEach { button ->
            button.isEnabled = enabled
            button.alpha = if (enabled) 1f else 0.55f
        }
    }

    private fun validWorkSiteIdOrShowError(): Long? {
        val id = workSiteId
        if (id != null && id > 0L) return id
        showToast(this, getString(R.string.something_went_wrong))
        return null
    }

}
