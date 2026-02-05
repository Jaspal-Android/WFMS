package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.AppListData
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAddTravelingDetailBinding
import com.atvantiq.wfms.models.empoyeeByCircle.Data
import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.reimbursement.TravelModeOption
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.files.PickMediaHelper
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.util.Locale

@AndroidEntryPoint
class AddTravelingDetailActivity :
    BaseActivity<ActivityAddTravelingDetailBinding, AddTravelDetailViewModel>() {
    var isOutstation: Boolean = false
    private var circleCode: String? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pickMediaHelper.handleCameraResult(success)
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(
            R.layout.activity_add_traveling_detail,
            AddTravelDetailViewModel::class.java
        )

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        handleToolbar()

        setImagePicker()

        isOutstation = intent.getBooleanExtra(SharingKeys.IS_OUTSTATION_CLAIM, false)
        circleCode = intent.extras?.getString(SharingKeys.EXTRA_CIRCLE_CODE)
        val defaultFrom = intent.getStringExtra(SharingKeys.EXTRA_DEFAULT_FROM)
        if (!defaultFrom.isNullOrBlank()) {
            viewModel.fromLocation.set(defaultFrom)
            binding.fromEt.isEnabled = false
        }
    }

    private fun handleToolbar() {
        binding.addTrevelingToolbar.toolbarTitle.text = getString(R.string.add_travel_entry)
        binding.addTrevelingToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: AddTravelDetailViewModel) {
        binding.vm = vm

        vm.clickEvents.observe(this) { event ->
            handleClickEvents(event)
        }

        vm.errorHandler.observe(this){ event->
            handleErrors(event)
        }

        viewModel.employeeByCircleResponse.observe(this) { response ->
            handleEmployeeByCircleResponse(response)
        }
    }

    private fun handleClickEvents(event: AddTravelingClickEvents) {
        when (event) {
            AddTravelingClickEvents.SELECT_TRAVEL_MODE -> {
                val items =
                    if (isOutstation) AppListData.outstationTravelModes else AppListData.localTravelModes
                showTravelModeSelectionDialog(items)
            }

            AddTravelingClickEvents.SELECT_TRAVELING_WITH -> {
                if (viewModel.employeesByCircle.isNotEmpty()) {
                    showTravelWithSelectionDialog(viewModel.employeesByCircle)
                } else {
                    viewModel.employeeByCircle(circleCode ?: "")
                }
            }

            AddTravelingClickEvents.ATTACHMENT_CLICK -> {
                pickMediaHelper.showDialog()
            }

            AddTravelingClickEvents.ON_DONE_CLICK -> {
                handleOnDoneClick()
            }

            AddTravelingClickEvents.ON_CANCEL_CLICK -> {
                finish()
            }
        }
    }

    private fun handleErrors(error: AddTravelDetailsErrorHandler) {
        when(error){
            AddTravelDetailsErrorHandler.ON_EMPTY_TRAVEL_MODE -> {
                binding.travelModeEt.error = getString(R.string.select_travel_mode)
            }
            AddTravelDetailsErrorHandler.ON_EMPTY_FROM_LOCATION -> {
                binding.fromEt.error = getString(R.string.please_enter_from_location)
            }
            AddTravelDetailsErrorHandler.ON_EMPTY_TO_LOCATION -> {
                binding.toEt.error = getString(R.string.please_enter_to_location)
            }
            AddTravelDetailsErrorHandler.ON_EMPTY_TRAVEL_AMOUNT -> {
                binding.amountEt.error = getString(R.string.please_enter_travel_amount)
            }
        }
    }

    private fun handleEmployeeByCircleResponse(response: ApiState<EmployeeByCircleResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    200 -> {
                        val employees = response.response?.data ?: emptyList()
                        viewModel.employeesByCircle = employees
                        showTravelWithSelectionDialog(employees)
                    }

                    else -> {
                        handleErrorResponse(
                            response.response?.code ?: 0,
                            response.response?.message
                        )
                    }
                }
            }

            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun handleErrorResponse(code: Int, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(
            this,
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong)
        )
    }

    private fun handleError(throwable: Throwable?) {
        if (throwable is HttpException) {
            if (throwable.code() == 401) {
                tokenExpiresAlert()
            }
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun showTravelModeSelectionDialog(travelModes: List<TravelModeOption>) {
        showSelectionDialog(
            items = travelModes,
            title = getString(R.string.select_travel_mode),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, item ->
                view.findViewById<TextView>(R.id.text1).text = item.label
            },
            onItemSelected = { selected ->
                binding.travelModeEt.error = null
                viewModel.selectedTravelMode.set(selected)
                viewModel.selectedTravelModeValue.set(selected.label)
            },
            filterCondition = { item, query ->
                item.label.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_data_available),
            retryAction = { },
            tag = "TravelModeSelectionDialog"
        )
    }


    private fun showTravelWithSelectionDialog(employeeList: List<Data>) {
        showSelectionDialog(
            items = employeeList,
            title = getString(R.string.travelingWith),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, item ->
                view.findViewById<TextView>(R.id.text1).text = item.name + " - " + item.code
            },
            onItemSelected = { selected ->
                binding.travelingWithEt.error = null
                binding.travelingWithEt.setText(selected.name + "  -  " + selected.code)
                viewModel.selectedEmployee.set(selected)
            },
            filterCondition = { item, query ->
                item.name?.lowercase(Locale.getDefault())
                    ?.contains(query.lowercase(Locale.getDefault())) ?: false
            },
            emptyMessage = getString(R.string.no_data_available),
            retryAction = { },
            tag = "TravelWithSelectionDialog"
        )
    }

    private fun handleOnDoneClick() {
        if(viewModel.validateTravelDetailOrPostError()){
            val travelExpense = viewModel.createTravelDetail()
            val intent = Intent().apply {
                putExtra(SharingKeys.TRAVELING_DETAILS, travelExpense)
            }
            setResult(RESULT_OK, intent)
            finish()
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
                    if (path.isNotBlank()) {
                        pickMediaHelper.compressImageTo1MB(path)
                        viewModel.attachmentPath.set(path)
                        binding.hasPreviewImage = true
                        var bitmap = pickMediaHelper.decodeBitmap(path)
                        binding.capturedImagePreview.setImageBitmap(bitmap)
                    }
                }

                override fun onError(message: String) {
                    binding.hasPreviewImage = false
                }
            }
        )
        pickMediaHelper.setPhotoPickerLauncher( photoPickerLauncher)
    }
}
