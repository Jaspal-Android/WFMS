package com.atvantiq.wfms.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.atvantiq.wfms.R
import com.google.android.material.color.MaterialColors

 class GradientButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyGradient()
    }

    private fun applyGradient() {
        val startColor = MaterialColors.getColor(this, R.attr.wfmsColorPrimaryDark)  // darker shade
        val endColor   = MaterialColors.getColor(this, R.attr.wfmsColorPrimary)      // base shade
        val radius     = resources.getDimension(R.dimen.card_radius_large)

        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = radius
        }
        setTextColor(Color.WHITE)
    }
}