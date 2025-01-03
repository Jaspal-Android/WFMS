package com.atvantiq.wfms.ui.screens.attendance.myProgress

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityMyProgressBinding
import com.atvantiq.wfms.ui.screens.adapters.MyProgressAdapter
import com.atvantiq.wfms.ui.screens.attendance.myProgress.progressDetails.ProgressDetailsActivity
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.DividerItemDecoration

class MyProgressActivity : BaseActivity<ActivityMyProgressBinding,MyProgressVM>() {

    private lateinit var myProgressAdapter: MyProgressAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_my_progress,MyProgressVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        setMyProgressList()
    }

    private fun setToolbar(){
        binding.myProgressToolbar.toolbarTitle.text = getString(R.string.my_progress)
        binding.myProgressToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: MyProgressVM) {
    }

    private fun setMyProgressList(){
        myProgressAdapter  = MyProgressAdapter(){
            Utils.jumpActivity(this,ProgressDetailsActivity::class.java)
        }
        binding.myProgressList.layoutManager = LinearLayoutManager(this)
        binding.myProgressList.addItemDecoration(DividerItemDecoration(this,R.drawable.custom_divider))
        binding.myProgressList.adapter = myProgressAdapter
    }
}