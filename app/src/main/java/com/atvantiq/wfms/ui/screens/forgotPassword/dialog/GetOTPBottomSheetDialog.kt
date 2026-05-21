package com.atvantiq.wfms.ui.screens.forgotPassword.dialog

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingBottomSheetFragment
import com.atvantiq.wfms.databinding.BottomSheetDialogGetOtpBinding

class GetOTPBottomSheetDialog(
    var onSubmitOTP: (otp: String) -> Unit,
    var onResendOTP: (() -> Unit)? = null
) : BaseBindingBottomSheetFragment<BottomSheetDialogGetOtpBinding>() {

    private var otpCode: String = ""

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.bottom_sheet_dialog_get_otp)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            clearOtpInputs()
        }
        setupOtpInputs()
        setListeners()
    }

    private fun clearOtpInputs() {
        binding.otpBox1.text?.clear()
        binding.otpBox2.text?.clear()
        binding.otpBox3.text?.clear()
        binding.otpBox4.text?.clear()
        binding.otpBox5.text?.clear()
        binding.otpBox6.text?.clear()
        binding.btnSubmit.visibility = View.INVISIBLE
    }

    private fun setupOtpInputs() {
        val otpBoxes = listOf(
            binding.otpBox1, binding.otpBox2, binding.otpBox3,
            binding.otpBox4, binding.otpBox5, binding.otpBox6
        )

        // Only allow digits
        otpBoxes.forEach { box ->
            box.filters = arrayOf(InputFilter.LengthFilter(1), InputFilter { source, _, _, _, _, _ ->
                if (source.isNullOrEmpty()) return@InputFilter null
                if (source.matches(Regex("\\d+"))) null else ""
            })
        }

        binding.otpBox1.requestFocus()

        otpBoxes.forEachIndexed { idx, editText ->
            // Select all text when focused (for tap or programmatic focus)
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    editText.post {
                        editText.selectAll()
                    }
                }
            }
            // Also handle tap to select all
            editText.setOnClickListener {
                editText.selectAll()
            }

            editText.addTextChangedListener(object : TextWatcher {
                private var isEditing = false
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (isEditing) return
                    // Handle paste of full OTP
                    if (count > 1 && s != null && s.length == 6) {
                        isEditing = true
                        for (i in 0..5) {
                            otpBoxes[i].setText(s[i].toString())
                        }
                        otpBoxes[5].clearFocus()
                        updateOtpCodeAndButton()
                        isEditing = false
                        return
                    }
                }
                override fun afterTextChanged(s: Editable?) {
                    if (isEditing) return
                    isEditing = true
                    // If more than 1 char (user pasted or typed fast), keep only last digit
                    if (s != null && s.length > 1) {
                        editText.setText(s.last().toString())
                        editText.setSelection(1)
                    }
                    if (s?.length == 1 && idx < 5) {
                        otpBoxes[idx + 1].requestFocus()
                    }
                    updateOtpCodeAndButton()
                    isEditing = false
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && idx > 0) {
                        otpBoxes[idx - 1].setText("")
                        otpBoxes[idx - 1].requestFocus()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun updateOtpCodeAndButton() {
        otpCode = binding.otpBox1.text.toString() +
                binding.otpBox2.text.toString() +
                binding.otpBox3.text.toString() +
                binding.otpBox4.text.toString() +
                binding.otpBox5.text.toString() +
                binding.otpBox6.text.toString()
        binding.btnSubmit.visibility = if (otpCode.length == 6) View.VISIBLE else View.INVISIBLE
    }

    private fun setListeners() {
        binding.btnSubmit.setOnClickListener {
            if (otpCode.length == 6) {
                // Hide keyboard
                val imm = context?.getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(binding.btnSubmit.windowToken, 0)
                onSubmitOTP.invoke(otpCode)
                Toast.makeText(context, otpCode, Toast.LENGTH_SHORT).show()
            }
        }
        binding.resendOtpText.setOnClickListener { onResendOTP?.invoke() }
    }
}
