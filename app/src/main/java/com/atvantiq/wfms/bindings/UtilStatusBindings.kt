package com.atvantiq.wfms.bindings

import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.databinding.BindingAdapter
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.utils.Utils

object UtilStatusBindings {

    @JvmStatic
    @BindingAdapter(value = ["assignedTaskStatus"])
    fun assignedTaskStatus(textView: TextView, status: String?) {
        when (status) {
            ValConstants.OPEN -> {
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.colorPrimary))
                textView.setBackgroundResource(R.drawable.status_primary_bg)
            }

            ValConstants.ACCEPTED -> {
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.orange))
                textView.setBackgroundResource(R.drawable.status_orange_bg)
            }

            ValConstants.COMPLETED -> {
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.green))
                textView.setBackgroundResource(R.drawable.status_green_bg)
            }

            ValConstants.ACCESS_ISSUE  -> {
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.red))
                textView.setBackgroundResource(R.drawable.status_red_bg)
            }

            ValConstants.REJECTED  -> {
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.red))
                textView.setBackgroundResource(R.drawable.status_red_bg)
            }

            else -> {
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.black))
                textView.setBackgroundResource(R.drawable.status_orange_bg)
            }
        }
    }
}