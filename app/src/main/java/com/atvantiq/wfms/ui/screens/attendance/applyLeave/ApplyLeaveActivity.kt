package com.atvantiq.wfms.ui.screens.attendance.applyLeave

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityApplyLeaveBinding
import com.atvantiq.wfms.ui.screens.dialogs.SimpleBottomSheetDialog
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.files.PickMediaHelper

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
    private lateinit var pickMediaHelper: PickMediaHelper
    /*----------------------------------------------------------------------*/

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_apply_leave, ApplyLeaveVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        setImagePicker()
    }

    private fun setToolbar() {
        binding.applyLeaveToolbar.toolbarTitle.text = getString(R.string.apply_leave)
        binding.applyLeaveToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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
                        viewModel.leaveAttachmentPath.set(pickMediaHelper.compressImageTo1MB(path))
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

    override fun subscribeToEvents(vm: ApplyLeaveVM) {
        binding.vm = vm
        vm.clickEvents.observe(this) { event ->
            handleEvents(event, vm)
        }

        vm.errorHandler.observe(this) { error ->
            handleErrors(error)
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

    private fun leaveTypeList(): MutableList<String> {
        val list = resources.getStringArray(R.array.leave_types).toMutableList()
        return list
    }

    private fun leaveApplyBottomSheet() {
        var simpleBottomSheetDialog = SimpleBottomSheetDialog(
            this,
            leaveTypeList(),
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