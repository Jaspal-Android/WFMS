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

class StartWorkBottomSheet : BottomSheetDialogFragment() {

	// latitude/longitude survive recreation via arguments; the image callback is
	// re-wired by the host. A no-arg constructor prevents the FragmentManager from
	// throwing InstantiationException on process-death/config-change restore.
	private var latitude: String = ""
	private var longitude: String = ""
	var onImageSelected: ((path: String) -> Unit)? = null

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

	private val photoPickerLauncher =
		registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
			pickMediaHelper.handlePhotoPickerResult(uri)
		}

	private lateinit var pickMediaHelper: PickMediaHelper
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
		arguments?.let {
			latitude = it.getString(ARG_LATITUDE).orEmpty()
			longitude = it.getString(ARG_LONGITUDE).orEmpty()
		}
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
			binding.btnPhoto.setOnClickListener{
				pickMediaHelper.onlyCameraMedia()
			}
			binding.btnDone.setOnClickListener {
				if (imagePath.isNullOrBlank()) {
					binding.hasPreviewImage = false
					binding.showImageError = true
				} else {
					binding.showImageError = false
					onImageSelected?.invoke(imagePath!!)
					dismiss()
				}
			}
			binding.btnCancel.setOnClickListener {
				dismiss()
			}
		}
	}

	companion object {
		private const val ARG_LATITUDE = "latitude"
		private const val ARG_LONGITUDE = "longitude"

		fun newInstance(latitude: String, longitude: String): StartWorkBottomSheet {
			return StartWorkBottomSheet().apply {
				arguments = Bundle().apply {
					putString(ARG_LATITUDE, latitude)
					putString(ARG_LONGITUDE, longitude)
				}
			}
		}
	}

}
