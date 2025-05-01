package com.atvantiq.wfms.ui.screens.dashboard

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.databinding.FragmentDashboardBinding
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.ui.screens.adapters.DashboardPagerAdapter
import com.atvantiq.wfms.ui.screens.adapters.MarqueeAdapter
import com.atvantiq.wfms.ui.screens.announcements.AnnouncementsActivity
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceStatusFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets.MyTargetsFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard.ProjectDashboardFragment
import com.atvantiq.wfms.utils.Utils
import com.google.android.material.tabs.TabLayoutMediator
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding,DashboardViewModel>() {

    @Inject
    lateinit var prefMain: SecurePrefMain

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_dashboard,DashboardViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: DashboardViewModel) {
        binding.vm = vm
        setupUserData()
        vm.clickEvents.observe(viewLifecycleOwner) {
            when (it) {
                DashboardClickEvents.onAnnouncementsClicks -> {
                    Utils.jumpActivity(requireContext(),AnnouncementsActivity::class.java)
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        setupTabBar()
        horizontalScrollTextView()
    }
    private fun setupUserData() {
        var userData: User? = PrefMethods.getUserData(prefMain) ?: return
        binding.appDashHeader.userData = userData
    }

    private fun setupTabBar(){
        val adapter = DashboardPagerAdapter(requireActivity())
        adapter.addFragment(AttendanceStatusFragment())
        adapter.addFragment(MyTargetsFragment())
        adapter.addFragment(ProjectDashboardFragment())
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.attendance_status)
                1 -> getString(R.string.my_targets)
                2 -> getString(R.string.projects)
                else -> getString(R.string.attendance_status)
            }
        }.attach()
    }

    private fun horizontalScrollTextView(){
        val items = listOf("New year celebrations are coming soon.", "Report files must be submitted before december", "Reimbursement forms are open now.")
        val adapter = MarqueeAdapter(items)
        binding.appDashHeader.marqueeRecyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.appDashHeader.marqueeRecyclerView.layoutManager = layoutManager
        /*val handler = Handler(Looper.getMainLooper())
        val scrollRunnable = object : Runnable {
            override fun run() {
                val currentPosition = layoutManager.findFirstVisibleItemPosition()
                val nextPosition = (currentPosition + 1) % items.size
                binding.appDashHeader.marqueeRecyclerView.smoothScrollToPosition(nextPosition)
                handler.postDelayed(this, 2000) // Adjust delay as needed
            }
        }
        handler.post(scrollRunnable)*/
    }

}