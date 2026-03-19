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

        // ── Root scroll container ──────────────────────────────────
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 16.dp(), 20.dp(), 32.dp())
        }

        // ── Title ──────────────────────────────────────────────────
        root.addView(TextView(ctx).apply {
            text = "Color Theme"
            textSize = 16f
            setTextColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorOnSurface))
            setPadding(4.dp(), 0, 0, 12.dp())
            typeface = Typeface.DEFAULT_BOLD
        })

        // ── Theme options ──────────────────────────────────────────
        val currentTheme = ThemeManager.getCurrentTheme(ctx)

        ThemeManager.WfmsTheme.entries.forEach { theme ->
            val isActive = theme == currentTheme

            val row = LinearLayout(ctx).apply {
                orientation  = LinearLayout.HORIZONTAL
                setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
                background   = if (isActive) {
                    GradientDrawable().apply {
                        setColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorSurfaceVariant))
                        cornerRadius = 12.dp().toFloat()
                    }
                } else null
                isClickable  = true
                isFocusable  = true
            }

            // Color swatch (two halves: primary + bg)
            val swatch = LinearLayout(ctx).apply {
                val size = 32.dp()
                layoutParams = LinearLayout.LayoutParams(size, size)
                background = GradientDrawable().apply {
                    cornerRadius = 8.dp().toFloat()
                    setColor(Color.parseColor(theme.bgHex))
                }
                clipToOutline = true
            }
            val swatchLeft = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(theme.primaryHex))
                    // Only round the left corners
                    cornerRadii = floatArrayOf(8.dp().toFloat(), 8.dp().toFloat(), 0f, 0f, 0f, 0f, 8.dp().toFloat(), 8.dp().toFloat())
                }
            }
            val swatchRight = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(theme.bgHex))
                    cornerRadii = floatArrayOf(0f, 0f, 8.dp().toFloat(), 8.dp().toFloat(), 8.dp().toFloat(), 8.dp().toFloat(), 0f, 0f)
                }
            }
            swatch.addView(swatchLeft)
            swatch.addView(swatchRight)

            // Theme name label
            val label = TextView(ctx).apply {
                text = theme.displayName
                textSize = 14f
                setTextColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorOnSurface))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 12.dp()
                    gravity = Gravity.CENTER_VERTICAL
                }
            }

            // Active checkmark
            val check = TextView(ctx).apply {
                text = "✓"
                textSize = 16f
                setTextColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorPrimary))
                isVisible = isActive
            }

            row.addView(swatch)
            row.addView(label)
            row.addView(check)

            row.setOnClickListener {
                ThemeManager.setTheme(ctx, theme)
                dismiss()
            }

            root.addView(row)
            root.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1
                ).apply { setMargins(0, 2.dp(), 0, 2.dp()) }
                setBackgroundColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorOutline))
                alpha = 0.3f
            })
        }

        // ── Divider ────────────────────────────────────────────────
        root.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, 8.dp(), 0, 8.dp()) }
            setBackgroundColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorOutline))
        })

        // ── Dark mode section title ────────────────────────────────
        root.addView(TextView(ctx).apply {
            text = "Appearance"
            textSize = 11f
            setTextColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorOnSurfaceVariant))
            setPadding(4.dp(), 8.dp(), 0, 4.dp())
            isAllCaps = true
            letterSpacing = 0.08f
        })

        // ── Dark mode rows ─────────────────────────────────────────
        val currentDark = ThemeManager.getDarkMode(ctx)

        listOf(
            Triple(ThemeManager.DarkMode.LIGHT,  "Light",        "☀️"),
            Triple(ThemeManager.DarkMode.DARK,   "Dark",         "🌙"),
            Triple(ThemeManager.DarkMode.SYSTEM, "System default","⚙️")
        ).forEach { (mode, label, emoji) ->

            val isActive = mode == currentDark
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
                background = if (isActive) {
                    GradientDrawable().apply {
                        setColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorSurfaceVariant))
                        cornerRadius = 12.dp().toFloat()
                    }
                } else null
                isClickable = true
                isFocusable = true
            }

            row.addView(TextView(ctx).apply {
                text = emoji
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(32.dp(), 32.dp()).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            })
            row.addView(TextView(ctx).apply {
                text = label
                textSize = 14f
                setTextColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorOnSurface))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 12.dp()
                    gravity = Gravity.CENTER_VERTICAL
                }
            })
            row.addView(TextView(ctx).apply {
                text = "✓"
                textSize = 16f
                setTextColor(ThemeManager.resolveColor(ctx, R.attr.wfmsColorPrimary))
                isVisible = isActive
            })

            row.setOnClickListener {
                ThemeManager.setDarkMode(ctx, mode)
                dismiss()
                // Recreate parent activity to apply the night mode change
                activity?.recreate()
            }

            root.addView(row)
        }

        return root
    }

    companion object {
        const val TAG = "ThemePickerBottomSheet"
    }
}
