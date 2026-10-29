package com.atvantiq.wfms.ui.screens.announcements

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityAnnouncementsBinding
import com.atvantiq.wfms.ui.screens.adapters.AnnouncementAdapter
import com.atvantiq.wfms.ui.screens.adapters.MarqueeAdapter
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class AnnouncementsActivity : BaseBindingActivity<ActivityAnnouncementsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_announcements)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setToolbar()
        initAnnouncementList()
    }

    private fun setToolbar(){
        binding.announceToolbar.toolbarTitle.text = getString(R.string.announcements)
        binding.announceToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initAnnouncementList(){
        val items = listOf("New year celebrations are coming soon.", "Report files must be submitted before december", "Reimbursement forms are open now.")
        val adapter = AnnouncementAdapter(items)
        binding.announmentsList.adapter = adapter
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.announmentsList.layoutManager = layoutManager
        binding.announmentsList.addItemDecoration(DividerItemDecoration(this,R.drawable.custom_divider))
    }

}