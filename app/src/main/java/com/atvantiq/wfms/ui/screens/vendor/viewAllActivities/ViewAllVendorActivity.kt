package com.atvantiq.wfms.ui.screens.vendor.viewAllActivities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityViewAllVendorBinding
import com.atvantiq.wfms.ui.screens.adapters.ApprovalsListAdapter
import com.atvantiq.wfms.ui.screens.adapters.VendorViewAllListAdapter
import com.atvantiq.wfms.ui.screens.attendance.approvals.ApprovalsVM
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.DividerItemDecoration

class ViewAllVendorActivity : BaseActivity<ActivityViewAllVendorBinding,ViewAllVendorVM>() {

    private lateinit var vendorViewAllListAdapter: VendorViewAllListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_view_all_vendor,ViewAllVendorVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
             insets
        }
        setToolbar()
        setVendorViewAllList()
    }

    override fun subscribeToEvents(vm: ViewAllVendorVM) {

    }

    private fun setToolbar(){
        binding.allViewToolbar.toolbarTitle.text = getString(R.string.view_all_activities)
        binding.allViewToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setVendorViewAllList(){
        vendorViewAllListAdapter  = VendorViewAllListAdapter{
            Utils.jumpActivity(this,VendorDetailsActivity::class.java)
        }
        binding.viewAllActivitiesList.addItemDecoration(DividerItemDecoration(this,R.drawable.custom_divider))
        binding.viewAllActivitiesList.layoutManager = LinearLayoutManager(this)
        binding.viewAllActivitiesList.adapter = vendorViewAllListAdapter
    }
}