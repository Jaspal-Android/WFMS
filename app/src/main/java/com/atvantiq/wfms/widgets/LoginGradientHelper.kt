package com.atvantiq.wfms.widgets

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.google.android.material.color.MaterialColors
import com.atvantiq.wfms.R

/**
 * LoginGradientHelper
 * ══════════════════════════════════════════════════════════
 * Applies theme-aware gradients to the login screen header
 * and the stylish "O" logo card.
 *
 * Call from LoginActivity.onCreate() after setContentView():
 *
 *   LoginGradientHelper.apply(binding.headerSection, binding.logoGradientBg)
 * ══════════════════════════════════════════════════════════
 */
object LoginGradientHelper {

    fun apply(headerView: View, logoView: View) {
        applyHeader(headerView)
        applyLogo(logoView)
    }

    /**
     * Header background — full width gradient
     * primaryDark (left) → gradientEnd (right)
     */
    private fun applyHeader(view: View) {
        val startColor = MaterialColors.getColor(view, R.attr.wfmsColorPrimaryDark)
        val endColor   = MaterialColors.getColor(view, R.attr.wfmsColorGradientEnd)

        view.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        )
    }

    /**
     * Logo card background — diagonal gradient for depth
     * gradientEnd (top-left) → primaryDark (bottom-right)
     * Reversed from header so logo feels like a light source
     */
    private fun applyLogo(view: View) {
        val startColor = MaterialColors.getColor(view, R.attr.wfmsColorGradientEnd)
        val endColor   = MaterialColors.getColor(view, R.attr.wfmsColorPrimaryDark)
        val radius     = view.context.resources.getDimension(R.dimen.card_radius_max)

        view.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor)
        ).apply {
            cornerRadius = radius
        }
    }
}