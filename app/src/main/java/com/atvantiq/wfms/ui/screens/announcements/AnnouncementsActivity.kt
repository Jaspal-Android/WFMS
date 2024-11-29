package com.atvantiq.wfms.ui.screens.announcements

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityAnnouncementsBinding
import com.atvantiq.wfms.ui.screens.adapters.AnnouncementAdapter
import com.atvantiq.wfms.ui.screens.adapters.MarqueeAdapter
import com.atvantiq.wfms.widgets.DividerItemDecoration

class AnnouncementsActivity : BaseBindingActivity<ActivityAnnouncementsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_announcements)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        initAnnouncementList()
    }

    private fun setToolbar(){
        binding.announceToolbar.toolbarTitle.text = "Announcements"
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