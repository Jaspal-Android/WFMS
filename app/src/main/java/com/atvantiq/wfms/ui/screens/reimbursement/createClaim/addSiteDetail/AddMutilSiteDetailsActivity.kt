package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addSiteDetail

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAddMutilSiteDetailsBinding

class AddMutilSiteDetailsActivity : BaseBindingActivity<ActivityAddMutilSiteDetailsBinding>() {

    private var selectedSite: String = "SITE001"

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_add_mutil_site_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        handleToolbar()
        initListeners()
    }

    private fun handleToolbar() {
        binding.addSiteToolbar.toolbarTitle.text = getString(R.string.add_site_details)
        binding.addSiteToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initListeners() {
        binding.btnDone.setOnClickListener {
            val siteIDStr = selectedSite.trim()
            val purposeStr = binding.etPurpose.text.toString().trim()
            if (validateInputs(siteIDStr, purposeStr)) {
                intent?.let {
                    it.putExtra(SharingKeys.SITE_ID, siteIDStr)
                    it.putExtra(SharingKeys.SITE_PURPOSE, purposeStr)
                    setResult(RESULT_OK, it)
                }
                finish()
            }
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    // Separate validation method for inputs
    private fun validateInputs(siteID: String, purpose: String): Boolean {
        if (siteID.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.site_id),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        if (purpose.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.enter_purpose),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }
}
