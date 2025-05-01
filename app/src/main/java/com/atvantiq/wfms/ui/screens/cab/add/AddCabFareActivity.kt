package com.atvantiq.wfms.ui.screens.cab.add

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityAddCabFareBinding
import com.atvantiq.wfms.ui.screens.adapters.CustomAutoCompleteAdapter
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInClickEvents
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInVM
import com.atvantiq.wfms.ui.screens.cab.CabClickEvents
import com.atvantiq.wfms.ui.screens.cab.CabViewModel
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.PermissionUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class AddCabFareActivity : BaseActivity<ActivityAddCabFareBinding,CabViewModel>() {

    /*Local variables*/
    private var circleList = listOf("CG", "HP", "HR", "MH", "PB", "RJ", "DL", "UPE")
    private var siteIdList = listOf("445213", "438803", "893438", "343554", "098787")
    private val employeeList = listOf("Arun Kumar-ATQ/PB/20","Vishal Sharma-ATQ/HP/40","Kuldeep-ATQ/PB/20","Nitesh-ATQ/PB/20","Karan-ATQ/PB/20")
    private val cabTypeList = listOf("Monthly", "On-call")

    private lateinit var circleAdapter: CustomAutoCompleteAdapter
    private lateinit var empListAdapter: CustomAutoCompleteAdapter
    private lateinit var cabTypeListAdapter: CustomAutoCompleteAdapter
    private lateinit var siteAdapter: CustomAutoCompleteAdapter
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
        get() = ActivityBinding(R.layout.activity_add_cab_fare,CabViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setUpPlaceLocations()
        setDateTimeAttendance()
        setImagePicker()
        setCircleList()
        setEmpList()
        setCabTypeList()
        setSiteList()
        initListeners()
    }

    override fun subscribeToEvents(vm: CabViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(this) {
            when (it) {
                CabClickEvents.ON_CAMERA_CLICK ->{
                    pickMediaHelper.onlyCameraMedia()
                }
                CabClickEvents.ON_SAVE_CLICK -> {
                    finish()
                }
                CabClickEvents.ON_CANCEL_CLICK ->{
                    finish()
                }
            }
        }
    }

    private fun setUpPlaceLocations(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestCurrentLocation()
    }

    private fun setUpToolbar() {
        binding.addSignInToolbar.toolbarTitle.text = getString(R.string.add_cab_fare)
        binding.addSignInToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setDateTimeAttendance(){
        binding.dateString = DateUtils.getCurrentDate()
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
        binding.circleEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.circleEt.showDropDown()
            }
        }
        binding.circleEt.setOnClickListener {
            binding.circleEt.showDropDown()

        }

        binding.empEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.empEt.showDropDown()
            }
        }
        binding.empEt.setOnClickListener {
            binding.empEt.showDropDown()

        }

        binding.cabTypeEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.cabTypeEt.showDropDown()
            }
        }
        binding.cabTypeEt.setOnClickListener {
            binding.cabTypeEt.showDropDown()

        }

        binding.siteEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.siteEt.showDropDown()
            }
        }
        binding.siteEt.setOnClickListener {
            binding.siteEt.showDropDown()

        }

        binding.etStartTime.setOnClickListener {
            DateUtils.showTimerPicker(this,object :DateUtils.TimeCallBack{
                override fun onTimeSelected(time: String, formatTime: String) {
                    binding.startTimeString = time
                }
            })
        }

        binding.etEndTime.setOnClickListener {
            DateUtils.showTimerPicker(this,object :DateUtils.TimeCallBack{
                override fun onTimeSelected(time: String, formatTime: String) {
                    binding.endTimeString = time
                }
            })
        }

        binding.etDate.setOnClickListener {
            DateUtils.onDateClick(this,object :DateUtils.DateCallBack{
                override fun onDateSelected(date: String, formatDate: String) {
                    binding.dateString = date
                }
            })
        }

    }

    private fun setCircleList() {
        circleAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, circleList)
        binding.circleEt.setAdapter(circleAdapter)
    }

    private fun setEmpList() {
        empListAdapter = CustomAutoCompleteAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            employeeList
        )
        binding.empEt.setAdapter(empListAdapter)
    }

    private fun setCabTypeList() {
        cabTypeListAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, cabTypeList)
        binding.cabTypeEt.setAdapter(cabTypeListAdapter)
    }

    private fun setSiteList() {
        siteAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, siteIdList)
        binding.siteEt.setAdapter(siteAdapter)
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