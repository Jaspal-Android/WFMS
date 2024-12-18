package com.atvantiq.wfms.ui.screens.attendance.signInActivity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivitySignInBinding
import com.atvantiq.wfms.ui.screens.adapters.MyTargetAdapter
import com.atvantiq.wfms.ui.screens.adapters.SignInListAdapter
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInActivity
import com.atvantiq.wfms.utils.Utils

class SignInActivity : BaseActivity<ActivitySignInBinding,SignInVM>() {

    private lateinit var signInAdapter: SignInListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_sign_in,SignInVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setSignInList()
    }

    private fun setUpToolbar(){
        binding.toolbarTitle.text   =  getString(R.string.sign_in)
        binding.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.addLoginActivity.setOnClickListener {
            Utils.jumpActivity(this,AddSignInActivity::class.java)
        }
    }

    override fun subscribeToEvents(vm: SignInVM) {

    }

    private fun setSignInList(){
        signInAdapter  = SignInListAdapter()
        binding.signInList.layoutManager = LinearLayoutManager(this)
        binding.signInList.adapter = signInAdapter
    }

}