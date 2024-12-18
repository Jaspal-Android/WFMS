package com.atvantiq.wfms.ui.screens.attendance

import android.os.Bundle
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentAttendanceBinding
import com.atvantiq.wfms.ui.screens.adapters.AttendanceOptionsAdapter
import com.atvantiq.wfms.ui.screens.attendance.approvals.ApprovalsActivity
import com.atvantiq.wfms.ui.screens.attendance.myProgress.MyProgressActivity
import com.atvantiq.wfms.ui.screens.attendance.signInActivity.SignInActivity
import com.atvantiq.wfms.utils.Utils

class AttendanceFragment : BaseFragment<FragmentAttendanceBinding,AttendanceViewModel>() {

    private lateinit var data:List<Pair<String,String>>
    private lateinit var optionsAdapter:AttendanceOptionsAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance,AttendanceViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        setAttendanceOptions()
    }

    private fun initOptionsDate(){
        data = listOf(
            getString(R.string.sign_in) to getString(R.string.mark_your_daily_attendance),
            getString(R.string.sign_out) to getString(R.string.update_status_of_your_work),
            getString(R.string.my_progress) to getString(R.string.monthly_progress),
            getString(R.string.approvals) to getString(R.string.pending_approvals)
        )
    }

    private fun setAttendanceOptions(){
        initOptionsDate()
        optionsAdapter = AttendanceOptionsAdapter(data){
            when(it){
                0->{
                    // SignIn Activity
                    Utils.jumpActivity(requireContext(),SignInActivity::class.java)
                }
                1->{
                    // SignOut Activity
                }
                2->{
                    // My Progress Activity
                    Utils.jumpActivity(requireContext(),MyProgressActivity::class.java)
                }
                3->{
                    // Approvals Activity
                    Utils.jumpActivity(requireContext(),ApprovalsActivity::class.java)
                }
            }
        }
        binding.optionsList.adapter = optionsAdapter
    }

}