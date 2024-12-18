package com.atvantiq.wfms.ui.screens.reimbursement.myClaims

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityMyClaimsBinding
import com.atvantiq.wfms.databinding.ItemMyClaimsBinding
import com.atvantiq.wfms.ui.screens.adapters.ApprovalsListAdapter
import com.atvantiq.wfms.ui.screens.adapters.MyClaimsListAdapter

class MyClaimsActivity : BaseActivity<ActivityMyClaimsBinding,MyClaimsVM>() {

    private lateinit var myClaimsListAdapter: MyClaimsListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_my_claims,MyClaimsVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
        myClaimsListAdapter  = MyClaimsListAdapter()
        binding.myClaimsList.layoutManager = LinearLayoutManager(this)
        binding.myClaimsList.adapter = myClaimsListAdapter
    }

}