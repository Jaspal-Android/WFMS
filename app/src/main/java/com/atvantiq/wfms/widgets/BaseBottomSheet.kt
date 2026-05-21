package com.atvantiq.wfms.widgets

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import com.atvantiq.wfms.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors

abstract class BaseBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also { dialog ->
            dialog.setOnShowListener {
                val sheet = (it as BottomSheetDialog)
                    .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val surfaceColor = MaterialColors.getColor(
                    requireContext(), R.attr.wfmsColorSurface, Color.WHITE
                )
                val radius = resources.getDimension(R.dimen.card_radius_max)
                sheet?.background = GradientDrawable().apply {
                    setColor(surfaceColor)
                    cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                }
            }
        }
    }
}