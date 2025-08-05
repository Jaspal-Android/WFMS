package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.detail

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAttendanceDetailBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.Record
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceStatusVM
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AttendanceDetailActivity : BaseActivity<ActivityAttendanceDetailBinding,AttendanceStatusVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_attendance_detail, AttendanceStatusVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        fetchAttendanceDetailFromBundle()
    }

    private fun fetchAttendanceDetailFromBundle() {
        val record: Record? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(SharingKeys.attendanceRecord, Record::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(SharingKeys.attendanceRecord) as? Record
        }
        setupUI(record)
    }

    private fun setupUI(record: Record?) {
       binding.item = record

       if(record?.checkin?.latitude!= null && record?.checkin?.logitude != null) {
           Utils.getAddressFromLatLong(this,record?.checkin?.latitude, record.checkin.logitude) { it ->
               binding.checkInAddressString = it
           }
       } else {
           binding.checkInAddressString  = getString(R.string.address_not_found)
       }

        if(record?.checkout?.latitude!= null && record?.checkout?.logitude != null) {
            Utils.getAddressFromLatLong(this,record?.checkout?.latitude, record.checkout.logitude) { it ->
                binding.checkOutAddressString = it
            }
        } else {
            binding.checkOutAddressString = getString(R.string.address_not_found)
        }
    }

    private fun setToolbar() {
        binding.attendanceDetailToolbar.toolbarTitle.text = getString(R.string.details)
        binding.attendanceDetailToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: AttendanceStatusVM) {

    }
}