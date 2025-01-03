package com.atvantiq.wfms.ui.screens.reimbursement

import android.os.Bundle
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentReimbursementBinding
import com.atvantiq.wfms.ui.screens.adapters.AttendanceOptionsAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.claimApprovals.ClaimApprovalsActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimActivity
import com.atvantiq.wfms.ui.screens.reimbursement.myClaims.MyClaimsActivity
import com.atvantiq.wfms.utils.Utils

class ReimbursementFragment : BaseFragment<FragmentReimbursementBinding,ReimbursementViewModel>() {

    private lateinit var data:List<Pair<String,String>>
    private lateinit var optionsAdapter: AttendanceOptionsAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_reimbursement,ReimbursementViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        setReimbursmentOptions()
    }

    private fun initOptionsDate(){
        data = listOf(
            getString(R.string.create_claim) to getString(R.string.add_new),
            getString(R.string.my_claims) to getString(R.string.sumitted_approved_claims),
            getString(R.string.my_approvals) to getString(R.string.pending_approvals_pm),
        )
    }

    private fun setReimbursmentOptions(){
        initOptionsDate()
        optionsAdapter = AttendanceOptionsAdapter(data){
            when(it){
                0->{
                    Utils.jumpActivity(requireContext(),CreateClaimActivity::class.java)
                }
                1->{
                    Utils.jumpActivity(requireContext(),MyClaimsActivity::class.java)
                }
                2->{
                    Utils.jumpActivity(requireContext(),ClaimApprovalsActivity::class.java)
                }
            }
        }
        binding.optionsList.adapter = optionsAdapter
    }

}