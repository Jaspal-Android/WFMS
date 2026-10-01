package com.atvantiq.wfms.ui.screens.reimbursement.myClaims

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityMyClaimsBinding
import com.atvantiq.wfms.databinding.ItemMyClaimsBinding
import com.atvantiq.wfms.ui.screens.adapters.ApprovalsListAdapter
import com.atvantiq.wfms.ui.screens.adapters.MyClaimsListAdapter
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class MyClaimsActivity : BaseActivity<ActivityMyClaimsBinding,MyClaimsVM>() {

    private lateinit var myClaimsListAdapter: MyClaimsListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_my_claims,MyClaimsVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setUpToolbar()
        setMyClaimsList()
    }

    private fun setUpToolbar(){
        binding.myClaimsToolbar.toolbarTitle.text = getString(R.string.my_claims)
        binding.myClaimsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: MyClaimsVM) {

    }

    private fun setMyClaimsList(){
        myClaimsListAdapter  = MyClaimsListAdapter{
            Utils.jumpActivity(this,MyClaimDetailsActivity::class.java)
        }
        binding.myClaimsList.addItemDecoration(DividerItemDecoration(this,R.drawable.custom_divider))
        binding.myClaimsList.layoutManager = LinearLayoutManager(this)
        binding.myClaimsList.adapter = myClaimsListAdapter
    }

}