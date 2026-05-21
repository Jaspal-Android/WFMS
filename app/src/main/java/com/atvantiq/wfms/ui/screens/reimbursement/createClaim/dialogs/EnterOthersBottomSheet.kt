package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.dialogs

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetEnterDaBinding
import com.atvantiq.wfms.databinding.BottomSheetEnterOthersBinding
import com.atvantiq.wfms.databinding.BottomSheetStartWorkBinding
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.atvantiq.wfms.widgets.BaseBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EnterOthersBottomSheet(var onDataSubmitted:(category:String,amount:String, path:String)->Unit) : BaseBottomSheet() {
	
	lateinit var binding: BottomSheetEnterOthersBinding
	private var imagePath: String? = null

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

	private val photoPickerLauncher =
		registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
			pickMediaHelper.handlePhotoPickerResult(uri)
		}

	private lateinit var pickMediaHelper: PickMediaHelper
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding =
			DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_enter_others, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setImagePicker()
		initListeners()
	}

	private fun setImagePicker(){
		pickMediaHelper = PickMediaHelper(requireContext(), cameraLauncher, galleryLauncher, permissionLauncher, object : PickMediaHelper.Callback {
			override fun onImagePicked(path: String, request: Int) {
				if(!path.isNullOrBlank()){
					imagePath = pickMediaHelper.compressImageTo1MB(path)
					binding.hasPreviewImage = true
					var bitmap = pickMediaHelper.decodeBitmap(path)
					binding.capturedImagePreview.setImageBitmap(bitmap)
				}
			}

			override fun onError(message: String) {
				binding.hasPreviewImage = false
			}
		})
		pickMediaHelper.setPhotoPickerLauncher( photoPickerLauncher)
	}

	private fun initListeners() {
		binding.apply {
			binding.btnUploadReceipt.setOnClickListener{
				pickMediaHelper.showDialog()
			}
			binding.btnDone.setOnClickListener {
					// Category validation
				val categoryRaw = runCatching { binding.categoryEt.text?.toString() }.getOrNull()
					?.trim()
					.takeUnless { it.isNullOrEmpty() }

				if (categoryRaw.isNullOrBlank()) {
					runCatching {
						binding.categoryEt.error = getString(R.string.category)
						binding.categoryEt.requestFocus()
					}
					return@setOnClickListener
				}

				// Amount validation (do not require attachment)
				val amountRaw = runCatching { binding.amountEt.text?.toString() }.getOrNull()
					?.trim()
					.takeUnless { it.isNullOrEmpty() }

				val parsedAmount = amountRaw?.toDoubleOrNull()
				val isAmountValid = !amountRaw.isNullOrBlank() && parsedAmount != null && parsedAmount > 0.0

				if (!isAmountValid) {
					runCatching { binding.amountEt.error = getString(R.string.enter_valid_amount) }
					return@setOnClickListener
				}

				binding.showImageError = false
				binding.hasPreviewImage = !imagePath.isNullOrBlank()

				onDataSubmitted.invoke(categoryRaw, parsedAmount.toString(), imagePath ?: "")
				dismiss()
			}
			binding.btnCancel.setOnClickListener {
				dismiss()
			}
		}
	}

}

