package com.atvantiq.wfms.ui.screens.login.withOtp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetRequestOtpBinding
import com.atvantiq.wfms.utils.ValidatorUtils
import com.atvantiq.wfms.widgets.BaseBottomSheet
import com.atvantiq.wfms.utils.dismissIfCallbacksMissing

class RequestOtpBottomSheet : BaseBottomSheet() {

    // Callbacks are wired by the host after construction. The no-arg constructor lets the
    // FragmentManager re-instantiate this sheet on restore (process death, recreate() on a theme
    // change) without an InstantiationException; an unwired sheet dismisses itself.
    var onSubmitEmail: ((email: String) -> Unit)? = null

    lateinit var binding: BottomSheetRequestOtpBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_request_otp, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (dismissIfCallbacksMissing(onSubmitEmail)) return
        initListeners()
    }
    private fun initListeners() {
        binding.btnDone.setOnClickListener {
            if (ValidatorUtils.isValidEmail(binding.emailEditText.text.toString().trim())) {
                binding.emailEditText.error = null
                onSubmitEmail?.invoke(binding.emailEditText.text.toString().trim())
                dismiss()
            }else {
                binding.emailEditText.error = getString(R.string.please_enter_email)
            }
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}