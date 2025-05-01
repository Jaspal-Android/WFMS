package com.atvantiq.wfms.ui.screens.forgotPassword.dialog

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingBottomSheetFragment
import com.atvantiq.wfms.databinding.BottomSheetDialogGetOtpBinding


class GetOTPBottomSheetDialog(var onSubmitOTP:(otp:String)->Unit) : BaseBindingBottomSheetFragment<BottomSheetDialogGetOtpBinding>() {

    private  var otpCode:String = ""

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.bottom_sheet_dialog_get_otp)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        setupOtpInputs()
        setListeners()
    }

    private fun setupOtpInputs() {
        binding.otpBox1.requestFocus()

        binding.otpBox1.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 1) binding.otpBox2.requestFocus()
        }

        binding.otpBox2.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 1) binding.otpBox3.requestFocus()
            else if (text?.isEmpty() == true) binding.otpBox1.requestFocus()
        }

        binding.otpBox3.doOnTextChanged { text, _, _, _ ->
            if (text?.length == 1) binding.otpBox4.requestFocus()
            else if (text?.isEmpty() == true) binding.otpBox2.requestFocus()
        }

        binding.otpBox4.doOnTextChanged { text, _, _, _ ->
            if (text?.isNotEmpty() == true) {
                binding.btnSubmit.visibility = View.VISIBLE
                binding.otpBox4.clearFocus()
                otpCode = binding.otpBox1.text.toString() + binding.otpBox2.text.toString() +
                        binding.otpBox3.text.toString() + binding.otpBox4.text.toString()
            } else if (text?.isEmpty() == true) {
                binding.otpBox3.requestFocus()
            }
        }

        binding.otpBox1.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && binding.otpBox1.text.isEmpty()) {
            }
            false
        }

        binding.otpBox2.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && binding.otpBox2.text.isEmpty()) {
                binding.otpBox1.text.clear()
                binding.otpBox1.requestFocus()
            }
            false
        }

        binding.otpBox3.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && binding.otpBox3.text.isEmpty()) {
                binding.otpBox2.text.clear()
                binding.otpBox2.requestFocus()
            }
            false
        }

        binding.otpBox4.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DEL && binding.otpBox4.text.isEmpty()) {
                binding.otpBox3.text.clear()
                binding.otpBox3.requestFocus()
            }
            false
        }
    }

    private fun setListeners(){
        binding.btnSubmit.setOnClickListener {
            onSubmitOTP.invoke(otpCode)
            Toast.makeText(context,otpCode,Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}