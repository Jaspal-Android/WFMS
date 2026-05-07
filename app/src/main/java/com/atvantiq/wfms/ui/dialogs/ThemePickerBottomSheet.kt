package com.atvantiq.wfms.ui.dialogs

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.atvantiq.wfms.R
import com.atvantiq.wfms.utils.ThemeManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ThemePickerBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp = ctx.resources.displayMetrics.density

        fun Int.dp() = (this * dp).toInt()

        // Centralize resolved theme colors in one place.
        val cOnSurface = ThemeManager.resolveColor(ctx, R.attr.wfmsColorOnSurface)
        val cOnSurfaceVariant = ThemeManager.resolveColor(ctx, R.attr.wfmsColorOnSurfaceVariant)
        val cPrimary = ThemeManager.resolveColor(ctx, R.attr.wfmsColorPrimary)
        val cOutline = ThemeManager.resolveColor(ctx, R.attr.wfmsColorOutline)
        val cSurfaceVariant = ThemeManager.resolveColor(ctx, R.attr.wfmsColorSurfaceVariant)

        fun buildRoundedBackground(color: Int): GradientDrawable =
            GradientDrawable().apply {
                setColor(color)
                cornerRadius = 12.dp().toFloat()
            }

        fun buildDivider(marginTopDp: Int, marginBottomDp: Int, alpha: Float = 1f): View =
            View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
                ).apply { setMargins(0, marginTopDp.dp(), 0, marginBottomDp.dp()) }
                setBackgroundColor(cOutline)
                this.alpha = alpha
            }

        fun title(text: String): TextView = TextView(ctx).apply {
            this.text = text
            textSize = 16f
            setTextColor(cOnSurface)
            setPadding(4.dp(), 0, 0, 12.dp())
            typeface = Typeface.DEFAULT_BOLD
        }

        fun sectionTitle(text: String): TextView = TextView(ctx).apply {
            this.text = text
            textSize = 11f
            setTextColor(cOnSurfaceVariant)
            setPadding(4.dp(), 8.dp(), 0, 4.dp())
            isAllCaps = true
            letterSpacing = 0.08f
        }

        fun checkmark(isActive: Boolean): TextView = TextView(ctx).apply {
            text = "✓"
            textSize = 16f
            setTextColor(cPrimary)
            isVisible = isActive
        }

        fun baseRow(isActive: Boolean): LinearLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
            background = if (isActive) buildRoundedBackground(cSurfaceVariant) else null
            isClickable = true
            isFocusable = true
        }

        fun buildThemeSwatch(theme: ThemeManager.WfmsTheme): View {
            val size = 32.dp()
            val radius = 8.dp().toFloat()

            val container = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(size, size)
                background = GradientDrawable().apply {
                    cornerRadius = radius
                    setColor(Color.parseColor(theme.bgHex))
                }
                clipToOutline = true
            }

            val left = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(theme.primaryHex))
                    // Round only the left corners
                    cornerRadii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                }
            }

            val right = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(theme.bgHex))
                    // Round only the right corners
                    cornerRadii = floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                }
            }

            container.addView(left)
            container.addView(right)
            return container
        }

        fun labelText(text: String): TextView = TextView(ctx).apply {
            this.text = text
            textSize = 14f
            setTextColor(cOnSurface)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12.dp()
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        fun emojiIcon(emoji: String): TextView = TextView(ctx).apply {
            text = emoji
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(32.dp(), 32.dp()).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        // ── Root container ─────────────────────────────────────────
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 16.dp(), 20.dp(), 32.dp())
        }

        // ── Theme section ──────────────────────────────────────────
        root.addView(title("Color Theme"))

        val currentTheme = ThemeManager.getCurrentTheme(ctx)
        ThemeManager.WfmsTheme.entries.forEachIndexed { index, theme ->
            val isActive = theme == currentTheme

            val row = baseRow(isActive).apply {
                addView(buildThemeSwatch(theme))
                addView(labelText(theme.displayName))
                addView(checkmark(isActive))

                setOnClickListener {
                    ThemeManager.setTheme(ctx, theme)
                    dismiss()
                }
            }

            root.addView(row)

            // Inter-item divider (skip after last item)
            if (index != ThemeManager.WfmsTheme.entries.lastIndex) {
                root.addView(buildDivider(marginTopDp = 2, marginBottomDp = 2, alpha = 0.3f))
            }
        }

        // ── Divider between sections ───────────────────────────────
        root.addView(buildDivider(marginTopDp = 8, marginBottomDp = 8, alpha = 1f))

        // ── Appearance section ─────────────────────────────────────
        root.addView(sectionTitle("Appearance"))

        val currentDark = ThemeManager.getDarkMode(ctx)
        val modes = listOf(
            Triple(ThemeManager.DarkMode.LIGHT, "Light", "☀️"),
            Triple(ThemeManager.DarkMode.DARK, "Dark", "🌙"),
            Triple(ThemeManager.DarkMode.SYSTEM, "System default", "⚙️")
        )

        modes.forEach { (mode, label, emoji) ->
            val isActive = mode == currentDark

            val row = baseRow(isActive).apply {
                // Slightly tighter padding for the appearance rows.
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())

                addView(emojiIcon(emoji))
                addView(labelText(label))
                addView(checkmark(isActive))

                setOnClickListener {
                    ThemeManager.setDarkMode(ctx, mode)
                    dismiss()
                    // Recreate parent activity to apply the night mode change
                    activity?.recreate()
                }
            }

            root.addView(row)
        }

        return root
    }

    companion object {
        const val TAG = "ThemePickerBottomSheet"
    }
}
