package com.atvantiq.wfms.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.atvantiq.wfms.R

class GradientLogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val label = TextView(context)

    init {
        label.text = "O"
        label.setTextColor(Color.WHITE)
        label.textSize = 28f
        label.setTypeface(label.typeface, android.graphics.Typeface.BOLD)
        addView(label, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).also { it.gravity = Gravity.CENTER })

        clipToOutline = true
        outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyGradient()
    }

    private fun applyGradient() {
        val startColor = MaterialColors.getColor(this, R.attr.wfmsColorGradientEnd)
        val endColor   = MaterialColors.getColor(this, R.attr.wfmsColorPrimaryDark)
        val radius     = resources.getDimension(R.dimen.card_radius_max)

        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = radius
        }
    }
}