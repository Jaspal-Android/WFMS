package com.atvantiq.wfms.ui.screens.attendance.applyLeave

import android.view.View
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityApplyLeaveBinding
import com.atvantiq.wfms.models.attendance.applyLeave.ApplyLeaveResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.dialogs.SimpleBottomSheetDialog
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class ApplyLeaveActivity : BaseActivity<ActivityApplyLeaveBinding, ApplyLeaveVM>() {

    /*Image Selection Code-------------------------------------------------*/
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

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            pickMediaHelper.handlePhotoPickerResult(uri)
        }

    private lateinit var pickMediaHelper: PickMediaHelper
    /*----------------------------------------------------------------------*/

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_apply_leave, ApplyLeaveVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        binding.applyLeaveToolbar.toolbarTitle.text = getString(R.string.apply_leave)
        binding.applyLeaveToolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setImagePicker()
        pickMediaHelper.restoreState(savedInstanceState)
        // After rotation or Android ending the process, the form comes back from the ViewModel;
        // the attachment's preview is redrawn here.
        if (savedInstanceState != null) showRestoredAttachment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pickMediaHelper.saveState(outState)
    }

    private fun showRestoredAttachment() {
        val attachment = viewModel.leaveAttachmentPath.get().orEmpty()
        if (attachment.isEmpty()) return
        lifecycleScope.launch {
            val preview = withContext(Dispatchers.IO) { pickMediaHelper.decodeBitmap(attachment) }
            if (preview == null) {
                // The prepared file is gone; ask for it again rather than send the leave without it.
                viewModel.leaveAttachmentPath.set("")
                return@launch
            }
            binding.hasPreviewImage = true
            binding.capturedImagePreview.setImageBitmap(preview)
        }
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
                        // The attachment is always the processed file. It used to be set to null
                        // when compression failed, so the leave went in without its certificate.
                        pickMediaHelper.prepareImage(path, lifecycleScope) { prepared ->
                            viewModel.leaveAttachmentPath.set(prepared.uploadPath)
                            binding.hasPreviewImage = true
                            binding.capturedImagePreview.setImageBitmap(prepared.preview)
                        }
                    }
                }

                override fun onError(message: String) {
                    binding.hasPreviewImage = false
                    showToast(this@ApplyLeaveActivity, message)
                }
            })
        pickMediaHelper.setPhotoPickerLauncher( photoPickerLauncher)
    }

    override fun subscribeToEvents(vm: ApplyLeaveVM) {
        binding.vm = vm
        vm.clickEvents.observe(this) { event ->
            handleEvents(event, vm)
        }

        vm.errorHandler.observe(this) { error ->
            handleErrors(error)
        }

        vm.applyLeaveResponse.observe(this,{handleApplyLeaveResponse(it)})
    }

    private fun handleApplyLeaveResponse(response: ApiState<ApplyLeaveResponse>) {
        when (response.status) {
            Status.SUCCESS -> handleApplyLeaveSuccess(response)
            Status.LOADING -> showProgress()
            Status.ERROR -> {
                dismissProgress()
                alertDialogShow(
                    this,
                    getString(R.string.alert),
                    response.throwable?.message.orEmpty()
                )
            }
        }
    }

    private fun handleApplyLeaveSuccess(response: ApiState<ApplyLeaveResponse>) {
        dismissProgress()
        if(response.response?.code == ValConstants.SUCCESS_CODE){
            alertDialogShow(this,response.response.message)
            showToast(this,response.response.message)
            viewModel.clearData()
            binding.hasPreviewImage = false
        } else {
            // The API reports auth failures in the body (HTTP 200 + code 401), which
            // handleRejectedResponse routes to the login dialog.
            handleRejectedResponse(response.response?.code, response.response?.message)
        }
    }

    private fun handleEvents(event: ApplyLeaveClickEvents, vm: ApplyLeaveVM) {
        when (event) {
            ApplyLeaveClickEvents.START_DATE_CLICK -> {
                binding.startDateEt.error = null
                DateUtils.onDateClick(this, object : DateUtils.DateCallBack {
                    override fun onDateSelected(date: String, formatDate: String) {
                        vm.leaveStartDate.set(date)
                    }
                })
            }

            ApplyLeaveClickEvents.END_DATE_CLICK -> {
                binding.endDateEt.error = null
                DateUtils.onDateClick(this, object : DateUtils.DateCallBack {
                    override fun onDateSelected(date: String, formatDate: String) {
                        vm.leaveEndDate.set(date)
                    }
                })
            }

            ApplyLeaveClickEvents.LEAVE_TYPE_CLICK -> {
                binding.leaveTypeEt.error = null
                leaveApplyBottomSheet()

            }

            ApplyLeaveClickEvents.ATTACHMENT_CLICK -> {
                pickMediaHelper.showDialog()
            }

            ApplyLeaveClickEvents.CANCEL_UPLOAD_IMAGE -> {
                vm.leaveAttachmentPath.set("")
                binding.hasPreviewImage = false
            }
        }
    }

    private fun handleErrors(error: ApplyLeaveErrorHandler) {
        when (error) {
            ApplyLeaveErrorHandler.START_DATE_EMPTY -> {
                binding.startDateEt.error = getString(R.string.leave_start_date_required)
                showToast(this, getString(R.string.leave_start_date_required))
            }
            ApplyLeaveErrorHandler.END_DATE_EMPTY -> {
                binding.endDateEt.error = getString(R.string.leave_end_date_required)
                showToast(this, getString(R.string.leave_end_date_required))
            }
            ApplyLeaveErrorHandler.LEAVE_TYPE_EMPTY -> {
                binding.leaveTypeEt.error = getString(R.string.leave_type_required)
                showToast(this, getString(R.string.leave_type_required))
            }
            ApplyLeaveErrorHandler.LEAVE_REASON_EMPTY -> {
                binding.leaveReasonEt.error = getString(R.string.reason_required)
                showToast(this, getString(R.string.reason_required))
            }
            ApplyLeaveErrorHandler.START_DATE_AFTER_END_DATE -> {
                binding.startDateEt.error = getString(R.string.start_must_be_before_end)
                showToast(this, getString(R.string.start_must_be_before_end))
            }

            ApplyLeaveErrorHandler.END_DATE_BEFORE_START_DATE -> {
                binding.endDateEt.error = getString(R.string.end_must_be_after_start)
                showToast(this, getString(R.string.end_must_be_after_start))
            }
        }
    }

    private fun leaveApplyBottomSheet() {
        val simpleBottomSheetDialog = SimpleBottomSheetDialog(
            resources.getStringArray(R.array.leave_types).toMutableList(),
            R.layout.item_generic_adapter,
            { view, item ->
                view.findViewById<TextView>(R.id.text1).text = item
            },
            { selectedItem ->
                viewModel.leaveType.set(selectedItem)
            },
            getString(R.string.select_leave_type)
        )
        simpleBottomSheetDialog.show(supportFragmentManager, "SimpleBottomSheetDialog")
    }
}