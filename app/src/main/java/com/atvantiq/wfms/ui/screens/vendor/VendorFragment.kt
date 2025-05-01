package com.atvantiq.wfms.ui.screens.vendor

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentVendorBinding
import com.atvantiq.wfms.ui.screens.adapters.AttendanceOptionsAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.claimApprovals.ClaimApprovalsActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimActivity
import com.atvantiq.wfms.ui.screens.reimbursement.myClaims.MyClaimsActivity
import com.atvantiq.wfms.ui.screens.vendor.loginDetails.VendorLoginDetailsActivity
import com.atvantiq.wfms.ui.screens.vendor.startActivity.VendorStartActivity
import com.atvantiq.wfms.ui.screens.vendor.viewAllActivities.ViewAllVendorActivity
import com.atvantiq.wfms.utils.Utils

class VendorFragment : BaseFragment<FragmentVendorBinding,VendorViewModel>() {

    private lateinit var data:List<Pair<String,String>>
    private lateinit var optionsAdapter: AttendanceOptionsAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_vendor,VendorViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: VendorViewModel) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        initListeners()
        setVendorOptions()
    }

    private fun initListeners(){
        binding.loginDetailsBt.setOnClickListener {
            Utils.jumpActivity(requireContext(),VendorLoginDetailsActivity::class.java)
        }
    }

    private fun initOptionsDate(){
        data = listOf(
            getString(R.string.start_activity) to getString(R.string.start_record_vendor_data),
            getString(R.string.view_all_activities) to getString(R.string.vendor_completed_tasks),
        )
    }

    private fun setVendorOptions(){
        initOptionsDate()
        optionsAdapter = AttendanceOptionsAdapter(data){
            when(it){
                0->{
                    Utils.jumpActivity(requireContext(), VendorStartActivity::class.java)
                }
                1->{
                    Utils.jumpActivity(requireContext(), ViewAllVendorActivity::class.java)
                }
            }
        }
        binding.optionsList.adapter = optionsAdapter
    }

}