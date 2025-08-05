package com.atvantiq.wfms.bindings

import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.databinding.BindingAdapter
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils

object UtilBindings {

    @JvmStatic
    @BindingAdapter(value = ["wishText"])
    fun wishText(textView: TextView, text: String?) {
        val greetingText = Utils.getGreeting(textView.context)
        textView.text = "$greetingText, $text"
    }

    @JvmStatic
    @BindingAdapter(value = ["attendanceDateFormat"])
    fun attendanceDateFormat(textView: TextView, date: String?) {
        if (date.isNullOrEmpty()) {
            textView.text = textView.context.getString(R.string.date_not_found)
        } else {
            val formattedDate = DateUtils.formatApiDateToTimeAndDate(date)
            textView.text = formattedDate ?: textView.context.getString(R.string.date_not_found)
        }
    }

}