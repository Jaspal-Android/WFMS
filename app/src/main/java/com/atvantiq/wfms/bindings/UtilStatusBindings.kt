package com.atvantiq.wfms.bindings

import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.ApprovalStatusCodes
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceApprovalStatus
import com.atvantiq.wfms.constants.ApprovalTextStatus

/**
 * UtilStatusBindings
 * ══════════════════════════════════════════════════════════
 * All status binding adapters updated to use semantic
 * status_*_bg and status_*_text colors defined in colors.xml.
 *
 * Uses applyStatus() helper to set both background color
 * and text color via GradientDrawable — this correctly
 * updates the existing status_chip_bg drawable shape
 * rather than replacing it with a flat color.
 * ══════════════════════════════════════════════════════════
 */
object UtilStatusBindings {

    /**
     * Applies background color + text color to a status chip TextView.
     * Reuses the existing drawable shape (rounded corners) and just
     * updates its fill color — preserving corner radius.
     */
    private fun TextView.applyStatus(bgColorRes: Int, textColorRes: Int) {
        // Set background using status_chip_bg drawable to keep rounded corners
        setBackgroundResource(R.drawable.status_gray_bg)
        (background as? GradientDrawable)?.setColor(
            ContextCompat.getColor(context, bgColorRes)
        )
        setTextColor(ContextCompat.getColor(context, textColorRes))
    }

    // ──────────────────────────────────────────────────────
    // Assigned Task Status  (String codes)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["assignedTaskStatus"])
    fun assignedTaskStatus(textView: TextView, status: String?) {
        when (status) {
            ValConstants.OPEN -> textView.applyStatus(
                R.color.status_leave_bg, R.color.status_leave_text
            )
            ValConstants.ACCEPTED -> textView.applyStatus(
                R.color.status_idle_bg, R.color.status_idle_text
            )
            ValConstants.COMPLETED -> textView.applyStatus(
                R.color.status_present_bg, R.color.status_present_text
            )
            ValConstants.ACCESS_ISSUE -> textView.applyStatus(
                R.color.status_absent_bg, R.color.status_absent_text
            )
            ValConstants.REJECTED -> textView.applyStatus(
                R.color.status_absent_bg, R.color.status_absent_text
            )
            else -> textView.applyStatus(
                R.color.status_unmarked_bg, R.color.status_unmarked_text
            )
        }
    }

    // ──────────────────────────────────────────────────────
    // Attendance Status  (Int codes)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["attendanceStatus"])
    fun attendanceStatus(textView: TextView, status: Int?) {
        when (status) {
            1 -> {
                textView.text = textView.context.getString(R.string.present)
                textView.applyStatus(R.color.status_present_bg, R.color.status_present_text)
            }
            2 -> {
                textView.text = textView.context.getString(R.string.absent)
                textView.applyStatus(R.color.status_absent_bg, R.color.status_absent_text)
            }
            3 -> {
                textView.text = textView.context.getString(R.string.leave)
                textView.applyStatus(R.color.status_leave_bg, R.color.status_leave_text)
            }
            4 -> {
                textView.text = textView.context.getString(R.string.idle)
                textView.applyStatus(R.color.status_idle_bg, R.color.status_idle_text)
            }
            5 -> {
                textView.text = textView.context.getString(R.string.holidays)
                textView.applyStatus(R.color.status_holiday_bg, R.color.status_holiday_text)
            }
            6 -> {
                textView.text = textView.context.getString(R.string.work_off)
                textView.applyStatus(R.color.status_work_off_bg, R.color.status_work_off_text)
            }
            else -> {
                textView.text = textView.context.getString(R.string.no_action)
                textView.applyStatus(R.color.status_unmarked_bg, R.color.status_unmarked_text)
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // Assigned Site Status  (Int codes)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["assignedSiteStatus"])
    fun assignedSiteStatus(textView: TextView, status: Int?) {
        when (status) {
            StatusCodes.OPEN -> {
                textView.text = textView.context.getString(R.string.open)
                textView.applyStatus(R.color.status_leave_bg, R.color.status_leave_text)
            }
            StatusCodes.ACCEPTED -> {
                textView.text = textView.context.getString(R.string.accepted)
                textView.applyStatus(R.color.status_idle_bg, R.color.status_idle_text)
            }
            // Started, not completed: the "Active" filter, and "In Progress" on the approval screens.
            StatusCodes.WIP -> {
                textView.text = textView.context.getString(R.string.in_progress)
                textView.applyStatus(R.color.status_incomplete_bg, R.color.status_incomplete_text)
            }
            StatusCodes.ACCESS_ISSUE -> {
                textView.text = textView.context.getString(R.string.access_issue)
                textView.applyStatus(R.color.status_absent_bg, R.color.status_absent_text)
            }
            StatusCodes.COMPLETED -> {
                textView.text = textView.context.getString(R.string.completed)
                textView.applyStatus(R.color.status_present_bg, R.color.status_present_text)
            }
            StatusCodes.REVISIT -> {
                textView.text = textView.context.getString(R.string.revisited)
                textView.applyStatus(R.color.status_work_off_bg, R.color.status_work_off_text)
            }
            StatusCodes.REJECTED -> {
                textView.text = textView.context.getString(R.string.rejected)
                textView.applyStatus(R.color.status_absent_bg, R.color.status_absent_text)
            }
            StatusCodes.REMOVED -> {
                textView.text = textView.context.getString(R.string.removed)
                textView.applyStatus(R.color.status_absent_na_bg, R.color.status_absent_na_text)
            }
            else -> {
                textView.text = textView.context.getString(R.string.not_available)
                textView.applyStatus(R.color.status_unmarked_bg, R.color.status_unmarked_text)
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // Work Approval Status  (Int codes)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["workApprovalStatus"])
    fun workApprovalStatus(textView: TextView, status: Int?) {
        when (status) {
            ApprovalStatusCodes.OPEN -> {
                textView.text = textView.context.getString(R.string.pending)
                textView.applyStatus(R.color.status_idle_bg, R.color.status_idle_text)
            }
            ApprovalStatusCodes.ACCEPTED -> {
                textView.text = textView.context.getString(R.string.accepted)
                textView.applyStatus(R.color.status_present_bg, R.color.status_present_text)
            }
            ApprovalStatusCodes.REJECTED -> {
                textView.text = textView.context.getString(R.string.rejected)
                textView.applyStatus(R.color.status_absent_bg, R.color.status_absent_text)
            }
            else -> {
                textView.text = textView.context.getString(R.string.not_available)
                textView.applyStatus(R.color.status_unmarked_bg, R.color.status_unmarked_text)
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // Site Active / Inactive  (`is_active`)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["siteActiveStatus"])
    fun siteActiveStatus(textView: TextView, isActive: Int?) {
        if (isActive == StatusCodes.SITE_ACTIVE) {
            textView.text = textView.context.getString(R.string.site_active)
            textView.applyStatus(R.color.status_present_bg, R.color.status_present_text)
        } else {
            textView.text = textView.context.getString(R.string.site_inactive)
            textView.applyStatus(R.color.status_unmarked_bg, R.color.status_unmarked_text)
        }
    }

    // ──────────────────────────────────────────────────────
    // Status from free text  (Work Approval `action`, claim `status`)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["approvalTextStatus"])
    fun approvalTextStatus(textView: TextView, text: String?) {
        val status = ApprovalTextStatus.from(text)
        textView.text = status.labelRes?.let { textView.context.getString(it) } ?: text.orEmpty()
        textView.applyStatus(status.backgroundRes, status.textRes)
    }

    // ──────────────────────────────────────────────────────
    // Claim status  (as approvalTextStatus, but Submitted is light blue on claims, as on iOS)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["claimStatus"])
    fun claimStatus(textView: TextView, text: String?) {
        val status = ApprovalTextStatus.from(text)
        textView.text = status.labelRes?.let { textView.context.getString(it) } ?: text.orEmpty()
        if (status == ApprovalTextStatus.SUBMITTED) {
            textView.applyStatus(R.color.status_leave_bg, R.color.status_leave_text)
        } else {
            textView.applyStatus(status.backgroundRes, status.textRes)
        }
    }

    // ──────────────────────────────────────────────────────
    // Work status of a site or work type  (3 In Progress, 5 Completed, else its label)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["workStatusCode", "workStatusLabel"], requireAll = false)
    fun workStatus(textView: TextView, code: Int?, label: String?) {
        when (code) {
            StatusCodes.WIP -> {
                textView.text = textView.context.getString(R.string.in_progress)
                textView.applyStatus(R.color.status_leave_bg, R.color.status_leave_text)
            }
            StatusCodes.COMPLETED -> {
                textView.text = textView.context.getString(R.string.completed_label)
                textView.applyStatus(R.color.status_present_bg, R.color.status_present_text)
            }
            else -> {
                textView.text = label ?: textView.context.getString(R.string.not_available_value)
                textView.applyStatus(R.color.status_unmarked_bg, R.color.status_unmarked_text)
            }
        }
    }

    // ──────────────────────────────────────────────────────
    // A PM / OPS / ADMIN approval step on a work type  (1 approved, 2 rejected, 0 pending)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["approvalStep", "approvalStepLabel"], requireAll = true)
    fun approvalStep(textView: TextView, step: Int?, label: String) {
        textView.text = textView.context.getString(R.string.approval_step_format, label, step ?: ApprovalStatusCodes.OPEN)
        when (step) {
            ApprovalStatusCodes.ACCEPTED -> textView.applyStatus(R.color.status_present_bg, R.color.status_present_text)
            ApprovalStatusCodes.REJECTED -> textView.applyStatus(R.color.status_incomplete_bg, R.color.status_incomplete_text)
            else -> textView.applyStatus(R.color.status_leave_bg, R.color.status_leave_text)
        }
    }

    // ──────────────────────────────────────────────────────
    // Attendance Approval chip  (status 0-8, spec 6.1)
    // ──────────────────────────────────────────────────────
    @JvmStatic
    @BindingAdapter(value = ["attendanceApprovalStatus"])
    fun attendanceApprovalStatus(textView: TextView, code: Int?) {
        val status = AttendanceApprovalStatus.from(code)
        textView.text = textView.context.getString(status.labelRes)
        textView.applyStatus(status.backgroundRes, status.textRes)
    }
}
