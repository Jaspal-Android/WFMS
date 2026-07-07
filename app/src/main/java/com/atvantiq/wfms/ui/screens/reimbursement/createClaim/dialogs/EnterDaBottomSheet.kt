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
import com.atvantiq.wfms.databinding.BottomSheetStartWorkBinding
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.atvantiq.wfms.widgets.BaseBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EnterDaBottomSheet(var title:String,var onDataSubmitted:(amount:String,path:String)->Unit) : BaseBottomSheet() {
	
	lateinit var binding: BottomSheetEnterDaBinding
	private var imagePath: String? = null

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

	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding =
			DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_enter_da, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		binding.tvTitle.text = title
		setImagePicker()
		initListeners()
	}

	private fun setImagePicker(){
		pickMediaHelper = PickMediaHelper(requireContext(), cameraLauncher, galleryLauncher, permissionLauncher, object : PickMediaHelper.Callback {
			override fun onImagePicked(path: String, request: Int) {
				if(path.isNotBlank()){
					imagePath = pickMediaHelper.compressImageTo1MB(path) ?: path
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
				// Amount validation (do not require attachment)
				val amountRaw = runCatching { binding.amountEt.text?.toString() }.getOrNull()
					?.trim()
					.takeUnless { it.isNullOrEmpty() }


				val parsedAmount = amountRaw?.toDoubleOrNull()
				if (amountRaw.isNullOrBlank() || parsedAmount == null || parsedAmount <= 0.0) {
					runCatching { binding.amountEt.error = getString(R.string.enter_valid_amount) }
					return@setOnClickListener
				}

				binding.showImageError = false
				binding.hasPreviewImage = !imagePath.isNullOrBlank()

				// Submit the raw validated string (amountRaw is smart-cast non-null here), not
				// parsedAmount.toString(): the Double round-trip corrupted currency
				// ("10.20"->"10.2", ">=10M"->scientific notation).
				onDataSubmitted.invoke(amountRaw, imagePath ?: "")
				dismiss()
			}
			binding.btnCancel.setOnClickListener {
				dismiss()
			}
		}
	}

}
