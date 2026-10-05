package com.atvantiq.wfms.utils

import java.util.Locale
import kotlin.math.abs

/** Money text shared by the dashboard revenue figures. */
object CurrencyFormatter {

    private const val MILLION = 1_000_000.0
    private const val THOUSAND = 1_000.0

    /** 1_250_000 -> "$1.3M", 108_000 -> "$108K", 950 -> "$950"; negatives keep their sign in front. */
    fun compact(amount: Double, symbol: String): String {
        val sign = if (amount < 0) "-" else ""
        val magnitude = abs(amount)
        val figure = when {
            magnitude >= MILLION -> String.format(Locale.US, "%.1fM", magnitude / MILLION)
            magnitude >= THOUSAND -> String.format(Locale.US, "%.0fK", magnitude / THOUSAND)
            else -> String.format(Locale.US, "%.0f", magnitude)
        }
        return "$sign$symbol$figure"
    }
}
