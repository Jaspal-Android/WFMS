package com.atvantiq.wfms.bindings

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.atvantiq.wfms.utils.Utils

object UtilBindings {

    @JvmStatic
    @BindingAdapter(value = ["wishText"])
    fun wishText(textView: TextView, text: String?) {
        val greetingText = Utils.getGreeting(textView.context)
        textView.text = "$greetingText, $text"
    }
}