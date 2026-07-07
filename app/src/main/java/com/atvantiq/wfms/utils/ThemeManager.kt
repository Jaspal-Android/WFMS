package com.atvantiq.wfms.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.Window
import android.view.WindowManager
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors


object ThemeManager {
    enum class WfmsTheme(
        val key: String,
        val displayName: String,
        val primaryHex: String,   // for swatch preview in UI
        val bgHex: String,
        @StyleRes val styleRes: Int
    ) {
        FOREST(
            key         = "forest",
            displayName = "Forest Green",
            primaryHex  = "#2D4A35",
            bgHex       = "#EDE9E2",
            styleRes    = R.style.ThemeOverlay_WFMS_Forest
        ),
        OCEAN(
            key         = "ocean",
            displayName = "Ocean Blue",
            primaryHex  = "#1E4A72",
            bgHex       = "#E8EFF5",
            styleRes    = R.style.ThemeOverlay_WFMS_Ocean
        ),
        ROSE(
            key         = "rose",
            displayName = "Dusty Rose",
            primaryHex  = "#8B3A52",
            bgHex       = "#F0E8E6",
            styleRes    = R.style.ThemeOverlay_WFMS_Rose
        ),
        INDIGO(
            key         = "indigo",
            displayName = "Slate Indigo",
            primaryHex  = "#3B3F8C",
            bgHex       = "#EAEBF2",
            styleRes    = R.style.ThemeOverlay_WFMS_Indigo
        ),
        AMBER(
            key         = "amber",
            displayName = "Warm Amber",
            primaryHex  = "#7A4F1A",
            bgHex       = "#F0EAE0",
            styleRes    = R.style.ThemeOverlay_WFMS_Amber
        ),
        SLATE(
            key         = "slate",
            displayName = "Cool Slate",
            primaryHex  = "#2C4A5A",
            bgHex       = "#E6EBF0",
            styleRes    = R.style.ThemeOverlay_WFMS_Slate
        );

        companion object {
            fun fromKey(key: String?): WfmsTheme =
                entries.firstOrNull { it.key == key } ?: FOREST
        }
    }

    enum class DarkMode(val key: String) {
        LIGHT("light"),
        DARK("dark"),
        SYSTEM("system");  // follows system setting

        companion object {
            fun fromKey(key: String?): DarkMode =
                entries.firstOrNull { it.key == key } ?: SYSTEM
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PrefKeys.WFMS_PREF, Context.MODE_PRIVATE)

    fun getCurrentTheme(context: Context): WfmsTheme =
        WfmsTheme.fromKey(prefs(context).getString(PrefKeys.KEY_THEME, WfmsTheme.FOREST.key))

    fun getDarkMode(context: Context): DarkMode =
        DarkMode.fromKey(prefs(context).getString(PrefKeys.KEY_DARK, DarkMode.SYSTEM.key))

    fun isDarkMode(context: Context): Boolean =
        getDarkMode(context) == DarkMode.DARK

    fun applyTheme(context: Context) {
        val theme = getCurrentTheme(context)
        context.setTheme(theme.styleRes)
        applyDarkMode(context)
    }

    fun applyStoredDarkMode(context: Context) {
        applyDarkMode(context)
    }

    fun setTheme(context: Context, theme: WfmsTheme) {
        prefs(context).edit()
            .putString(PrefKeys.KEY_THEME, theme.key)
            .apply()
        // Recreate to apply new theme
        (context as? AppCompatActivity)?.recreate()
    }

    fun setDarkMode(context: Context, mode: DarkMode) {
        prefs(context).edit()
            .putString(PrefKeys.KEY_DARK, mode.key)
            .apply()
        applyDarkMode(context)
    }

    fun toggleDarkMode(context: Context) {
        val current = getDarkMode(context)
        val next = when (current) {
            DarkMode.LIGHT  -> DarkMode.DARK
            DarkMode.DARK   -> DarkMode.LIGHT
            DarkMode.SYSTEM -> DarkMode.DARK
        }
        setDarkMode(context, next)
    }

    private fun applyDarkMode(context: Context) {
        val nightMode = when (getDarkMode(context)) {
            DarkMode.LIGHT  -> AppCompatDelegate.MODE_NIGHT_NO
            DarkMode.DARK   -> AppCompatDelegate.MODE_NIGHT_YES
            DarkMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun resolveColor(context: Context, attrRes: Int): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(attrRes))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

    fun apply(vararg cards: MaterialCardView) {
        cards.forEach { card ->
            card.post {
                val color = MaterialColors.getColor(
                    card,
                    R.attr.wfmsColorSurface
                )
                card.setCardBackgroundColor(color)
            }
        }
    }
}
