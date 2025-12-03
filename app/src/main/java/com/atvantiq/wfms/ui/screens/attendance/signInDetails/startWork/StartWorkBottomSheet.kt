package com.atvantiq.wfms.ui.screens.attendance.signInDetails.startWork

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetStartWorkBinding
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StartWorkBottomSheet(var latitude:String,var longitude:String,var onImageSelected:(path:String)->Unit) : BottomSheetDialogFragment() {
	
	lateinit var binding: BottomSheetStartWorkBinding
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
			DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_start_work, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setImagePicker()
		setLocationLatLon()
		initListeners()
	}

	private fun setLocationLatLon(){
		binding.locationString = "$latitude $longitude"
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
	}

	private fun initListeners() {
		binding.apply {
			binding.btnPhoto.setOnClickListener{
				pickMediaHelper.onlyCameraMedia()
			}
			binding.btnDone.setOnClickListener {
				if (imagePath.isNullOrBlank()) {
					binding.hasPreviewImage = false
					binding.showImageError = true
				} else {
					binding.showImageError = false
					onImageSelected(imagePath!!)
					dismiss()
				}
			}
			binding.btnCancel.setOnClickListener {
				dismiss()
			}
		}
	}

}