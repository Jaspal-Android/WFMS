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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        applyGradient() // re-apply when enabled state changes
    }

    private fun applyGradient() {
        val radius = resources.getDimension(R.dimen.card_radius)

        val background = if (isEnabled) {
            // Active — gradient
            val startColor = resolveColor(
                primaryAttr = R.attr.wfmsColorPrimaryDark,
                fallbackAttr = com.google.android.material.R.attr.colorPrimary
            )
            val endColor = resolveColor(
                primaryAttr = R.attr.wfmsColorGradientEnd,
                fallbackAttr = com.google.android.material.R.attr.colorPrimary
            )

            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(startColor, endColor)
            ).apply { cornerRadius = radius }
        } else {
            GradientDrawable().apply {
                setColor(context.getColor(R.color.lightGray))
                cornerRadius = radius
            }
        }

        this.background = background
        setTextColor(Color.WHITE)
    }

    private fun resolveColor(primaryAttr: Int, fallbackAttr: Int): Int {
        val primary = MaterialColors.getColorOrNull(context, primaryAttr)
        if (primary != null) return primary

        val fallback = MaterialColors.getColorOrNull(context, fallbackAttr)
        if (fallback != null) return fallback

        return Color.WHITE
    }
}