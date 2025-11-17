package com.atvantiq.wfms.bindings

import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.databinding.BindingAdapter
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.AttendanceStatus
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


    @JvmStatic
    @BindingAdapter(value = ["attendanceStatus"])
    fun attendanceStatus(textView: TextView, status: Int?) {
        when (status) {
            1 -> {
                textView.text = textView.context.getString(R.string.present)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.green))
                textView.setBackgroundResource(R.drawable.status_green_bg)
            }
            2  -> {
                textView.text = textView.context.getString(R.string.absent)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.red))
                textView.setBackgroundResource(R.drawable.status_red_bg)
            }
            3  -> {
                textView.text = textView.context.getString(R.string.leave)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.red))
                textView.setBackgroundResource(R.drawable.status_red_bg)
            }
            4 -> {
                textView.text = textView.context.getString(R.string.idle)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.orange))
                textView.setBackgroundResource(R.drawable.status_orange_bg)
            }
            5 -> {
                textView.text = textView.context.getString(R.string.holidays)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.colorPrimary))
                textView.setBackgroundResource(R.drawable.status_primary_bg)
            }
            6 -> {
                textView.text = textView.context.getString(R.string.work_off)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.colorPrimary))
                textView.setBackgroundResource(R.drawable.status_primary_bg)
            }

            else -> {
                textView.text = textView.context.getString(R.string.no_action)
                textView.setTextColor(ActivityCompat.getColor(textView.context, R.color.black))
                textView.setBackgroundResource(R.drawable.status_orange_bg)
            }
        }
    }
}